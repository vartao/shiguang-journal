const DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const DAY_IN_MS = 24 * 60 * 60 * 1000

/**
 * Date-only strings are interpreted in local time instead of UTC.
 */
export function parseLocalDate(value) {
  if (value instanceof Date) return new Date(value.getTime())

  if (typeof value === 'string') {
    const match = value.trim().match(DATE_ONLY_PATTERN)
    if (match) {
      const year = Number(match[1])
      const monthIndex = Number(match[2]) - 1
      const day = Number(match[3])
      const parsed = new Date(year, monthIndex, day)
      if (parsed.getFullYear() === year && parsed.getMonth() === monthIndex && parsed.getDate() === day) {
        return parsed
      }
      return new Date(Number.NaN)
    }
  }

  return new Date(value)
}

export function toLocalDateInputValue(value = new Date()) {
  const date = parseLocalDate(value)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * Calendar-day difference (left - right), unaffected by daylight-saving hour changes.
 */
export function differenceInCalendarDays(left, right) {
  const leftDate = parseLocalDate(left)
  const rightDate = parseLocalDate(right)
  if (Number.isNaN(leftDate.getTime()) || Number.isNaN(rightDate.getTime())) return Number.NaN

  const leftOrdinal = Date.UTC(leftDate.getFullYear(), leftDate.getMonth(), leftDate.getDate())
  const rightOrdinal = Date.UTC(rightDate.getFullYear(), rightDate.getMonth(), rightDate.getDate())
  return Math.round((leftOrdinal - rightOrdinal) / DAY_IN_MS)
}

export function getNextYearlyOccurrence(value, today = new Date()) {
  const original = parseLocalDate(value)
  const current = parseLocalDate(today)
  if (Number.isNaN(original.getTime()) || Number.isNaN(current.getTime())) return new Date(Number.NaN)

  let occurrence = occurrenceInYear(original, current.getFullYear())
  if (differenceInCalendarDays(occurrence, current) < 0) {
    occurrence = occurrenceInYear(original, current.getFullYear() + 1)
  }
  return occurrence
}

function occurrenceInYear(original, year) {
  const month = original.getMonth()
  const lastDay = new Date(year, month + 1, 0).getDate()
  return new Date(year, month, Math.min(original.getDate(), lastDay))
}
