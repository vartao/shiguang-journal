const DEFAULT_FORM = {
  title: '',
  description: '',
  icon: '✨',
  category: 'other',
  priority: 0
}

export function createWishForm(item) {
  if (!item) return { ...DEFAULT_FORM }
  return {
    title: item.title || '',
    description: item.description || '',
    icon: item.icon || '✨',
    category: item.category || 'other',
    priority: Number(item.priority) || 0
  }
}

export function toWishPayload(form) {
  return {
    title: form.title?.trim() || '',
    description: form.description?.trim() || '',
    icon: form.icon || '✨',
    category: form.category || 'other',
    priority: Number(form.priority) || 0
  }
}
