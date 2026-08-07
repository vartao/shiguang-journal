import { parseLocalDate } from '@/utils/date'

// 导出手账的时间范围选项
export const RANGE_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: '30d', label: '近 30 天' },
  { value: '90d', label: '近 90 天' },
  { value: 'year', label: '今年' }
]

/**
 * 笔记的展示日期：优先 noteDate，其次创建时间
 * @param {object} note
 * @returns {string}
 */
export function getNoteDate(note) {
  return note?.noteDate || note?.createdAt || note?.createTime || ''
}

/**
 * 按展示日期升序排列（不改动原数组）
 * @param {object[]} notes
 * @returns {object[]}
 */
export function sortNotesByDateAsc(notes) {
  return [...notes].sort((a, b) => toTime(getNoteDate(a)) - toTime(getNoteDate(b)))
}

/**
 * 时间范围的起始日期（含当天）；'all' 返回 null 表示不过滤
 * @param {string} range
 * @param {Date|string} [today]
 * @returns {Date|null}
 */
export function getRangeStart(range, today = new Date()) {
  const current = parseLocalDate(today)
  if (Number.isNaN(current.getTime())) return null
  if (range === '30d' || range === '90d') {
    const days = range === '30d' ? 30 : 90
    return new Date(current.getFullYear(), current.getMonth(), current.getDate() - days)
  }
  if (range === 'year') {
    return new Date(current.getFullYear(), 0, 1)
  }
  return null
}

/**
 * 按时间范围过滤笔记（'all' 时原样返回）
 * @param {object[]} notes
 * @param {string} range
 * @param {Date|string} [today]
 * @returns {object[]}
 */
export function filterNotesByRange(notes, range, today = new Date()) {
  const start = getRangeStart(range, today)
  if (!start) return notes
  return notes.filter((note) => {
    const time = toTime(getNoteDate(note))
    return !Number.isNaN(time) && time >= start.getTime()
  })
}

/**
 * 时间范围的中文文案
 * @param {string} range
 * @returns {string}
 */
export function getRangeLabel(range) {
  return RANGE_OPTIONS.find((option) => option.value === range)?.label || RANGE_OPTIONS[0].label
}

function toTime(value) {
  if (!value) return Number.NaN
  return parseLocalDate(value).getTime()
}
