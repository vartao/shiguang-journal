<template>
  <div class="travel-page">
    <div class="container">
      <header class="travel-header anim-fade-in-up">
        <h1 class="travel-header__title">我们的足迹</h1>
        <p class="travel-header__subtitle">一起走过的地方，都被点亮</p>
        <div class="travel-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <section class="travel-map-card anim-fade-in-up stagger-2">
        <ChinaMap :summary="mapSummary" :selected="selectedProvince" @select="handleMapSelect" />
      </section>

      <div class="travel-toolbar anim-fade-in-up stagger-3">
        <div class="travel-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            class="travel-tab"
            :class="{ active: activeTab === tab.value }"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
        <button class="add-travel-btn" @click="openCreate">
          <span class="add-icon">✈️</span><span>记录足迹</span>
        </button>
      </div>

      <div v-if="selectedProvince" class="province-filter anim-fade-in-up">
        <span>正在看「{{ selectedProvince }}」的足迹</span>
        <button class="province-filter__clear" @click="selectedProvince = ''">× 清除筛选</button>
      </div>

      <LoadingState v-if="loading" message="正在展开地图…" />
      <ErrorState v-else-if="loadError" message="足迹暂时没有加载成功。" @retry="fetchData" />
      <template v-else-if="filteredTravels.length">
        <div v-for="yearGroup in groupedTravels" :key="yearGroup.year" class="travel-year-group">
          <h2 class="travel-year">{{ yearGroup.year }}</h2>
          <div v-for="seasonGroup in yearGroup.seasons" :key="seasonGroup.season" class="travel-season-group">
            <span class="travel-season-tag">{{ seasonGroup.season }}</span>
            <div class="travel-grid">
              <article
                v-for="(travel, idx) in seasonGroup.travels"
                :key="travel.id"
                class="travel-card"
                :style="{ animationDelay: idx * 0.06 + 's' }"
                @click="openDetail(travel)"
              >
                <div class="travel-card__cover">
                  <img v-if="travel.cover" :src="travel.cover" :alt="travel.title" loading="lazy" />
                  <div v-else class="travel-card__cover-placeholder">{{ travel.province }}</div>
                  <span v-if="Number(travel.status) === 0" class="travel-card__badge">想去</span>
                  <span v-if="travel.photoCount" class="travel-card__photo-count">📷 {{ travel.photoCount }}</span>
                </div>
                <div class="travel-card__body">
                  <h3 class="travel-card__title">{{ travel.title }}</h3>
                  <p class="travel-card__meta">
                    {{ travel.city
                    }}<template v-if="travel.travelDate"> · {{ formatDate(travel.travelDate) }}</template>
                  </p>
                  <p v-if="travel.note" class="travel-card__note">{{ travel.note }}</p>
                </div>
              </article>
            </div>
          </div>
        </div>
      </template>
      <EmptyState
        v-else
        icon="🗺️"
        :title="emptyTitle"
        :message="selectedProvince ? '这个省份还没有点亮，去走走吧' : '记下第一段旅程，点亮地图上的第一块拼图'"
      >
        <button v-if="!selectedProvince" class="btn-vintage btn-vintage--red" @click="openCreate">
          记录第一段旅程
        </button>
      </EmptyState>
    </div>

    <!-- 新增/编辑足迹 -->
    <BaseModal
      v-model="showEditor"
      :title="editingId ? '编辑足迹' : '记录足迹'"
      eyebrow="FOOTPRINT"
      :busy="submitting"
      @close="resetEditor"
    >
      <form class="travel-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="travel-title">标题</label>
          <input
            id="travel-title"
            v-model="form.title"
            class="input-vintage"
            placeholder="这次旅行叫什么？"
            maxlength="200"
            required
          />
        </div>
        <div class="form-row">
          <div class="form-group form-group--half">
            <label for="travel-province">省份</label>
            <select id="travel-province" v-model="form.province" class="input-vintage" required>
              <option value="" disabled>选择省份</option>
              <option v-for="name in provinceNames" :key="name" :value="name">{{ name }}</option>
            </select>
          </div>
          <div class="form-group form-group--half">
            <label for="travel-city">城市</label>
            <input
              id="travel-city"
              v-model="form.city"
              class="input-vintage"
              placeholder="比如：大理"
              maxlength="50"
              required
            />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group form-group--half">
            <label for="travel-date">出行日期</label>
            <input id="travel-date" v-model="form.travelDate" class="input-vintage" type="date" />
          </div>
          <div class="form-group form-group--half">
            <label>状态</label>
            <div class="status-picker">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                type="button"
                class="status-chip"
                :class="{ active: form.status === option.value }"
                @click="form.status = option.value"
              >
                {{ option.icon }} {{ option.label }}
              </button>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label for="travel-weather">天气（可选）</label>
          <input
            id="travel-weather"
            v-model="form.weather"
            class="input-vintage"
            placeholder="比如：晴转多云"
            maxlength="20"
          />
        </div>
        <div class="form-group">
          <label for="travel-note">旅行手记</label>
          <textarea
            id="travel-note"
            v-model="form.note"
            class="input-vintage"
            rows="3"
            placeholder="记下这一天的心情…"
            maxlength="2000"
          ></textarea>
        </div>
        <div class="form-group">
          <label>封面照片</label>
          <ImageUploader v-model="form.coverImages" :max-count="1" category="travel" />
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn-vintage btn-vintage--outline" :disabled="submitting" @click="closeEditor">
          取消
        </button>
        <button
          type="button"
          class="btn-vintage btn-vintage--red"
          :disabled="!canSubmit || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '保存中…' : editingId ? '保存修改' : '点亮地图 ✈️' }}
        </button>
      </template>
    </BaseModal>

    <!-- 足迹详情 -->
    <BaseModal v-model="showDetail" class="travel-detail-modal" :title="detail?.title || '足迹详情'" eyebrow="JOURNEY">
      <div v-if="detailLoading" class="travel-detail-loading"><LoadingState message="正在翻开这一页…" /></div>
      <div v-else-if="detail" class="travel-detail">
        <div class="travel-detail__meta">
          <span class="travel-detail__place">📍 {{ detail.province }} · {{ detail.city }}</span>
          <span v-if="detail.travelDate" class="travel-detail__date">{{ formatDate(detail.travelDate) }}</span>
          <span v-if="detail.weather" class="travel-detail__weather">{{ detail.weather }}</span>
          <span class="travel-detail__status" :class="{ planned: Number(detail.status) === 0 }">
            {{ Number(detail.status) === 0 ? '想去' : '已去过' }}
          </span>
        </div>
        <p v-if="detail.note" class="travel-detail__note">{{ detail.note }}</p>

        <div v-if="detail.photos && detail.photos.length" class="travel-detail__photos">
          <div v-for="photo in detail.photos" :key="photo.id" class="travel-detail__photo">
            <img :src="photo.photoUrl" alt="旅行照片" loading="lazy" />
            <button class="travel-detail__photo-delete" title="删除照片" @click="handleDeletePhoto(photo)">×</button>
          </div>
        </div>

        <div class="travel-detail__upload">
          <ImageUploader
            v-model="detailUploadImages"
            :max-count="20"
            category="travel"
            @uploaded="handlePhotoUploaded"
          />
        </div>

        <div class="travel-detail__actions">
          <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="openEditFromDetail">
            编辑足迹
          </button>
          <button
            class="btn-vintage btn-vintage--outline btn-vintage--small travel-detail__delete"
            @click="handleDeleteTravel"
          >
            删除足迹
          </button>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  addTravelPhoto,
  createTravel,
  deleteTravel,
  deleteTravelPhoto,
  getTravelDetail,
  getTravelMap,
  getTravels,
  updateTravel
} from '@/api/travel'
import BaseModal from '@/components/BaseModal.vue'
import ChinaMap from '@/components/ChinaMap.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingState from '@/components/LoadingState.vue'
import chinaMapData from '@/components/chinaMapData'
import { requestConfirm } from '@/composables/confirmService'

const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const travels = ref([])
const mapList = ref([])
const activeTab = ref('all')
const selectedProvince = ref('')

const showEditor = ref(false)
const editingId = ref(null)
const showDetail = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailUploadImages = ref([])

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'visited', label: '已去过' },
  { value: 'planned', label: '想去' }
]
const statusOptions = [
  { value: 1, label: '已去过', icon: '✓' },
  { value: 0, label: '想去', icon: '♡' }
]
const seasonOrder = ['春', '夏', '秋', '冬']
const provinceNames = chinaMapData.map((p) => p.name)

const form = ref(createEmptyForm())

const mapSummary = computed(() => {
  const map = {}
  for (const item of mapList.value) {
    map[item.province] = { visited: item.visited, planned: item.planned }
  }
  return map
})

const filteredTravels = computed(() => {
  let list = travels.value
  if (activeTab.value === 'visited') list = list.filter((t) => Number(t.status) === 1)
  if (activeTab.value === 'planned') list = list.filter((t) => Number(t.status) === 0)
  if (selectedProvince.value) list = list.filter((t) => t.province === selectedProvince.value)
  return list
})

const groupedTravels = computed(() => {
  const yearMap = new Map()
  for (const travel of filteredTravels.value) {
    const year = travel.travelDate ? String(travel.travelDate).slice(0, 4) : '待定'
    const season = seasonOf(travel.travelDate)
    if (!yearMap.has(year)) yearMap.set(year, new Map())
    const seasonMap = yearMap.get(year)
    if (!seasonMap.has(season)) seasonMap.set(season, [])
    seasonMap.get(season).push(travel)
  }
  return [...yearMap.entries()]
    .sort((a, b) => (a[0] === '待定' ? 1 : b[0] === '待定' ? -1 : Number(b[0]) - Number(a[0])))
    .map(([year, seasonMap]) => ({
      year,
      seasons: [...seasonMap.entries()]
        .sort((a, b) => seasonOrder.indexOf(a[0]) - seasonOrder.indexOf(b[0]))
        .map(([season, list]) => ({ season, travels: list }))
    }))
})

const canSubmit = computed(() => form.value.title.trim() && form.value.province && form.value.city.trim())

const emptyTitle = computed(() => {
  if (selectedProvince.value) return selectedProvince.value + '还没有足迹'
  if (activeTab.value === 'visited') return '还没有去过的足迹'
  if (activeTab.value === 'planned') return '还没有想去的地方'
  return '地图还没有被点亮'
})

function createEmptyForm(source) {
  return {
    title: source?.title || '',
    province: source?.province || '',
    city: source?.city || '',
    travelDate: source?.travelDate ? String(source.travelDate).slice(0, 10) : '',
    status: source ? Number(source.status) : 1,
    weather: source?.weather || '',
    note: source?.note || '',
    coverImages: source?.cover ? [{ url: source.cover }] : []
  }
}

function seasonOf(dateStr) {
  if (!dateStr) return '未标注'
  const month = Number(String(dateStr).slice(5, 7))
  if (month >= 3 && month <= 5) return '春'
  if (month >= 6 && month <= 8) return '夏'
  if (month >= 9 && month <= 11) return '秋'
  return '冬'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const [y, m, d] = String(dateStr).slice(0, 10).split('-')
  return `${y}.${m}.${d}`
}

async function fetchData() {
  loading.value = true
  loadError.value = false
  try {
    const [travelRes, mapRes] = await Promise.all([getTravels(), getTravelMap()])
    travels.value = travelRes.data || []
    mapList.value = mapRes.data || []
  } catch {
    loadError.value = true
    travels.value = []
    mapList.value = []
  } finally {
    loading.value = false
  }
}

function handleMapSelect(name) {
  selectedProvince.value = name
}

function openCreate() {
  editingId.value = null
  form.value = createEmptyForm()
  showEditor.value = true
}

function openEdit(travel) {
  editingId.value = travel.id
  form.value = createEmptyForm(travel)
  showEditor.value = true
}

function resetEditor() {
  editingId.value = null
  form.value = createEmptyForm()
}

function closeEditor() {
  showEditor.value = false
  resetEditor()
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const payload = {
      title: form.value.title.trim(),
      province: form.value.province,
      city: form.value.city.trim(),
      travelDate: form.value.travelDate || null,
      status: form.value.status,
      weather: form.value.weather || null,
      note: form.value.note || null,
      cover: form.value.coverImages[0]?.url || null
    }
    if (editingId.value) {
      await updateTravel(editingId.value, payload)
    } else {
      await createTravel(payload)
    }
    closeEditor()
    await fetchData()
  } finally {
    submitting.value = false
  }
}

async function openDetail(travel) {
  showDetail.value = true
  detailLoading.value = true
  detail.value = travel
  detailUploadImages.value = []
  try {
    const res = await getTravelDetail(travel.id)
    detail.value = res.data || travel
  } catch {
    // 详情加载失败时保留列表数据，错误由拦截器提示
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetail() {
  if (!detail.value?.id) return
  const res = await getTravelDetail(detail.value.id)
  detail.value = res.data || detail.value
}

async function handlePhotoUploaded(data) {
  try {
    const url = typeof data === 'string' ? data : data?.url || data?.photoUrl
    if (!url || String(url).startsWith('blob:')) return
    await addTravelPhoto(detail.value.id, url)
    detailUploadImages.value = []
    await refreshDetail()
    await fetchData()
  } catch {
    // 错误由拦截器提示
  }
}

async function handleDeletePhoto(photo) {
  const confirmed = await requestConfirm({
    title: '删除照片',
    message: '确定要删除这张照片吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  try {
    await deleteTravelPhoto(detail.value.id, photo.id)
    await refreshDetail()
    await fetchData()
  } catch {
    // 保留界面状态；错误由请求拦截器提示
  }
}

function openEditFromDetail() {
  const travel = detail.value
  showDetail.value = false
  openEdit(travel)
}

async function handleDeleteTravel() {
  const travel = detail.value
  const confirmed = await requestConfirm({
    title: '删除足迹',
    message: '确定要删除“' + (travel.title || '') + '”吗？其中的照片也会一起删除。',
    confirmText: '删除'
  })
  if (!confirmed) return
  try {
    await deleteTravel(travel.id)
    showDetail.value = false
    detail.value = null
    await fetchData()
  } catch {
    // 错误由请求拦截器提示
  }
}

onMounted(fetchData)
</script>

<style scoped>
.travel-page {
  padding: 32px 0 60px;
}

.travel-header {
  text-align: center;
  margin-bottom: 28px;
  position: relative;
}

.travel-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.travel-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.travel-header__deco {
  margin-top: 12px;
}

.star {
  display: inline-block;
  color: var(--mustard);
  margin: 0 8px;
  animation: twinkle 2s ease-in-out infinite;
}
.s1 {
  font-size: 16px;
  animation-delay: 0s;
}
.s2 {
  font-size: 22px;
  animation-delay: 0.4s;
}
.s3 {
  font-size: 14px;
  animation-delay: 0.8s;
}

/* 地图卡片 */
.travel-map-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 20px;
  margin-bottom: 24px;
  position: relative;
}

.travel-map-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--paper-texture);
  opacity: 0.25;
  pointer-events: none;
}

/* 工具栏 */
.travel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.travel-tabs {
  display: flex;
  gap: 8px;
}

.travel-tab {
  padding: 8px 24px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  color: var(--text-muted);
  background: transparent;
  border: var(--border-vintage);
  border-radius: 20px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.travel-tab:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.1);
}

.travel-tab.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

.add-travel-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--olive), var(--olive-light));
  border-radius: var(--radius-sm);
  box-shadow: 0 3px 12px rgba(var(--olive-rgb), 0.3);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.add-travel-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(var(--olive-rgb), 0.4);
}

/* 省份筛选提示条 */
.province-filter {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 10px 16px;
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.12);
  border: 1px dashed rgba(var(--brown-rgb), 0.25);
  border-radius: var(--radius-sm);
}

.province-filter__clear {
  padding: 4px 12px;
  font-size: 12px;
  font-family: var(--font-display);
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
  border: 1px solid rgba(var(--red-rgb), 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.province-filter__clear:hover {
  background: rgba(var(--red-rgb), 0.18);
}

/* 年份 / 季节分组 */
.travel-year-group {
  margin-bottom: 32px;
}

.travel-year {
  font-family: var(--font-handwrite);
  font-size: 30px;
  color: var(--brown-deep);
  margin-bottom: 12px;
}

.travel-season-group {
  margin-bottom: 20px;
}

.travel-season-tag {
  display: inline-block;
  padding: 3px 14px;
  margin-bottom: 12px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--olive);
  background: rgba(var(--olive-rgb), 0.12);
  border: 1px solid rgba(var(--olive-rgb), 0.3);
  border-radius: 14px;
}

/* 足迹卡片 */
.travel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.travel-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: all var(--transition-normal);
  animation: fade-in-up 0.4s ease both;
}

.travel-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lifted);
}

.travel-card__cover {
  position: relative;
  height: 160px;
  overflow: hidden;
}

.travel-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.travel-card__cover-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: rgba(var(--cream-static-rgb), 0.9);
  background: linear-gradient(145deg, rgba(var(--kraft-rgb), 0.7), rgba(var(--brown-rgb), 0.55));
}

.travel-card__badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 10px;
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  color: var(--mustard);
  background: rgba(var(--cream-static-rgb), 0.85);
  border: 1px solid rgba(var(--mustard-rgb), 0.5);
  border-radius: 12px;
}

.travel-card__photo-count {
  position: absolute;
  bottom: 10px;
  right: 10px;
  padding: 2px 8px;
  font-size: 11px;
  color: rgba(var(--cream-static-rgb), 0.95);
  background: rgba(var(--brown-rgb), 0.45);
  border-radius: 10px;
}

.travel-card__body {
  padding: 16px 18px 18px;
}

.travel-card__title {
  font-family: var(--font-handwrite);
  font-size: 21px;
  color: var(--brown-deep);
  margin-bottom: 6px;
}

.travel-card__meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.travel-card__note {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 表单 */
.travel-form .form-group {
  margin-bottom: 20px;
}

.travel-form label {
  display: block;
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-group--half {
  flex: 1;
  min-width: 0;
}

.status-picker {
  display: flex;
  gap: 8px;
}

.status-chip {
  padding: 8px 18px;
  font-size: 13px;
  font-family: var(--font-display);
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.1);
  border: 1px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 18px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.status-chip:hover {
  background: rgba(var(--kraft-rgb), 0.2);
}

.status-chip.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

/* 详情弹窗 */
.travel-detail-modal :deep(.base-modal) {
  width: min(760px, 100%);
}

.travel-detail-loading {
  padding: 20px 0;
}

.travel-detail__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.travel-detail__place {
  font-family: var(--font-display);
  letter-spacing: 1px;
}

.travel-detail__date,
.travel-detail__weather {
  padding: 2px 10px;
  background: rgba(var(--kraft-rgb), 0.12);
  border-radius: 10px;
}

.travel-detail__status {
  padding: 2px 10px;
  color: var(--olive);
  background: rgba(var(--olive-rgb), 0.12);
  border: 1px solid rgba(var(--olive-rgb), 0.3);
  border-radius: 10px;
}

.travel-detail__status.planned {
  color: var(--mustard);
  background: rgba(var(--mustard-rgb), 0.12);
  border-color: rgba(var(--mustard-rgb), 0.3);
}

.travel-detail__note {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 18px;
  padding: 14px 16px;
  background: rgba(var(--kraft-rgb), 0.08);
  border-left: 3px solid rgba(var(--brown-rgb), 0.25);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.travel-detail__photos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.travel-detail__photo {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: var(--border-vintage);
}

.travel-detail__photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.travel-detail__photo-delete {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  font-size: 16px;
  color: rgba(var(--cream-static-rgb), 0.95);
  background: rgba(var(--brown-rgb), 0.55);
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.travel-detail__photo:hover .travel-detail__photo-delete {
  opacity: 1;
}

.travel-detail__upload {
  margin-bottom: 18px;
}

.travel-detail__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px dashed rgba(var(--brown-medium-rgb), 0.25);
  padding-top: 16px;
}

.travel-detail__delete {
  color: var(--faded-red);
  border-color: rgba(var(--red-rgb), 0.4);
}

.travel-detail__delete:hover {
  background: rgba(var(--red-rgb), 0.1);
}

/* Animations */
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .travel-page {
    padding: 24px 0 48px;
  }

  .travel-header {
    margin-bottom: 20px;
  }

  .travel-header__title {
    font-size: 28px;
  }

  .travel-map-card {
    padding: 14px;
  }

  .travel-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .travel-tabs {
    justify-content: center;
  }

  .travel-tab {
    padding: 6px 16px;
    font-size: 13px;
  }

  .add-travel-btn {
    justify-content: center;
  }

  .travel-grid {
    grid-template-columns: 1fr;
  }

  .travel-year {
    font-size: 24px;
  }

  .travel-card__title {
    overflow-wrap: break-word;
  }

  .travel-form .input-vintage {
    font-size: 16px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .status-chip {
    padding: 8px 14px;
    min-height: 40px;
  }
}

@media (max-width: 480px) {
  .travel-header__title {
    font-size: 24px;
  }

  .travel-tab {
    padding: 6px 12px;
  }

  .travel-detail__photos {
    grid-template-columns: repeat(2, 1fr);
  }

  .travel-detail__actions {
    flex-direction: column;
  }
}
</style>
