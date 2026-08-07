import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

const { push } = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('@/router', () => ({ default: { push } }))
vi.mock('@/api/auth', () => ({ login: vi.fn(), register: vi.fn(), getProfile: vi.fn() }))

import { normalizeUser, useUserStore } from './user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    push.mockClear()
    setActivePinia(createPinia())
  })

  it('normalizes login envelopes and persists token/user state', () => {
    expect(normalizeUser({ token: 'x', user: { userId: 7, nickname: '小青' } })).toMatchObject({
      id: 7,
      userId: 7,
      nickname: '小青'
    })
    const store = useUserStore()
    store.setToken('token-1')
    store.setUserInfo({ id: 7, nickname: '小青' })
    expect(localStorage.getItem('ql_token')).toBe('token-1')
    expect(JSON.parse(localStorage.getItem('ql_user'))).toEqual({ id: 7, nickname: '小青' })
  })

  it('clears all session state on logout', () => {
    localStorage.setItem('ql_couple', '{}')
    const store = useUserStore()
    store.setToken('token-1')
    store.setUserInfo({ id: 7 })
    store.logout()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('ql_token')).toBeNull()
    expect(localStorage.getItem('ql_user')).toBeNull()
    expect(localStorage.getItem('ql_couple')).toBeNull()
    expect(push).toHaveBeenCalledWith({ name: 'Login' })
  })

  it('recovers when cached user JSON is corrupt', () => {
    localStorage.setItem('ql_user', '{broken')
    setActivePinia(createPinia())

    const store = useUserStore()

    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('ql_user')).toBeNull()
  })
})
