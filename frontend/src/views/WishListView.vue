<template>
  <div class="wish-page">
    <div class="container">
      <header class="wish-header anim-fade-in-up">
        <h1 class="wish-header__title">心愿许愿池</h1>
        <p class="wish-header__subtitle">把想做的事写下来，一起实现吧</p>
        <div class="wish-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <div class="wish-tabs anim-fade-in-up stagger-2">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="wish-tab"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>
      <button class="add-wish-btn anim-fade-in-up stagger-2" @click="openCreate">
        <span class="add-icon">✨</span><span>许个愿</span>
      </button>

      <LoadingState v-if="loading" message="正在翻阅心愿…" />
      <ErrorState v-else-if="loadError" message="心愿列表暂时没有加载成功。" @retry="fetchWishes" />
      <div v-else-if="filteredWishes.length" class="wish-grid anim-fade-in-up stagger-3">
        <div
          v-for="(wish, idx) in filteredWishes"
          :key="wish.id"
          class="wish-card"
          :class="['wish-card--' + wish.category, { 'wish-card--done': Number(wish.status) === 1 }]"
          :style="{ animationDelay: idx * 0.08 + 's' }"
        >
          <div v-if="Number(wish.status) === 1" class="wish-card__badge"><span>✓</span> 已达成</div>
          <div v-else-if="Number(wish.priority) === 2" class="wish-card__badge wish-card__badge--urgent">
            <span>★</span> 紧急
          </div>
          <div class="wish-card__icon">{{ wish.icon || categoryIcons[wish.category] || '✨' }}</div>
          <h3 class="wish-card__title">{{ wish.title }}</h3>
          <p v-if="wish.description" class="wish-card__desc">{{ wish.description }}</p>
          <div class="wish-card__meta">
            <span class="wish-card__category">{{ categoryLabels[wish.category] || '其他' }}</span
            ><span class="wish-card__author">{{ wish.creatorNickname || '匿名' }}</span>
          </div>
          <div class="wish-card__actions">
            <button
              v-if="Number(wish.status) === 0"
              class="wish-card__btn wish-card__btn--complete"
              title="标记已达成"
              @click="handleComplete(wish)"
            >
              ✓ 达成
            </button>
            <button v-if="canManageWish(wish)" class="wish-card__btn" title="编辑" @click="openEdit(wish)">编辑</button>
            <button
              v-if="canManageWish(wish)"
              class="wish-card__btn wish-card__btn--delete"
              title="删除"
              @click="handleDelete(wish)"
            >
              删除
            </button>
          </div>
          <div v-if="Number(wish.status) === 1 && wish.completerNickname" class="wish-card__completed-info">
            {{ wish.completerNickname }} 标记达成
          </div>
        </div>
      </div>
      <EmptyState
        v-else
        icon="✨"
        :title="activeTab === 'done' ? '还没有达成的心愿' : '还没有许愿'"
        :message="activeTab === 'done' ? '完成的心愿会出现在这里' : '写下第一个心愿，开始你们的愿望清单吧'"
      >
        <button v-if="activeTab !== 'done'" class="btn-vintage btn-vintage--red" @click="openCreate">许第一个愿</button>
      </EmptyState>
    </div>

    <BaseModal
      v-model="showModal"
      :title="editingId ? '编辑心愿' : '许个愿'"
      eyebrow="WISH LIST"
      :busy="submitting"
      @close="resetEditor"
    >
      <form class="wish-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="wish-title">心愿标题</label
          ><input
            id="wish-title"
            v-model="form.title"
            class="input-vintage"
            placeholder="想做什么？"
            maxlength="200"
            required
          />
        </div>
        <div class="form-group">
          <label for="wish-description">详细描述</label
          ><textarea
            id="wish-description"
            v-model="form.description"
            class="input-vintage"
            rows="3"
            placeholder="描述一下你的心愿…"
            maxlength="1000"
          ></textarea>
        </div>
        <div class="form-row">
          <div class="form-group form-group--half">
            <label>分类</label>
            <div class="category-picker">
              <button
                v-for="(label, key) in categoryLabels"
                :key="key"
                type="button"
                class="category-chip"
                :class="{ active: form.category === key }"
                @click="selectCategory(key)"
              >
                {{ categoryIcons[key] }} {{ label }}
              </button>
            </div>
          </div>
          <div class="form-group form-group--half">
            <label>优先级</label>
            <div class="priority-picker">
              <button
                v-for="priority in priorities"
                :key="priority.value"
                type="button"
                class="priority-chip"
                :class="{ active: form.priority === priority.value }"
                @click="form.priority = priority.value"
              >
                {{ priority.icon }} {{ priority.label }}
              </button>
            </div>
          </div>
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
          {{ submitting ? '保存中…' : editingId ? '保存修改' : '许愿 ✨' }}
        </button></template
      >
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { completeWish, createWish, deleteWish, getWishes, updateWish } from '@/api/wish'
import BaseModal from '@/components/BaseModal.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { createWishForm, toWishPayload } from '@/features/wish/wishForm'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id || userStore.userInfo?.userId)
const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const editingId = ref(null)
const activeTab = ref('all')
const wishes = ref([])
const tabs = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '许愿中' },
  { value: 'done', label: '已达成' }
]
const categoryLabels = { travel: '旅行', food: '美食', movie: '电影', gift: '礼物', other: '其他' }
const categoryIcons = { travel: '✈️', food: '🍴', movie: '🎬', gift: '🎁', other: '✨' }
const priorities = [
  { value: 0, label: '普通', icon: '○' },
  { value: 1, label: '重要', icon: '★' },
  { value: 2, label: '紧急', icon: '⚡' }
]
const form = ref(createWishForm())

const filteredWishes = computed(() => {
  if (activeTab.value === 'pending') return wishes.value.filter((wish) => Number(wish.status) === 0)
  if (activeTab.value === 'done') return wishes.value.filter((wish) => Number(wish.status) === 1)
  return wishes.value
})

function selectCategory(key) {
  form.value.category = key
  form.value.icon = categoryIcons[key]
}

function canManageWish(wish) {
  return currentUserId.value != null && String(wish.userId) === String(currentUserId.value)
}

async function fetchWishes() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getWishes()
    wishes.value = res.data || []
  } catch {
    loadError.value = true
    wishes.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = createWishForm()
  showModal.value = true
}

function openEdit(wish) {
  editingId.value = wish.id
  form.value = createWishForm(wish)
  showModal.value = true
}

function resetEditor() {
  editingId.value = null
  form.value = createWishForm()
}

function closeModal() {
  showModal.value = false
  resetEditor()
}

async function handleSubmit() {
  if (!form.value.title.trim() || submitting.value) return
  submitting.value = true
  try {
    const payload = toWishPayload(form.value)
    if (editingId.value) {
      const id = editingId.value
      const res = await updateWish(id, payload)
      const saved = res.data || payload
      wishes.value = wishes.value.map((wish) => (wish.id === id ? { ...wish, ...saved } : wish))
    } else {
      const res = await createWish(payload)
      if (res.data?.id) wishes.value = [res.data, ...wishes.value]
      else await fetchWishes()
    }
    closeModal()
  } finally {
    submitting.value = false
  }
}

async function handleComplete(wish) {
  const res = await completeWish(wish.id)
  const saved = res.data
  wishes.value = wishes.value.map((item) => (item.id === wish.id ? (saved?.id ? saved : { ...item, status: 1 }) : item))
}

async function handleDelete(wish) {
  const confirmed = await requestConfirm({
    title: '删除心愿',
    message: '确定要删除“' + wish.title + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deleteWish(wish.id)
  wishes.value = wishes.value.filter((item) => item.id !== wish.id)
}

onMounted(fetchWishes)
</script>

<style scoped>
.wish-page {
  padding: 32px 0 60px;
}

.wish-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.wish-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.wish-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-muted);
  letter-spacing: 2px;
}

.wish-header__deco {
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

/* Tabs */
.wish-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}

.wish-tab {
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

.wish-tab:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.1);
}

.wish-tab.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

/* Add button */
.add-wish-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto 32px;
  padding: 12px 32px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 1px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--mustard), #b8931f);
  border-radius: var(--radius-sm);
  box-shadow: 0 3px 12px rgba(var(--mustard-rgb), 0.3);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.add-wish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(var(--mustard-rgb), 0.4);
}

.add-icon {
  font-size: 18px;
}

/* Wish Grid */
.wish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.wish-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 24px;
  position: relative;
  transition: all var(--transition-normal);
  animation: fade-in-up 0.4s ease both;
}

.wish-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: inherit;
  background: var(--paper-texture);
  opacity: 0.25;
  pointer-events: none;
}

.wish-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lifted);
}

.wish-card--done {
  opacity: 0.7;
}

.wish-card--done .wish-card__title {
  text-decoration: line-through;
  color: var(--text-muted);
}

/* Badge */
.wish-card__badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 3px 10px;
  font-size: 11px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  color: var(--olive);
  background: rgba(var(--olive-rgb), 0.12);
  border: 1px solid rgba(var(--olive-rgb), 0.3);
  border-radius: 12px;
}

.wish-card__badge--urgent {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
  border-color: rgba(var(--red-rgb), 0.3);
}

/* Icon */
.wish-card__icon {
  font-size: 32px;
  margin-bottom: 12px;
  filter: sepia(0.2);
}

.wish-card__title {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.wish-card__desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
}

.wish-card__meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.wish-card__category {
  padding: 2px 8px;
  background: rgba(var(--kraft-rgb), 0.12);
  border-radius: 8px;
}

/* Actions */
.wish-card__actions {
  display: flex;
  gap: 8px;
  border-top: 1px solid rgba(var(--brown-rgb), 0.1);
  padding-top: 12px;
}

.wish-card__btn {
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

.wish-card__btn--complete:hover {
  color: var(--olive);
  background: rgba(var(--olive-rgb), 0.1);
  border-color: rgba(var(--olive-rgb), 0.3);
}

.wish-card__btn--delete:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
  border-color: rgba(var(--red-rgb), 0.3);
}

.wish-card__completed-info {
  font-size: 11px;
  color: var(--olive);
  margin-top: 8px;
  font-style: italic;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(30, 20, 10, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.wish-modal {
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 28px;
}

.modal-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.modal-card__header h2 {
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: var(--brown-deep);
}

.modal-card__close {
  font-size: 24px;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.modal-card__close:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
}

/* Form */
.wish-form .form-group {
  margin-bottom: 20px;
}

.wish-form label {
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

.category-picker,
.priority-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-chip,
.priority-chip {
  padding: 6px 12px;
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

.category-chip:hover,
.priority-chip:hover {
  background: rgba(var(--kraft-rgb), 0.2);
}

.category-chip.active,
.priority-chip.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
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
  .wish-page {
    padding: 24px 0 48px;
  }

  .wish-header {
    margin-bottom: 24px;
  }

  .wish-header__title {
    font-size: 28px;
  }

  .wish-grid {
    grid-template-columns: 1fr;
  }

  .wish-card {
    padding: 20px;
  }

  .wish-card__icon {
    font-size: 28px;
  }

  .wish-card__title,
  .wish-card__desc {
    overflow-wrap: break-word;
  }

  .wish-card__actions {
    flex-wrap: wrap;
  }

  .wish-card__btn {
    padding: 8px 16px;
    font-size: 13px;
    min-height: 40px;
  }

  .wish-tabs {
    gap: 6px;
    flex-wrap: wrap;
  }

  .wish-tab {
    padding: 6px 16px;
    font-size: 13px;
  }

  .wish-form .input-vintage {
    font-size: 16px;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .category-chip,
  .priority-chip {
    padding: 8px 14px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .wish-header__title {
    font-size: 24px;
  }

  .wish-card {
    padding: 16px;
  }

  .wish-tab {
    padding: 6px 12px;
  }

  .add-wish-btn {
    padding: 12px 24px;
  }
}

.wish-card__btn:not(.wish-card__btn--complete):not(.wish-card__btn--delete):hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.14);
}
</style>
