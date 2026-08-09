// 自定义底部导航：时间线 / 足迹 / ✏️写笔记 / 留言板 / 我的
// 各 tab 页在 onShow 中调用 this.getTabBar().setData({ selected: N })
// N: 0=时间线 1=足迹 2=留言板 3=我的（写笔记不占位，为跳转按钮）
Component({
  data: {
    selected: 0,
    // 中心按钮左侧的 tab
    tabs: [
      { path: '/pages/timeline/timeline', label: '时间线', icon: '🎞️', tabIndex: 0 },
      { path: '/pages/travels/travels', label: '足迹', icon: '🗺️', tabIndex: 1 }
    ],
    // 中心按钮右侧的 tab
    tabsRight: [
      { path: '/pages/board/board', label: '留言板', icon: '💬', tabIndex: 2 },
      { path: '/pages/mine/mine', label: '我的', icon: '🏠', tabIndex: 3 }
    ]
  },

  methods: {
    onTap(e) {
      const index = e.currentTarget.dataset.index
      const all = this.data.tabs.concat(this.data.tabsRight)
      const target = all.find((t) => t.tabIndex === index)
      if (!target) return
      wx.vibrateShort({ type: 'light' })
      wx.switchTab({ url: target.path })
    },

    onWrite() {
      wx.vibrateShort({ type: 'light' })
      wx.navigateTo({ url: '/pages/write/write' })
    }
  }
})
