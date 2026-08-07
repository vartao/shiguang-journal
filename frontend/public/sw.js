const CACHE_NAME = 'qinglv-v2'
const OFFLINE_URL = '/'

const PRECACHE_ASSETS = [
  '/',
  '/index.html',
  '/manifest.json'
]

// 安装：预缓存关键资源
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(PRECACHE_ASSETS))
      .then(() => self.skipWaiting())
  )
})

// 激活：清理旧缓存
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  )
})

// 拦截请求：网络优先，缓存兜底
self.addEventListener('fetch', (event) => {
  // 跳过非 GET 请求
  if (event.request.method !== 'GET') return

  const url = new URL(event.request.url)

  // API 请求：网络优先
  if (url.pathname.startsWith('/api/')) {
    event.respondWith(
      fetch(event.request)
        .then(response => {
          // 成功响应存入缓存
          const clone = response.clone()
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone))
          return response
        })
        .catch(() => caches.match(event.request))
    )
    return
  }

  // 上传的图片：缓存优先
  if (url.pathname.startsWith('/uploads/')) {
    event.respondWith(
      caches.match(event.request).then(cached => {
        if (cached) return cached
        return fetch(event.request).then(response => {
          const clone = response.clone()
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone))
          return response
        })
      })
    )
    return
  }

  // 静态资源：缓存优先，网络兜底
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached
      return fetch(event.request).then(response => {
        // 只缓存同源的成功响应
        if (response.ok && url.origin === location.origin) {
          const clone = response.clone()
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone))
        }
        return response
      }).catch(() => {
        // 导航请求回退到离线页
        if (event.request.mode === 'navigate') {
          return caches.match(OFFLINE_URL)
        }
      })
    })
  )
})
