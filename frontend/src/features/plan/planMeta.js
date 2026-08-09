/**
 * 行程状态元数据：0-计划中 1-已出发 2-已完成
 */
export const planStatusOptions = [
  { value: 0, label: '计划中' },
  { value: 1, label: '已出发' },
  { value: 2, label: '已完成' }
]

export function planStatusLabel(status) {
  const option = planStatusOptions.find((item) => item.value === Number(status))
  return option ? option.label : '计划中'
}
