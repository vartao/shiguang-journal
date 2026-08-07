import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCoupleInfo, bindPartner, createCouple } from '@/api/auth'
import { readStoredJson, writeStoredJson } from '@/utils/storage'
import { differenceInCalendarDays } from '@/utils/date'

export function normalizeCouple(data) {
  if (!data) return null
  return {
    ...data,
    // 兼容 startDate / togetherDate
    startDate: data.startDate || data.togetherDate || '',
    togetherDate: data.togetherDate || data.startDate || '',
    togetherDays: data.togetherDays ?? null,
    partnerName: data.partnerName || data.user2Nickname || data.user1Nickname || ''
  }
}

export const useCoupleStore = defineStore('couple', () => {
  const coupleInfo = ref(readStoredJson('ql_couple'))
  const loading = ref(false)

  const hasCouple = computed(() => !!coupleInfo.value)
  const coupleName = computed(() => coupleInfo.value?.name || '我们的小窝')
  const startDate = computed(() => coupleInfo.value?.startDate || coupleInfo.value?.togetherDate || '')
  const partnerName = computed(() => coupleInfo.value?.partnerName || '')
  const inviteCode = computed(() => coupleInfo.value?.inviteCode || '')

  const daysTogether = computed(() => {
    if (coupleInfo.value?.togetherDays != null) {
      return Number(coupleInfo.value.togetherDays) || 0
    }
    if (!startDate.value) return 0
    const days = differenceInCalendarDays(new Date(), startDate.value)
    return Number.isNaN(days) ? 0 : Math.max(0, days)
  })

  function setCoupleInfo(info) {
    const normalized = normalizeCouple(info)
    coupleInfo.value = normalized
    writeStoredJson('ql_couple', normalized)
  }

  async function fetchCoupleInfo() {
    loading.value = true
    try {
      const res = await getCoupleInfo()
      if (res.data) setCoupleInfo(res.data)
      else clearCouple()
      return res.data
    } catch (e) {
      console.warn('获取情侣信息失败:', e)
      return null
    } finally {
      loading.value = false
    }
  }

  async function bind(code) {
    const res = await bindPartner({ inviteCode: code })
    if (res.data) {
      setCoupleInfo(res.data)
    }
    return res
  }

  async function create(name, date) {
    const res = await createCouple({ name, startDate: date, togetherDate: date })
    if (res.data) {
      setCoupleInfo(res.data)
    }
    return res
  }

  function clearCouple() {
    coupleInfo.value = null
    localStorage.removeItem('ql_couple')
  }

  return {
    coupleInfo,
    loading,
    hasCouple,
    coupleName,
    startDate,
    partnerName,
    inviteCode,
    daysTogether,
    setCoupleInfo,
    fetchCoupleInfo,
    bind,
    create,
    clearCouple
  }
})
