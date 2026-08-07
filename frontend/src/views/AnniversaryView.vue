<template>
  <div class="anniversary-page">
    <div class="container">
      <section class="together-section anim-fade-in-up">
        <div class="together-card">
          <div class="together-card__deco-top">
            <span class="deco-heart left">&#x2764;</span><span class="deco-line"></span
            ><span class="deco-heart right">&#x2764;</span>
          </div>
          <p class="together-label">我们已经在一起</p>
          <div class="together-number anim-count-glow">
            <span class="number-main">{{ coupleStore.daysTogether }}</span
            ><span class="number-unit">天</span>
          </div>
          <p v-if="coupleStore.startDate" class="together-date">从 {{ formatDate(coupleStore.startDate) }} 开始</p>
          <p v-else class="together-date">还没有设置在一起的日期哦</p>
          <div class="together-stats">
            <div class="stat-item">
              <span class="stat-value">{{ Math.floor(coupleStore.daysTogether / 7) }}</span
              ><span class="stat-label">周</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ Math.floor(coupleStore.daysTogether / 30) }}</span
              ><span class="stat-label">月</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ (coupleStore.daysTogether / 365).toFixed(1) }}</span
              ><span class="stat-label">年</span>
            </div>
          </div>
        </div>
      </section>

      <section v-if="!coupleStore.startDate" class="setup-section anim-fade-in-up stagger-2">
        <div class="setup-card">
          <h3>设置你们的纪念日</h3>
          <div class="setup-form">
            <input v-model="setupDate" type="date" class="input-vintage" /><button
              class="btn-vintage btn-vintage--red btn-vintage--small"
              :disabled="!setupDate"
              @click="handleSetup"
            >
              记住这一天
            </button>
          </div>
        </div>
      </section>

      <section class="anniversary-section">
        <div class="section-header anim-fade-in-up stagger-3">
          <h2 class="section-title">重要日子</h2>
          <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="openCreate">+ 添加纪念日</button>
        </div>
        <LoadingState v-if="loading" message="正在翻阅日历…" />
        <ErrorState v-else-if="loadError" message="纪念日暂时没有加载成功。" @retry="fetchAnniversaries" />
        <div v-else-if="anniversaries.length" class="anniversary-grid">
          <div
            v-for="(item, index) in anniversaries"
            :key="item.id"
            class="anniversary-item anim-polaroid-drop"
            :class="'stagger-' + ((index % 8) + 1)"
          >
            <CountdownCard :title="item.title" :date="item.date" :icon="item.icon || '💕'" :type="item.uiType" />
            <div class="anniversary-item__actions">
              <button type="button" class="anniversary-action" @click="openEdit(item)">编辑</button
              ><button type="button" class="anniversary-action anniversary-action--danger" @click="handleDelete(item)">
                删除
              </button>
            </div>
          </div>
        </div>
        <EmptyState v-else icon="📅" title="还没有纪念日" message="添加你们的重要日子，让每个纪念日都充满期待"
          ><button class="btn-vintage btn-vintage--red btn-vintage--small" @click="openCreate">
            添加第一个纪念日
          </button></EmptyState
        >
      </section>
    </div>

    <BaseModal
      v-model="showModal"
      :title="editingId ? '编辑纪念日' : '添加纪念日'"
      eyebrow="IMPORTANT DAY"
      :busy="saving"
      @close="resetEditor"
    >
      <form class="anniversary-form" @submit.prevent="handleSave">
        <div class="form-group">
          <label class="field-label" for="anniversary-title">纪念日名称</label
          ><input
            id="anniversary-title"
            v-model="form.title"
            type="text"
            class="input-vintage"
            maxlength="100"
            placeholder="例如：恋爱纪念日、第一次约会…"
          />
        </div>
        <div class="form-group">
          <label class="field-label" for="anniversary-date">日期</label
          ><input id="anniversary-date" v-model="form.date" type="date" class="input-vintage" />
        </div>
        <div class="form-group">
          <span class="field-label">类型</span>
          <div class="type-selector">
            <button
              v-for="type in typeOptions"
              :key="type.value"
              type="button"
              class="type-btn"
              :class="{ active: form.type === type.value }"
              @click="form.type = type.value"
            >
              {{ type.label }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <span class="field-label">图标</span>
          <div class="icon-selector">
            <button
              v-for="icon in iconOptions"
              :key="icon"
              type="button"
              class="icon-btn"
              :class="{ active: form.icon === icon }"
              @click="form.icon = icon"
            >
              {{ icon }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label class="field-label" for="anniversary-remark">备注</label
          ><textarea
            id="anniversary-remark"
            v-model="form.remark"
            class="input-vintage anniversary-form__remark"
            rows="3"
            maxlength="500"
            placeholder="写下一点特别的回忆…"
          ></textarea>
        </div>
      </form>
      <template #footer
        ><button
          type="button"
          class="btn-vintage btn-vintage--outline btn-vintage--small"
          :disabled="saving"
          @click="closeEditor"
        >
          取消</button
        ><button
          type="button"
          class="btn-vintage btn-vintage--red btn-vintage--small"
          :disabled="!canSave || saving"
          @click="handleSave"
        >
          {{ saving ? '保存中…' : '保存' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { createAnniversary, deleteAnniversary, getAnniversaries, updateAnniversary } from '@/api/anniversary'
import BaseModal from '@/components/BaseModal.vue'
import CountdownCard from '@/components/CountdownCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import {
  createAnniversaryForm,
  toAnniversaryPayload,
  toAnniversaryUiType
} from '@/features/anniversary/anniversaryForm'
import { useCoupleStore } from '@/stores/couple'
import { parseLocalDate } from '@/utils/date'

const coupleStore = useCoupleStore()
const anniversaries = ref([])
const loading = ref(true)
const loadError = ref(false)
const showModal = ref(false)
const saving = ref(false)
const editingId = ref(null)
const setupDate = ref('')
const form = ref(createAnniversaryForm())
const iconOptions = ['💕', '🎂', '✈️', '🌹', '💍', '🎄', '🌙', '⭐', '🎵', '📷']
const typeOptions = [
  { value: 'recurring', label: '每年重复' },
  { value: 'future', label: '倒计时' },
  { value: 'past', label: '已过去' }
]
const canSave = computed(() => Boolean(form.value.title.trim() && form.value.date))

function normalizeItem(item) {
  return { ...item, title: item.title || item.name || '', uiType: toAnniversaryUiType(item) }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = parseLocalDate(dateStr)
  return date.getFullYear() + '年' + (date.getMonth() + 1) + '月' + date.getDate() + '日'
}

async function fetchAnniversaries() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getAnniversaries()
    anniversaries.value = (res.data || []).map(normalizeItem)
  } catch {
    loadError.value = true
    anniversaries.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = createAnniversaryForm()
  showModal.value = true
}

function openEdit(item) {
  editingId.value = item.id
  form.value = createAnniversaryForm(item)
  showModal.value = true
}

function resetEditor() {
  editingId.value = null
  form.value = createAnniversaryForm()
}

function closeEditor() {
  showModal.value = false
  resetEditor()
}

async function handleSetup() {
  if (!setupDate.value) return
  try {
    const { updateCouple } = await import('@/api/auth')
    const res = await updateCouple({ togetherDate: setupDate.value })
    coupleStore.setCoupleInfo(
      res.data || { ...coupleStore.coupleInfo, startDate: setupDate.value, togetherDate: setupDate.value }
    )
  } catch {
    // 请求失败时保留服务端已确认的状态；错误由请求拦截器提示。
  }
}

async function handleSave() {
  if (!canSave.value || saving.value) return
  saving.value = true
  try {
    const payload = toAnniversaryPayload(form.value)
    if (editingId.value) {
      const id = editingId.value
      const res = await updateAnniversary(id, payload)
      const saved = normalizeItem(res.data || { ...payload, id })
      anniversaries.value = anniversaries.value.map((item) => (item.id === id ? { ...item, ...saved } : item))
    } else {
      const res = await createAnniversary(payload)
      const saved = normalizeItem(res.data || payload)
      if (saved.id) anniversaries.value = [saved, ...anniversaries.value]
      else await fetchAnniversaries()
    }
    closeEditor()
  } finally {
    saving.value = false
  }
}

async function handleDelete(item) {
  const confirmed = await requestConfirm({
    title: '删除纪念日',
    message: '确定要删除“' + item.title + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deleteAnniversary(item.id)
  anniversaries.value = anniversaries.value.filter((entry) => entry.id !== item.id)
}

onMounted(() => {
  fetchAnniversaries()
  if (!coupleStore.hasCouple) coupleStore.fetchCoupleInfo()
})
</script>

<style scoped>
.anniversary-page {
  padding: 40px 0 80px;
}

/* 在一起天数 */
.together-section {
  display: flex;
  justify-content: center;
  margin-bottom: 48px;
}

.together-card {
  text-align: center;
  padding: 48px 60px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lifted);
  position: relative;
  overflow: hidden;
}

.together-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 0%, rgba(var(--red-rgb), 0.06) 0%, transparent 60%);
  pointer-events: none;
}

.together-card__deco-top {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
}

.deco-heart {
  color: var(--faded-red);
  font-size: 14px;
  animation: heartbeat 2.5s ease-in-out infinite;
}

.deco-heart.right {
  animation-delay: 0.3s;
}

.deco-line {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--kraft-paper), transparent);
}

.together-label {
  font-family: var(--font-display);
  font-size: 16px;
  color: var(--text-secondary);
  letter-spacing: 3px;
  margin-bottom: 12px;
}

.together-number {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.number-main {
  font-family: var(--font-serif);
  font-size: 72px;
  font-weight: 700;
  color: var(--faded-red);
  line-height: 1;
  letter-spacing: -2px;
}

.number-unit {
  font-family: var(--font-handwrite);
  font-size: 28px;
  color: var(--text-secondary);
}

.together-date {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 1px;
  margin-bottom: 24px;
}

/* 小统计 */
.together-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding-top: 20px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.15);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-value {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--brown-medium);
}

.stat-label {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: rgba(var(--brown-rgb), 0.15);
}

/* 设置区域 */
.setup-section {
  display: flex;
  justify-content: center;
  margin-bottom: 48px;
}

.setup-card {
  text-align: center;
  padding: 24px 32px;
  background: rgba(var(--cream-rgb), 0.8);
  border: var(--border-dashed);
  border-radius: var(--radius-md);
}

.setup-card h3 {
  font-family: var(--font-handwrite);
  font-size: 20px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.setup-form {
  display: flex;
  gap: 12px;
  align-items: center;
}

.setup-form .input-vintage {
  width: auto;
}

/* 纪念日列表 */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.section-title {
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: var(--brown-deep);
}

.anniversary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 28px;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 14, 10, 0.6);
  backdrop-filter: blur(4px);
  padding: 20px;
}

.modal-card {
  width: 100%;
  max-width: 420px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lifted);
}

.modal-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid rgba(var(--brown-rgb), 0.1);
}

.modal-card__header h3 {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
}

.modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--text-muted);
  border-radius: 50%;
  transition: all var(--transition-fast);
}

.modal-close:hover {
  background: rgba(var(--brown-rgb), 0.08);
}

.modal-card__body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
}

.type-selector {
  display: flex;
  gap: 8px;
}

.type-btn {
  flex: 1;
  padding: 8px 12px;
  font-size: 13px;
  font-family: var(--font-display);
  color: var(--text-secondary);
  background: rgba(var(--cream-rgb), 0.6);
  border: 1.5px solid rgba(var(--brown-rgb), 0.2);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.type-btn.active {
  border-color: var(--faded-red);
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.06);
}

.icon-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border: 1.5px solid rgba(var(--brown-rgb), 0.15);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  border-color: var(--kraft-paper);
  transform: scale(1.1);
}

.icon-btn.active {
  border-color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.08);
  transform: scale(1.1);
}

.modal-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 20px;
  border-top: 1px solid rgba(var(--brown-rgb), 0.08);
}

.modal-enter-active {
  animation: modal-backdrop-in 0.3s ease both;
}

.modal-leave-active {
  animation: modal-backdrop-in 0.2s ease reverse both;
}

@media (max-width: 768px) {
  .anniversary-page {
    padding: 24px 0 60px;
  }

  .together-section {
    margin-bottom: 32px;
  }

  .together-card {
    padding: 36px 24px;
  }

  .number-main {
    font-size: 52px;
  }

  .number-unit {
    font-size: 22px;
  }

  .together-stats {
    gap: 16px;
  }

  .section-header {
    margin-bottom: 20px;
  }

  .section-title {
    font-size: 22px;
  }

  .anniversary-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 20px;
  }

  .anniversary-item :deep(.number-value) {
    font-size: 34px;
  }

  .anniversary-action {
    padding: 8px 16px;
    font-size: 13px;
    min-height: 40px;
  }

  .setup-card {
    padding: 20px;
  }

  .setup-form {
    flex-direction: column;
    align-items: stretch;
  }

  .setup-form .input-vintage {
    width: 100%;
  }

  .anniversary-form .input-vintage {
    font-size: 16px;
  }

  .icon-btn {
    width: 40px;
    height: 40px;
  }
}

@media (max-width: 480px) {
  .together-card {
    padding: 28px 16px;
  }

  .deco-line {
    width: 40px;
  }

  .number-main {
    font-size: 44px;
  }

  .together-stats {
    gap: 12px;
  }

  .stat-value {
    font-size: 18px;
  }

  .anniversary-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

.anniversary-item {
  display: grid;
  gap: 10px;
}
.anniversary-item__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 8px;
}
.anniversary-action {
  padding: 5px 12px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 12px;
  background: rgba(var(--cream-rgb), 0.7);
  border: 1px solid rgba(var(--brown-rgb), 0.18);
  border-radius: 14px;
  cursor: pointer;
}
.anniversary-action:hover {
  color: var(--brown-deep);
  background: var(--cream-warm);
}
.anniversary-action--danger:hover {
  color: var(--faded-red);
  border-color: rgba(var(--red-rgb), 0.35);
}
.anniversary-form .form-group {
  margin-bottom: 20px;
}
.anniversary-form .form-group:last-child {
  margin-bottom: 0;
}
.anniversary-form__remark {
  resize: vertical;
}
</style>
