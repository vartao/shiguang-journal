import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useThemeStore } from './theme'

describe('theme store', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    setActivePinia(createPinia())
  })

  it('uses the saved theme first', () => {
    localStorage.setItem('ql_theme', 'dark')
    setActivePinia(createPinia())
    const store = useThemeStore()
    store.init()
    expect(store.isDark).toBe(true)
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('falls back to system preference', () => {
    window.matchMedia = vi.fn(() => ({ matches: true }))
    const store = useThemeStore()
    store.init()
    expect(store.isDark).toBe(true)
    expect(document.documentElement.dataset.theme).toBe('dark')
  })
})
