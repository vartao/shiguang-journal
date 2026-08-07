import request from './request'

/**
 * 获取恋爱数据统计
 */
export function getStats() {
  return request.get('/stats')
}
