import request from './request'

/**
 * 获取惊喜列表
 */
export function getSurprises() {
  return request.get('/surprises')
}

/**
 * 获取惊喜详情
 * @param {number} id
 */
export function getSurpriseDetail(id) {
  return request.get(`/surprises/${id}`)
}

/**
 * 创建惊喜
 * @param {{ title: string, content: string, unlockDate?: string, unlockCondition?: string, unlockPassword?: string, coverImage?: string, hint?: string }} data
 */
export function createSurprise(data) {
  const unlockCondition =
    data.unlockCondition || (data.unlockPassword ? 'password' : data.unlockDate ? 'date' : 'click')

  return request.post('/surprises', {
    title: data.title,
    content: data.content,
    hint: data.hint,
    coverImage: data.coverImage,
    unlockCondition,
    unlockDate: data.unlockDate,
    unlockPassword: data.unlockPassword
  })
}

/**
 * 解锁惊喜
 * @param {number} id
 * @param {string} [password]
 */
export function unlockSurprise(id, password) {
  const body = password ? { password } : {}
  return request.post(`/surprises/${id}/unlock`, body)
}

/**
 * 删除惊喜（仅创建者）
 * @param {number} id
 */
export function deleteSurprise(id) {
  return request.delete(`/surprises/${id}`)
}
