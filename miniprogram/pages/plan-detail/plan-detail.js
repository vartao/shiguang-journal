// 行程详情：头部信息 + 每日安排编辑器
const api = require('../../utils/api')
const { formatDate } = require('../../utils/util')
const { toast } = api

const STATUS_META = [
  { label: '计划中', cls: 'st--olive' },
  { label: '已出发', cls: 'st--mustard' },
  { label: '已完成', cls: 'st--blue' }
]

function parseDays(raw) {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return []
    return arr.map((d) => ({
      date: formatDate(d.date) || '',
      items: Array.isArray(d.items) ? d.items.map(String) : []
    }))
  } catch (_e) {
    return []
  }
}

function nextDate(dateStr) {
  const m = String(dateStr || '').match(/(\d{4})-(\d{2})-(\d{2})/)
  const base = m ? new Date(+m[1], +m[2] - 1, +m[3]) : new Date()
  base.setDate(base.getDate() + 1)
  const pad = (n) => (n < 10 ? '0' + n : '' + n)
  return `${base.getFullYear()}-${pad(base.getMonth() + 1)}-${pad(base.getDate())}`
}

Page({
  data: {
    plan: null,
    days: [],
    dayInputs: [],
    savingDays: false,
    dirty: false,
    showEditor: false,
    saving: false,
    statusMeta: STATUS_META,
    form: { title: '', destination: '', startDate: '', endDate: '', budget: '', note: '', cover: '', status: 0 }
  },

  onLoad(options) {
    if (!getApp().isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this._id = options.id
    this.load()
  },

  async load() {
    try {
      const p = await api.request({ path: `/plans/${this._id}` })
      const s = STATUS_META[p.status] || STATUS_META[0]
      const days = parseDays(p.days)
      this.setData({
        plan: {
          ...p,
          coverUrl: api.fullUrl(p.cover),
          startText: formatDate(p.startDate),
          endText: formatDate(p.endDate),
          statusLabel: s.label,
          statusCls: s.cls
        },
        days,
        dayInputs: days.map(() => ''),
        dirty: false
      })
    } catch (_e) {
      /* 错误已 toast */
    }
  },

  /* ============ 头部 ============ */

  /** 状态 chip 点击循环切换并即时保存 */
  async cycleStatus() {
    const { plan } = this.data
    if (!plan) return
    const status = ((plan.status != null ? plan.status : 0) + 1) % 3
    try {
      await api.request({ path: `/plans/${plan.id}`, method: 'PUT', data: { title: plan.title, status } })
      const s = STATUS_META[status]
      this.setData({ 'plan.status': status, 'plan.statusLabel': s.label, 'plan.statusCls': s.cls })
      toast(`已标记为${s.label}`)
    } catch (_e) {
      /* 错误已 toast */
    }
  },

  deletePlan() {
    const { plan } = this.data
    if (!plan) return
    wx.showModal({
      title: '删除行程',
      content: `确定删除「${plan.title}」吗？`,
      confirmColor: '#a34f54',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/plans/${plan.id}`, method: 'DELETE' })
          toast('已删除')
          wx.navigateBack()
        } catch (_e) {
          /* 错误已 toast */
        }
      }
    })
  },

  /* ============ 每日安排编辑器 ============ */

  onItemInput(e) {
    const idx = Number(e.currentTarget.dataset.index)
    this.setData({ [`dayInputs[${idx}]`]: e.detail.value })
  },

  addItem(e) {
    const idx = Number(e.currentTarget.dataset.index)
    const text = (this.data.dayInputs[idx] || '').trim()
    if (!text) return toast('先写点安排吧')
    const days = this.data.days.slice()
    days[idx] = { ...days[idx], items: days[idx].items.concat(text) }
    this.setData({ days, [`dayInputs[${idx}]`]: '', dirty: true })
  },

  delItem(e) {
    const { day, item } = e.currentTarget.dataset
    const days = this.data.days.slice()
    const items = days[day].items.slice()
    items.splice(item, 1)
    days[day] = { ...days[day], items }
    this.setData({ days, dirty: true })
  },

  onDayDateChange(e) {
    const idx = Number(e.currentTarget.dataset.index)
    this.setData({ [`days[${idx}].date`]: e.detail.value, dirty: true })
  },

  addDay() {
    const { days, plan } = this.data
    const last = days[days.length - 1]
    const date = last && last.date ? nextDate(last.date) : formatDate(plan && plan.startDate) || formatDate(new Date())
    this.setData({ days: days.concat({ date, items: [] }), dayInputs: this.data.dayInputs.concat(''), dirty: true })
  },

  delDay(e) {
    const idx = Number(e.currentTarget.dataset.index)
    const days = this.data.days.slice()
    const dayInputs = this.data.dayInputs.slice()
    days.splice(idx, 1)
    dayInputs.splice(idx, 1)
    this.setData({ days, dayInputs, dirty: true })
  },

  async saveDays() {
    const { plan, days } = this.data
    if (!plan) return
    this.setData({ savingDays: true })
    try {
      await api.request({
        path: `/plans/${plan.id}`,
        method: 'PUT',
        data: { title: plan.title, days: JSON.stringify(days) }
      })
      this.setData({ savingDays: false, dirty: false })
      toast('行程已保存')
    } catch (_e) {
      this.setData({ savingDays: false })
    }
  },

  /* ============ 编辑行程信息 ============ */

  openEdit() {
    const { plan } = this.data
    if (!plan) return
    this.setData({
      showEditor: true,
      form: {
        title: plan.title || '',
        destination: plan.destination || '',
        startDate: formatDate(plan.startDate),
        endDate: formatDate(plan.endDate),
        budget: plan.budget != null ? String(plan.budget) : '',
        note: plan.note || '',
        cover: plan.cover || '',
        coverUrl: plan.coverUrl,
        status: plan.status != null ? plan.status : 0
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
    const { form, plan } = this.data
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
      await api.request({ path: `/plans/${plan.id}`, method: 'PUT', data })
      this.setData({ showEditor: false, saving: false })
      toast('已更新')
      this.load()
    } catch (_e) {
      this.setData({ saving: false })
    }
  },

  noop() {}
})
