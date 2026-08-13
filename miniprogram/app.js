// 拾光手账小程序 - 全局逻辑
const api = require('./utils/api')
const env = require('./config/env')
const { featureForRoute } = require('./utils/features')

App({
  globalData: {
    apiBase: '',
    envVersion: 'develop',
    token: '',
    userInfo: null,
    features: {}, // 功能开关 map，见 /api/features
    featuresLoaded: false,
    featuresPromise: null
  },

  onLaunch() {
    const config = env.validateApiBase()
    this.globalData.apiBase = env.apiBase()
    this.globalData.envVersion = config.env

    // 恢复登录态与当前账号缓存。功能缓存按账号隔离，避免切换账号时串数据。
    this.globalData.token = wx.getStorageSync('ql_token') || ''
    this.globalData.userInfo = wx.getStorageSync('ql_user') || null
    this.restoreFeatureCache()
    if (this.globalData.token) this.refreshFeatures()
  },

  /** 是否已登录 */
  isLoggedIn() {
    return !!this.globalData.token
  },

  /** 当前登录用户 ID */
  currentUserId() {
    const user = this.globalData.userInfo || {}
    const id = user.id != null ? user.id : user.userId
    return id != null ? String(id) : ''
  },

  featureCacheKey() {
    const id = this.currentUserId()
    return id ? `ql_features_${id}` : 'ql_features'
  },

  restoreFeatureCache() {
    const scoped = wx.getStorageSync(this.featureCacheKey())
    const legacy = this.currentUserId() ? wx.getStorageSync('ql_features') : null
    const cached = scoped || legacy
    this.globalData.features = cached && typeof cached === 'object' ? cached : {}
    this.globalData.featuresLoaded = !!cached
  },

  /** 登录成功后调用：保存 token 与用户信息 */
  setLogin(token, userInfo) {
    this.globalData.token = token
    this.globalData.userInfo = userInfo
    this.globalData.features = {}
    this.globalData.featuresLoaded = false
    wx.setStorageSync('ql_token', token)
    wx.setStorageSync('ql_user', userInfo)
    this.restoreFeatureCache()
    this.refreshFeatures()
  },

  /** 清理本地登录态；redirect=false 用于请求层统一清理后自行决定跳转。 */
  logout(redirect = true) {
    const featureKey = this.featureCacheKey()
    this.globalData.token = ''
    this.globalData.userInfo = null
    this.globalData.features = {}
    this.globalData.featuresLoaded = false
    this.globalData.featuresPromise = null
    wx.removeStorageSync('ql_token')
    wx.removeStorageSync('ql_user')
    wx.removeStorageSync('ql_couple')
    wx.removeStorageSync(featureKey)
    wx.removeStorageSync('ql_features') // 兼容旧版本缓存
    if (redirect) wx.reLaunch({ url: '/pages/login/login' })
  },

  /** 拉取功能开关；并发调用复用同一请求，失败时沿用当前账号缓存。 */
  refreshFeatures(force = false) {
    if (!this.isLoggedIn()) return Promise.resolve(this.globalData.features)
    if (this.globalData.featuresPromise && !force) return this.globalData.featuresPromise

    const promise = api
      .request({ path: '/features', loading: false, retry: true })
      .then((data) => {
        this.globalData.features = data && typeof data === 'object' ? data : {}
        this.globalData.featuresLoaded = true
        wx.setStorageSync(this.featureCacheKey(), this.globalData.features)
        wx.removeStorageSync('ql_features')
        const pages = getCurrentPages()
        const cur = pages[pages.length - 1]
        if (cur && typeof cur.onFeaturesChange === 'function') {
          cur.onFeaturesChange(this.globalData.features)
        }
        return this.globalData.features
      })
      .catch(() => this.globalData.features)
      .finally(() => {
        if (this.globalData.featuresPromise === promise) this.globalData.featuresPromise = null
      })
    this.globalData.featuresPromise = promise
    return promise
  },

  /** 功能是否开启（后端无记录默认开启） */
  isFeatureOn(key) {
    if (!key) return true
    return this.globalData.features[key] !== false
  },

  /** 页面统一守卫：校验登录与功能开关。返回 true 才继续加载业务数据。 */
  async guardPage(featureKey, options = {}) {
    if (!this.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return false
    }

    const key = featureKey || featureForRoute(options.route)
    if (!this.globalData.featuresLoaded) await this.refreshFeatures()
    if (!key || this.isFeatureOn(key)) return true

    if (!options.silent) {
      wx.showToast({ title: '该功能已在小窝中关闭', icon: 'none', duration: 2200 })
    }
    const pages = getCurrentPages()
    if (pages.length > 1 && !options.preferHome) {
      setTimeout(() => wx.navigateBack({ delta: 1 }), 250)
    } else {
      setTimeout(() => wx.reLaunch({ url: '/pages/mine/mine' }), 250)
    }
    return false
  }
})
