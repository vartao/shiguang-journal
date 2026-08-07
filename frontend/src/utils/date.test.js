import { describe, expect, it } from 'vitest'
import { differenceInCalendarDays, getNextYearlyOccurrence, parseLocalDate, toLocalDateInputValue } from './date'

describe('local date utilities', () => {
  it('keeps date-only values on the same local calendar day', () => {
    const parsed = parseLocalDate('2026-08-04')
    expect([parsed.getFullYear(), parsed.getMonth() + 1, parsed.getDate()]).toEqual([2026, 8, 4])
  })

  it('formats local dates without converting through UTC', () => {
    expect(toLocalDateInputValue(new Date(2026, 0, 2, 1, 30))).toBe('2026-01-02')
    expect(toLocalDateInputValue('invalid')).toBe('')
  })

  it('calculates calendar-day differences', () => {
    expect(differenceInCalendarDays('2026-08-05', '2026-08-04')).toBe(1)
    expect(differenceInCalendarDays('2026-08-04', '2026-08-05')).toBe(-1)
  })

  it('keeps today as the next occurrence and handles leap-day anniversaries', () => {
    expect(toLocalDateInputValue(getNextYearlyOccurrence('2020-05-20', new Date(2026, 4, 20, 22)))).toBe('2026-05-20')
    expect(toLocalDateInputValue(getNextYearlyOccurrence('2024-02-29', new Date(2025, 1, 28)))).toBe('2025-02-28')
  })
})
