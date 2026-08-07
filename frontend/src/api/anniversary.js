import request from './request'

/**
 * 获取纪念日列表
 */
export function getAnniversaries() {
  return request.get('/anniversaries')
}

/**
 * 创建纪念日
 * 前端常用 title，后端字段为 name
 * @param {{ title?: string, name?: string, date: string, type?: string, icon?: string, remark?: string, repeatYearly?: number }} data
 */
export function createAnniversary(data) {
  return request.post('/anniversaries', {
    name: data.name || data.title,
    title: data.title || data.name,
    date: data.date,
    type: data.type,
    icon: data.icon,
    remark: data.remark || data.description,
    repeatYearly: data.repeatYearly != null ? data.repeatYearly : 1
  })
}

/**
 * 更新纪念日
 * @param {number} id
 * @param {object} data
 */
export function updateAnniversary(id, data) {
  return request.put(`/anniversaries/${id}`, {
    name: data.name || data.title,
    date: data.date,
    type: data.type,
    icon: data.icon,
    remark: data.remark || data.description,
    repeatYearly: data.repeatYearly
  })
}

/**
 * 删除纪念日
 * @param {number} id
 */
export function deleteAnniversary(id) {
  return request.delete(`/anniversaries/${id}`)
}
