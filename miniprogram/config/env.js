// 小程序运行环境配置。
// 正式发布前必须把 trial / release 替换为已在微信公众平台配置的 HTTPS 合法域名。
const API_BASES = {
  develop: 'http://localhost:8090/api',
  trial: 'https://CHANGE_ME.example.com/api',
  release: 'https://CHANGE_ME.example.com/api'
}

function envVersion() {
  try {
    const info = wx.getAccountInfoSync()
    return (info && info.miniProgram && info.miniProgram.envVersion) || 'develop'
  } catch (_e) {
    return 'develop'
  }
}

function normalizeBase(value) {
  return String(value || '').trim().replace(/\/+$/, '')
}

function apiBase() {
  const env = envVersion()
  // 仅开发版允许用本地缓存临时覆盖，方便真机联调局域网地址；体验版/正式版不接受缓存注入。
  const override = env === 'develop' ? normalizeBase(wx.getStorageSync('ql_api_base_override')) : ''
  return override || normalizeBase(API_BASES[env] || API_BASES.develop)
}

function validateApiBase(base = apiBase()) {
  const env = envVersion()
  if (!base || /CHANGE_ME/i.test(base)) {
    return { valid: false, env, message: '请先配置小程序后端 HTTPS 地址' }
  }
  if (env !== 'develop' && !/^https:\/\//i.test(base)) {
    return { valid: false, env, message: '体验版和正式版必须使用 HTTPS 后端地址' }
  }
  return { valid: true, env, message: '' }
}

module.exports = { API_BASES, envVersion, apiBase, validateApiBase }
