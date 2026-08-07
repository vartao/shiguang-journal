import { describe, expect, it } from 'vitest'
import { createWishForm, toWishPayload } from './wishForm'

describe('wish form helpers', () => {
  it('creates a blank form for a new wish', () => {
    expect(createWishForm()).toEqual({ title: '', description: '', icon: '✨', category: 'other', priority: 0 })
  })

  it('hydrates an edit form without mutating status fields', () => {
    const source = { title: 'Trip', description: 'Sea', icon: '✈', category: 'travel', priority: 2, status: 1 }
    expect(createWishForm(source)).toEqual({
      title: 'Trip',
      description: 'Sea',
      icon: '✈',
      category: 'travel',
      priority: 2
    })
    expect(source.status).toBe(1)
  })

  it('only sends editable fields and trims user text', () => {
    expect(
      toWishPayload({ title: '  Trip ', description: ' Sea ', icon: '✈', category: 'travel', priority: 2, status: 1 })
    ).toEqual({
      title: 'Trip',
      description: 'Sea',
      icon: '✈',
      category: 'travel',
      priority: 2
    })
  })
})
