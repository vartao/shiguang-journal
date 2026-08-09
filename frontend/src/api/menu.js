import request from './request'

/**
 * 获取菜品池列表
 * @param {{ category?: string }} params
 */
export function getDishes(params = {}) {
  return request.get('/dishes', { params })
}

/**
 * 添加菜品
 * @param {{ name: string, category?: string, note?: string }} data
 */
export function createDish(data) {
  return request.post('/dishes', data)
}

/**
 * 删除菜品
 * @param {number} id
 */
export function deleteDish(id) {
  return request.delete(`/dishes/${id}`)
}

/**
 * 随机抽一道菜
 * @param {{ category?: string }} params
 */
export function getRandomDish(params = {}) {
  return request.get('/dishes/random', { params })
}

/**
 * 获取某天菜单
 * @param {{ date?: string }} params date 格式 yyyy-MM-dd
 */
export function getMenu(params = {}) {
  return request.get('/menu', { params })
}

/**
 * 给某天某餐次加一道菜
 * @param {{ mealDate: string, mealType: string, dishName: string }} data
 */
export function addMenuItem(data) {
  return request.post('/menu', data)
}

/**
 * 删除菜单中的一项
 * @param {number} id
 */
export function deleteMenuItem(id) {
  return request.delete(`/menu/${id}`)
}
