import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BaseModal from './BaseModal.vue'
import ErrorState from './ErrorState.vue'

describe('BaseModal', () => {
  it('requests close when the overlay is clicked', async () => {
    const wrapper = mount(BaseModal, {
      props: { modelValue: true, title: 'Editor' },
      slots: { default: '<p>content</p>' }
    })
    await wrapper.get('[data-testid="modal-overlay"]').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('requests close on Escape', async () => {
    const wrapper = mount(BaseModal, { props: { modelValue: true } })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:modelValue')).toEqual([[false]])
  })

  it('does not close while busy', async () => {
    const wrapper = mount(BaseModal, { props: { modelValue: true, busy: true } })
    await wrapper.get('[data-testid="modal-overlay"]').trigger('click')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:modelValue')).toBeUndefined()
  })
})

describe('ErrorState', () => {
  it('emits retry from its action', async () => {
    const wrapper = mount(ErrorState, { props: { message: 'Load failed' } })
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('retry')).toHaveLength(1)
  })
})
