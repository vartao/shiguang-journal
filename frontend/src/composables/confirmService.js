import { reactive } from 'vue'

const defaults = {
  open: false,
  title: '请确认',
  message: '',
  confirmText: '确认',
  cancelText: '取消',
  danger: true,
  busy: false
}

export const confirmState = reactive({ ...defaults })
let activeResolver = null

function resetState() {
  Object.assign(confirmState, defaults)
}

function settle(value) {
  const resolver = activeResolver
  activeResolver = null
  resetState()
  resolver?.(value)
}

export function requestConfirm(options = {}) {
  if (activeResolver) settle(false)
  Object.assign(confirmState, defaults, options, { open: true, busy: false })
  return new Promise((resolve) => {
    activeResolver = resolve
  })
}

export function resolveConfirm(value = true) {
  if (confirmState.busy) return
  settle(Boolean(value))
}

export function cancelConfirm() {
  settle(false)
}
