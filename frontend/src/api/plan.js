import request from './request'

/**
 * 获取行程列表
 */
export function getPlans() {
  return request.get('/plans')
}

/**
 * 获取单个行程
 * @param {number} id
 */
export function getPlan(id) {
  return request.get(`/plans/${id}`)
}

/**
 * 创建行程
 * @param {object} data
 */
export function createPlan(data) {
  return request.post('/plans', data)
}

/**
 * 更新行程
 * @param {number} id
 * @param {object} data
 */
export function updatePlan(id, data) {
  return request.put(`/plans/${id}`, data)
}

/**
 * 删除行程
 * @param {number} id
 */
export function deletePlan(id) {
  return request.delete(`/plans/${id}`)
}
