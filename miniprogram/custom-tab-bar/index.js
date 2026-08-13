// 自定义底部导航：功能开关与“我的”宫格共用同一份元数据。
const { FEATURES } = require('../utils/features')

const HOME_TAB = { path: '/pages/mine/mine', label: '我的', icon: '🏠', tabIndex: 3, key: '' }

Component({
  data: {
    selected: 0,
    tabs: [],
    tabsRight: [],
    showWrite: true
  },

  lifetimes: {
    attached() {
      this.syncFeatures()
    }
  },

  pageLifetimes: {
    show() {
      this.syncFeatures()
    }
  },

  methods: {
    syncFeatures() {
      const app = getApp()
      const tabFeatures = FEATURES.filter((item) => item.tab)
      const visible = (key) => !key || app.isFeatureOn(key)
      this.setData({
        tabs: tabFeatures.filter((item) => item.tabIndex < 2 && visible(item.key)),
        tabsRight: tabFeatures.filter((item) => item.tabIndex >= 2 && visible(item.key)).concat(HOME_TAB),
        showWrite: visible('notes')
      })
    },

    onTap(e) {
      const index = Number(e.currentTarget.dataset.index)
      const all = this.data.tabs.concat(this.data.tabsRight)
      const target = all.find((item) => item.tabIndex === index)
      if (!target) return
      wx.vibrateShort({ type: 'light' })
      wx.switchTab({ url: target.path })
    },

    onWrite() {
      if (!getApp().isFeatureOn('notes')) {
        wx.showToast({ title: '时间线功能已关闭', icon: 'none' })
        return
      }
      wx.vibrateShort({ type: 'light' })
      wx.navigateTo({ url: '/pages/write/write' })
    }
  }
})
