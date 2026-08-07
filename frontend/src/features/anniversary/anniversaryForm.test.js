import { describe, expect, it } from 'vitest'
import { createAnniversaryForm, toAnniversaryPayload, toAnniversaryUiType } from './anniversaryForm'

describe('anniversary form helpers', () => {
  it('maps recurring and one-off dates into UI types', () => {
    expect(toAnniversaryUiType({ repeatYearly: 1, date: '2020-01-01' }, '2026-07-28')).toBe('recurring')
    expect(toAnniversaryUiType({ repeatYearly: 0, date: '2027-01-01' }, '2026-07-28')).toBe('future')
    expect(toAnniversaryUiType({ repeatYearly: 0, date: '2025-01-01' }, '2026-07-28')).toBe('past')
  })

  it('hydrates an edit form from compatible backend fields', () => {
    expect(
      createAnniversaryForm(
        { name: 'First date', date: '2024-02-03', icon: '❤', remark: 'Cafe', repeatYearly: 1 },
        '2026-07-28'
      )
    ).toEqual({
      title: 'First date',
      date: '2024-02-03',
      type: 'recurring',
      icon: '❤',
      remark: 'Cafe'
    })
  })

  it('normalizes the API payload and trims text', () => {
    expect(
      toAnniversaryPayload({
        title: '  First date  ',
        date: '2024-02-03',
        type: 'future',
        icon: '❤',
        remark: '  Cafe  '
      })
    ).toEqual({
      title: 'First date',
      name: 'First date',
      date: '2024-02-03',
      type: 'custom',
      repeatYearly: 0,
      icon: '❤',
      remark: 'Cafe'
    })
  })
})
