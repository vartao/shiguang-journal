// 网络层：Promise 化 wx.request，统一 JWT / 错误提示 / 401 处理
const TOKEN_KEY = 'ql_token'

function getApp_() {
  try {
    return getApp()
  } catch (_e) {
    return null
  }
}

function baseUrl() {
  const app = getApp_()
  return (app && app.globalData.apiBase) || 'http://localhost:8090/api'
}

function toast(msg) {
  if (!msg) return
  wx.showToast({ title: String(msg).slice(0, 24), icon: 'none', duration: 2200 })
}

/**
 * 统一请求
 * @param {object} opts { path, method='GET', data, auth=true, loading=true, loadingText='加载中…' }
 * @returns {Promise<any>} 业务 data
 */
function request(opts) {
  const { path, method = 'GET', data, auth = true, loading = true, loadingText } = opts
  if (loading) wx.showLoading({ title: loadingText || '加载中…', mask: true })
  const header = { 'Content-Type': 'application/json' }
  const token = wx.getStorageSync(TOKEN_KEY)
  if (auth && token) header.Authorization = `Bearer ${token}`

  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl() + path,
      method,
      data,
      header,
      timeout: 15000,
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401 || body.code === 401) {
          handleUnauthorized()
          reject(new Error(body.message || '登录已过期'))
          return
        }
        if (res.statusCode === 200 && body.code === 200) {
          resolve(body.data)
          return
        }
        const msg = body.message || '服务器开小差了，稍后再试'
        toast(msg)
        reject(new Error(msg))
      },
      fail() {
        const msg = '网络可能走神了，请检查连接'
        toast(msg)
        reject(new Error(msg))
      },
      complete() {
        if (loading) wx.hideLoading()
      }
    })
  })
}

function handleUnauthorized() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync('ql_user')
  const app = getApp_()
  if (app) {
    app.globalData.token = ''
    app.globalData.userInfo = null
  }
  const pages = getCurrentPages()
  const cur = pages[pages.length - 1]
  if (cur && cur.route !== 'pages/login/login') {
    wx.reLaunch({ url: '/pages/login/login' })
  }
}

/**
 * 上传图片（先压缩），返回可访问 URL
 * @param {string} filePath wx.chooseMedia 返回的 tempFilePath
 */
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    const doUpload = (path) => {
      const token = wx.getStorageSync(TOKEN_KEY)
      wx.uploadFile({
        url: baseUrl() + '/upload',
        filePath: path,
        name: 'file',
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success(res) {
          try {
            const body = JSON.parse(res.data)
            if (body.code === 200 && body.data) {
              resolve(body.data)
            } else {
              toast(body.message || '上传失败')
              reject(new Error(body.message || '上传失败'))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail() {
          toast('上传失败，请检查网络')
          reject(new Error('上传失败'))
        }
      })
    }
    wx.compressImage({
      src: filePath,
      quality: 80,
      success: (r) => doUpload(r.tempFilePath),
      fail: () => doUpload(filePath) // 压缩失败回退原图
    })
  })
}

/** 选图并上传多张，返回 URL 数组 */
async function chooseAndUpload(count) {
  const res = await wx.chooseMedia({ count: count || 1, mediaType: ['image'] })
  const urls = []
  for (const f of res.tempFiles) {
    const url = await uploadImage(f.tempFilePath)
    urls.push(url)
  }
  return urls
}

/** 拼接上传资源的完整地址（后端返回 /uploads/xxx 相对路径） */
function fullUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//.test(url)) return url
  const base = baseUrl().replace(/\/api$/, '')
  return base + url
}

module.exports = { request, uploadImage, chooseAndUpload, fullUrl, toast }
