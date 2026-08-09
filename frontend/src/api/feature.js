import request from './request'

/**
 * 获取当前空间功能开关
 */
export function getFeatures() {
  return request.get('/features')
}

/**
 * 更新某个功能开关
 * @param {string} key
 * @param {boolean} enabled
 */
export function updateFeature(key, enabled) {
  return request.put(`/features/${key}`, { enabled })
}
