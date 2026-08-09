// 心愿许愿池
const api = require('../../utils/api')

const TABS = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '许愿中' },
  { key: 'done', label: '已达成' }
]

const CATEGORIES = [
  { key: 'travel', label: '旅行', emoji: '✈️' },
  { key: 'food', label: '美食', emoji: '🍴' },
  { key: 'movie', label: '电影', emoji: '🎬' },
  { key: 'gift', label: '礼物', emoji: '🎁' },
  { key: 'other', label: '其他', emoji: '✨' }
]

const PRIORITIES = [
  { value: 0, label: '普通', icon: '○' },
  { value: 1, label: '重要', icon: '★' },
  { value: 2, label: '紧急', icon: '⚡' }
]

function blankForm() {
  return { title: '', description: '', category: 'other', icon: '✨', priority: 0 }
}

Page({
  data: {
    tabs: TABS,
    categories: CATEGORIES,
    priorities: PRIORITIES,
    tab: 'all',
    loading: true,
    wishes: [], // 已装饰：icon 显示、分类文案、优先级文案、canManage
    showEdit: false,
    editingId: 0,
    form: blankForm(),
    saving: false
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

  currentUserId() {
    const app = getApp()
    const info = app.globalData && app.globalData.userInfo
    return info && info.id != null ? String(info.id) : ''
  },

  decorate(list) {
    const uid = this.currentUserId()
    return (list || []).map((w) => {
      const cat = CATEGORIES.find((c) => c.key === w.category) || CATEGORIES[4]
      const pri = PRIORITIES.find((p) => p.value === Number(w.priority)) || PRIORITIES[0]
      return {
        ...w,
        catLabel: cat.label,
        catEmoji: cat.emoji,
        iconShow: w.icon || cat.emoji,
        priLabel: pri.label,
        priIcon: pri.icon,
        urgent: Number(w.priority) === 2,
        done: Number(w.status) === 1,
        canManage: !!uid && String(w.userId) === uid
      }
    })
  },

  async load(fromPull) {
    this.setData({ loading: !fromPull })
    try {
      const list = await api.request({ path: '/wishes', loading: false })
      this.setData({ wishes: this.decorate(list) })
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.key })
  },

  /* 达成 */
  async complete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '心愿达成',
      content: '把这个心愿标记为已达成吗？',
      confirmText: '达成 ✓',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.request({ path: `/wishes/${id}/complete`, method: 'POST', loadingText: '记录中…' })
          api.toast('心愿达成，恭喜！')
          this.load()
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  },

  /* 新建 / 编辑 */
  openCreate() {
    this.setData({ showEdit: true, editingId: 0, form: blankForm() })
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id
    const wish = this.data.wishes.find((w) => String(w.id) === String(id))
    if (!wish) return
    this.setData({
      showEdit: true,
      editingId: wish.id,
      form: {
        title: wish.title || '',
        description: wish.description || '',
        category: wish.category || 'other',
        icon: wish.icon || wish.catEmoji,
        priority: Number(wish.priority) || 0
      }
    })
  },

  closeEdit() {
    if (this.data.saving) return
    this.setData({ showEdit: false, editingId: 0 })
  },

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onDescInput(e) {
    this.setData({ 'form.description': e.detail.value })
  },

  selectCategory(e) {
    const key = e.currentTarget.dataset.key
    const cat = CATEGORIES.find((c) => c.key === key)
    this.setData({ 'form.category': key, 'form.icon': cat ? cat.emoji : '✨' })
  },

  selectPriority(e) {
    this.setData({ 'form.priority': Number(e.currentTarget.dataset.value) })
  },

  async submit() {
    const { form, editingId, saving } = this.data
    if (saving) return
    const title = (form.title || '').trim()
    if (!title) {
      api.toast('心愿标题不能为空')
      return
    }
    this.setData({ saving: true })
    try {
      const data = {
        title,
        description: (form.description || '').trim(),
        icon: form.icon,
        category: form.category,
        priority: form.priority
      }
      if (editingId) {
        await api.request({ path: `/wishes/${editingId}`, method: 'PUT', data, loadingText: '保存中…' })
        api.toast('已保存')
      } else {
        await api.request({ path: '/wishes', method: 'POST', data, loadingText: '许愿中…' })
        api.toast('许愿成功 ✨')
      }
      this.setData({ showEdit: false, editingId: 0 })
      this.load()
    } catch (_e) {
      // 提示已由请求层处理
    } finally {
      this.setData({ saving: false })
    }
  },

  /* 删除（仅创建者） */
  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除心愿',
      content: '删除后找不回来，确定吗？',
      confirmText: '删除',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.request({ path: `/wishes/${id}`, method: 'DELETE', loadingText: '删除中…' })
          api.toast('已删除')
          this.load()
        } catch (_e) {
          // 提示已由请求层处理
        }
      }
    })
  }
})
