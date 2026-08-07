import { afterEach } from 'vitest'

const storage = new Map()
const localStorageMock = {
  getItem(key) {
    return storage.has(String(key)) ? storage.get(String(key)) : null
  },
  setItem(key, value) {
    storage.set(String(key), String(value))
  },
  removeItem(key) {
    storage.delete(String(key))
  },
  clear() {
    storage.clear()
  },
  key(index) {
    return Array.from(storage.keys())[index] ?? null
  },
  get length() {
    return storage.size
  }
}
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, configurable: true })
Object.defineProperty(window, 'localStorage', { value: localStorageMock, configurable: true })

if (!window.matchMedia) window.matchMedia = () => ({ matches: false, addEventListener() {}, removeEventListener() {} })

if (!globalThis.requestAnimationFrame) {
  Object.defineProperty(globalThis, 'requestAnimationFrame', {
    value: (callback) => setTimeout(callback, 0),
    configurable: true
  })
}

afterEach(() => {
  document.body.innerHTML = ''
  localStorage.clear()
})
