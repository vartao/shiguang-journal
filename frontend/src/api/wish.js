import request from './request'

/**
 * 获取心愿列表
 * @param {{ status?: number }} params
 */
export function getWishes(params = {}) {
  return request.get('/wishes', { params })
}

/**
 * 创建心愿
 * @param {{ title: string, description?: string, icon?: string, category?: string, priority?: number }} data
 */
export function createWish(data) {
  return request.post('/wishes', data)
}

/**
 * 更新心愿
 * @param {number} id
 * @param {object} data
 */
export function updateWish(id, data) {
  return request.put(`/wishes/${id}`, data)
}

/**
 * 完成心愿
 * @param {number} id
 */
export function completeWish(id) {
  return request.post(`/wishes/${id}/complete`)
}

/**
 * 删除心愿
 * @param {number} id
 */
export function deleteWish(id) {
  return request.delete(`/wishes/${id}`)
}
