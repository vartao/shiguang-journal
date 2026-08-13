// 登录 / 注册 / 绑定小窝
const api = require('../../utils/api')

Page({
  data: {
    mode: 'login', // login | register
    loading: false,
    today: '',
    form: {
      username: '',
      password: '',
      nickname: '',
      role: '', // boy / girl / ''（保密）
      inviteCode: '',
      togetherDate: ''
    }
  },

  onLoad() {
    const now = new Date()
    const pad = (n) => (n < 10 ? '0' + n : '' + n)
    this.setData({ today: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}` })
    this.jumpIfLoggedIn()
  },

  onShow() {
    this.jumpIfLoggedIn()
  },

  /** 已登录进入首个可用 tab；时间线关闭时回到小窝。 */
  jumpIfLoggedIn() {
    const app = getApp()
    if (!app.isLoggedIn()) return
    const url = app.isFeatureOn('notes') ? '/pages/timeline/timeline' : '/pages/mine/mine'
    wx.switchTab({ url })
  },

  switchMode(e) {
    const mode = e.currentTarget.dataset.mode
    if (mode && mode !== this.data.mode) {
      this.setData({ mode })
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  /** 性别选择：再点一次取消（保密） */
  selectRole(e) {
    const role = e.currentTarget.dataset.role
    this.setData({ 'form.role': this.data.form.role === role ? '' : role })
  },

  onDateChange(e) {
    this.setData({ 'form.togetherDate': e.detail.value })
  },

  validate() {
    const { mode, form } = this.data
    const username = form.username.trim()
    if (!username) {
      api.toast('请输入用户名')
      return false
    }
    if (mode === 'register') {
      if (username.length < 3 || username.length > 20) {
        api.toast('用户名长度为3-20个字符')
        return false
      }
      if (!form.nickname.trim()) {
        api.toast('请输入昵称')
        return false
      }
    }
    if (!form.password) {
      api.toast('请输入密码')
      return false
    }
    if (mode === 'register' && (form.password.length < 6 || form.password.length > 32)) {
      api.toast('密码长度为6-32个字符')
      return false
    }
    return true
  },

  async submit() {
    if (this.data.loading || !this.validate()) return
    const { mode, form } = this.data
    const inviteCode = form.inviteCode.trim()
    this.setData({ loading: true })
    try {
      const isLogin = mode === 'login'
      const data = isLogin
        ? { username: form.username.trim(), password: form.password }
        : {
            username: form.username.trim(),
            password: form.password,
            nickname: form.nickname.trim(),
            role: form.role || undefined,
            inviteCode: inviteCode || undefined,
            togetherDate: !inviteCode && form.togetherDate ? form.togetherDate : undefined
          }
      const res = await api.request({
        path: isLogin ? '/auth/login' : '/auth/register',
        method: 'POST',
        data,
        auth: false,
        loadingText: isLogin ? '登录中…' : '注册中…'
      })
      getApp().setLogin(res.token, res.user || res)
      // 缓存小窝信息（失败不阻塞进入）
      try {
        const couple = await api.request({ path: '/couple/info', loading: false })
        wx.setStorageSync('ql_couple', couple)
      } catch (_e) {
        // 静默
      }
      const app = getApp()
      const target = app.isFeatureOn('notes') ? '/pages/timeline/timeline' : '/pages/mine/mine'
      wx.switchTab({ url: target })
    } catch (_e) {
      // 错误信息已由 request 统一 toast
    } finally {
      this.setData({ loading: false })
    }
  }
})
