import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

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
    meta: { title: '相册墙', requiresAuth: true }
  },
  {
    path: '/albums/:id',
    name: 'AlbumDetail',
    component: () => import('@/views/AlbumDetail.vue'),
    meta: { title: '相册详情', requiresAuth: true }
  },
  {
    path: '/anniversary',
    name: 'Anniversary',
    component: () => import('@/views/AnniversaryView.vue'),
    meta: { title: '纪念日', requiresAuth: true }
  },
  {
    path: '/surprise',
    name: 'Surprise',
    component: () => import('@/views/SurpriseView.vue'),
    meta: { title: '惊喜盒子', requiresAuth: true }
  },
  {
    path: '/surprise/:id',
    name: 'SurpriseDetail',
    component: () => import('@/views/SurpriseDetail.vue'),
    meta: { title: '拆惊喜', requiresAuth: true }
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
    meta: { title: '心愿许愿池', requiresAuth: true }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/views/StatsView.vue'),
    meta: { title: '恋爱数据', requiresAuth: true }
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
  } else if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
