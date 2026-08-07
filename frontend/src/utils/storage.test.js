import { beforeEach, describe, expect, it } from 'vitest'
import { readStoredJson, writeStoredJson } from './storage'

describe('storage utilities', () => {
  beforeEach(() => localStorage.clear())

  it('round-trips JSON values', () => {
    writeStoredJson('profile', { id: 7 })
    expect(readStoredJson('profile')).toEqual({ id: 7 })
  })

  it('recovers from corrupt JSON and removes the bad cache', () => {
    localStorage.setItem('profile', '{broken')
    expect(readStoredJson('profile', null)).toBeNull()
    expect(localStorage.getItem('profile')).toBeNull()
  })
})
