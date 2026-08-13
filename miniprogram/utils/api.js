// 网络层：Promise 化 wx.request，统一 JWT / 错误提示 / 401 / 429 退避重试
const env = require('../config/env')

const TOKEN_KEY = 'ql_token'
const DEFAULT_TIMEOUT = 15000
const MAX_BACKOFF = 30000
let loadingCount = 0
let unauthorizedHandling = false

function getApp_() {
  try {
    return getApp()
  } catch (_e) {
    return null
  }
}

function baseUrl() {
  const app = getApp_()
  return (app && app.globalData.apiBase) || env.apiBase()
}

function toast(msg) {
  if (!msg) return
  wx.showToast({ title: String(msg).slice(0, 24), icon: 'none', duration: 2200 })
}

function beginLoading(text) {
  loadingCount++
  wx.showLoading({ title: text || '加载中…', mask: true })
}

function endLoading() {
  loadingCount = Math.max(0, loadingCount - 1)
  if (loadingCount === 0) wx.hideLoading()
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function headerValue(headers, name) {
  if (!headers) return ''
  const target = String(name).toLowerCase()
  const key = Object.keys(headers).find((item) => item.toLowerCase() === target)
  return key ? headers[key] : ''
}

function retryAfterMs(headers, attempt) {
  const raw = headerValue(headers, 'retry-after')
  if (raw !== '') {
    const seconds = Number(raw)
    if (Number.isFinite(seconds) && seconds >= 0) return Math.min(MAX_BACKOFF, Math.max(1000, seconds * 1000))
    const at = Date.parse(raw)
    if (Number.isFinite(at)) return Math.min(MAX_BACKOFF, Math.max(1000, at - Date.now()))
  }
  const exponential = Math.min(MAX_BACKOFF, 1000 * 2 ** Math.min(attempt, 5))
  return exponential + Math.floor(Math.random() * 400)
}

function isSuccess(res, body) {
  return res.statusCode >= 200 && res.statusCode < 300 && body.code === 200
}

function isRateLimited(res, body) {
  return res.statusCode === 429 || body.code === 429
}

function shouldRetryFailure(method, retry, attempt, maxRetries) {
  if (!retry || attempt >= maxRetries) return false
  return ['GET', 'HEAD', 'OPTIONS'].includes(method)
}

function rawRequest({ url, method, data, header, timeout }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method,
      data,
      header,
      timeout,
      success: resolve,
      fail: reject
    })
  })
}

/**
 * 统一请求。
 * 429 默认持续按 Retry-After / 指数退避重试，直到成功或返回其他结果。
 * 普通网络失败仅在 retry=true 且为幂等请求时重试 maxRetries 次。
 */
async function request(opts) {
  const {
    path,
    method = 'GET',
    data,
    auth = true,
    loading = true,
    loadingText,
    retry = false,
    maxRetries = 2,
    timeout = DEFAULT_TIMEOUT,
    max429Retries = Infinity
  } = opts
  const upperMethod = String(method).toUpperCase()
  const config = env.validateApiBase(baseUrl())
  if (!config.valid) {
    toast(config.message)
    throw new Error(config.message)
  }

  if (loading) beginLoading(loadingText)
  let attempt = 0
  let rateLimitAttempt = 0
  let retryNoticeShown = false

  try {
    while (true) {
      const header = { 'Content-Type': 'application/json' }
      const token = wx.getStorageSync(TOKEN_KEY)
      if (auth && token) header.Authorization = `Bearer ${token}`

      let res
      try {
        res = await rawRequest({ url: baseUrl() + path, method: upperMethod, data, header, timeout })
      } catch (_error) {
        if (shouldRetryFailure(upperMethod, retry, attempt, maxRetries)) {
          await sleep(Math.min(4000, 600 * 2 ** attempt))
          attempt++
          continue
        }
        const msg = '网络可能走神了，请检查连接'
        toast(msg)
        throw new Error(msg)
      }

      const body = res.data || {}
      if (res.statusCode === 401 || body.code === 401) {
        handleUnauthorized()
        throw new Error(body.message || '登录已过期')
      }

      if (isRateLimited(res, body) && rateLimitAttempt < max429Retries) {
        const delay = retryAfterMs(res.header, rateLimitAttempt)
        if (!retryNoticeShown) {
          toast('请求较多，正在自动重试')
          retryNoticeShown = true
        }
        rateLimitAttempt++
        await sleep(delay)
        continue
      }

      if (isSuccess(res, body)) return body.data

      const msg = body.message || (isRateLimited(res, body) ? '请求过于频繁，请稍后再试' : '服务器开小差了，稍后再试')
      toast(msg)
      const error = new Error(msg)
      error.statusCode = res.statusCode
      error.code = body.code
      throw error
    }
  } finally {
    if (loading) endLoading()
  }
}

function handleUnauthorized() {
  if (unauthorizedHandling) return
  unauthorizedHandling = true
  const app = getApp_()
  if (app && typeof app.logout === 'function') {
    app.logout(false)
  } else {
    wx.removeStorageSync(TOKEN_KEY)
    wx.removeStorageSync('ql_user')
    wx.removeStorageSync('ql_couple')
    wx.removeStorageSync('ql_features')
  }

  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  if (!cur || cur.route !== 'pages/login/login') {
    wx.reLaunch({
      url: '/pages/login/login',
      complete: () => {
        unauthorizedHandling = false
      }
    })
  } else {
    unauthorizedHandling = false
  }
}

/** 上传图片（先压缩），返回可访问 URL；429 同样自动退避重试。 */
function uploadOnce(filePath) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync(TOKEN_KEY)
    wx.uploadFile({
      url: baseUrl() + '/upload',
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      timeout: 30000,
      success: resolve,
      fail: reject
    })
  })
}

async function uploadWithRetry(filePath) {
  let attempt = 0
  let noticeShown = false
  while (true) {
    let res
    try {
      res = await uploadOnce(filePath)
    } catch (_e) {
      throw new Error('上传失败，请检查网络')
    }

    let body = {}
    try {
      body = JSON.parse(res.data || '{}')
    } catch (_e) {
      throw new Error('上传响应格式异常')
    }
    if (res.statusCode === 401 || body.code === 401) {
      handleUnauthorized()
      throw new Error(body.message || '登录已过期')
    }
    if (isRateLimited(res, body)) {
      if (!noticeShown) {
        toast('上传较多，正在自动重试')
        noticeShown = true
      }
      await sleep(retryAfterMs(res.header, attempt++))
      continue
    }
    if (isSuccess(res, body)) return body.data && (body.data.url || body.data)
    throw new Error(body.message || '上传失败')
  }
}

function compressImage(filePath) {
  return new Promise((resolve) => {
    wx.compressImage({
      src: filePath,
      quality: 80,
      success: (res) => resolve(res.tempFilePath),
      fail: () => resolve(filePath)
    })
  })
}

async function uploadImage(filePath) {
  const config = env.validateApiBase(baseUrl())
  if (!config.valid) {
    toast(config.message)
    throw new Error(config.message)
  }
  beginLoading('上传中…')
  try {
    const compressed = await compressImage(filePath)
    return await uploadWithRetry(compressed)
  } catch (error) {
    if (error && error.message && !/登录已过期/.test(error.message)) toast(error.message)
    throw error
  } finally {
    endLoading()
  }
}

/** 选图并上传多张，返回 URL 数组 */
async function chooseAndUpload(count) {
  const res = await wx.chooseMedia({ count: count || 1, mediaType: ['image'], sourceType: ['album', 'camera'] })
  const urls = []
  for (const file of res.tempFiles || []) {
    const url = await uploadImage(file.tempFilePath)
    if (url) urls.push(url)
  }
  return urls
}

/** 拼接上传资源的完整地址（后端返回 /uploads/xxx 相对路径） */
function fullUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  const base = baseUrl().replace(/\/api$/, '')
  return base + (String(url).startsWith('/') ? url : '/' + url)
}

module.exports = {
  request,
  uploadImage,
  chooseAndUpload,
  fullUrl,
  toast,
  retryAfterMs
}
