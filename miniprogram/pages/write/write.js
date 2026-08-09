// 写笔记 / 编辑笔记
const api = require('../../utils/api')
const util = require('../../utils/util')

function mapList(map) {
  return Object.keys(map).map((key) => ({
    key,
    emoji: map[key].emoji,
    label: map[key].label
  }))
}

Page({
  data: {
    id: null,
    title: '',
    content: '',
    mood: '',
    weather: '',
    moods: [],
    weathers: [],
    noteDate: '',
    today: '',
    images: [],
    maxImages: 9,
    loadingDetail: false,
    submitting: false
  },

  onLoad(options) {
    const today = util.formatDate(new Date())
    this.setData({
      moods: mapList(util.MOODS),
      weathers: mapList(util.WEATHERS),
      noteDate: today,
      today
    })
    if (options && options.id) {
      this.setData({ id: Number(options.id), loadingDetail: true })
      wx.setNavigationBarTitle({ title: '编辑这页回忆' })
      this.loadDetail(Number(options.id))
    }
  },

  async loadDetail(id) {
    try {
      const note = await api.request({ path: `/notes/${id}` })
      const images = (note.images || []).map(api.fullUrl).filter(Boolean)
      if (!images.length && note.coverImage) images.push(api.fullUrl(note.coverImage))
      this.setData({
        title: note.title || '',
        content: note.content || '',
        mood: note.mood || '',
        weather: note.weather || '',
        noteDate: util.formatDate(note.noteDate || note.createdAt) || this.data.noteDate,
        images,
        loadingDetail: false
      })
    } catch (_e) {
      this.setData({ loadingDetail: false })
      wx.navigateBack()
    }
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value })
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value })
  },

  onMoodTap(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ mood: this.data.mood === key ? '' : key })
  },

  onWeatherTap(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ weather: this.data.weather === key ? '' : key })
  },

  onDateChange(e) {
    this.setData({ noteDate: e.detail.value })
  },

  async onAddImage() {
    const room = this.data.maxImages - this.data.images.length
    if (room <= 0) {
      api.toast('最多贴 9 张照片')
      return
    }
    wx.showLoading({ title: '上传中…', mask: true })
    try {
      const urls = await api.chooseAndUpload(room)
      const added = urls.map(api.fullUrl)
      this.setData({ images: this.data.images.concat(added) })
    } catch (_e) {
      // 取消选择或上传失败（上传失败已提示）
    } finally {
      wx.hideLoading()
    }
  },

  onRemoveImage(e) {
    const { index } = e.currentTarget.dataset
    const images = this.data.images.slice()
    images.splice(index, 1)
    this.setData({ images })
  },

  onPreviewImage(e) {
    const { index } = e.currentTarget.dataset
    wx.previewImage({ current: this.data.images[index], urls: this.data.images })
  },

  async onSubmit() {
    const { id, title, content, mood, weather, noteDate, images, submitting } = this.data
    if (submitting) return
    if (!title.trim()) {
      api.toast('给这页起个标题吧')
      return
    }
    if (!content.trim()) {
      api.toast('内容还是空白的哦')
      return
    }
    this.setData({ submitting: true })
    // 后端期望相对路径，把拼出的完整地址还原为 /uploads/xxx
    const rel = images.map((u) => {
      const i = u.indexOf('/uploads/')
      return i >= 0 ? u.slice(i) : u
    })
    const data = {
      title: title.trim(),
      content: content.trim(),
      mood: mood || null,
      weather: weather || null,
      noteDate,
      images: rel,
      coverImage: rel[0] || null
    }
    try {
      if (id) {
        await api.request({ path: `/notes/${id}`, method: 'PUT', data, loadingText: '更新中…' })
        api.toast('已更新这页回忆')
      } else {
        await api.request({ path: '/notes', method: 'POST', data, loadingText: '封存中…' })
        api.toast('已封存进胶卷')
      }
      setTimeout(() => {
        wx.switchTab({ url: '/pages/timeline/timeline' })
      }, 600)
    } catch (_e) {
      // 错误已提示
    } finally {
      this.setData({ submitting: false })
    }
  }
})
