import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ChinaMap from './ChinaMap.vue'
import chinaMapData from './chinaMapData'

describe('ChinaMap', () => {
  it('shows the province name and travel summary on hover', async () => {
    const province = chinaMapData[0]
    const wrapper = mount(ChinaMap, {
      props: {
        summary: {
          [province.name]: { visited: 2, planned: 1, cities: ['武汉', '宜昌'] }
        }
      }
    })

    wrapper
      .get('.china-map__province')
      .element.dispatchEvent(new MouseEvent('pointerenter', { clientX: 120, clientY: 80 }))
    await wrapper.vm.$nextTick()

    const tooltip = wrapper.get('[role="tooltip"]')
    expect(tooltip.isVisible()).toBe(true)
    expect(tooltip.text()).toContain(province.name)
    expect(tooltip.text()).toContain('去过 2 次')
    expect(tooltip.text()).toContain('想去 1 处')
    expect(tooltip.text()).toContain('城市：武汉 · 宜昌')
  })
})
