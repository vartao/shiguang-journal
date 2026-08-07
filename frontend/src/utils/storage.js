export function readStoredJson(key, fallback = null) {
  const raw = localStorage.getItem(key)
  if (raw == null) return fallback

  try {
    return JSON.parse(raw)
  } catch {
    // Corrupt or manually edited cache must not prevent the app from starting.
    localStorage.removeItem(key)
    return fallback
  }
}

export function writeStoredJson(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}
