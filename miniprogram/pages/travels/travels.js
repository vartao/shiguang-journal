// 足迹地图：中国地图点亮 + 年份/季节分组列表
const api = require('../../utils/api')
const { formatDate, seasonOf } = require('../../utils/util')
const { toast } = api
const chinaMapData = require('../../utils/chinaMapData')

// 解析 SVG path（只含 M/L/Z 绝对指令），预计算指令序列与包围盒
function parsePath(d) {
  const cmds = []
  const re = /([MLZ])([^MLZ]*)/g
  let m
  while ((m = re.exec(d)) !== null) {
    if (m[1] === 'Z') {
      cmds.push({ c: 'Z' })
      continue
    }
    const nums = m[2].split(/[\s,]+/).filter(Boolean).map(Number)
    for (let i = 0; i + 1 < nums.length; i += 2) {
      cmds.push({ c: m[1] === 'M' && i === 0 ? 'M' : 'L', x: nums[i], y: nums[i + 1] })
    }
  }
  return cmds
}

const PROVINCES = chinaMapData.map((p) => {
  const cmds = parsePath(p.d)
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  cmds.forEach((c) => {
    if (c.c === 'Z') return
    if (c.x < minX) minX = c.x
    if (c.y < minY) minY = c.y
    if (c.x > maxX) maxX = c.x
    if (c.y > maxY) maxY = c.y
  })
  return { name: p.name, cmds, bbox: { minX, minY, maxX, maxY, area: (maxX - minX) * (maxY - minY) } }
})

const FILL_DEFAULT = 'rgba(212, 165, 116, 0.15)'
const FILL_VISITED = 'rgba(125, 140, 92, 0.6)'
const FILL_PLANNED = 'rgba(201, 162, 39, 0.5)'
const FILL_SELECTED = 'rgba(193, 102, 107, 0.45)'
const STROKE = 'rgba(92, 61, 46, 0.25)'
const STROKE_SELECTED = '#5c3d2e'

Page({
  data: {
    loading: true,
    tab: 'all', // all | visited | planned
    filterProvince: '',
    groups: [],
    provinceNames: PROVINCES.map((p) => p.name),
    // 编辑弹层
    showEditor: false,
    editingId: null,
    saving: false,
    form: { title: '', provinceIndex: -1, city: '', date: '', status: 1, weather: '', note: '', cover: '' },
    // 详情弹层
    showDetail: false,
    detail: null
  },

  onLoad() {
    if (!getApp().isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.loadAll()
  },

  onShow() {
    const tb = typeof this.getTabBar === 'function' && this.getTabBar()
    if (tb) tb.setData({ selected: 1 })
  },

  onReady() {
    this.initMap()
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh())
  },

  async loadAll() {
    try {
      const [list, mapSummary] = await Promise.all([
        api.request({ path: '/travels' }),
        api.request({ path: '/travels/map' })
      ])
      this._list = (list || []).map((t) => ({
        ...t,
        coverUrl: api.fullUrl(t.cover),
        dateText: formatDate(t.travelDate),
        seasonText: t.season || seasonOf(t.travelDate) || '春'
      }))
      this._mapSummary = mapSummary || []
      this.setData({ loading: false })
      this.buildGroups()
      this.drawMap()
    } catch (_e) {
      this.setData({ loading: false })
    }
  },

  /** 按 年份 → 季节 分组（日期倒序） */
  buildGroups() {
    const { tab, filterProvince } = this.data
    let list = this._list || []
    if (tab === 'visited') list = list.filter((t) => t.status === 1)
    if (tab === 'planned') list = list.filter((t) => t.status === 0)
    if (filterProvince) list = list.filter((t) => t.province === filterProvince)
    list = list.slice().sort((a, b) => String(b.travelDate || '').localeCompare(String(a.travelDate || '')))

    const yearMap = new Map()
    list.forEach((t) => {
      const year = String(t.travelDate || '').slice(0, 4) || '未定日期'
      if (!yearMap.has(year)) yearMap.set(year, { year, seasonMap: new Map() })
      const yg = yearMap.get(year)
      if (!yg.seasonMap.has(t.seasonText)) yg.seasonMap.set(t.seasonText, [])
      yg.seasonMap.get(t.seasonText).push(t)
    })
    const groups = [...yearMap.values()].map((yg) => ({
      year: yg.year,
      seasons: [...yg.seasonMap.entries()].map(([name, items]) => ({ name, items }))
    }))
    this.setData({ groups })
  },

  onTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    this.buildGroups()
  },

  clearProvince() {
    this.setData({ filterProvince: '' })
    this.buildGroups()
    this.drawMap()
  },

  /* ================= 地图 ================= */

  initMap() {
    this.createSelectorQuery()
      .select('#chinaMap')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) return
        const canvas = res[0].node
        const dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : wx.getSystemInfoSync().pixelRatio) || 2
        canvas.width = res[0].width * dpr
        canvas.height = res[0].height * dpr
        this._canvas = canvas
        this._ctx = canvas.getContext('2d')
        this._cssW = res[0].width
        this._cssH = res[0].height
        this.drawMap()
      })
  },

  /** 地图变换：viewBox 1000x780 等比缩放居中 */
  mapTransform() {
    const scale = Math.min(this._canvas.width / 1000, this._canvas.height / 780)
    return {
      scale,
      offX: (this._canvas.width - 1000 * scale) / 2,
      offY: (this._canvas.height - 780 * scale) / 2
    }
  },

  summaryOf(name) {
    const s = (this._mapSummary || []).find((m) => m.province === name)
    return s || { visited: 0, planned: 0 }
  },

  drawMap() {
    if (!this._ctx) return
    const ctx = this._ctx
    const { scale, offX, offY } = this.mapTransform()
    const { filterProvince } = this.data
    ctx.clearRect(0, 0, this._canvas.width, this._canvas.height)

    PROVINCES.forEach((p) => {
      const s = this.summaryOf(p.name)
      const selected = filterProvince === p.name
      let fill = FILL_DEFAULT
      if (s.visited > 0) fill = FILL_VISITED
      else if (s.planned > 0) fill = FILL_PLANNED
      if (selected) fill = FILL_SELECTED

      ctx.beginPath()
      p.cmds.forEach((c) => {
        if (c.c === 'Z') ctx.closePath()
        else if (c.c === 'M') ctx.moveTo(offX + c.x * scale, offY + c.y * scale)
        else ctx.lineTo(offX + c.x * scale, offY + c.y * scale)
      })
      ctx.fillStyle = fill
      ctx.fill()
      ctx.lineWidth = selected ? 2 : 1
      ctx.strokeStyle = selected ? STROKE_SELECTED : STROKE
      ctx.stroke()
    })
  },

  /** 点击命中：viewBox 坐标下包围盒粗判，重叠时取包围盒最小的省（处理北京/天津嵌在河北内） */
  onMapTap(e) {
    if (!this._canvas) return
    const scale = Math.min(this._cssW / 1000, this._cssH / 780)
    const offX = (this._cssW - 1000 * scale) / 2
    const offY = (this._cssH - 780 * scale) / 2
    const vx = (e.detail.x - offX) / scale
    const vy = (e.detail.y - offY) / scale

    let hit = null
    PROVINCES.forEach((p) => {
      const b = p.bbox
      if (vx >= b.minX && vx <= b.maxX && vy >= b.minY && vy <= b.maxY) {
        if (!hit || b.area < hit.bbox.area) hit = p
      }
    })
    if (!hit) return
    const filterProvince = this.data.filterProvince === hit.name ? '' : hit.name
    this.setData({ filterProvince })
    this.buildGroups()
    this.drawMap()
  },

  /* ================= 新建 / 编辑 ================= */

  openCreate() {
    this.setData({
      showEditor: true,
      editingId: null,
      form: { title: '', provinceIndex: -1, city: '', date: '', status: 1, weather: '', note: '', cover: '' }
    })
  },

  openEdit() {
    const t = this.data.detail
    if (!t) return
    this.setData({
      showDetail: false,
      showEditor: true,
      editingId: t.id,
      form: {
        title: t.title || '',
        provinceIndex: Math.max(0, this.data.provinceNames.indexOf(t.province)),
        city: t.city || '',
        date: formatDate(t.travelDate),
        status: t.status != null ? t.status : 1,
        weather: t.weather || '',
        note: t.note || '',
        cover: t.cover || '',
        coverUrl: t.coverUrl || api.fullUrl(t.cover)
      }
    })
  },

  closeEditor() {
    this.setData({ showEditor: false })
  },

  onFormInput(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ [`form.${key}`]: e.detail.value })
  },

  onProvinceChange(e) {
    this.setData({ 'form.provinceIndex': Number(e.detail.value) })
  },

  onDateChange(e) {
    this.setData({ 'form.date': e.detail.value })
  },

  onStatusPick(e) {
    this.setData({ 'form.status': Number(e.currentTarget.dataset.status) })
  },

  async chooseCover() {
    try {
      const urls = await api.chooseAndUpload(1)
      if (urls[0]) this.setData({ 'form.cover': urls[0], 'form.coverUrl': api.fullUrl(urls[0]) })
    } catch (_e) {
      /* 用户取消 */
    }
  },

  async submitForm() {
    const { form, editingId, provinceNames } = this.data
    if (!form.title.trim()) return toast('标题不能为空')
    if (form.provinceIndex < 0) return toast('请选择省份')
    if (!form.city.trim()) return toast('请填写城市')
    const data = {
      title: form.title.trim(),
      province: provinceNames[form.provinceIndex],
      city: form.city.trim(),
      travelDate: form.date || null,
      season: form.date ? seasonOf(form.date) : null,
      weather: form.weather.trim() || null,
      note: form.note.trim() || null,
      cover: form.cover || null,
      status: form.status
    }
    this.setData({ saving: true })
    try {
      if (editingId) {
        await api.request({ path: `/travels/${editingId}`, method: 'PUT', data })
        toast('已更新')
      } else {
        await api.request({ path: '/travels', method: 'POST', data })
        toast('足迹已记录')
      }
      this.setData({ showEditor: false, saving: false })
      this.loadAll()
    } catch (_e) {
      this.setData({ saving: false })
    }
  },

  /* ================= 详情 ================= */

  async openDetail(e) {
    const id = e.currentTarget.dataset.id
    try {
      const t = await api.request({ path: `/travels/${id}` })
      this.setData({
        showDetail: true,
        detail: {
          ...t,
          coverUrl: api.fullUrl(t.cover),
          dateText: formatDate(t.travelDate),
          seasonText: t.season || seasonOf(t.travelDate),
          photos: (t.photos || []).map((p) => ({ ...p, url: api.fullUrl(p.photoUrl) }))
        }
      })
    } catch (_e) {
      /* 错误已 toast */
    }
  },

  closeDetail() {
    this.setData({ showDetail: false, detail: null })
  },

  async addPhotos() {
    const { detail } = this.data
    if (!detail) return
    try {
      const urls = await api.chooseAndUpload(9)
      for (const url of urls) {
        await api.request({ path: `/travels/${detail.id}/photos`, method: 'POST', data: { photoUrl: url }, loading: false })
      }
      if (urls.length) {
        toast('照片已添加')
        this.refreshDetail(detail.id)
        this.loadAll()
      }
    } catch (_e) {
      /* 用户取消或失败 */
    }
  },

  delPhoto(e) {
    const { detail } = this.data
    const photoId = e.currentTarget.dataset.id
    if (!detail) return
    wx.showModal({
      title: '删除照片',
      content: '确定删除这张照片吗？',
      confirmColor: '#a34f54',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/travels/${detail.id}/photos/${photoId}`, method: 'DELETE' })
          toast('已删除')
          this.refreshDetail(detail.id)
        } catch (_err) {
          /* 错误已 toast */
        }
      }
    })
  },

  async refreshDetail(id) {
    try {
      const t = await api.request({ path: `/travels/${id}`, loading: false })
      this.setData({
        detail: {
          ...t,
          coverUrl: api.fullUrl(t.cover),
          dateText: formatDate(t.travelDate),
          seasonText: t.season || seasonOf(t.travelDate),
          photos: (t.photos || []).map((p) => ({ ...p, url: api.fullUrl(p.photoUrl) }))
        }
      })
    } catch (_e) {
      /* 忽略 */
    }
  },

  deleteTravel() {
    const { detail } = this.data
    if (!detail) return
    wx.showModal({
      title: '删除足迹',
      content: `确定删除「${detail.title}」吗？照片也会一起删除。`,
      confirmColor: '#a34f54',
      success: async (r) => {
        if (!r.confirm) return
        try {
          await api.request({ path: `/travels/${detail.id}`, method: 'DELETE' })
          toast('已删除')
          this.setData({ showDetail: false, detail: null })
          this.loadAll()
        } catch (_e) {
          /* 错误已 toast */
        }
      }
    })
  },

  previewPhoto(e) {
    const { detail } = this.data
    if (!detail) return
    wx.previewImage({
      current: e.currentTarget.dataset.url,
      urls: detail.photos.map((p) => p.url)
    })
  },

  noop() {}
})
