import request from './request'

/**
 * 获取小事记列表
 * @param {{ category?: string, year?: number }} params
 */
export function fetchLogs(params = {}) {
  return request.get('/logs', { params })
}

/**
 * 创建小事记
 * @param {{ title: string, category?: string, logDate: string, note?: string, cover?: string, rating?: number }} data
 */
export function createLog(data) {
  return request.post('/logs', data)
}

/**
 * 更新小事记
 * @param {number} id
 * @param {object} data
 */
export function updateLog(id, data) {
  return request.put(`/logs/${id}`, data)
}

/**
 * 删除小事记
 * @param {number} id
 */
export function deleteLog(id) {
  return request.delete(`/logs/${id}`)
}
