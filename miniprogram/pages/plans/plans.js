// 行程计划列表
const api = require('../../utils/api')
const { formatDate } = require('../../utils/util')
const { toast } = api

const STATUS_META = [
  { label: '计划中', cls: 'st--olive' },
  { label: '已出发', cls: 'st--mustard' },
  { label: '已完成', cls: 'st--blue' }
]

function decorate(p) {
  const s = STATUS_META[p.status] || STATUS_META[0]
  const start = formatDate(p.startDate)
  const end = formatDate(p.endDate)
  return {
    ...p,
    coverUrl: api.fullUrl(p.cover),
    dateRange: start && end ? `${start} ~ ${end}` : start || end || '日期待定',
    statusLabel: s.label,
    statusCls: s.cls
  }
}

Page({
  data: {
    loading: true,
    list: [],
    showEditor: false,
    editingId: null,
    saving: false,
    statusMeta: STATUS_META,
    form: { title: '', destination: '', startDate: '', endDate: '', budget: '', note: '', cover: '', status: 0 }
  },

  onLoad() {
    if (!getApp().isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },

  onShow() {
    if (this._needRefresh) {
      this._needRefresh = false
      this.load()
    }
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    try {
      const list = await api.request({ path: '/plans' })
      this.setData({ loading: false, list: (list || []).map(decorate) })
    } catch (_e) {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/plan-detail/plan-detail?id=${e.currentTarget.dataset.id}` })
    this._needRefresh = true
  },

  /* ============ 新建 / 编辑 ============ */

  openCreate() {
    this.setData({
      showEditor: true,
      editingId: null,
      form: { title: '', destination: '', startDate: '', endDate: '', budget: '', note: '', cover: '', status: 0 }
    })
  },

  openEdit(e) {
    const p = this.data.list.find((x) => x.id === e.currentTarget.dataset.id)
    if (!p) return
    this.setData({
      showEditor: true,
      editingId: p.id,
      form: {
        title: p.title || '',
        destination: p.destination || '',
        startDate: formatDate(p.startDate),
        endDate: formatDate(p.endDate),
        budget: p.budget != null ? String(p.budget) : '',
        note: p.note || '',
        cover: p.cover || '',
        coverUrl: p.coverUrl,
        status: p.status != null ? p.status : 0
      }
    })
  },

  closeEditor() {
    this.setData({ showEditor: false })
  },

  onFormInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.key}`]: e.detail.value })
  },

  onDateChange(e) {
    this.setData({ [`form.${e.currentTarget.dataset.key}`]: e.detail.value })
  },

  onStatusPick(e) {
    this.setData({ 'form.status': Number(e.currentTarget.dataset.status) })
  },

  async chooseCover() {
    try {
      const urls = await api.chooseAndUpload(1)
      if (urls[0]) this.setData({ 'form.cover': urls[0], 'form.coverUrl': api.fullUrl(urls[0]) })
    } catch (_e) {
      /* 用户取消 */
    }
  },

  async submitForm() {
    const { form, editingId } = this.data
    if (!form.title.trim()) return toast('行程标题不能为空')
    if (form.startDate && form.endDate && form.endDate < form.startDate) return toast('结束日期不能早于开始日期')
    const data = {
      title: form.title.trim(),
      destination: form.destination.trim() || null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      budget: form.budget !== '' ? Number(form.budget) : null,
      note: form.note.trim() || null,
      cover: form.cover || null,
      status: form.status
    }
    if (data.budget != null && (isNaN(data.budget) || data.budget < 0)) return toast('预算格式不正确')
    this.setData({ saving: true })
    try {
      if (editingId) {
        await api.request({ path: `/plans/${editingId}`, method: 'PUT', data })
        toast('已更新')
      } else {
        await api.request({ path: '/plans', method: 'POST', data })
        toast('行程已创建')
      }
      this.setData({ showEditor: false, saving: false })
      this.load()
    } catch (_e) {
      this.setData({ saving: false })
    }
  },

  deletePlan(e) {
    const id = e.currentTarget.dataset.id
    const p = this.data.list.find((x) => x.id === id)
    wx.showModal({
      title: '删除行程',
      content: `确定删除「${p ? p.title : ''}」吗？`,
      confirmColor: '#a34f54',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/plans/${id}`, method: 'DELETE' })
          toast('已删除')
          this.load()
        } catch (_err) {
          /* 错误已 toast */
        }
      }
    })
  },

  noop() {}
})
