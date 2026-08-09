// 相册详情：照片管理
const api = require('../../utils/api')

Page({
  data: {
    id: 0,
    loading: true,
    album: null,
    photos: [], // 每项含 full（完整地址）
    showEdit: false,
    form: { name: '', description: '' },
    saving: false,
    uploading: false
  },

  onLoad(options) {
    this.setData({ id: Number(options.id) || 0 })
  },

  onShow() {
    if (!getApp().isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },

  onPullDownRefresh() {
    this.load(true)
  },

  async load(fromPull) {
    if (!this.data.id) return
    this.setData({ loading: !fromPull })
    try {
      const data = await api.request({ path: `/albums/${this.data.id}`, loading: false })
      const photos = (data.photos || []).map((p) => ({ ...p, full: api.fullUrl(p.photoUrl) }))
      this.setData({
        album: { id: data.id, name: data.name, description: data.description },
        photos
      })
      wx.setNavigationBarTitle({ title: data.name || '相册' })
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  goBack() {
    wx.navigateBack()
  },

  /* 预览 */
  preview(e) {
    const index = e.currentTarget.dataset.index
    const urls = this.data.photos.map((p) => p.full)
    wx.previewImage({ current: urls[index], urls })
  },

  /* 长按删除照片 */
  removePhoto(e) {
    const { id, index } = e.currentTarget.dataset
    wx.showModal({
      title: '删除照片',
      content: '这张照片删除后找不回来，确定吗？',
      confirmText: '删除',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.request({ path: `/albums/${this.data.id}/photos/${id}`, method: 'DELETE', loadingText: '删除中…' })
          const photos = this.data.photos.slice()
          photos.splice(index, 1)
          this.setData({ photos })
          api.toast('已删除')
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  },

  /* 批量上传 */
  async uploadPhotos() {
    if (this.data.uploading) return
    let urls
    try {
      urls = await api.chooseAndUpload(20)
    } catch (_e) {
      return // 取消或上传失败
    }
    if (!urls.length) return
    this.setData({ uploading: true })
    wx.showLoading({ title: '收录照片中…', mask: true })
    let ok = 0
    for (const url of urls) {
      try {
        await api.request({
          path: `/albums/${this.data.id}/photos`,
          method: 'POST',
          data: { url },
          loading: false
        })
        ok += 1
      } catch (_e) {
        // 单张失败继续下一张
      }
    }
    wx.hideLoading()
    this.setData({ uploading: false })
    api.toast(ok === urls.length ? `已收录 ${ok} 张照片` : `成功 ${ok} 张，失败 ${urls.length - ok} 张`)
    if (ok) this.load()
  },

  /* 编辑相册信息 */
  openEdit() {
    const { album } = this.data
    if (!album) return
    this.setData({
      showEdit: true,
      form: { name: album.name || '', description: album.description || '' }
    })
  },

  closeEdit() {
    if (this.data.saving) return
    this.setData({ showEdit: false })
  },

  onNameInput(e) {
    this.setData({ 'form.name': e.detail.value })
  },

  onDescInput(e) {
    this.setData({ 'form.description': e.detail.value })
  },

  async saveEdit() {
    const { form, saving } = this.data
    if (saving) return
    const name = (form.name || '').trim()
    if (!name) {
      api.toast('相册名称不能为空')
      return
    }
    this.setData({ saving: true })
    try {
      await api.request({
        path: `/albums/${this.data.id}`,
        method: 'PUT',
        data: { name, description: (form.description || '').trim() },
        loadingText: '保存中…'
      })
      api.toast('已保存')
      this.setData({ showEdit: false })
      this.load()
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ saving: false })
    }
  },

  /* 删除相册 */
  removeAlbum() {
    wx.showModal({
      title: '删除相册',
      content: '相册和里面的照片都会被删除，确定吗？',
      confirmText: '删除',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.request({ path: `/albums/${this.data.id}`, method: 'DELETE', loadingText: '删除中…' })
          api.toast('相册已删除')
          setTimeout(() => wx.navigateBack(), 600)
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  }
})
