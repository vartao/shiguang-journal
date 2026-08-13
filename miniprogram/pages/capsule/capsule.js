// 时间胶囊
const api = require('../../utils/api')
const util = require('../../utils/util')

function tomorrow() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

Page({
  data: {
    loading: true,
    waiting: [], // 未开启（含 canOpen 高亮）
    opened: [], // 已开启
    minDate: '',
    showCreate: false,
    form: { title: '', message: '', openDate: '', photoUrls: [] },
    photoFulls: [],
    saving: false,
    letter: null // 拆开的信件（含照片完整地址）
  },

  onLoad() {
    const t = tomorrow()
    this.setData({ minDate: t, 'form.openDate': t })
  },

  async onShow() {
    if (!(await getApp().guardPage('capsule'))) return
    this.load()
  },

  onPullDownRefresh() {
    this.load(true)
  },

  currentUserId() {
    const app = getApp()
    const info = app.globalData && app.globalData.userInfo
    return info && info.id != null ? String(info.id) : ''
  },

  decorate(c) {
    const uid = this.currentUserId()
    return {
      ...c,
      openDateCN: util.formatDateCN(c.openDate),
      openedTimeCN: c.openedTime ? util.formatDateCN(c.openedTime) : '',
      canDelete: !!uid && String(c.userId) === uid
    }
  },

  async load(fromPull) {
    this.setData({ loading: !fromPull })
    try {
      const list = await api.request({ path: '/capsules', loading: false })
      const all = (list || []).map((c) => this.decorate(c))
      this.setData({
        waiting: all.filter((c) => Number(c.isOpened) !== 1),
        opened: all.filter((c) => Number(c.isOpened) === 1)
      })
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  /* 拆信 */
  tapCapsule(e) {
    const id = e.currentTarget.dataset.id
    const capsule = this.data.waiting.find((c) => String(c.id) === String(id))
    if (!capsule || !capsule.canOpen) return
    wx.showModal({
      title: '拆开时间胶囊',
      content: `「${capsule.title}」到日子了，要现在拆开吗？`,
      confirmText: '拆开 ✨',
      success: async (res) => {
        if (!res.confirm) return
        try {
          const data = await api.request({
            path: `/capsules/${capsule.id}/open`,
            method: 'POST',
            loadingText: '拆信中…'
          })
          this.setData({ letter: this.toLetter(data) })
          this.load()
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  },

  /* 查看已开启的信 */
  viewOpened(e) {
    const id = e.currentTarget.dataset.id
    const capsule = this.data.opened.find((c) => String(c.id) === String(id))
    if (!capsule) return
    this.setData({ letter: this.toLetter(capsule) })
  },

  toLetter(c) {
    return {
      title: c.title,
      message: c.message || '',
      createTimeCN: util.formatDateCN(c.createTime),
      openDateCN: util.formatDateCN(c.openDate),
      openedTimeCN: c.openedTime ? util.formatDateCN(c.openedTime) : '',
      openerNickname: c.openerNickname || '',
      photos: (c.photoUrls || []).map((u) => api.fullUrl(u))
    }
  },

  closeLetter() {
    this.setData({ letter: null })
  },

  /* 删除（仅自己创建） */
  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除时间胶囊',
      content: '删除后这封信就寄不出去了，确定吗？',
      confirmText: '删除',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.request({ path: `/capsules/${id}`, method: 'DELETE', loadingText: '删除中…' })
          api.toast('已删除')
          this.load()
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  },

  /* 新建 */
  openCreate() {
    this.setData({
      showCreate: true,
      form: { title: '', message: '', openDate: this.data.minDate, photoUrls: [] },
      photoFulls: []
    })
  },

  closeCreate() {
    if (this.data.saving) return
    this.setData({ showCreate: false })
  },

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onMessageInput(e) {
    this.setData({ 'form.message': e.detail.value })
  },

  onDateChange(e) {
    this.setData({ 'form.openDate': e.detail.value })
  },

  async choosePhotos() {
    try {
      const left = 6 - this.data.form.photoUrls.length
      if (left <= 0) {
        api.toast('最多放 6 张照片')
        return
      }
      const urls = await api.chooseAndUpload(left)
      const photoUrls = this.data.form.photoUrls.concat(urls)
      this.setData({ 'form.photoUrls': photoUrls, photoFulls: photoUrls.map((u) => api.fullUrl(u)) })
    } catch (_e) {
      // 用户取消或上传失败
    }
  },

  removePhoto(e) {
    const index = e.currentTarget.dataset.index
    const photoUrls = this.data.form.photoUrls.slice()
    photoUrls.splice(index, 1)
    this.setData({ 'form.photoUrls': photoUrls, photoFulls: photoUrls.map((u) => api.fullUrl(u)) })
  },

  async submit() {
    const { form, saving } = this.data
    if (saving) return
    const title = (form.title || '').trim()
    if (!title) {
      api.toast('给这封信起个标题吧')
      return
    }
    if (!form.openDate) {
      api.toast('选一个开启日期')
      return
    }
    this.setData({ saving: true })
    try {
      await api.request({
        path: '/capsules',
        method: 'POST',
        data: {
          title,
          message: (form.message || '').trim(),
          openDate: form.openDate,
          photoUrls: form.photoUrls
        },
        loadingText: '封存中…'
      })
      api.toast('胶囊已封存 💌')
      this.setData({ showCreate: false })
      this.load()
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ saving: false })
    }
  }
})
