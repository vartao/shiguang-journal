// 拾光手账小程序 - 全局逻辑
const api = require('./utils/api')

App({
  globalData: {
    // 开发联调地址；真机/生产改为你的 HTTPS 域名（需在小程序后台配置 request 合法域名）
    apiBase: 'http://localhost:8090/api',
    token: '',
    userInfo: null,
    features: {}, // 功能开关 map，见 /api/features
    featuresLoaded: false
  },

  onLaunch() {
    // 恢复登录态
    this.globalData.token = wx.getStorageSync('ql_token') || ''
    this.globalData.userInfo = wx.getStorageSync('ql_user') || null
    this.globalData.features = wx.getStorageSync('ql_features') || {}
    if (this.globalData.token) {
      this.refreshFeatures()
    }
  },

  /** 是否已登录 */
  isLoggedIn() {
    return !!this.globalData.token
  },

  /** 登录成功后调用：保存 token 与用户信息 */
  setLogin(token, userInfo) {
    this.globalData.token = token
    this.globalData.userInfo = userInfo
    wx.setStorageSync('ql_token', token)
    wx.setStorageSync('ql_user', userInfo)
    this.refreshFeatures()
  },

  /** 退出登录 */
  logout() {
    this.globalData.token = ''
    this.globalData.userInfo = null
    this.globalData.features = {}
    this.globalData.featuresLoaded = false
    wx.removeStorageSync('ql_token')
    wx.removeStorageSync('ql_user')
    wx.removeStorageSync('ql_couple')
    wx.removeStorageSync('ql_features')
    wx.reLaunch({ url: '/pages/login/login' })
  },

  /** 拉取功能开关（失败沿用缓存） */
  async refreshFeatures() {
    try {
      const data = await api.request({ path: '/features', loading: false })
      this.globalData.features = data || {}
      this.globalData.featuresLoaded = true
      wx.setStorageSync('ql_features', this.globalData.features)
      // 通知已打开页面刷新入口（如 mine 页）
      const pages = getCurrentPages()
      const cur = pages[pages.length - 1]
      if (cur && typeof cur.onFeaturesChange === 'function') {
        cur.onFeaturesChange(this.globalData.features)
      }
    } catch (_e) {
      // 静默，沿用缓存
    }
  },

  /** 功能是否开启（无记录默认开） */
  isFeatureOn(key) {
    if (!key) return true
    return this.globalData.features[key] !== false
  }
})
