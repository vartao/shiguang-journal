import { beforeEach, describe, expect, it } from 'vitest'
import { cancelConfirm, confirmState, requestConfirm, resolveConfirm } from './confirmService'

describe('confirmService', () => {
  beforeEach(() => cancelConfirm())

  it('resolves true when the active confirmation is accepted', async () => {
    const result = requestConfirm({ message: 'Delete item?' })
    expect(confirmState.open).toBe(true)
    expect(confirmState.message).toBe('Delete item?')

    resolveConfirm(true)

    await expect(result).resolves.toBe(true)
    expect(confirmState.open).toBe(false)
  })

  it('resolves false when cancelled', async () => {
    const result = requestConfirm({ message: 'Cancel me' })
    cancelConfirm()
    await expect(result).resolves.toBe(false)
  })

  it('cancels an older request before opening a newer one', async () => {
    const first = requestConfirm({ message: 'First' })
    const second = requestConfirm({ message: 'Second' })

    await expect(first).resolves.toBe(false)
    expect(confirmState.message).toBe('Second')
    resolveConfirm(true)
    await expect(second).resolves.toBe(true)
  })
})
