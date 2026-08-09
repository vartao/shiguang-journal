// 我的 / 小窝总览
const api = require('../../utils/api')

// 功能入口清单（anniversary / surprise 暂无小程序页面，只参与开关管理，不进宫格）
const FEATURES = [
  { key: 'notes', label: '时间线', icon: '🎞️', path: '/pages/timeline/timeline', tab: true },
  { key: 'albums', label: '相册墙', icon: '📸', path: '/pages/albums/albums' },
  { key: 'anniversary', label: '纪念日', icon: '💑', path: '', noPage: true },
  { key: 'surprise', label: '惊喜盒', icon: '🎁', path: '', noPage: true },
  { key: 'wishes', label: '心愿池', icon: '✨', path: '/pages/wishes/wishes' },
  { key: 'stats', label: '数据', icon: '📊', path: '/pages/stats/stats' },
  { key: 'travels', label: '足迹', icon: '🗺️', path: '/pages/travels/travels', tab: true },
  { key: 'plans', label: '行程', icon: '🧳', path: '/pages/plans/plans' },
  { key: 'menu', label: '吃什么', icon: '🍚', path: '/pages/menu/menu' },
  { key: 'capsule', label: '胶囊', icon: '⏳', path: '/pages/capsule/capsule' },
  { key: 'logs', label: '小事记', icon: '📖', path: '/pages/logs/logs' },
  { key: 'board', label: '留言板', icon: '💬', path: '/pages/board/board', tab: true }
]

function firstChar(name) {
  const s = String(name || '').trim()
  return s ? s.charAt(0) : '?'
}

Page({
  data: {
    me: { name: '我', avatar: '', initial: '?' },
    partner: { name: 'TA', avatar: '', initial: '?' },
    coupleName: '我们的小窝',
    togetherDays: 0,
    inviteCode: '',
    hasPartner: false,
    bindCode: '',
    binding: false,
    grid: [],
    featureSheet: false,
    featureList: [],
    profileSheet: false,
    profileNickname: '',
    profileAvatar: '',
    savingProfile: false
  },

  onShow() {
    const tb = typeof this.getTabBar === 'function' && this.getTabBar()
    if (tb) tb.setData({ selected: 3 })
    if (!getApp().isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.applyCachedCouple()
    this.refreshCouple()
    this.buildGrid()
  },

  /** 供 app.refreshFeatures 回调：开关变化后刷新宫格 */
  onFeaturesChange() {
    this.buildGrid()
  },

  /** 先用缓存渲染，避免空白 */
  applyCachedCouple() {
    const couple = wx.getStorageSync('ql_couple')
    if (couple) this.renderCouple(couple)
  },

  async refreshCouple() {
    try {
      const couple = await api.request({ path: '/couple/info', loading: false })
      wx.setStorageSync('ql_couple', couple)
      this.renderCouple(couple)
    } catch (_e) {
      // 静默，沿用缓存
    }
  },

  renderCouple(couple) {
    if (!couple) return
    const app = getApp()
    const meInfo = app.globalData.userInfo || {}
    const myId = meInfo.id || meInfo.userId
    const isU1 = couple.userId1 === myId
    const meSide = isU1
      ? { name: couple.user1Nickname, avatar: couple.user1Avatar }
      : { name: couple.user2Nickname, avatar: couple.user2Avatar }
    const partnerSide = isU1
      ? { name: couple.user2Nickname, avatar: couple.user2Avatar }
      : { name: couple.user1Nickname, avatar: couple.user1Avatar }
    const hasPartner = !!(couple.userId1 && couple.userId2)

    const meName = meInfo.nickname || meSide.name || '我'
    const meAvatar = api.fullUrl(meSide.avatar || meInfo.avatar || '')
    const partnerName = hasPartner ? partnerSide.name || couple.partnerName || 'TA' : '虚位以待'
    const partnerAvatar = hasPartner ? api.fullUrl(partnerSide.avatar || '') : ''

    this.setData({
      me: { name: meName, avatar: meAvatar, initial: firstChar(meName) },
      partner: { name: partnerName, avatar: partnerAvatar, initial: firstChar(partnerName) },
      coupleName: couple.name || '我们的小窝',
      togetherDays: couple.togetherDays || 0,
      inviteCode: couple.inviteCode || '',
      hasPartner
    })
  },

  /** 按功能开关过滤出宫格入口 */
  buildGrid() {
    const app = getApp()
    const grid = FEATURES.filter((f) => !f.noPage && app.isFeatureOn(f.key))
    const featureList = FEATURES.map((f) => ({
      key: f.key,
      label: f.label,
      icon: f.icon,
      enabled: app.isFeatureOn(f.key),
      saving: false
    }))
    this.setData({ grid, featureList })
  },

  openFeature(e) {
    const item = e.currentTarget.dataset.item
    if (!item || !item.path) return
    if (item.tab) {
      wx.switchTab({ url: item.path })
    } else {
      wx.navigateTo({
        url: item.path,
        fail: () => api.toast('页面建设中，敬请期待')
      })
    }
  },

  copyInvite() {
    const code = this.data.inviteCode
    if (!code) return
    wx.setClipboardData({
      data: code,
      success: () => api.toast('邀请码已复制，快发给 TA 吧')
    })
  },

  onBindInput(e) {
    this.setData({ bindCode: e.detail.value })
  },

  async bindPartner() {
    const code = this.data.bindCode.trim()
    if (!code || this.data.binding) return
    if (code.length < 4) {
      api.toast('邀请码长度不太对，一般是 6 位')
      return
    }
    this.setData({ binding: true })
    try {
      const couple = await api.request({
        path: '/couple/bind',
        method: 'POST',
        data: { inviteCode: code },
        loadingText: '绑定中…'
      })
      wx.setStorageSync('ql_couple', couple)
      this.renderCouple(couple)
      this.setData({ bindCode: '' })
      api.toast('绑定成功，欢迎入住小窝')
      getApp().refreshFeatures()
    } catch (_e) {
      // 错误信息已由 request 统一 toast
    } finally {
      this.setData({ binding: false })
    }
  },

  /* ===== 功能管理弹层 ===== */
  openFeatureSheet() {
    this.buildGrid()
    this.setData({ featureSheet: true })
  },

  closeFeatureSheet() {
    this.setData({ featureSheet: false })
  },

  async toggleFeature(e) {
    const key = e.currentTarget.dataset.key
    const enabled = e.detail.value
    const featureList = this.data.featureList.map((f) => (f.key === key ? { ...f, saving: true } : f))
    this.setData({ featureList })
    try {
      const map = await api.request({
        path: `/features/${key}`,
        method: 'PUT',
        data: { enabled },
        loading: false
      })
      const app = getApp()
      app.globalData.features = map || {}
      wx.setStorageSync('ql_features', app.globalData.features)
    } catch (_e) {
      // 失败时 buildGrid 会把开关拨回原状
    }
    this.buildGrid()
  },

  /* ===== 我的资料弹层 ===== */
  openProfile() {
    this.setData({
      profileSheet: true,
      profileNickname: this.data.me.name === '我' ? '' : this.data.me.name,
      profileAvatar: this.data.me.avatar
    })
  },

  closeProfile() {
    this.setData({ profileSheet: false })
  },

  onNicknameInput(e) {
    this.setData({ profileNickname: e.detail.value })
  },

  /** 更新 globalData 与缓存中的用户信息 */
  applyUserInfo(userInfo) {
    if (!userInfo) return
    const app = getApp()
    app.globalData.userInfo = { ...(app.globalData.userInfo || {}), ...userInfo }
    wx.setStorageSync('ql_user', app.globalData.userInfo)
  },

  async changeAvatar() {
    try {
      const urls = await api.chooseAndUpload(1)
      if (!urls || !urls.length) return
      const userInfo = await api.request({
        path: '/auth/profile',
        method: 'PUT',
        data: { avatar: urls[0] },
        loadingText: '更新头像…'
      })
      this.applyUserInfo(userInfo)
      this.setData({
        profileAvatar: api.fullUrl(urls[0]),
        'me.avatar': api.fullUrl(urls[0])
      })
      this.refreshCouple()
    } catch (_e) {
      // 取消选择或失败均有提示，无需额外处理
    }
  },

  async saveProfile() {
    const nickname = this.data.profileNickname.trim()
    if (!nickname) {
      api.toast('昵称不能为空')
      return
    }
    if (this.data.savingProfile) return
    this.setData({ savingProfile: true })
    try {
      const userInfo = await api.request({
        path: '/auth/profile',
        method: 'PUT',
        data: { nickname },
        loadingText: '保存中…'
      })
      this.applyUserInfo(userInfo)
      this.setData({ profileSheet: false })
      api.toast('资料已保存')
      this.refreshCouple()
    } catch (_e) {
      // 错误信息已由 request 统一 toast
    } finally {
      this.setData({ savingProfile: false })
    }
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要离开小窝吗？再回来要重新登录哦',
      confirmText: '退出',
      confirmColor: '#c1666b',
      success: (res) => {
        if (res.confirm) getApp().logout()
      }
    })
  }
})
