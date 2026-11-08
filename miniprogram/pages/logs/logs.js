// 小事记：一起做过的事打卡（分类 + 0-5 星评分）
const api = require('../../utils/api')
const util = require('../../utils/util')

const CATEGORIES = util.LOG_CATEGORIES
const CHIPS = [{ key: '', emoji: '📖', label: '全部' }].concat(
  Object.keys(CATEGORIES).map((key) => ({ key, emoji: CATEGORIES[key].emoji, label: CATEGORIES[key].label }))
)

function todayStr() {
  const d = new Date()
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

Page({
  data: {
    loading: true,
    visible: [], // 过滤后展示
    chips: CHIPS,
    activeKey: '',
    yearOptions: ['全部年份'],
    yearIndex: 0,
    showForm: false,
    editing: false,
    form: { id: null, title: '', category: 'other', logDate: '', note: '', rating: 0, cover: '' },
    coverFull: '',
    saving: false
  },

  async onShow() {
    if (!(await getApp().guardPage('logs'))) return
    this.load()
  },

  onPullDownRefresh() {
    this.load(true)
  },

  decorate(l) {
    const meta = CATEGORIES[l.category] || CATEGORIES.other
    return {
      ...l,
      dateCN: util.formatDateCN(l.logDate),
      categoryEmoji: meta.emoji,
      categoryLabel: meta.label,
      rating: Number(l.rating) || 0,
      coverFull: l.cover ? api.fullUrl(l.cover) : ''
    }
  },

  applyFilter() {
    const { logs, activeKey, activeYear } = this.data
    const visible = (logs || []).filter((l) => {
      if (activeKey && l.category !== activeKey) return false
      if (activeYear && String(l.logDate || '').slice(0, 4) !== activeYear) return false
      return true
    })
    this.setData({ visible })
  },

  async load(fromPull) {
    this.setData({ loading: !fromPull })
    try {
      const list = await api.request({ path: '/logs', loading: false })
      const logs = (list || []).map((l) => this.decorate(l))
      const years = [...new Set(logs.map((l) => String(l.logDate || '').slice(0, 4)).filter(Boolean))].sort((a, b) =>
        b.localeCompare(a)
      )
      const yearIndex = this.data.yearIndex > 0 && this.data.yearIndex <= years.length ? this.data.yearIndex : 0
      this.setData({
        logs,
        yearOptions: ['全部年份'].concat(years),
        yearIndex,
        activeYear: yearIndex > 0 ? years[yearIndex - 1] : ''
      })
      this.applyFilter()
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  /* 筛选 */
  tapChip(e) {
    this.setData({ activeKey: e.currentTarget.dataset.key || '' })
    this.applyFilter()
  },

  onYearChange(e) {
    const index = Number(e.detail.value) || 0
    const year = index > 0 ? String(this.data.yearOptions[index] || '') : ''
    this.setData({ yearIndex: index, activeYear: year })
    this.applyFilter()
  },

  /* 记录表单 */
  openCreate() {
    this.setData({
      showForm: true,
      editing: false,
      form: { id: null, title: '', category: 'other', logDate: todayStr(), note: '', rating: 0, cover: '' },
      coverFull: ''
    })
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id
    const log = this.data.logs.find((l) => String(l.id) === String(id))
    if (!log) return
    this.setData({
      showForm: true,
      editing: true,
      form: {
        id: log.id,
        title: log.title || '',
        category: CATEGORIES[log.category] ? log.category : 'other',
        logDate: log.logDate || todayStr(),
        note: log.note || '',
        rating: Number(log.rating) || 0,
        cover: log.cover || ''
      },
      coverFull: log.cover ? api.fullUrl(log.cover) : ''
    })
  },

  closeForm() {
    if (this.data.saving) return
    this.setData({ showForm: false })
  },

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onNoteInput(e) {
    this.setData({ 'form.note': e.detail.value })
  },

  onDateChange(e) {
    this.setData({ 'form.logDate': e.detail.value })
  },

  tapCategory(e) {
    this.setData({ 'form.category': e.currentTarget.dataset.key || 'other' })
  },

  tapRating(e) {
    const value = Number(e.currentTarget.dataset.value) || 0
    // 再点一次同星级清除评分
    this.setData({ 'form.rating': this.data.form.rating === value ? 0 : value })
  },

  async chooseCover() {
    if (this.data.form.cover) {
      api.toast('已经选了封面，先删掉再换')
      return
    }
    try {
      const urls = await api.chooseAndUpload(1)
      if (!urls || !urls.length) return
      this.setData({ 'form.cover': urls[0], coverFull: api.fullUrl(urls[0]) })
    } catch (_e) {
      // 用户取消或上传失败
    }
  },

  removeCover() {
    this.setData({ 'form.cover': '', coverFull: '' })
  },

  async submit() {
    const { form, saving } = this.data
    if (saving) return
    const title = (form.title || '').trim()
    if (!title) {
      api.toast('写个标题吧')
      return
    }
    if (!form.logDate) {
      api.toast('选一下发生的日期')
      return
    }
    this.setData({ saving: true })
    try {
      const body = {
        title,
        category: CATEGORIES[form.category] ? form.category : 'other',
        logDate: form.logDate,
        note: (form.note || '').trim(),
        rating: form.rating > 0 ? form.rating : null
      }
      if (form.cover) body.cover = form.cover
      await api.request({
        path: form.id ? `/logs/${form.id}` : '/logs',
        method: form.id ? 'PUT' : 'POST',
        data: body,
        loadingText: form.id ? '更新中…' : '记录中…'
      })
      api.toast(form.id ? '已更新 ✨' : '记下啦 📖')
      this.setData({ showForm: false })
      this.load()
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ saving: false })
    }
  },

  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除小事记',
      content: '这条小事就从手账里撕掉了，确定吗？',
      confirmText: '删除',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.request({ path: `/logs/${id}`, method: 'DELETE', loadingText: '删除中…' })
          api.toast('已删除')
          this.load()
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  }
})
