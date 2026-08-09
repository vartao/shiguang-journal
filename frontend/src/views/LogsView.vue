<template>
  <div class="logs-page">
    <div class="container">
      <header class="logs-header anim-fade-in-up">
        <h1 class="logs-header__title">小事记</h1>
        <p class="logs-header__subtitle">一起做过的事，都值得打个卡</p>
        <div class="logs-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <div class="logs-filters anim-fade-in-up stagger-2">
        <div class="category-chips">
          <button
            v-for="chip in categoryChips"
            :key="chip.value"
            class="category-chip"
            :class="{ active: activeCategory === chip.value }"
            @click="activeCategory = chip.value"
          >
            {{ chip.icon }} {{ chip.label }}
          </button>
        </div>
        <select v-model="activeYear" class="year-select input-vintage" aria-label="按年份筛选">
          <option value="">全部年份</option>
          <option v-for="year in yearOptions" :key="year" :value="year">{{ year }} 年</option>
        </select>
      </div>
      <button class="add-log-btn anim-fade-in-up stagger-2" @click="openCreate">
        <span class="add-icon">📖</span><span>记一笔</span>
      </button>

      <LoadingState v-if="loading" message="正在翻开小事记…" />
      <ErrorState v-else-if="loadError" message="小事记暂时没有加载成功。" @retry="fetchLogList" />
      <div v-else-if="filteredLogs.length" class="logs-grid anim-fade-in-up stagger-3">
        <div
          v-for="(log, idx) in filteredLogs"
          :key="log.id"
          class="log-card"
          :style="{ animationDelay: idx * 0.08 + 's' }"
        >
          <div class="log-card__cover">
            <img v-if="log.cover" :src="log.cover" :alt="log.title" loading="lazy" />
            <div v-else class="log-card__cover-placeholder">
              <span>{{ categoryMeta[log.category]?.icon || '✨' }}</span>
            </div>
            <span class="log-card__badge"
              >{{ categoryMeta[log.category]?.icon }} {{ categoryMeta[log.category]?.label || '其他' }}</span
            >
          </div>
          <div class="log-card__body">
            <h3 class="log-card__title">{{ log.title }}</h3>
            <div class="log-card__meta">
              <span class="log-card__date">{{ log.logDate }}</span>
              <span v-if="Number(log.rating) > 0" class="log-card__stars" :title="log.rating + ' 星'">
                <span v-for="i in 5" :key="i" class="star-icon" :class="{ filled: i <= Number(log.rating) }">★</span>
              </span>
            </div>
            <p v-if="log.note" class="log-card__note">{{ log.note }}</p>
            <div class="log-card__actions">
              <button class="log-card__btn" title="编辑" @click="openEdit(log)">编辑</button>
              <button class="log-card__btn log-card__btn--delete" title="删除" @click="handleDelete(log)">删除</button>
            </div>
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        icon="📖"
        :title="hasFilter ? '没有符合条件的小事' : '还没有打卡记录'"
        :message="hasFilter ? '换个分类或年份看看' : '记下一起做过的第一件小事吧'"
      >
        <button v-if="!hasFilter" class="btn-vintage btn-vintage--red" @click="openCreate">记第一笔</button>
      </EmptyState>
    </div>

    <BaseModal
      v-model="showModal"
      :title="editingId ? '编辑小事' : '记一笔'"
      eyebrow="COUPLE LOG"
      :busy="submitting"
      @close="resetEditor"
    >
      <form class="log-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label>分类</label>
          <div class="category-picker">
            <button
              v-for="chip in editableCategoryChips"
              :key="chip.value"
              type="button"
              class="category-chip"
              :class="{ active: form.category === chip.value }"
              @click="form.category = chip.value"
            >
              {{ chip.icon }} {{ chip.label }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label for="log-title">标题</label
          ><input
            id="log-title"
            v-model="form.title"
            class="input-vintage"
            placeholder="一起做了什么？"
            maxlength="200"
            required
          />
        </div>
        <div class="form-row">
          <div class="form-group form-group--half">
            <label for="log-date">发生日期</label
            ><input id="log-date" v-model="form.logDate" type="date" class="input-vintage" required />
          </div>
          <div class="form-group form-group--half">
            <label>评分</label>
            <div class="rating-picker" role="radiogroup" aria-label="评分">
              <button
                v-for="i in 5"
                :key="i"
                type="button"
                class="rating-star"
                :class="{ filled: i <= form.rating }"
                :title="i + ' 星'"
                @click="selectRating(i)"
              >
                ★
              </button>
              <span class="rating-hint">{{ form.rating > 0 ? form.rating + ' 星' : '不评分' }}</span>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label for="log-note">一句话手记</label
          ><textarea
            id="log-note"
            v-model="form.note"
            class="input-vintage"
            rows="3"
            placeholder="当时的心情，一句话就好…"
            maxlength="2000"
          ></textarea>
        </div>
        <div class="form-group">
          <label>封面图（选填）</label>
          <ImageUploader v-model="form.coverImages" :max-count="1" category="couple" />
        </div>
      </form>
      <template #footer
        ><button type="button" class="btn-vintage btn-vintage--outline" :disabled="submitting" @click="closeModal">
          取消</button
        ><button type="button" class="btn-vintage btn-vintage--red" :disabled="!canSubmit" @click="handleSubmit">
          {{ submitting ? '保存中…' : editingId ? '保存修改' : '打卡 📖' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { createLog, deleteLog, fetchLogs, updateLog } from '@/api/log'
import BaseModal from '@/components/BaseModal.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { toLocalDateInputValue } from '@/utils/date'

const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const editingId = ref(null)
const activeCategory = ref('')
const activeYear = ref('')
const logs = ref([])

const categoryMeta = {
  movie: { label: '电影', icon: '🎬' },
  food: { label: '美食', icon: '🍴' },
  show: { label: '演出', icon: '🎭' },
  travel: { label: '旅行', icon: '✈️' },
  date: { label: '约会', icon: '💑' },
  sport: { label: '运动', icon: '🏸' },
  other: { label: '其他', icon: '✨' }
}
const categoryChips = [{ value: '', label: '全部', icon: '📖' }, ...toChips()]
const editableCategoryChips = toChips()

function toChips() {
  return Object.entries(categoryMeta).map(([value, meta]) => ({ value, label: meta.label, icon: meta.icon }))
}

function createForm(log) {
  return {
    title: log?.title || '',
    category: log?.category || 'other',
    logDate: log?.logDate || toLocalDateInputValue(),
    note: log?.note || '',
    rating: Number(log?.rating) || 0,
    coverImages: log?.cover ? [{ url: log.cover }] : []
  }
}

const form = ref(createForm())

const yearOptions = computed(() => {
  const years = new Set()
  for (const log of logs.value) {
    const year = String(log.logDate || '').slice(0, 4)
    if (year) years.add(year)
  }
  return [...years].sort((a, b) => b.localeCompare(a))
})

const filteredLogs = computed(() =>
  logs.value.filter((log) => {
    if (activeCategory.value && log.category !== activeCategory.value) return false
    if (activeYear.value && String(log.logDate || '').slice(0, 4) !== activeYear.value) return false
    return true
  })
)

const hasFilter = computed(() => Boolean(activeCategory.value || activeYear.value))
const canSubmit = computed(() => Boolean(form.value.title.trim() && form.value.logDate) && !submitting.value)

async function fetchLogList() {
  loading.value = true
  loadError.value = false
  try {
    const res = await fetchLogs()
    logs.value = res.data || []
  } catch {
    loadError.value = true
    logs.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = createForm()
  showModal.value = true
}

function openEdit(log) {
  editingId.value = log.id
  form.value = createForm(log)
  showModal.value = true
}

function resetEditor() {
  editingId.value = null
  form.value = createForm()
}

function closeModal() {
  showModal.value = false
  resetEditor()
}

function selectRating(value) {
  form.value.rating = form.value.rating === value ? 0 : value
}

function toPayload() {
  const cover = form.value.coverImages[0]
  return {
    title: form.value.title.trim(),
    category: form.value.category,
    logDate: form.value.logDate,
    note: form.value.note.trim(),
    cover: cover ? cover.url || cover : null,
    rating: form.value.rating
  }
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const payload = toPayload()
    if (editingId.value) {
      const id = editingId.value
      const res = await updateLog(id, payload)
      const saved = res.data || payload
      logs.value = logs.value.map((log) => (log.id === id ? { ...log, ...saved } : log))
    } else {
      const res = await createLog(payload)
      if (res.data?.id) logs.value = [res.data, ...logs.value]
      else await fetchLogList()
    }
    closeModal()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(log) {
  const confirmed = await requestConfirm({
    title: '删除小事记',
    message: '确定要删除“' + log.title + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deleteLog(log.id)
  logs.value = logs.value.filter((item) => item.id !== log.id)
}

onMounted(fetchLogList)
</script>

<style scoped>
.logs-page {
  padding: 32px 0 60px;
}

.logs-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.logs-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.logs-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.logs-header__deco {
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

/* 筛选栏 */
.logs-filters {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}

.category-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.category-chip {
  padding: 8px 16px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-muted);
  background: transparent;
  border: var(--border-vintage);
  border-radius: 20px;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.category-chip:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.1);
}

.category-chip.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

.year-select {
  width: auto;
  padding: 8px 12px;
  font-size: 13px;
}

/* 新增按钮 */
.add-log-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto 32px;
  padding: 12px 32px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--mustard), rgba(var(--mustard-rgb), 0.8));
  border-radius: var(--radius-sm);
  box-shadow: 0 3px 12px rgba(var(--mustard-rgb), 0.3);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.add-log-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(var(--mustard-rgb), 0.4);
}

.add-icon {
  font-size: 18px;
}

/* 卡片网格 */
.logs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.log-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  position: relative;
  transition: all var(--transition-normal);
  animation: fade-in-up 0.4s ease both;
}

.log-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lifted);
}

.log-card__cover {
  position: relative;
  height: 160px;
}

.log-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.log-card__cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: linear-gradient(145deg, rgba(var(--kraft-rgb), 0.25), rgba(var(--mustard-rgb), 0.2));
  filter: sepia(0.2);
}

.log-card__badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 10px;
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  color: var(--cream);
  background: rgba(var(--brown-rgb), 0.65);
  border-radius: 12px;
  backdrop-filter: blur(2px);
}

.log-card__body {
  padding: 16px 20px 20px;
}

.log-card__title {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.log-card__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.log-card__stars {
  display: inline-flex;
  gap: 1px;
  font-size: 13px;
}

.star-icon {
  color: rgba(var(--brown-rgb), 0.2);
}

.star-icon.filled {
  color: var(--mustard);
}

.log-card__note {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.log-card__actions {
  display: flex;
  gap: 8px;
  border-top: 1px solid rgba(var(--brown-rgb), 0.1);
  padding-top: 12px;
}

.log-card__btn {
  padding: 6px 14px;
  font-size: 12px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid rgba(var(--brown-rgb), 0.15);
}

.log-card__btn:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.14);
}

.log-card__btn--delete:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
  border-color: rgba(var(--red-rgb), 0.3);
}

/* 表单 */
.log-form .form-group {
  margin-bottom: 20px;
}

.log-form label {
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
}

.category-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-picker .category-chip {
  padding: 6px 12px;
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.1);
  border: 1px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 16px;
}

.category-picker .category-chip:hover {
  background: rgba(var(--kraft-rgb), 0.2);
}

.category-picker .category-chip.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

.rating-picker {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 42px;
}

.rating-star {
  font-size: 26px;
  line-height: 1;
  background: transparent;
  border: none;
  padding: 0 2px;
  cursor: pointer;
  color: rgba(var(--brown-rgb), 0.2);
  transition:
    color var(--transition-fast),
    transform var(--transition-fast);
}

.rating-star:hover {
  transform: scale(1.15);
}

.rating-star.filled {
  color: var(--mustard);
}

.rating-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

/* 动画 */
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

/* 响应式 */
@media (max-width: 768px) {
  .logs-page {
    padding: 24px 0 48px;
  }

  .logs-header {
    margin-bottom: 24px;
  }

  .logs-header__title {
    font-size: 28px;
  }

  .logs-grid {
    grid-template-columns: 1fr;
  }

  .log-card__title,
  .log-card__note {
    overflow-wrap: break-word;
  }

  .category-chip {
    padding: 6px 14px;
    font-size: 13px;
  }

  .log-form .input-vintage {
    font-size: 16px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .log-card__btn {
    padding: 8px 16px;
    font-size: 13px;
    min-height: 40px;
  }
}

@media (max-width: 480px) {
  .logs-header__title {
    font-size: 24px;
  }

  .log-card__body {
    padding: 14px 16px 16px;
  }

  .add-log-btn {
    padding: 12px 24px;
  }

  .rating-star {
    font-size: 24px;
  }
}
</style>
