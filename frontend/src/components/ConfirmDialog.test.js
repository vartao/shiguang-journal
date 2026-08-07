import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'
import ConfirmDialog from './ConfirmDialog.vue'
import { cancelConfirm, requestConfirm } from '@/composables/confirmService'

describe('ConfirmDialog', () => {
  beforeEach(() => cancelConfirm())

  it('confirms through the primary button', async () => {
    const wrapper = mount(ConfirmDialog, { attachTo: document.body })
    const result = requestConfirm({ title: 'Danger', message: 'Delete item?' })
    await wrapper.vm.$nextTick()

    expect(wrapper.get('[role="dialog"]').text()).toContain('Delete item?')
    await wrapper.get('[data-testid="confirm-action"]').trigger('click')
    await expect(result).resolves.toBe(true)
  })

  it('cancels through the cancel button', async () => {
    const wrapper = mount(ConfirmDialog)
    const result = requestConfirm({ message: 'Keep item?' })
    await wrapper.vm.$nextTick()
    await wrapper.get('[data-testid="cancel-action"]').trigger('click')
    await expect(result).resolves.toBe(false)
  })
})
