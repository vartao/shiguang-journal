import request from './request'

/**
 * 获取留言列表（最近 N 条）
 * @param {{ limit?: number }} params
 */
export function getMessages(params = {}) {
  return request.get('/messages', { params })
}

/**
 * 发布留言
 * @param {{ content: string }} data
 */
export function createMessage(data) {
  return request.post('/messages', data)
}

/**
 * 删除留言（仅本人）
 * @param {number} id
 */
export function deleteMessage(id) {
  return request.delete(`/messages/${id}`)
}
