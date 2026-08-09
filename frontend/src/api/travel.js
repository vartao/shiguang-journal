import request from './request'

/**
 * 获取足迹列表
 * @param {{ status?: number }} params
 */
export function getTravels(params = {}) {
  return request.get('/travels', { params })
}

/**
 * 地图点亮聚合：[{ province, visited, planned }]
 */
export function getTravelMap() {
  return request.get('/travels/map')
}

/**
 * 创建足迹
 * @param {object} data
 */
export function createTravel(data) {
  return request.post('/travels', data)
}

/**
 * 足迹详情（含照片列表）
 * @param {number} id
 */
export function getTravelDetail(id) {
  return request.get(`/travels/${id}`)
}

/**
 * 更新足迹
 * @param {number} id
 * @param {object} data
 */
export function updateTravel(id, data) {
  return request.put(`/travels/${id}`, data)
}

/**
 * 删除足迹
 * @param {number} id
 */
export function deleteTravel(id) {
  return request.delete(`/travels/${id}`)
}

/**
 * 添加旅行照片（photoUrl 为已上传的图片地址）
 * @param {number} id 足迹ID
 * @param {string} photoUrl
 */
export function addTravelPhoto(id, photoUrl) {
  return request.post(`/travels/${id}/photos`, { photoUrl })
}

/**
 * 删除旅行照片
 * @param {number} id 足迹ID
 * @param {number} photoId
 */
export function deleteTravelPhoto(id, photoId) {
  return request.delete(`/travels/${id}/photos/${photoId}`)
}
