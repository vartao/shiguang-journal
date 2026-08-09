<template>
  <div class="plan-detail-page">
    <div class="container">
      <button class="back-btn" @click="goBack"><span>←</span> 返回行程列表</button>

      <LoadingState v-if="loading" message="正在打开行程…" />
      <ErrorState v-else-if="loadError" message="行程暂时没有加载成功。" @retry="fetchPlan" />

      <template v-else-if="plan">
        <!-- 行程概要 -->
        <header class="plan-hero anim-fade-in-up">
          <div class="plan-hero__cover">
            <img v-if="plan.cover" :src="plan.cover" :alt="plan.title" />
            <div v-else class="plan-hero__cover-placeholder" aria-hidden="true">✈️</div>
          </div>
          <div class="plan-hero__body">
            <h1 class="plan-hero__title">{{ plan.title }}</h1>
            <div class="plan-hero__meta">
              <span v-if="plan.destination" class="plan-hero__meta-item">📍 {{ plan.destination }}</span>
              <span v-if="dateRange" class="plan-hero__meta-item">🗓 {{ dateRange }}</span>
              <span v-if="plan.budget != null" class="plan-hero__meta-item">💰 预算 ¥{{ plan.budget }}</span>
            </div>
            <div class="status-picker">
              <button
                v-for="option in planStatusOptions"
                :key="option.value"
                type="button"
                class="status-chip"
                :class="['status-chip--' + option.value, { active: statusDraft === option.value }]"
                @click="changeStatus(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
            <p v-if="plan.note" class="plan-hero__note">{{ plan.note }}</p>
            <div class="plan-hero__actions">
              <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="openEdit">编辑信息</button>
              <button class="btn-vintage btn-vintage--outline btn-vintage--small delete-btn" @click="handleDelete">
                删除行程
              </button>
            </div>
          </div>
        </header>

        <!-- 每日安排 -->
        <section class="days-section anim-fade-in-up stagger-2">
          <h2 class="days-section__title">每日安排</h2>

          <div v-if="days.length" class="day-list">
            <div v-for="(day, dayIndex) in days" :key="dayIndex" class="day-card">
              <div class="day-card__header">
                <span class="day-card__label">第 {{ dayIndex + 1 }} 天 · {{ day.date }}</span>
                <button class="day-card__remove" title="删除这一天" @click="removeDay(dayIndex)">×</button>
              </div>
              <ul v-if="day.items.length" class="day-card__items">
                <li v-for="(item, itemIndex) in day.items" :key="itemIndex" class="day-card__item">
                  <span class="day-card__item-text">{{ item }}</span>
                  <button class="day-card__item-remove" title="删除这条安排" @click="removeItem(dayIndex, itemIndex)">
                    ×
                  </button>
                </li>
              </ul>
              <p v-else class="day-card__empty">这一天还没有安排</p>
              <div class="day-card__add">
                <input
                  v-model="day.newItem"
                  class="input-vintage"
                  placeholder="例如：10:00 高铁出发"
                  @keyup.enter="addItem(dayIndex)"
                />
                <button
                  class="btn-vintage btn-vintage--red btn-vintage--small"
                  :disabled="!day.newItem.trim()"
                  @click="addItem(dayIndex)"
                >
                  添加
                </button>
              </div>
            </div>
          </div>
          <p v-else class="days-empty">还没有每日安排，从第一天开始吧</p>

          <button class="add-day-btn" @click="addDay"><span>+</span> 添加一天</button>

          <div class="days-save">
            <button class="btn-vintage btn-vintage--red" :disabled="saving" @click="handleSave">
              {{ saving ? '保存中…' : '保存行程' }}
            </button>
          </div>
        </section>
      </template>
    </div>

    <!-- 编辑基本信息 -->
    <BaseModal v-model="showModal" title="编辑行程" eyebrow="TRAVEL PLAN" :busy="submitting" @close="resetEditor">
      <form class="plan-form" @submit.prevent="handleSubmitInfo">
        <div class="form-group">
          <label for="edit-title">行程标题</label
          ><input
            id="edit-title"
            v-model="form.title"
            class="input-vintage"
            placeholder="例如：海边三日游"
            maxlength="200"
            required
          />
        </div>
        <div class="form-group">
          <label for="edit-destination">目的地</label
          ><input
            id="edit-destination"
            v-model="form.destination"
            class="input-vintage"
            placeholder="想去哪里？"
            maxlength="200"
          />
        </div>
        <div class="form-row">
          <div class="form-group form-group--half">
            <label for="edit-start">开始日期</label
            ><input id="edit-start" v-model="form.startDate" type="date" class="input-vintage" />
          </div>
          <div class="form-group form-group--half">
            <label for="edit-end">结束日期</label
            ><input id="edit-end" v-model="form.endDate" type="date" class="input-vintage" />
          </div>
        </div>
        <div class="form-group">
          <label for="edit-budget">预算（元）</label
          ><input
            id="edit-budget"
            v-model="form.budget"
            type="number"
            min="0"
            step="0.01"
            class="input-vintage"
            placeholder="大概要花多少钱？"
          />
        </div>
        <div class="form-group">
          <label for="edit-note">备注</label
          ><textarea
            id="edit-note"
            v-model="form.note"
            class="input-vintage"
            rows="3"
            placeholder="有什么要特别记住的？"
            maxlength="1000"
          ></textarea>
        </div>
        <div class="form-group">
          <label>封面图</label>
          <ImageUploader v-model="form.coverImages" :max-count="1" category="travel" />
        </div>
      </form>
      <template #footer
        ><button type="button" class="btn-vintage btn-vintage--outline" :disabled="submitting" @click="closeModal">
          取消</button
        ><button
          type="button"
          class="btn-vintage btn-vintage--red"
          :disabled="!form.title.trim() || submitting"
          @click="handleSubmitInfo"
        >
          {{ submitting ? '保存中…' : '保存修改' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deletePlan, getPlan, updatePlan } from '@/api/plan'
import BaseModal from '@/components/BaseModal.vue'
import ErrorState from '@/components/ErrorState.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { planStatusOptions } from '@/features/plan/planMeta'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const planId = route.params.id

const plan = ref(null)
const days = ref([])
const statusDraft = ref(0)
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const form = ref(createForm())

const dateRange = computed(() => {
  if (!plan.value) return ''
  const { startDate, endDate } = plan.value
  if (startDate && endDate) return startDate + ' ~ ' + endDate
  if (startDate) return startDate + ' 出发'
  if (endDate) return '至 ' + endDate
  return ''
})

function createForm(source = {}) {
  return {
    title: source.title || '',
    destination: source.destination || '',
    startDate: source.startDate || '',
    endDate: source.endDate || '',
    budget: source.budget != null ? source.budget : '',
    note: source.note || '',
    coverImages: source.cover ? [{ url: source.cover }] : []
  }
}

function parseDays(raw) {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((day) => day && day.date)
      .map((day) => ({
        date: String(day.date),
        items: Array.isArray(day.items) ? day.items.map((text) => String(text)) : [],
        newItem: ''
      }))
      .sort((a, b) => a.date.localeCompare(b.date))
  } catch {
    return []
  }
}

function serializeDays() {
  return JSON.stringify(days.value.map((day) => ({ date: day.date, items: day.items })))
}

async function fetchPlan() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getPlan(planId)
    plan.value = res.data
    days.value = parseDays(res.data?.days)
    statusDraft.value = Number(res.data?.status || 0)
  } catch {
    loadError.value = true
    plan.value = null
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/plans')
}

/* ---------- 每日安排编辑（本地） ---------- */

function todayString() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return now.getFullYear() + '-' + month + '-' + day
}

function nextDayString(dateString) {
  const date = new Date(dateString + 'T00:00:00')
  if (Number.isNaN(date.getTime())) return todayString()
  date.setDate(date.getDate() + 1)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

function addDay() {
  const last = days.value[days.value.length - 1]
  const date = last ? nextDayString(last.date) : plan.value?.startDate || todayString()
  days.value.push({ date, items: [], newItem: '' })
  days.value.sort((a, b) => a.date.localeCompare(b.date))
}

async function removeDay(dayIndex) {
  const day = days.value[dayIndex]
  const confirmed = await requestConfirm({
    title: '删除这一天',
    message: '确定要删除「第 ' + (dayIndex + 1) + ' 天 · ' + day.date + '」的全部安排吗？',
    confirmText: '删除'
  })
  if (!confirmed) return
  days.value.splice(dayIndex, 1)
}

function addItem(dayIndex) {
  const day = days.value[dayIndex]
  const text = day.newItem.trim()
  if (!text) return
  day.items.push(text)
  day.newItem = ''
}

async function removeItem(dayIndex, itemIndex) {
  const text = days.value[dayIndex].items[itemIndex]
  const confirmed = await requestConfirm({
    title: '删除安排',
    message: '确定要删除“' + text + '”吗？',
    confirmText: '删除'
  })
  if (!confirmed) return
  days.value[dayIndex].items.splice(itemIndex, 1)
}

/* ---------- 保存 ---------- */

function buildPayload(extra = {}) {
  return {
    title: plan.value.title,
    destination: plan.value.destination || null,
    startDate: plan.value.startDate || null,
    endDate: plan.value.endDate || null,
    budget: plan.value.budget != null ? plan.value.budget : null,
    note: plan.value.note || null,
    cover: plan.value.cover || null,
    status: statusDraft.value,
    days: serializeDays(),
    ...extra
  }
}

async function handleSave() {
  if (saving.value) return
  saving.value = true
  try {
    const res = await updatePlan(planId, buildPayload())
    if (res.data) {
      plan.value = res.data
      days.value = parseDays(res.data.days)
      statusDraft.value = Number(res.data.status || 0)
    }
    showToast('行程已保存')
  } finally {
    saving.value = false
  }
}

function changeStatus(value) {
  if (statusDraft.value === value || saving.value) return
  statusDraft.value = value
  handleSave()
}

/* ---------- 基本信息编辑 ---------- */

function openEdit() {
  form.value = createForm(plan.value)
  showModal.value = true
}

function resetEditor() {
  form.value = createForm(plan.value || {})
}

function closeModal() {
  showModal.value = false
  resetEditor()
}

async function handleSubmitInfo() {
  if (!form.value.title.trim() || submitting.value) return
  submitting.value = true
  try {
    const payload = buildPayload({
      title: form.value.title.trim(),
      destination: form.value.destination.trim() || null,
      startDate: form.value.startDate || null,
      endDate: form.value.endDate || null,
      budget: form.value.budget !== '' && form.value.budget != null ? Number(form.value.budget) : null,
      note: form.value.note.trim() || null,
      cover: form.value.coverImages[0]?.url || null
    })
    const res = await updatePlan(planId, payload)
    if (res.data) {
      plan.value = res.data
      days.value = parseDays(res.data.days)
      statusDraft.value = Number(res.data.status || 0)
    }
    closeModal()
  } finally {
    submitting.value = false
  }
}

async function handleDelete() {
  const confirmed = await requestConfirm({
    title: '删除行程',
    message: '确定要删除“' + plan.value.title + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deletePlan(planId)
  router.push('/plans')
}

onMounted(fetchPlan)
</script>

<style scoped>
.plan-detail-page {
  padding: 32px 0 60px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  padding: 8px 16px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-secondary);
  background: transparent;
  border: var(--border-vintage);
  border-radius: 20px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.1);
}

/* Hero */
.plan-hero {
  display: flex;
  gap: 24px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 24px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
}

.plan-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--paper-texture);
  opacity: 0.25;
  pointer-events: none;
}

.plan-hero__cover {
  flex: 0 0 220px;
  height: 160px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid rgba(var(--brown-rgb), 0.15);
}

.plan-hero__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: sepia(0.15);
}

.plan-hero__cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: linear-gradient(145deg, rgba(var(--kraft-rgb), 0.35), rgba(var(--blue-rgb), 0.25));
}

.plan-hero__body {
  flex: 1;
  min-width: 0;
  position: relative;
}

.plan-hero__title {
  font-family: var(--font-handwrite);
  font-size: 32px;
  color: var(--brown-deep);
  margin-bottom: 10px;
}

.plan-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 12px;
}

.plan-hero__meta-item {
  font-size: 13px;
  color: var(--text-secondary);
}

.plan-hero__note {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin-top: 12px;
  white-space: pre-wrap;
}

.plan-hero__actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.delete-btn:hover {
  color: var(--faded-red);
  border-color: rgba(var(--red-rgb), 0.4);
}

/* Status picker */
.status-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.status-chip {
  padding: 6px 14px;
  font-size: 12px;
  font-family: var(--font-display);
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.1);
  border: 1px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 16px;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.status-chip:hover {
  background: rgba(var(--kraft-rgb), 0.2);
}

.status-chip--0.active {
  color: var(--cream);
  background: var(--olive);
  border-color: var(--olive);
}

.status-chip--1.active {
  color: var(--cream);
  background: var(--mustard);
  border-color: var(--mustard);
}

.status-chip--2.active {
  color: var(--cream);
  background: rgb(var(--blue-rgb));
  border-color: rgb(var(--blue-rgb));
}

/* Days */
.days-section__title {
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: var(--brown-deep);
  margin-bottom: 18px;
}

.day-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.day-card {
  background: var(--gradient-paper);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
  padding: 18px 20px;
}

.day-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  border-bottom: 1px dashed rgba(var(--brown-medium-rgb), 0.25);
  padding-bottom: 10px;
}

.day-card__label {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--brown-deep);
  letter-spacing: 1px;
}

.day-card__remove {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--text-muted);
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.day-card__remove:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
}

.day-card__items {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.day-card__item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
}

.day-card__item::before {
  content: '✦';
  color: var(--mustard);
  flex: 0 0 auto;
}

.day-card__item-text {
  flex: 1;
  min-width: 0;
  overflow-wrap: break-word;
}

.day-card__item-remove {
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: var(--text-muted);
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  opacity: 0;
  transition: all var(--transition-fast);
}

.day-card__item:hover .day-card__item-remove {
  opacity: 1;
}

.day-card__item-remove:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
}

.day-card__empty {
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
  margin-bottom: 12px;
}

.day-card__add {
  display: flex;
  gap: 10px;
}

.day-card__add .input-vintage {
  flex: 1;
}

.days-empty {
  font-size: 14px;
  color: var(--text-muted);
  font-style: italic;
  text-align: center;
  padding: 24px 0;
}

.add-day-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  margin-top: 16px;
  padding: 14px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.06);
  border: var(--border-dashed);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.add-day-btn:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.12);
}

.days-save {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* Form */
.plan-form .form-group {
  margin-bottom: 20px;
}

.plan-form label {
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

/* Responsive */
@media (max-width: 768px) {
  .plan-detail-page {
    padding: 24px 0 48px;
  }

  .plan-hero {
    flex-direction: column;
    padding: 20px;
  }

  .plan-hero__cover {
    flex: none;
    width: 100%;
    height: 180px;
  }

  .plan-hero__title {
    font-size: 26px;
  }

  .day-card__item-remove {
    opacity: 1;
  }

  .plan-form .input-vintage {
    font-size: 16px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .status-chip {
    padding: 8px 14px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .plan-hero__title {
    font-size: 22px;
  }

  .plan-hero__cover {
    height: 140px;
  }

  .plan-hero__actions {
    flex-wrap: wrap;
  }

  .day-card {
    padding: 16px;
  }

  .day-card__add {
    flex-direction: column;
  }
}
</style>
