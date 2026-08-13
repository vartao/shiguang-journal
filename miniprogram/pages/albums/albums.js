// 相册墙
const api = require('../../utils/api')

const EMPTY_FORM = { name: '', description: '', coverImage: '' }

Page({
  data: {
    loading: true,
    albums: [],
    showCreate: false,
    form: { ...EMPTY_FORM },
    coverFull: '',
    submitting: false
  },

  async onShow() {
    if (!(await getApp().guardPage('albums'))) return
    this.load()
  },

  onPullDownRefresh() {
    this.load(true)
  },

  async load(fromPull) {
    this.setData({ loading: !fromPull })
    try {
      const list = await api.request({ path: '/albums', loading: false })
      const albums = (list || []).map((a) => ({
        ...a,
        coverFull: api.fullUrl(a.coverImage || a.cover)
      }))
      this.setData({ albums })
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/album-detail/album-detail?id=${id}` })
  },

  openCreate() {
    this.setData({ showCreate: true, form: { ...EMPTY_FORM }, coverFull: '' })
  },

  closeCreate() {
    if (this.data.submitting) return
    this.setData({ showCreate: false })
  },

  onNameInput(e) {
    this.setData({ 'form.name': e.detail.value })
  },

  onDescInput(e) {
    this.setData({ 'form.description': e.detail.value })
  },

  async chooseCover() {
    try {
      const urls = await api.chooseAndUpload(1)
      this.setData({ 'form.coverImage': urls[0], coverFull: api.fullUrl(urls[0]) })
    } catch (_e) {
      // 用户取消或上传失败
    }
  },

  async submit() {
    const { form, submitting } = this.data
    if (submitting) return
    const name = (form.name || '').trim()
    if (!name) {
      api.toast('先给相册起个名字吧')
      return
    }
    this.setData({ submitting: true })
    try {
      await api.request({
        path: '/albums',
        method: 'POST',
        data: {
          name,
          description: (form.description || '').trim(),
          coverImage: form.coverImage || ''
        },
        loadingText: '创建中…'
      })
      api.toast('相册创建好啦')
      this.setData({ showCreate: false })
      this.load()
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ submitting: false })
    }
  }
})
