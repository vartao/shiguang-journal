<template>
  <div class="plan-page">
    <div class="container">
      <header class="plan-header anim-fade-in-up">
        <h1 class="plan-header__title">出发前的计划</h1>
        <p class="plan-header__subtitle">把每一次出发，都安排成期待</p>
        <div class="plan-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <button class="add-plan-btn anim-fade-in-up stagger-2" @click="openCreate">
        <span class="add-icon">🧳</span><span>新建行程</span>
      </button>

      <LoadingState v-if="loading" message="正在收拾行囊…" />
      <ErrorState v-else-if="loadError" message="行程列表暂时没有加载成功。" @retry="fetchPlans" />
      <div v-else-if="plans.length" class="plan-grid anim-fade-in-up stagger-3">
        <div
          v-for="(plan, idx) in plans"
          :key="plan.id"
          class="plan-card"
          :style="{ animationDelay: idx * 0.08 + 's' }"
          @click="goToDetail(plan)"
        >
          <div class="plan-card__cover">
            <img v-if="plan.cover" :src="plan.cover" :alt="plan.title" />
            <div v-else class="plan-card__cover-placeholder" aria-hidden="true">✈️</div>
            <span class="plan-card__badge" :class="'plan-card__badge--' + Number(plan.status || 0)">
              {{ planStatusLabel(plan.status) }}
            </span>
          </div>
          <div class="plan-card__body">
            <h3 class="plan-card__title">{{ plan.title }}</h3>
            <p v-if="plan.destination" class="plan-card__destination">📍 {{ plan.destination }}</p>
            <p v-if="dateRange(plan)" class="plan-card__dates">🗓 {{ dateRange(plan) }}</p>
            <p v-if="plan.budget != null" class="plan-card__budget">💰 预算 ¥{{ plan.budget }}</p>
            <div class="plan-card__actions">
              <button class="plan-card__btn" title="编辑" @click.stop="openEdit(plan)">编辑</button>
              <button class="plan-card__btn plan-card__btn--delete" title="删除" @click.stop="handleDelete(plan)">
                删除
              </button>
            </div>
          </div>
        </div>
      </div>
      <EmptyState v-else icon="🧳" title="还没有行程计划" message="写下第一次出发的期待，把日子安排得闪闪发光">
        <button class="btn-vintage btn-vintage--red" @click="openCreate">规划一次出行</button>
      </EmptyState>
    </div>

    <BaseModal
      v-model="showModal"
      :title="editingId ? '编辑行程' : '新建行程'"
      eyebrow="TRAVEL PLAN"
      :busy="submitting"
      @close="resetEditor"
    >
      <form class="plan-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="plan-title">行程标题</label
          ><input
            id="plan-title"
            v-model="form.title"
            class="input-vintage"
            placeholder="例如：海边三日游"
            maxlength="200"
            required
          />
        </div>
        <div class="form-group">
          <label for="plan-destination">目的地</label
          ><input
            id="plan-destination"
            v-model="form.destination"
            class="input-vintage"
            placeholder="想去哪里？"
            maxlength="200"
          />
        </div>
        <div class="form-row">
          <div class="form-group form-group--half">
            <label for="plan-start">开始日期</label
            ><input id="plan-start" v-model="form.startDate" type="date" class="input-vintage" />
          </div>
          <div class="form-group form-group--half">
            <label for="plan-end">结束日期</label
            ><input id="plan-end" v-model="form.endDate" type="date" class="input-vintage" />
          </div>
        </div>
        <div class="form-group">
          <label for="plan-budget">预算（元）</label
          ><input
            id="plan-budget"
            v-model="form.budget"
            type="number"
            min="0"
            step="0.01"
            class="input-vintage"
            placeholder="大概要花多少钱？"
          />
        </div>
        <div class="form-group">
          <label for="plan-note">备注</label
          ><textarea
            id="plan-note"
            v-model="form.note"
            class="input-vintage"
            rows="3"
            placeholder="有什么要特别记住的？"
            maxlength="1000"
          ></textarea>
        </div>
        <div class="form-group">
          <label>状态</label>
          <div class="status-picker">
            <button
              v-for="option in planStatusOptions"
              :key="option.value"
              type="button"
              class="status-chip"
              :class="['status-chip--' + option.value, { active: form.status === option.value }]"
              @click="form.status = option.value"
            >
              {{ option.label }}
            </button>
          </div>
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
          @click="handleSubmit"
        >
          {{ submitting ? '保存中…' : editingId ? '保存修改' : '创建行程 🧳' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createPlan, deletePlan, getPlans, updatePlan } from '@/api/plan'
import BaseModal from '@/components/BaseModal.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { planStatusLabel, planStatusOptions } from '@/features/plan/planMeta'

const router = useRouter()

const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const editingId = ref(null)
const plans = ref([])

function createForm(plan = {}) {
  return {
    title: plan.title || '',
    destination: plan.destination || '',
    startDate: plan.startDate || '',
    endDate: plan.endDate || '',
    budget: plan.budget != null ? plan.budget : '',
    note: plan.note || '',
    status: plan.status != null ? Number(plan.status) : 0,
    coverImages: plan.cover ? [{ url: plan.cover }] : []
  }
}

const form = ref(createForm())

function dateRange(plan) {
  if (plan.startDate && plan.endDate) return plan.startDate + ' ~ ' + plan.endDate
  if (plan.startDate) return plan.startDate + ' 出发'
  if (plan.endDate) return '至 ' + plan.endDate
  return ''
}

function goToDetail(plan) {
  router.push(`/plans/${plan.id}`)
}

async function fetchPlans() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getPlans()
    plans.value = res.data || []
  } catch {
    loadError.value = true
    plans.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = createForm()
  showModal.value = true
}

function openEdit(plan) {
  editingId.value = plan.id
  form.value = createForm(plan)
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

function toPayload() {
  return {
    title: form.value.title.trim(),
    destination: form.value.destination.trim() || null,
    startDate: form.value.startDate || null,
    endDate: form.value.endDate || null,
    budget: form.value.budget !== '' && form.value.budget != null ? Number(form.value.budget) : null,
    note: form.value.note.trim() || null,
    status: form.value.status,
    cover: form.value.coverImages[0]?.url || null
  }
}

async function handleSubmit() {
  if (!form.value.title.trim() || submitting.value) return
  submitting.value = true
  try {
    const payload = toPayload()
    if (editingId.value) {
      const id = editingId.value
      const res = await updatePlan(id, payload)
      const saved = res.data || payload
      plans.value = plans.value.map((plan) => (plan.id === id ? { ...plan, ...saved } : plan))
    } else {
      const res = await createPlan(payload)
      if (res.data?.id) plans.value = [res.data, ...plans.value]
      else await fetchPlans()
    }
    closeModal()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(plan) {
  const confirmed = await requestConfirm({
    title: '删除行程',
    message: '确定要删除“' + plan.title + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deletePlan(plan.id)
  plans.value = plans.value.filter((item) => item.id !== plan.id)
}

onMounted(fetchPlans)
</script>

<style scoped>
.plan-page {
  padding: 32px 0 60px;
}

.plan-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.plan-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.plan-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.plan-header__deco {
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

/* Add button */
.add-plan-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto 32px;
  padding: 12px 32px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--mustard), var(--kraft-dark));
  border-radius: var(--radius-sm);
  box-shadow: 0 3px 12px rgba(var(--mustard-rgb), 0.3);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.add-plan-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(var(--mustard-rgb), 0.4);
}

.add-icon {
  font-size: 18px;
}

/* Plan Grid */
.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.plan-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal);
  animation: fade-in-up 0.4s ease both;
}

.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lifted);
}

/* Cover */
.plan-card__cover {
  position: relative;
  height: 150px;
  overflow: hidden;
}

.plan-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: sepia(0.15);
  transition: transform var(--transition-normal);
}

.plan-card:hover .plan-card__cover img {
  transform: scale(1.05);
}

.plan-card__cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  background: linear-gradient(145deg, rgba(var(--kraft-rgb), 0.35), rgba(var(--blue-rgb), 0.25));
}

.plan-card__badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 3px 12px;
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  border-radius: 12px;
  border: 1px solid;
  background: rgba(var(--cream-rgb), 0.9);
}

.plan-card__badge--0 {
  color: var(--olive);
  border-color: rgba(var(--olive-rgb), 0.4);
}

.plan-card__badge--1 {
  color: var(--mustard);
  border-color: rgba(var(--mustard-rgb), 0.4);
}

.plan-card__badge--2 {
  color: rgb(var(--blue-rgb));
  border-color: rgba(var(--blue-rgb), 0.4);
}

/* Body */
.plan-card__body {
  padding: 18px 20px 16px;
}

.plan-card__title {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.plan-card__destination,
.plan-card__dates,
.plan-card__budget {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.plan-card__budget {
  color: var(--text-muted);
}

/* Actions */
.plan-card__actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  border-top: 1px solid rgba(var(--brown-rgb), 0.1);
  padding-top: 12px;
}

.plan-card__btn {
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

.plan-card__btn:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.14);
}

.plan-card__btn--delete:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
  border-color: rgba(var(--red-rgb), 0.3);
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
  .plan-page {
    padding: 24px 0 48px;
  }

  .plan-header {
    margin-bottom: 24px;
  }

  .plan-header__title {
    font-size: 28px;
  }

  .plan-grid {
    grid-template-columns: 1fr;
  }

  .plan-card__title {
    overflow-wrap: break-word;
  }

  .plan-card__btn {
    padding: 8px 16px;
    font-size: 13px;
    min-height: 40px;
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
  .plan-header__title {
    font-size: 24px;
  }

  .plan-card__cover {
    height: 120px;
  }

  .add-plan-btn {
    padding: 12px 24px;
  }
}
</style>
