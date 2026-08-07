export function getBusinessError(response) {
  if (!response || response.code === undefined || response.code === 200 || response.code === 0) return null
  return {
    message: response.message || '请求失败',
    unauthorized: Number(response.code) === 401
  }
}

export function getTransportErrorMessage(error = {}) {
  if (error.response) {
    const status = Number(error.response.status)
    if (status === 401) return '登录已过期，请重新登录'
    if (status === 403) return '没有权限执行此操作'
    if (status === 404) return '请求的资源不存在'
    if (status >= 500) return '服务器开小差了，请稍后再试'
    return error.response.data?.message || '网络请求失败'
  }
  if (error.code === 'ECONNABORTED') return '请求超时，请检查网络'
  return '网络连接失败，请检查网络'
}
