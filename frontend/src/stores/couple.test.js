import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/auth', () => ({ getCoupleInfo: vi.fn(), bindPartner: vi.fn(), createCouple: vi.fn() }))
import { getCoupleInfo } from '@/api/auth'
import { normalizeCouple, useCoupleStore } from './couple'

describe('couple store', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('normalizes date and partner compatibility fields', () => {
    expect(normalizeCouple({ togetherDate: '2024-01-02', user2Nickname: '小侣' })).toMatchObject({
      startDate: '2024-01-02',
      togetherDate: '2024-01-02',
      partnerName: '小侣'
    })
  })

  it('persists normalized couple information', () => {
    const store = useCoupleStore()
    store.setCoupleInfo({ startDate: '2024-01-02', user1Nickname: '小青' })
    expect(store.coupleInfo.partnerName).toBe('小青')
    expect(JSON.parse(localStorage.getItem('ql_couple')).togetherDate).toBe('2024-01-02')
  })

  it('clears stale cached couple information when the server returns no couple', async () => {
    localStorage.setItem('ql_couple', JSON.stringify({ id: 9, name: '旧小窝' }))
    getCoupleInfo.mockResolvedValue({ data: null })
    setActivePinia(createPinia())
    const store = useCoupleStore()

    await store.fetchCoupleInfo()

    expect(store.coupleInfo).toBeNull()
    expect(localStorage.getItem('ql_couple')).toBeNull()
  })
})
