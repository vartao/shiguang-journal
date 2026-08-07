import { toLocalDateInputValue } from '@/utils/date'

const DEFAULT_FORM = {
  title: '',
  date: '',
  type: 'recurring',
  icon: '💕',
  remark: ''
}

export function toAnniversaryUiType(item = {}, today = toLocalDateInputValue()) {
  if (item.repeatYearly === 1 || item.repeatYearly === true) return 'recurring'
  return item.date && item.date < today ? 'past' : 'future'
}

export function createAnniversaryForm(item, today) {
  if (!item) return { ...DEFAULT_FORM }
  return {
    title: item.title || item.name || '',
    date: item.date || '',
    type: toAnniversaryUiType(item, today),
    icon: item.icon || '💕',
    remark: item.remark || item.description || ''
  }
}

export function toAnniversaryPayload(form) {
  const title = form.title?.trim() || ''
  return {
    title,
    name: title,
    date: form.date,
    type: 'custom',
    repeatYearly: form.type === 'recurring' ? 1 : 0,
    icon: form.icon || '💕',
    remark: form.remark?.trim() || ''
  }
}
