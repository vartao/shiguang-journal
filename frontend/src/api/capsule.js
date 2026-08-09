import request from './request'

/**
 * 获取时间胶囊列表
 */
export function getCapsules() {
  return request.get('/capsules')
}

/**
 * 创建时间胶囊
 * @param {{ title: string, message?: string, openDate: string, photoUrls?: string[] }} data
 */
export function createCapsule(data) {
  return request.post('/capsules', data)
}

/**
 * 拆开时间胶囊
 * @param {number} id
 */
export function openCapsule(id) {
  return request.post(`/capsules/${id}/open`)
}

/**
 * 删除时间胶囊（仅创建者）
 * @param {number} id
 */
export function deleteCapsule(id) {
  return request.delete(`/capsules/${id}`)
}
