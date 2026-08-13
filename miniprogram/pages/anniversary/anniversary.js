// 纪念日：倒计时 + 新建 / 编辑 / 删除
const api = require('../../utils/api')
const util = require('../../utils/util')

const ICONS = ['💕', '💑', '🎂', '💍', '🌹', '🏠', '✈️', '✨']
const REPEAT_OPTIONS = [
  { value: 1, label: '每年纪念', desc: '每年都会重新倒计时' },
  { value: 0, label: '只纪念一次', desc: '适合某个特别约定' }
]

function blankForm(today) {
  return { title: '', date: today, repeatYearly: 1, icon: '💕', remark: '' }
}

function countdownText(item, today) {
  const recurring = Number(item.repeatYearly) === 1
  const rawDays = Number(item.daysUntilNext)
  const dateDiff = util.daysBetween(today, item.date)
  const days = Number.isFinite(rawDays) ? rawDays : Math.max(0, dateDiff)

  if (recurring) {
    if (days === 0) return { value: '今天', unit: '就是纪念日', tone: 'today', sort: 0 }
    return { value: String(days), unit: '天后', tone: days <= 7 ? 'soon' : '', sort: days }
  }
  if (dateDiff === 0) return { value: '今天', unit: '就是约定日', tone: 'today', sort: 0 }
  if (dateDiff > 0) return { value: String(dateDiff), unit: '天后', tone: dateDiff <= 7 ? 'soon' : '', sort: dateDiff }
  return { value: String(Math.abs(dateDiff)), unit: '天前', tone: 'past', sort: 100000 + Math.abs(dateDiff) }
}

Page({
  data: {
    loading: true,
    items: [],
    icons: ICONS,
    repeatOptions: REPEAT_OPTIONS,
    today: '',
    showEdit: false,
    editingId: 0,
    form: blankForm(''),
    saving: false,
    deletingId: 0,
    canSave: false
  },

  onLoad() {
    const today = util.formatDate(new Date())
    this.setData({ today, form: blankForm(today) })
  },

  async onShow() {
    if (!(await getApp().guardPage('anniversary'))) return
    this.load()
  },

  onPullDownRefresh() {
    this.load(true)
  },

  decorate(item) {
    const count = countdownText(item, this.data.today)
    const recurring = Number(item.repeatYearly) === 1
    return {
      ...item,
      title: item.title || item.name || '未命名纪念日',
      iconShow: item.icon || '💕',
      dateCN: util.formatDateCN(item.date),
      recurring,
      repeatText: recurring ? '每年纪念' : '一次纪念',
      yearsText: recurring && Number(item.years) > 0 ? `相伴 ${Number(item.years)} 年` : '',
      countValue: count.value,
      countUnit: count.unit,
      countTone: count.tone,
      sortValue: count.sort
    }
  },

  async load(fromPull) {
    this.setData({ loading: !fromPull })
    try {
      const list = await api.request({ path: '/anniversaries', loading: false })
      const items = (list || []).map((item) => this.decorate(item)).sort((a, b) => a.sortValue - b.sortValue)
      this.setData({ items })
    } catch (_e) {
      // 请求层已统一提示
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  openCreate() {
    this.setData({ showEdit: true, editingId: 0, form: blankForm(this.data.today), canSave: false })
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.items.find((it) => String(it.id) === String(id))
    if (!item) return
    this.setData({
      showEdit: true,
      editingId: item.id,
      form: {
        title: item.title || '',
        date: item.date || this.data.today,
        repeatYearly: Number(item.repeatYearly) === 1 ? 1 : 0,
        icon: item.iconShow || '💕',
        remark: item.remark || ''
      },
      canSave: !!(item.title && item.date)
    })
  },

  closeEdit() {
    if (this.data.saving) return
    this.setData({ showEdit: false, editingId: 0, canSave: false })
  },

  onTitleInput(e) {
    const title = e.detail.value
    this.setData({ 'form.title': title, canSave: !!(title.trim() && this.data.form.date) })
  },

  onDateChange(e) {
    const date = e.detail.value
    this.setData({ 'form.date': date, canSave: !!((this.data.form.title || '').trim() && date) })
  },

  onRemarkInput(e) {
    this.setData({ 'form.remark': e.detail.value })
  },

  selectIcon(e) {
    this.setData({ 'form.icon': e.currentTarget.dataset.icon })
  },

  selectRepeat(e) {
    this.setData({ 'form.repeatYearly': Number(e.currentTarget.dataset.value) })
  },

  async submit() {
    const { form, editingId, saving } = this.data
    if (saving) return
    const title = (form.title || '').trim()
    if (!title) {
      api.toast('给这个日子起个名字吧')
      return
    }
    if (!form.date) {
      api.toast('请选择纪念日期')
      return
    }

    const data = {
      name: title,
      title,
      date: form.date,
      type: 'custom',
      icon: form.icon || '💕',
      repeatYearly: Number(form.repeatYearly),
      remark: (form.remark || '').trim()
    }

    this.setData({ saving: true })
    try {
      if (editingId) {
        await api.request({ path: `/anniversaries/${editingId}`, method: 'PUT', data, loadingText: '保存中…' })
        api.toast('纪念日已更新')
      } else {
        await api.request({ path: '/anniversaries', method: 'POST', data, loadingText: '收藏中…' })
        api.toast('这个日子记住啦')
      }
      this.setData({ showEdit: false, editingId: 0, canSave: false })
      this.load()
    } catch (_e) {
      // 请求层已统一提示
    } finally {
      this.setData({ saving: false })
    }
  },

  remove(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.items.find((it) => String(it.id) === String(id))
    if (!item) return
    wx.showModal({
      title: '删除纪念日',
      content: `确定不再记录“${item.title}”吗？`,
      confirmText: '删除',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm) return
        try {
          if (this.data.deletingId) return
          this.setData({ deletingId: Number(id) })
          try {
            await api.request({ path: `/anniversaries/${id}`, method: 'DELETE', loadingText: '删除中…' })
            api.toast('已删除')
            await this.load()
          } finally {
            this.setData({ deletingId: 0 })
          }
        } catch (_e) {
          // 请求层已统一提示
        }
      }
    })
  }
})
