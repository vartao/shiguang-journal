import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getFeatures, updateFeature } from '@/api/feature'
import { readStoredJson, writeStoredJson } from '@/utils/storage'

/** 功能项元数据：key → 导航/设置页展示信息（顺序即导航顺序） */
export const FEATURE_META = [
  { key: 'notes', label: '时间线', icon: '🎞️', path: '/', desc: '用笔记记录每一天' },
  { key: 'albums', label: '相册墙', icon: '📸', path: '/albums', desc: '照片与相册' },
  { key: 'anniversary', label: '纪念日', icon: '💑', path: '/anniversary', desc: '重要的日子倒计时' },
  { key: 'surprise', label: '惊喜盒', icon: '🎁', path: '/surprise', desc: '藏一份心意等 TA 拆' },
  { key: 'wishes', label: '心愿池', icon: '✨', path: '/wishes', desc: '想一起做的事' },
  { key: 'stats', label: '数据', icon: '📊', path: '/stats', desc: '恋爱数据统计' },
  { key: 'travels', label: '足迹', icon: '🗺️', path: '/travels', desc: '点亮去过的地图' },
  { key: 'plans', label: '行程', icon: '🧳', path: '/plans', desc: '出发前的计划' },
  { key: 'menu', label: '吃什么', icon: '🍚', path: '/menu', desc: '菜品池与今日菜单' },
  { key: 'capsule', label: '胶囊', icon: '⏳', path: '/capsule', desc: '写给未来的信' },
  { key: 'logs', label: '小事记', icon: '📖', path: '/logs', desc: '一起做过的事打卡' },
  { key: 'board', label: '留言板', icon: '💬', path: '/board', desc: '两个人的悄悄话' }
]

const DEFAULT_ENABLED = Object.fromEntries(FEATURE_META.map((f) => [f.key, true]))

export const useFeatureStore = defineStore('feature', () => {
  const enabled = ref({ ...DEFAULT_ENABLED, ...(readStoredJson('ql_features') || {}) })
  const loaded = ref(false)

  function isOn(key) {
    if (!key) return true
    return enabled.value[key] !== false
  }

  function applyMap(map) {
    if (!map) return
    enabled.value = { ...DEFAULT_ENABLED, ...map }
    writeStoredJson('ql_features', enabled.value)
  }

  async function fetchFeatures() {
    try {
      const res = await getFeatures()
      applyMap(res.data)
      loaded.value = true
    } catch {
      // 网络失败沿用本地缓存，入口不闪动
    }
    return enabled.value
  }

  async function toggle(key, value) {
    const res = await updateFeature(key, value)
    applyMap(res.data)
    return enabled.value
  }

  function reset() {
    enabled.value = { ...DEFAULT_ENABLED }
    localStorage.removeItem('ql_features')
    loaded.value = false
  }

  return { enabled, loaded, isOn, fetchFeatures, toggle, reset }
})
