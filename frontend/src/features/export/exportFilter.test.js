import { describe, expect, it } from 'vitest'
import { filterNotesByRange, getNoteDate, getRangeLabel, sortNotesByDateAsc } from './exportFilter'

const today = new Date(2026, 7, 7) // 2026-08-07

describe('export filter helpers', () => {
  it('sorts notes by note date ascending without mutating the source', () => {
    const notes = [
      { id: 1, noteDate: '2026-08-01' },
      { id: 2, createdAt: '2026-01-05T10:00:00' },
      { id: 3, noteDate: '2026-05-20' }
    ]
    expect(sortNotesByDateAsc(notes).map((n) => n.id)).toEqual([2, 3, 1])
    expect(notes.map((n) => n.id)).toEqual([1, 2, 3])
  })

  it('keeps every note for the all range', () => {
    const notes = [{ noteDate: '2020-01-01' }, { noteDate: '2026-08-07' }]
    expect(filterNotesByRange(notes, 'all', today)).toHaveLength(2)
  })

  it('filters notes outside the recent day ranges', () => {
    const notes = [
      { id: 1, noteDate: '2026-08-01' }, // 近 30 天内
      { id: 2, noteDate: '2026-06-01' }, // 近 90 天内、30 天外
      { id: 3, noteDate: '2026-01-01' } // 90 天外
    ]
    expect(filterNotesByRange(notes, '30d', today).map((n) => n.id)).toEqual([1])
    expect(filterNotesByRange(notes, '90d', today).map((n) => n.id)).toEqual([1, 2])
  })

  it('keeps only notes from the current year for the year range', () => {
    const notes = [
      { id: 1, noteDate: '2026-01-01' },
      { id: 2, noteDate: '2025-12-31' }
    ]
    expect(filterNotesByRange(notes, 'year', today).map((n) => n.id)).toEqual([1])
  })

  it('drops notes without a parsable date when a range is applied', () => {
    expect(filterNotesByRange([{ id: 1 }], '30d', today)).toEqual([])
  })

  it('resolves range labels and the note display date', () => {
    expect(getRangeLabel('90d')).toBe('近 90 天')
    expect(getRangeLabel('unknown')).toBe('全部')
    expect(getNoteDate({ noteDate: '2026-08-01' })).toBe('2026-08-01')
    expect(getNoteDate({ createdAt: '2026-01-05T10:00:00' })).toBe('2026-01-05T10:00:00')
    expect(getNoteDate({})).toBe('')
  })
})
