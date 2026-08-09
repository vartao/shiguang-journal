import request from './request'

/**
 * 获取恋爱数据统计
 */
export function getStats() {
  return request.get('/stats')
}

/**
 * 那年今天（历年同月同日的记录）
 */
export function getOnThisDay() {
  return request.get('/stats/on-this-day')
}
