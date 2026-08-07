import request from './request'

/**
 * 获取笔记评论列表
 * @param {number|string} noteId
 */
export function getComments(noteId) {
  return request.get(`/notes/${noteId}/comments`)
}

/**
 * 发表评论
 * @param {number|string} noteId
 * @param {string} content
 */
export function addComment(noteId, content) {
  return request.post(`/notes/${noteId}/comments`, { content })
}

/**
 * 删除评论
 * @param {number|string} commentId
 */
export function deleteComment(commentId) {
  return request.delete(`/notes/comments/${commentId}`)
}
