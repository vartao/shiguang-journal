// 通用工具函数
const WEEKDAYS = ['日', '一', '二', '三', '四', '五', '六']

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

/** 解析 'yyyy-MM-dd' 或 'yyyy-MM-dd HH:mm:ss' 为 Date（避免 iOS new Date 兼容问题） */
function parseDate(str) {
  if (!str) return null
  const m = String(str).match(/(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/)
  if (!m) return null
  return new Date(+m[1], +m[2] - 1, +m[3], +(m[4] || 0), +(m[5] || 0), +(m[6] || 0))
}

/** 2026-08-09 */
function formatDate(str) {
  const d = parseDate(str) || (str instanceof Date ? str : null)
  if (!d || isNaN(d)) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 2026年8月9日 */
function formatDateCN(str) {
  const d = parseDate(str) || (str instanceof Date ? str : null)
  if (!d || isNaN(d)) return ''
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

/** 2026年8月9日 · 星期六 */
function formatDateline(str) {
  const d = parseDate(str)
  if (!d || isNaN(d)) return ''
  return `${formatDateCN(str)} · 星期${WEEKDAYS[d.getDay()]}`
}

/** 08-09 14:30 / 昨天 14:30 / 2025-08-09 */
function formatMessageTime(str) {
  const d = parseDate(str)
  if (!d || isNaN(d)) return ''
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const yesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1)
  if (sameDay) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (d.toDateString() === yesterday.toDateString()) return `昨天 ${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (d.getFullYear() === now.getFullYear()) return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  return formatDate(str)
}

/** 月份推季节 */
function seasonOf(dateStr) {
  const d = parseDate(dateStr)
  if (!d || isNaN(d)) return ''
  const m = d.getMonth() + 1
  if (m >= 3 && m <= 5) return '春'
  if (m >= 6 && m <= 8) return '夏'
  if (m >= 9 && m <= 11) return '秋'
  return '冬'
}

/** 两个日期相差天数（b - a） */
function daysBetween(a, b) {
  const da = parseDate(a) || a
  const db = parseDate(b) || b
  if (!da || !db) return 0
  return Math.round((db.setHours(0, 0, 0, 0) - new Date(da).setHours(0, 0, 0, 0)) / 86400000)
}

const MOODS = {
  happy: { emoji: '😊', label: '开心' },
  love: { emoji: '🥰', label: '甜蜜' },
  excited: { emoji: '🤩', label: '兴奋' },
  calm: { emoji: '😌', label: '平静' },
  miss: { emoji: '🥺', label: '想念' },
  funny: { emoji: '🤣', label: '搞笑' },
  sad: { emoji: '😢', label: '难过' },
  angry: { emoji: '😤', label: '生气' }
}

const WEATHERS = {
  sunny: { emoji: '☀️', label: '晴天' },
  cloudy: { emoji: '⛅', label: '多云' },
  rainy: { emoji: '🌧️', label: '下雨' },
  snowy: { emoji: '❄️', label: '下雪' },
  windy: { emoji: '🌬️', label: '刮风' }
}

const LOG_CATEGORIES = {
  movie: { emoji: '🎬', label: '电影' },
  food: { emoji: '🍴', label: '美食' },
  show: { emoji: '🎭', label: '演出' },
  travel: { emoji: '✈️', label: '旅行' },
  date: { emoji: '💑', label: '约会' },
  sport: { emoji: '🏸', label: '运动' },
  other: { emoji: '✨', label: '其他' }
}

const DISH_CATEGORIES = {
  breakfast: { emoji: '🍳', label: '早餐' },
  lunch: { emoji: '🍚', label: '午餐' },
  dinner: { emoji: '🍲', label: '晚餐' },
  night: { emoji: '🌙', label: '宵夜' },
  snack: { emoji: '🍰', label: '甜点' },
  drink: { emoji: '🧋', label: '饮品' }
}

function mapOf(map, key) {
  return map[key] || { emoji: '✨', label: key || '其他' }
}

module.exports = {
  parseDate,
  formatDate,
  formatDateCN,
  formatDateline,
  formatMessageTime,
  seasonOf,
  daysBetween,
  MOODS,
  WEATHERS,
  LOG_CATEGORIES,
  DISH_CATEGORIES,
  mapOf
}
