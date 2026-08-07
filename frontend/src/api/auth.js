import request from './request'

/**
 * 用户登录
 * @param {{ username: string, password: string }} data
 */
export function login(data) {
  return request.post('/auth/login', data)
}

/**
 * 用户注册
 * @param {{ username: string, password: string, nickname: string, role?: string, inviteCode?: string, togetherDate?: string }} data
 */
export function register(data) {
  return request.post('/auth/register', data)
}

/**
 * 获取当前用户信息
 */
export function getProfile() {
  return request.get('/auth/profile')
}

/**
 * 获取情侣空间信息
 */
export function getCoupleInfo() {
  return request.get('/couple/info')
}

/**
 * 创建情侣空间（若注册时未创建）
 * @param {{ name: string, startDate?: string, togetherDate?: string }} data
 */
export function createCouple(data) {
  return request.post('/couple/create', data)
}

/**
 * 绑定伴侣（邀请码）
 * @param {{ inviteCode: string }} data
 */
export function bindPartner(data) {
  return request.post('/couple/bind', data)
}

/**
 * 更新用户头像
 * @param {{ avatar: string }} data
 */
export function updateAvatar(data) {
  return request.put('/auth/avatar', data)
}

/**
 * 更新个人资料
 * @param {{ nickname?: string, avatar?: string }} data
 */
export function updateProfile(data) {
  return request.put('/auth/profile', data)
}

/**
 * 更新情侣空间信息
 * @param {{ name?: string, togetherDate?: string, announcement?: string, avatar?: string }} data
 */
export function updateCouple(data) {
  return request.put('/couple/update', data)
}
