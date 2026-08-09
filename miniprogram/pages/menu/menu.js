// 今天吃什么：随机选菜 + 今日菜单 + 菜品池
const api = require('../../utils/api')
const util = require('../../utils/util')

const CAT_KEYS = Object.keys(util.DISH_CATEGORIES)
const DISH_CATS = CAT_KEYS.map((k) => ({ key: k, ...util.DISH_CATEGORIES[k] }))
const RANDOM_CATS = [{ key: '', emoji: '🍽️', label: '全部' }].concat(DISH_CATS)
const MEALS = [
  { key: 'breakfast', emoji: '🍳', label: '早餐' },
  { key: 'lunch', emoji: '🍚', label: '午餐' },
  { key: 'dinner', emoji: '🍲', label: '晚餐' }
]

/** 菜品池按分类分组（未知分类归入“其他”） */
function buildGroups(dishes) {
  const groups = []
  CAT_KEYS.forEach((k) => {
    const list = dishes.filter((d) => d.category === k)
    if (list.length) groups.push({ key: k, ...util.DISH_CATEGORIES[k], dishes: list })
  })
  const others = dishes.filter((d) => CAT_KEYS.indexOf(d.category) < 0)
  if (others.length) groups.push({ key: '_other', emoji: '✨', label: '其他', dishes: others })
  return groups
}

function dateOf(offset) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return util.formatDate(d)
}

Page({
  data: {
    // 随机选菜
    randomCats: RANDOM_CATS,
    rcat: '',
    rolling: false,
    rollingText: '',
    result: null,
    mealPick: 'lunch',
    pickMeals: MEALS,
    // 今日菜单
    dateOffset: 0,
    dateLabel: '今天',
    menuDate: '',
    mealList: [],
    // 菜品池
    dishes: [],
    poolGroups: [],
    // 加一道弹层
    mealSheet: { show: false, type: '', label: '', manual: '', pool: [] },
    // 添道菜弹层
    dishSheet: { show: false, name: '', category: 'lunch', note: '' },
    dishCats: DISH_CATS,
    saving: false
  },

  onShow() {
    if (!getApp().isLoggedIn()) return
    this.refreshAll()
  },

  onPullDownRefresh() {
    this.refreshAll(true)
  },

  onHide() {
    this.stopRolling()
  },

  async refreshAll(fromPull) {
    await Promise.all([this.loadDishes(), this.loadMenu()])
    if (fromPull) wx.stopPullDownRefresh()
  },

  async loadDishes() {
    try {
      const data = await api.request({ path: '/dishes', loading: false })
      const dishes = data || []
      this.setData({ dishes, poolGroups: buildGroups(dishes) })
    } catch (_e) {
      // 错误提示已由 request 统一处理
    }
  },

  async loadMenu() {
    const date = dateOf(this.data.dateOffset)
    try {
      const data = await api.request({ path: `/menu?date=${date}`, loading: false })
      const raw = data || []
      const mealList = MEALS.map((m) => ({ ...m, items: raw.filter((i) => i.mealType === m.key) }))
      this.setData({
        mealList,
        menuDate: date,
        dateLabel: this.data.dateOffset === 0 ? '今天' : '明天'
      })
    } catch (_e) {}
  },

  // ============ 随机选菜 ============
  onRcatTap(e) {
    this.setData({ rcat: e.currentTarget.dataset.key })
  },

  filteredPool() {
    const { dishes, rcat } = this.data
    return rcat ? dishes.filter((d) => d.category === rcat) : dishes
  },

  stopRolling() {
    if (this.rollTimer) {
      clearInterval(this.rollTimer)
      this.rollTimer = null
    }
  },

  async onRandom() {
    if (this.data.rolling) return
    const pool = this.filteredPool()
    this.setData({ rolling: true, result: null })
    // 90ms 轮播池内菜名
    this.rollTimer = setInterval(() => {
      const t = pool.length ? pool[Math.floor(Math.random() * pool.length)].name : '…'
      this.setData({ rollingText: t })
    }, 90)
    // 同时请求后端随机结果
    const cat = this.data.rcat
    let dish = null
    try {
      dish = await api.request({
        path: `/dishes/random${cat ? `?category=${cat}` : ''}`,
        loading: false
      })
    } catch (_e) {
      // 池为空等错误已由 request toast
    }
    // 滚满 1.4s 再定格
    await new Promise((r) => setTimeout(r, 1400))
    this.stopRolling()
    this.setData({ rolling: false, rollingText: '', result: dish })
  },

  onPickMeal(e) {
    this.setData({ mealPick: e.currentTarget.dataset.key })
  },

  async onAddResult() {
    const { result, mealPick } = this.data
    if (!result || this.data.saving) return
    this.setData({ saving: true })
    try {
      await api.request({
        path: '/menu',
        method: 'POST',
        data: { mealDate: dateOf(0), mealType: mealPick, dishName: result.name }
      })
      api.toast('已加入今日菜单')
      this.setData({ result: null })
      if (this.data.dateOffset === 0) this.loadMenu()
    } catch (_e) {
    } finally {
      this.setData({ saving: false })
    }
  },

  // ============ 今日菜单 ============
  onDatePrev() {
    if (this.data.dateOffset === 0) return
    this.setData({ dateOffset: 0 }, () => this.loadMenu())
  },

  onDateNext() {
    if (this.data.dateOffset === 1) return
    this.setData({ dateOffset: 1 }, () => this.loadMenu())
  },

  onDelMeal(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '从菜单里去掉这道？',
      confirmText: '去掉',
      confirmColor: '#c1666b',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/menu/${id}`, method: 'DELETE' })
          api.toast('已去掉')
          this.loadMenu()
        } catch (_e) {}
      }
    })
  },

  // ============ 加一道（弹层：池内选择 / 手填） ============
  onOpenMealSheet(e) {
    const type = e.currentTarget.dataset.type
    const meal = MEALS.find((m) => m.key === type) || MEALS[1]
    const pool = this.data.dishes.map((d) => ({
      ...d,
      catLabel: util.mapOf(util.DISH_CATEGORIES, d.category).label
    }))
    this.setData({
      mealSheet: { show: true, type, label: `${meal.emoji} ${meal.label}`, manual: '', pool }
    })
  },

  onCloseMealSheet() {
    this.setData({ 'mealSheet.show': false })
  },

  onMealManual(e) {
    this.setData({ 'mealSheet.manual': e.detail.value })
  },

  async addMealItem(dishName) {
    const name = (dishName || '').trim()
    if (!name) {
      api.toast('写个菜名吧')
      return
    }
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      await api.request({
        path: '/menu',
        method: 'POST',
        data: { mealDate: this.data.menuDate, mealType: this.data.mealSheet.type, dishName: name }
      })
      this.setData({ 'mealSheet.show': false })
      api.toast('已加入菜单')
      this.loadMenu()
    } catch (_e) {
    } finally {
      this.setData({ saving: false })
    }
  },

  onMealManualAdd() {
    this.addMealItem(this.data.mealSheet.manual)
  },

  onPickDish(e) {
    this.addMealItem(e.currentTarget.dataset.name)
  },

  // ============ 菜品池 ============
  onDelDish(e) {
    const { id, name } = e.currentTarget.dataset
    wx.showModal({
      title: `把「${name}」移出菜品池？`,
      confirmText: '删除',
      confirmColor: '#c1666b',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/dishes/${id}`, method: 'DELETE' })
          api.toast('已删除')
          this.loadDishes()
        } catch (_e) {}
      }
    })
  },

  onOpenDishSheet() {
    this.setData({ dishSheet: { show: true, name: '', category: 'lunch', note: '' } })
  },

  onCloseDishSheet() {
    this.setData({ 'dishSheet.show': false })
  },

  onDishName(e) {
    this.setData({ 'dishSheet.name': e.detail.value })
  },

  onDishCat(e) {
    this.setData({ 'dishSheet.category': e.currentTarget.dataset.key })
  },

  onDishNote(e) {
    this.setData({ 'dishSheet.note': e.detail.value })
  },

  async onSaveDish() {
    const s = this.data.dishSheet
    if (!s.name.trim()) {
      api.toast('菜名不能空着哦')
      return
    }
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      await api.request({
        path: '/dishes',
        method: 'POST',
        data: { name: s.name.trim(), category: s.category, note: s.note.trim() }
      })
      this.setData({ 'dishSheet.show': false })
      api.toast('已加入菜品池')
      this.loadDishes()
    } catch (_e) {
    } finally {
      this.setData({ saving: false })
    }
  }
})
