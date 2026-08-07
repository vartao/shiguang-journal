import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SurpriseDetail from './SurpriseDetail.vue'

const { getSurpriseDetail } = vi.hoisted(() => ({ getSurpriseDetail: vi.fn() }))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '7' } }),
  useRouter: () => ({ push: vi.fn() })
}))
vi.mock('@/api/surprise', () => ({
  getSurpriseDetail,
  unlockSurprise: vi.fn(),
  deleteSurprise: vi.fn()
}))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ userInfo: null }) }))
vi.mock('@/composables/confirmService', () => ({ requestConfirm: vi.fn() }))

describe('SurpriseDetail', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders stored content as text instead of executable HTML', async () => {
    const malicious = '<img src=x onerror=alert(1)>\n第二行'
    getSurpriseDetail.mockResolvedValue({
      data: { id: 7, title: '惊喜', content: malicious, unlocked: 1, unlockCondition: 'click' }
    })

    const wrapper = mount(SurpriseDetail, {
      global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } }
    })
    await flushPromises()

    const content = wrapper.get('.letter-text')
    expect(content.text()).toBe(malicious)
    expect(content.find('img').exists()).toBe(false)
  })
})
