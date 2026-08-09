import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFeatureStore } from '@/stores/feature'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '我们的时间线', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/write',
    name: 'NoteEditor',
    component: () => import('@/views/NoteEditor.vue'),
    meta: { title: '写一篇笔记', requiresAuth: true }
  },
  {
    path: '/albums',
    name: 'Albums',
    component: () => import('@/views/AlbumView.vue'),
    meta: { title: '相册墙', requiresAuth: true, feature: 'albums' }
  },
  {
    path: '/albums/:id',
    name: 'AlbumDetail',
    component: () => import('@/views/AlbumDetail.vue'),
    meta: { title: '相册详情', requiresAuth: true, feature: 'albums' }
  },
  {
    path: '/anniversary',
    name: 'Anniversary',
    component: () => import('@/views/AnniversaryView.vue'),
    meta: { title: '纪念日', requiresAuth: true, feature: 'anniversary' }
  },
  {
    path: '/surprise',
    name: 'Surprise',
    component: () => import('@/views/SurpriseView.vue'),
    meta: { title: '惊喜盒子', requiresAuth: true, feature: 'surprise' }
  },
  {
    path: '/surprise/:id',
    name: 'SurpriseDetail',
    component: () => import('@/views/SurpriseDetail.vue'),
    meta: { title: '拆惊喜', requiresAuth: true, feature: 'surprise' }
  },
  {
    path: '/space',
    name: 'Space',
    component: () => import('@/views/SpaceView.vue'),
    meta: { title: '我们的小窝', requiresAuth: true }
  },
  {
    path: '/wishes',
    name: 'Wishes',
    component: () => import('@/views/WishListView.vue'),
    meta: { title: '心愿许愿池', requiresAuth: true, feature: 'wishes' }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/views/StatsView.vue'),
    meta: { title: '恋爱数据', requiresAuth: true, feature: 'stats' }
  },
  {
    path: '/travels',
    name: 'Travels',
    component: () => import('@/views/TravelView.vue'),
    meta: { title: '我们的足迹', requiresAuth: true, feature: 'travels' }
  },
  {
    path: '/plans',
    name: 'Plans',
    component: () => import('@/views/PlansView.vue'),
    meta: { title: '行程计划', requiresAuth: true, feature: 'plans' }
  },
  {
    path: '/plans/:id',
    name: 'PlanDetail',
    component: () => import('@/views/PlanDetail.vue'),
    meta: { title: '行程详情', requiresAuth: true, feature: 'plans' }
  },
  {
    path: '/menu',
    name: 'Menu',
    component: () => import('@/views/MenuView.vue'),
    meta: { title: '今天吃什么', requiresAuth: true, feature: 'menu' }
  },
  {
    path: '/capsule',
    name: 'Capsule',
    component: () => import('@/views/CapsuleView.vue'),
    meta: { title: '时间胶囊', requiresAuth: true, feature: 'capsule' }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('@/views/LogsView.vue'),
    meta: { title: '小事记', requiresAuth: true, feature: 'logs' }
  },
  {
    path: '/board',
    name: 'Board',
    component: () => import('@/views/BoardView.vue'),
    meta: { title: '留言板', requiresAuth: true, feature: 'board' }
  },
  {
    path: '/export',
    name: 'Export',
    component: () => import('@/views/ExportView.vue'),
    meta: { title: '导出手账', requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.title) {
    document.title = `${to.meta.title} - 拾光手账`
  }

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'Home' })
    return
  }
  // 功能被关闭的页面直接访问时回首页
  if (to.meta.feature && !useFeatureStore().isOn(to.meta.feature)) {
    next({ name: 'Home' })
    return
  }
  next()
})

export default router
