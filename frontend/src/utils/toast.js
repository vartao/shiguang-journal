export function showToast(message, { duration = 2500 } = {}) {
  if (!message || typeof document === 'undefined') return

  const existing = document.querySelector('.app-toast')
  if (existing) existing.remove()

  const toast = document.createElement('div')
  toast.className = 'app-toast'
  toast.textContent = message
  document.body.appendChild(toast)

  requestAnimationFrame(() => toast.classList.add('show'))
  setTimeout(() => {
    toast.classList.remove('show')
    setTimeout(() => toast.remove(), 300)
  }, duration)
}
