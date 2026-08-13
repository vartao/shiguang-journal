// 小程序功能元数据：入口、导航与页面守卫共用，避免多处顺序和路径漂移。
const FEATURES = [
  { key: 'notes', label: '时间线', icon: '🎞️', path: '/pages/timeline/timeline', tab: true, tabIndex: 0 },
  { key: 'albums', label: '相册墙', icon: '📸', path: '/pages/albums/albums' },
  { key: 'anniversary', label: '纪念日', icon: '💑', path: '/pages/anniversary/anniversary' },
  { key: 'surprise', label: '惊喜盒', icon: '🎁', path: '/pages/surprise/surprise' },
  { key: 'wishes', label: '心愿池', icon: '✨', path: '/pages/wishes/wishes' },
  { key: 'stats', label: '数据', icon: '📊', path: '/pages/stats/stats' },
  { key: 'travels', label: '足迹', icon: '🗺️', path: '/pages/travels/travels', tab: true, tabIndex: 1 },
  { key: 'plans', label: '行程', icon: '🧳', path: '/pages/plans/plans' },
  { key: 'menu', label: '吃什么', icon: '🍚', path: '/pages/menu/menu' },
  { key: 'capsule', label: '胶囊', icon: '⏳', path: '/pages/capsule/capsule' },
  { key: 'logs', label: '小事记', icon: '📖', path: '/pages/logs/logs' },
  { key: 'board', label: '留言板', icon: '💬', path: '/pages/board/board', tab: true, tabIndex: 2 }
]

const PAGE_FEATURES = {
  'pages/timeline/timeline': 'notes',
  'pages/write/write': 'notes',
  'pages/albums/albums': 'albums',
  'pages/album-detail/album-detail': 'albums',
  'pages/anniversary/anniversary': 'anniversary',
  'pages/surprise/surprise': 'surprise',
  'pages/wishes/wishes': 'wishes',
  'pages/stats/stats': 'stats',
  'pages/travels/travels': 'travels',
  'pages/plans/plans': 'plans',
  'pages/plan-detail/plan-detail': 'plans',
  'pages/menu/menu': 'menu',
  'pages/capsule/capsule': 'capsule',
  'pages/logs/logs': 'logs',
  'pages/board/board': 'board'
}

function featureForRoute(route) {
  return PAGE_FEATURES[String(route || '').replace(/^\//, '')] || ''
}

module.exports = { FEATURES, PAGE_FEATURES, featureForRoute }
