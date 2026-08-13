// 惊喜盒：创建彩蛋、三种解锁方式、解锁后阅读、创建者撤回
const api = require('../../utils/api')
const util = require('../../utils/util')

const CONDITIONS = [
  { key: 'date', icon: '📅', label: '等到那天', desc: '到期自动打开' },
  { key: 'password', icon: '🔐', label: '秘密口令', desc: '猜对密码才能看' },
  { key: 'click', icon: '✨', label: '现在可拆', desc: '轻轻一点就出现' }
]

function today() {
  return util.formatDate(new Date())
}

function blankForm(minDate) {
  return { title: '', content: '', hint: '', unlockCondition: 'date', unlockDate: minDate, unlockPassword: '', coverImage: '' }
}

Page({
  data: {
    loading: true,
    items: [],
    conditions: CONDITIONS,
    minDate: '',
    showCreate: false,
    form: blankForm(''),
    coverFull: '',
    saving: false,
    canSave: false,
    openingId: 0,
    deletingId: 0,
    detail: null,
    password: '',
    unlocking: false
  },

  onLoad() {
    const minDate = today()
    this.setData({ minDate, form: blankForm(minDate) })
  },

  async onShow() {
    if (!(await getApp().guardPage('surprise'))) return
    this.load()
  },

  onPullDownRefresh() {
    this.load(true)
  },

  currentUserId() {
    const app = getApp()
    const info = app.globalData && app.globalData.userInfo
    const id = info && (info.id != null ? info.id : info.userId)
    return id != null ? String(id) : ''
  },

  isUnlocked(item) {
    return item && (item.unlocked === true || Number(item.unlocked) === 1 || item.isUnlocked === true)
  },

  decorate(item) {
    const unlocked = this.isUnlocked(item)
    const condition = CONDITIONS.find((it) => it.key === item.unlockCondition) || CONDITIONS[2]
    const daysLeft = item.unlockDate ? util.daysBetween(util.formatDate(new Date()), item.unlockDate) : 0
    let statusText = condition.label
    let canUnlock = item.unlockCondition !== 'date' || daysLeft <= 0
    if (unlocked) {
      statusText = '已经拆开'
      canUnlock = true
    } else if (item.unlockCondition === 'date') {
      statusText = daysLeft > 0 ? `还有 ${daysLeft} 天` : '今天可以拆'
    } else if (item.unlockCondition === 'password') {
      statusText = '需要口令'
    } else {
      statusText = '轻触可拆'
    }

    const uid = this.currentUserId()
    return {
      ...item,
      unlocked,
      conditionIcon: condition.icon,
      conditionLabel: condition.label,
      statusText,
      canUnlock,
      daysLeft,
      coverFull: api.fullUrl(item.coverImage),
      creatorName: item.creatorNickname || item.fromName || 'TA',
      createdAtCN: util.formatDateCN(item.createdAt || item.createTime),
      unlockDateCN: util.formatDateCN(item.unlockDate),
      canDelete: !!uid && String(item.userId) === uid,
      displayHint: item.hint || (unlocked ? '惊喜已经揭晓，点开再看一次' : '这里藏着一句只给你的话')
    }
  },

  async load(fromPull) {
    this.setData({ loading: !fromPull })
    try {
      const list = await api.request({ path: '/surprises', loading: false })
      this.setData({ items: (list || []).map((item) => this.decorate(item)) })
    } catch (_e) {
      // 请求层已统一提示
    } finally {
      this.setData({ loading: false })
      if (fromPull) wx.stopPullDownRefresh()
    }
  },

  openCreate() {
    this.setData({ showCreate: true, form: blankForm(this.data.minDate), coverFull: '', canSave: false })
  },

  closeCreate() {
    if (this.data.saving) return
    this.setData({ showCreate: false, canSave: false })
  },

  onTitleInput(e) {
    const title = e.detail.value
    this.setData({ 'form.title': title }, () => this.updateCanSave())
  },
  onContentInput(e) {
    const content = e.detail.value
    this.setData({ 'form.content': content }, () => this.updateCanSave())
  },
  onHintInput(e) { this.setData({ 'form.hint': e.detail.value }) },
  onPasswordInput(e) {
    this.setData({ 'form.unlockPassword': e.detail.value }, () => this.updateCanSave())
  },
  onUnlockDateChange(e) {
    this.setData({ 'form.unlockDate': e.detail.value }, () => this.updateCanSave())
  },
  selectCondition(e) {
    this.setData({ 'form.unlockCondition': e.currentTarget.dataset.key }, () => this.updateCanSave())
  },

  updateCanSave() {
    const form = this.data.form
    const hasBase = !!((form.title || '').trim() && (form.content || '').trim())
    const hasCondition =
      form.unlockCondition === 'date'
        ? !!form.unlockDate
        : form.unlockCondition === 'password'
          ? !!(form.unlockPassword || '').trim()
          : true
    this.setData({ canSave: hasBase && hasCondition })
  },

  async chooseCover() {
    try {
      const urls = await api.chooseAndUpload(1)
      if (!urls || !urls.length) return
      this.setData({ 'form.coverImage': urls[0], coverFull: api.fullUrl(urls[0]) })
    } catch (_e) {
      // 用户取消或上传失败
    }
  },

  removeCover() {
    this.setData({ 'form.coverImage': '', coverFull: '' })
  },

  async submit() {
    const { form, saving } = this.data
    if (saving) return
    const title = (form.title || '').trim()
    const content = (form.content || '').trim()
    const password = (form.unlockPassword || '').trim()
    if (!title) {
      api.toast('给惊喜起个标题吧')
      return
    }
    if (!content) {
      api.toast('惊喜内容还没有写')
      return
    }
    if (form.unlockCondition === 'date' && !form.unlockDate) {
      api.toast('请选择解锁日期')
      return
    }
    if (form.unlockCondition === 'password' && !password) {
      api.toast('请设置秘密口令')
      return
    }

    const data = {
      title,
      content,
      hint: (form.hint || '').trim(),
      coverImage: form.coverImage || '',
      unlockCondition: form.unlockCondition,
      unlockDate: form.unlockCondition === 'date' ? form.unlockDate : null,
      unlockPassword: form.unlockCondition === 'password' ? password : null
    }

    this.setData({ saving: true })
    try {
      await api.request({ path: '/surprises', method: 'POST', data, loadingText: '藏好惊喜…' })
      api.toast('惊喜已经藏好啦')
      this.setData({ showCreate: false, canSave: false })
      this.load()
    } catch (_e) {
      // 请求层已统一提示
    } finally {
      this.setData({ saving: false })
    }
  },
  async openDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id || this.data.openingId) return
    this.setData({ openingId: Number(id) })
    try {
      const data = await api.request({ path: `/surprises/${id}`, loadingText: '打开盒子…' })
      this.setData({ detail: this.decorate(data), password: '' })
    } catch (_e) {
      // 请求层已统一提示
    } finally {
      this.setData({ openingId: 0 })
    }
  },

  closeDetail() {
    if (this.data.unlocking) return
    this.setData({ detail: null, password: '' })
  },

  stopEvent() {},

  onUnlockPasswordInput(e) {
    this.setData({ password: e.detail.value })
  },

  async unlock() {
    const detail = this.data.detail
    if (!detail || detail.unlocked || this.data.unlocking) return
    if (detail.unlockCondition === 'date' && !detail.canUnlock) {
      api.toast(`还要等 ${detail.daysLeft} 天哦`)
      return
    }
    const password = (this.data.password || '').trim()
    if (detail.unlockCondition === 'password' && !password) {
      api.toast('请输入秘密口令')
      return
    }

    this.setData({ unlocking: true })
    try {
      const data = await api.request({
        path: `/surprises/${detail.id}/unlock`,
        method: 'POST',
        data: detail.unlockCondition === 'password' ? { password } : {},
        loadingText: '拆惊喜中…'
      })
      wx.vibrateShort({ type: 'medium' })
      this.setData({ detail: this.decorate(data), password: '' })
      this.load()
    } catch (_e) {
      // 请求层已统一提示
    } finally {
      this.setData({ unlocking: false })
    }
  },

  previewCover() {
    const cover = this.data.detail && this.data.detail.coverFull
    if (cover) wx.previewImage({ current: cover, urls: [cover] })
  },

  remove(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.items.find((it) => String(it.id) === String(id)) || this.data.detail
    if (!item) return
    wx.showModal({
      title: '撤回惊喜',
      content: `确定撤回“${item.title}”吗？撤回后对方就看不到了。`,
      confirmText: '撤回',
      confirmColor: '#a34f54',
      success: async (res) => {
        if (!res.confirm || this.data.deletingId) return
        this.setData({ deletingId: Number(id) })
        try {
          await api.request({ path: `/surprises/${id}`, method: 'DELETE', loadingText: '撤回中…' })
          api.toast('惊喜已撤回')
          this.setData({ detail: null })
          this.load()
        } catch (_e) {
          // 请求层已统一提示
        } finally {
          this.setData({ deletingId: 0 })
        }
      }
    })
  }
})