import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getProfile } from '@/api/auth'
import router from '@/router'
import { readStoredJson, writeStoredJson } from '@/utils/storage'

export function normalizeUser(payload) {
  if (!payload) return null
  // 后端 LoginVO: { token, user: {...}, userId, nickname... }
  // 或 profile 直接返回 user 对象
  const raw = payload.user || payload
  return {
    id: raw.id ?? raw.userId ?? payload.userId ?? null,
    userId: raw.userId ?? raw.id ?? payload.userId ?? null,
    username: raw.username ?? payload.username ?? '',
    nickname: raw.nickname ?? payload.nickname ?? '',
    avatar: raw.avatar ?? payload.avatar ?? '',
    coupleId: raw.coupleId ?? payload.coupleId ?? null,
    role: raw.role ?? payload.role ?? '',
    gender: raw.gender ?? payload.gender ?? 0
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('ql_token') || '')
  const userInfo = ref(readStoredJson('ql_user'))

  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '恋人')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const role = computed(() => userInfo.value?.role || '')
  const coupleId = computed(() => userInfo.value?.coupleId || null)

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('ql_token', newToken)
  }

  function setUserInfo(info) {
    userInfo.value = info
    writeStoredJson('ql_user', info)
  }

  async function login(credentials) {
    const res = await apiLogin(credentials)
    const data = res.data || {}
    setToken(data.token)
    setUserInfo(normalizeUser(data))
    return res
  }

  async function register(form) {
    const res = await apiRegister(form)
    const data = res.data || {}
    if (data.token) {
      setToken(data.token)
      setUserInfo(normalizeUser(data))
    }
    return res
  }

  async function fetchProfile() {
    try {
      const res = await getProfile()
      const info = normalizeUser(res.data)
      setUserInfo(info)
      return info
    } catch (e) {
      logout()
      throw e
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('ql_token')
    localStorage.removeItem('ql_user')
    localStorage.removeItem('ql_couple')
    router.push({ name: 'Login' })
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    nickname,
    avatar,
    role,
    coupleId,
    setToken,
    setUserInfo,
    login,
    register,
    fetchProfile,
    logout
  }
})
