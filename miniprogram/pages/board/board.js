// 留言板：双人气泡悄悄话
const api = require('../../utils/api')
const util = require('../../utils/util')

const TEN_MIN = 10 * 60 * 1000

/** 预处理消息：插入时间分隔条标记、合并相邻同人消息的头像 */
function buildItems(msgs, myId) {
  const items = []
  let prev = null
  msgs.forEach((m) => {
    const t = util.parseDate(m.createTime)
    const gap = prev && prev.time && t ? t - prev.time : Infinity
    const showTime = !prev || gap > TEN_MIN
    const showAvatar = !prev || prev.userId !== m.userId || showTime
    items.push({
      id: m.id,
      mine: !!(myId && m.userId === myId),
      nickname: m.nickname || 'TA',
      initial: (m.nickname || 'TA').charAt(0),
      avatarUrl: api.fullUrl(m.avatar),
      content: m.content,
      timeText: util.formatMessageTime(m.createTime),
      showTime,
      showAvatar
    })
    prev = { userId: m.userId, time: t }
  })
  return items
}

Page({
  data: {
    items: [],
    loading: true,
    draft: '',
    scrollTo: '',
    sending: false
  },

  raw: [],

  onShow() {
    const tb = typeof this.getTabBar === 'function' && this.getTabBar()
    if (tb) tb.setData({ selected: 2 })
    if (!getApp().isLoggedIn()) return
    this.load()
  },

  async load() {
    try {
      const data = await api.request({ path: '/messages?limit=50', loading: false })
      // 后端按时间倒序返回，翻转为正序展示
      this.raw = (data || []).slice().reverse()
      this.rebuild()
    } catch (_e) {
      // 错误提示已由 request 统一处理
    } finally {
      this.setData({ loading: false })
    }
  },

  rebuild() {
    const app = getApp()
    const myId = app.globalData.userInfo && app.globalData.userInfo.id
    this.setData({ items: buildItems(this.raw, myId) }, () => this.scrollToBottom())
  },

  scrollToBottom() {
    this.setData({ scrollTo: '' }, () => {
      setTimeout(() => this.setData({ scrollTo: 'msg-bottom' }), 60)
    })
  },

  onInput(e) {
    this.setData({ draft: e.detail.value })
  },

  async onSend() {
    const content = this.data.draft.trim()
    if (!content || this.data.sending) return
    this.setData({ sending: true })
    try {
      const msg = await api.request({ path: '/messages', method: 'POST', data: { content }, loading: false })
      if (msg) this.raw.push(msg)
      this.setData({ draft: '' })
      this.rebuild()
    } catch (_e) {
      // 错误提示已由 request 统一处理
    } finally {
      this.setData({ sending: false })
    }
  },

  // 自己的气泡长按删除
  onBubbleLong(e) {
    const { id, mine } = e.currentTarget.dataset
    if (!mine) return
    wx.showModal({
      title: '撤回这句话？',
      content: '删掉之后双方都看不到了',
      confirmText: '删除',
      confirmColor: '#c1666b',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/messages/${id}`, method: 'DELETE' })
          this.raw = this.raw.filter((m) => m.id !== id)
          this.rebuild()
          api.toast('已删除')
        } catch (_e) {}
      }
    })
  }
})
