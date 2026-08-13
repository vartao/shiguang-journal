// 时间线首页：笔记列表 + 搜索 + 心情筛选 + 那年今天 + 点赞/评论
const api = require('../../utils/api')
const util = require('../../utils/util')

const PAGE_SIZE = 10

const OTD_ICONS = { note: '📖', travel: '✈️', log: '✨' }

function mapList(map) {
  return Object.keys(map).map((key) => ({
    key,
    emoji: map[key].emoji,
    label: map[key].label
  }))
}

// 后端字段统一为视图模型
function mapNote(note) {
  const dateStr = note.noteDate || note.createdAt || note.createTime || ''
  const d = util.parseDate(dateStr)
  const images = (note.images || []).map(api.fullUrl).filter(Boolean)
  return {
    id: note.id,
    userId: note.userId,
    title: note.title,
    content: note.content,
    day: d ? String(d.getDate()) : '--',
    month: d ? `${d.getMonth() + 1}月` : '',
    images,
    cover: images[0] || api.fullUrl(note.coverImage) || '',
    extraCount: Math.max(0, images.length - 1),
    mood: note.mood || '',
    moodEmoji: note.mood ? util.mapOf(util.MOODS, note.mood).emoji : '',
    moodLabel: note.mood ? util.mapOf(util.MOODS, note.mood).label : '',
    weatherEmoji: note.weather ? util.mapOf(util.WEATHERS, note.weather).emoji : '',
    weatherLabel: note.weather ? util.mapOf(util.WEATHERS, note.weather).label : '',
    authorName: note.authorName || note.authorNickname || '',
    liked: !!note.liked,
    likeCount: note.likeCount != null ? note.likeCount : note.likes || 0,
    commentCount: note.commentCount || 0,
    isTop: note.isTop === 1
  }
}

function recordsOf(data) {
  if (!data) return { records: [], total: 0 }
  if (Array.isArray(data)) return { records: data, total: data.length }
  const records = data.records || data.list || []
  const total = data.total != null ? data.total : records.length
  return { records, total }
}

Page({
  data: {
    notes: [],
    total: 0,
    hasMore: false,
    initialLoading: true,
    loadingMore: false,
    keyword: '',
    mood: '',
    moods: [],
    onThisDay: [],
    myUserId: null,
    // 评论 sheet
    commentShow: false,
    commentNoteId: null,
    comments: [],
    commentLoading: false,
    commentText: '',
    commentSending: false
  },

  onLoad() {
    this.setData({ moods: mapList(util.MOODS) })
    this._pageNum = 1
    this._searchTimer = null
  },

  async onShow() {
    const tb = typeof this.getTabBar === 'function' && this.getTabBar()
    if (tb) tb.setData({ selected: 0 })

    if (!(await getApp().guardPage('notes', { preferHome: true }))) return
    const user = getApp().globalData.userInfo || {}
    this.setData({ myUserId: user.id || user.userId || null })
    this.refresh()
    this.loadOnThisDay()
  },

  onPullDownRefresh() {
    Promise.all([this.refresh(), this.loadOnThisDay()]).finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  /** 回到第一页重新加载 */
  async refresh() {
    this._pageNum = 1
    await this.fetchNotes(1, false)
  },

  async loadMore() {
    if (this.data.loadingMore || !this.data.hasMore) return
    await this.fetchNotes(this._pageNum + 1, true)
  },

  async fetchNotes(pageNum, append) {
    const { keyword, mood } = this.data
    const searching = !!keyword.trim()
    if (append) this.setData({ loadingMore: true })
    try {
      const data = searching
        ? await api.request({
            path: `/notes/search?keyword=${encodeURIComponent(keyword.trim())}&page=${pageNum}&size=${PAGE_SIZE}`,
            loading: false
          })
        : await api.request({
            path: `/notes?pageNum=${pageNum}&pageSize=${PAGE_SIZE}${mood ? `&mood=${mood}` : ''}`,
            loading: false
          })
      const { records, total } = recordsOf(data)
      const mapped = records.map(mapNote)
      const notes = append ? this.data.notes.concat(mapped) : mapped
      this._pageNum = pageNum
      this.setData({
        notes,
        total: searching ? this.data.total : total,
        hasMore: notes.length < total,
        initialLoading: false,
        loadingMore: false
      })
    } catch (_e) {
      this.setData({ initialLoading: false, loadingMore: false })
    }
  },

  /** 那年今天（未加入空间或无数据时不显示） */
  async loadOnThisDay() {
    try {
      const list = await api.request({ path: '/stats/on-this-day', loading: false })
      const items = (list || []).slice(0, 5).map((it) => ({
        id: it.id,
        type: it.type,
        icon: OTD_ICONS[it.type] || '✨',
        year: it.year,
        title: it.title || '无标题'
      }))
      this.setData({ onThisDay: items })
    } catch (_e) {
      this.setData({ onThisDay: [] })
    }
  },

  /* ---------- 搜索与筛选 ---------- */
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ keyword })
    if (this._searchTimer) clearTimeout(this._searchTimer)
    this._searchTimer = setTimeout(() => this.refresh(), 400)
  },

  onSearchClear() {
    if (!this.data.keyword) return
    if (this._searchTimer) clearTimeout(this._searchTimer)
    this.setData({ keyword: '' })
    this.refresh()
  },

  onMoodTap(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ mood: this.data.mood === key ? '' : key, keyword: '' })
    this.refresh()
  },

  /* ---------- 图片 ---------- */
  onPreview(e) {
    const { id } = e.currentTarget.dataset
    const note = this.data.notes.find((n) => n.id === id)
    if (!note || !note.images.length) return
    wx.previewImage({ current: note.cover, urls: note.images })
  },

  /* ---------- 点赞（乐观更新） ---------- */
  async onLike(e) {
    const { id } = e.currentTarget.dataset
    const idx = this.data.notes.findIndex((n) => n.id === id)
    if (idx < 0) return
    const note = this.data.notes[idx]
    const liked = !note.liked
    const likeCount = Math.max(0, note.likeCount + (liked ? 1 : -1))
    this.setData({
      [`notes[${idx}].liked`]: liked,
      [`notes[${idx}].likeCount`]: likeCount
    })
    try {
      const res = await api.request({ path: `/notes/${id}/like`, method: 'POST', loading: false })
      this.setData({
        [`notes[${idx}].liked`]: !!res.liked,
        [`notes[${idx}].likeCount`]: res.likeCount != null ? res.likeCount : likeCount
      })
    } catch (_e) {
      // 回滚
      this.setData({
        [`notes[${idx}].liked`]: note.liked,
        [`notes[${idx}].likeCount`]: note.likeCount
      })
    }
  },

  /* ---------- 删除 ---------- */
  onDelete(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '删除这篇笔记？',
      content: '撕下的这页就找不回来啦',
      confirmText: '删除',
      confirmColor: '#c1666b',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/notes/${id}`, method: 'DELETE', loadingText: '删除中…' })
          api.toast('已删除')
          const notes = this.data.notes.filter((n) => n.id !== id)
          this.setData({ notes, total: Math.max(0, this.data.total - 1) })
        } catch (_e) {
          // 错误已提示
        }
      }
    })
  },

  /* ---------- 评论 ---------- */
  async onOpenComments(e) {
    const { id } = e.currentTarget.dataset
    this.setData({
      commentShow: true,
      commentNoteId: id,
      comments: [],
      commentText: '',
      commentLoading: true
    })
    await this.loadComments(id)
  },

  async loadComments(id) {
    try {
      const list = await api.request({ path: `/notes/${id}/comments`, loading: false })
      const comments = (list || []).map((c) => ({
        id: c.id,
        userId: c.userId,
        content: c.content,
        author: c.authorNickname || 'TA',
        time: util.formatMessageTime(c.createTime),
        mine: this.data.myUserId != null && c.userId === this.data.myUserId
      }))
      this.setData({ comments, commentLoading: false })
    } catch (_e) {
      this.setData({ commentLoading: false })
    }
  },

  onCloseComments() {
    this.setData({ commentShow: false, commentNoteId: null })
  },

  onCommentInput(e) {
    this.setData({ commentText: e.detail.value })
  },

  async onSendComment() {
    const { commentNoteId, commentText, commentSending } = this.data
    const content = (commentText || '').trim()
    if (!commentNoteId || !content || commentSending) return
    this.setData({ commentSending: true })
    try {
      await api.request({
        path: `/notes/${commentNoteId}/comments`,
        method: 'POST',
        data: { content },
        loading: false
      })
      this.setData({ commentText: '' })
      await this.loadComments(commentNoteId)
      const idx = this.data.notes.findIndex((n) => n.id === commentNoteId)
      if (idx >= 0) {
        this.setData({ [`notes[${idx}].commentCount`]: this.data.notes[idx].commentCount + 1 })
      }
    } catch (_e) {
      // 错误已提示
    } finally {
      this.setData({ commentSending: false })
    }
  },

  onDeleteComment(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '删除这条评论？',
      confirmText: '删除',
      confirmColor: '#c1666b',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/notes/comments/${id}`, method: 'DELETE', loading: false })
          const comments = this.data.comments.filter((c) => c.id !== id)
          this.setData({ comments })
          const idx = this.data.notes.findIndex((n) => n.id === this.data.commentNoteId)
          if (idx >= 0) {
            this.setData({
              [`notes[${idx}].commentCount`]: Math.max(0, this.data.notes[idx].commentCount - 1)
            })
          }
        } catch (_e) {
          // 错误已提示
        }
      }
    })
  },

  onGoWrite() {
    wx.navigateTo({ url: '/pages/write/write' })
  }
})
