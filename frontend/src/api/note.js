import request from './request'

/**
 * 获取笔记列表（时间线）
 * @param {{ page?: number, size?: number, mood?: string }} params
 */
export function getNotes(params = {}) {
  return request.get('/notes', { params })
}

/**
 * 获取笔记详情
 * @param {number} id
 */
export function getNoteDetail(id) {
  return request.get(`/notes/${id}`)
}

/**
 * 创建笔记
 * @param {{ title: string, content: string, mood: string, images: string[], noteDate: string }} data
 */
export function createNote(data) {
  return request.post('/notes', data)
}

/**
 * 更新笔记
 * @param {number} id
 * @param {object} data
 */
export function updateNote(id, data) {
  return request.put(`/notes/${id}`, data)
}

/**
 * 删除笔记
 * @param {number} id
 */
export function deleteNote(id) {
  return request.delete(`/notes/${id}`)
}

/**
 * 搜索笔记
 * @param {string} keyword
 * @param {{ page?: number, size?: number }} params
 */
export function searchNotes(keyword, params = {}) {
  return request.get('/notes/search', { params: { keyword, ...params } })
}

/**
 * 给笔记点赞/比心
 * @param {number} id
 */
export function likeNote(id) {
  return request.post(`/notes/${id}/like`)
}
