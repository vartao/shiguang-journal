// 恋爱数据报告
const api = require('../../utils/api')
const { MOODS, daysBetween, formatDate } = require('../../utils/util')

const OTD_TYPES = {
  note: { emoji: '📝', label: '笔记' },
  travel: { emoji: '✈️', label: '足迹' },
  log: { emoji: '✨', label: '小事记' }
}

Page({
  data: {
    loading: true,
    togetherDays: null,
    cells: [],
    activeDays30: 0,
    activePct: 0,
    longestStreak: 0,
    streakPct: 0,
    moods: [],
    onThisDay: []
  },

  onLoad() {
    if (!getApp().isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    const [stats, couple, mapSummary, onThisDay] = await Promise.all([
      api.request({ path: '/stats', loading: false }).catch(() => null),
      api.request({ path: '/couple/info', loading: false }).catch(() => null),
      api.request({ path: '/travels/map', loading: false }).catch(() => []),
      api.request({ path: '/stats/on-this-day', loading: false }).catch(() => [])
    ])

    const s = stats || {}
    const provinceCount = (mapSummary || []).filter((m) => m.visited > 0).length

    // 在一起天数：优先后端 togetherDays，否则按 startDate 自算
    let togetherDays = couple && couple.togetherDays != null ? couple.togetherDays : null
    if (togetherDays == null && couple && (couple.togetherDate || couple.startDate)) {
      togetherDays = daysBetween(couple.togetherDate || couple.startDate, new Date()) + 1
    }

    const cells = [
      { num: s.noteCount || 0, label: '篇笔记' },
      { num: s.photoCount || 0, label: '张照片' },
      { num: s.albumCount || 0, label: '本相册' },
      { num: s.totalLikes || 0, label: '次点赞' },
      { num: s.anniversaryCount || 0, label: '个纪念日' },
      { num: s.surpriseCount || 0, label: '个惊喜' },
      { num: provinceCount, label: '点亮省份' }
    ]

    // 心情分布（按次数倒序）
    const dist = s.moodDistribution || {}
    const moodArr = Object.keys(dist).map((k) => {
      const meta = MOODS[k] || { emoji: '✨', label: k }
      return { key: k, emoji: meta.emoji, label: meta.label, count: dist[k] }
    })
    moodArr.sort((a, b) => b.count - a.count)
    const maxCount = moodArr.length ? moodArr[0].count : 1
    moodArr.forEach((m) => {
      m.pct = Math.max(4, Math.round((m.count / maxCount) * 100))
    })

    const activeDays30 = s.activeDays30 || 0
    const longestStreak = s.longestStreak || 0

    const otd = (onThisDay || []).map((it) => {
      const meta = OTD_TYPES[it.type] || OTD_TYPES.note
      return { ...it, emoji: meta.emoji, typeLabel: meta.label, dateText: formatDate(it.date) }
    })

    this.setData({
      loading: false,
      togetherDays,
      cells,
      activeDays30,
      activePct: Math.min(100, Math.round((activeDays30 / 30) * 100)),
      longestStreak,
      streakPct: Math.min(100, Math.round((longestStreak / 30) * 100)),
      moods: moodArr,
      onThisDay: otd
    })
  }
})
