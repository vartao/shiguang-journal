<template>
  <div class="capsule-page">
    <div class="container">
      <header class="capsule-header anim-fade-in-up">
        <h1 class="capsule-header__title">时间胶囊</h1>
        <p class="capsule-header__subtitle">写一封信，寄给未来的我们</p>
        <div class="capsule-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <button class="add-capsule-btn anim-fade-in-up stagger-2" @click="openCreate">
        <span class="add-icon">⏳</span><span>封存一颗胶囊</span>
      </button>

      <LoadingState v-if="loading" message="正在翻找胶囊…" />
      <ErrorState v-else-if="loadError" message="胶囊列表暂时没有加载成功。" @retry="fetchCapsules" />
      <template v-else>
        <EmptyState
          v-if="!capsules.length"
          icon="⏳"
          title="还没有时间胶囊"
          message="写一封信封存起来，约定一个未来的日子一起拆开"
        >
          <button class="btn-vintage btn-vintage--red" @click="openCreate">封存第一颗胶囊</button>
        </EmptyState>

        <template v-else>
          <!-- 等待开启 -->
          <section v-if="waitingCapsules.length" class="capsule-section anim-fade-in-up stagger-3">
            <h2 class="capsule-section__title">等待开启</h2>
            <div class="capsule-grid">
              <div
                v-for="(capsule, idx) in waitingCapsules"
                :key="capsule.id"
                class="capsule-card"
                :class="{ 'capsule-card--ready': capsule.canOpen }"
                :style="{ animationDelay: idx * 0.08 + 's' }"
                @click="capsule.canOpen && handleOpen(capsule)"
              >
                <span v-if="capsule.canOpen" class="capsule-card__stamp stamp-badge">可拆封</span>
                <div class="capsule-card__icon">{{ capsule.canOpen ? '💌' : '✉️' }}</div>
                <h3 class="capsule-card__title">{{ capsule.title }}</h3>
                <p class="capsule-card__date">开启日期 {{ formatDate(capsule.openDate) }}</p>
                <p v-if="capsule.canOpen" class="capsule-card__countdown capsule-card__countdown--ready">可以拆了 ✨</p>
                <p v-else class="capsule-card__countdown">倒计时 {{ capsule.daysLeft }} 天</p>
                <div class="capsule-card__meta">
                  <span>{{ capsule.creatorNickname || '匿名' }} 封存</span>
                  <button
                    v-if="canManage(capsule)"
                    class="capsule-card__btn capsule-card__btn--delete"
                    title="删除"
                    @click.stop="handleDelete(capsule)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </section>

          <!-- 已开启 -->
          <section v-if="openedCapsules.length" class="capsule-section anim-fade-in-up stagger-4">
            <h2 class="capsule-section__title">已开启</h2>
            <div class="capsule-grid">
              <div
                v-for="(capsule, idx) in openedCapsules"
                :key="capsule.id"
                class="capsule-card capsule-card--opened"
                :style="{ animationDelay: idx * 0.08 + 's' }"
                @click="showLetter(capsule)"
              >
                <span class="capsule-card__stamp stamp-badge stamp-badge--done">已拆封</span>
                <div class="capsule-card__icon">📖</div>
                <h3 class="capsule-card__title">{{ capsule.title }}</h3>
                <p v-if="capsule.message" class="capsule-card__excerpt">{{ excerpt(capsule.message) }}</p>
                <p class="capsule-card__date">
                  开启于 {{ formatDateTime(capsule.openedTime)
                  }}<template v-if="capsule.openerNickname"> · {{ capsule.openerNickname }} 拆开</template>
                </p>
                <div class="capsule-card__meta">
                  <span>{{ capsule.creatorNickname || '匿名' }} 封存</span>
                  <button
                    v-if="canManage(capsule)"
                    class="capsule-card__btn capsule-card__btn--delete"
                    title="删除"
                    @click.stop="handleDelete(capsule)"
                  >
                    删除
                  </button>
                </div>
              </div>
            </div>
          </section>
        </template>
      </template>
    </div>

    <!-- 新建胶囊 -->
    <BaseModal v-model="showModal" title="封存一颗胶囊" eyebrow="TIME CAPSULE" :busy="submitting" @close="resetEditor">
      <form class="capsule-form" @submit.prevent="handleSubmit">
        <div class="form-group">
          <label for="capsule-title">胶囊标题</label
          ><input
            id="capsule-title"
            v-model="form.title"
            class="input-vintage"
            placeholder="给这颗胶囊起个名字"
            maxlength="200"
            required
          />
        </div>
        <div class="form-group">
          <label for="capsule-message">想对未来的我们说的话</label
          ><textarea
            id="capsule-message"
            v-model="form.message"
            class="input-vintage"
            rows="6"
            placeholder="写下此刻的心情、约定或者小秘密…"
            maxlength="5000"
          ></textarea>
        </div>
        <div class="form-group">
          <label for="capsule-open-date">开启日期</label
          ><input
            id="capsule-open-date"
            v-model="form.openDate"
            type="date"
            class="input-vintage"
            :min="minOpenDate"
            required
          />
          <p class="form-hint">在开启日期到来之前，内容对双方都保密</p>
        </div>
        <div class="form-group">
          <label>附上照片（选填）</label>
          <ImageUploader v-model="form.photoUrls" :max-count="6" category="couple" />
        </div>
      </form>
      <template #footer
        ><button type="button" class="btn-vintage btn-vintage--outline" :disabled="submitting" @click="closeModal">
          取消</button
        ><button
          type="button"
          class="btn-vintage btn-vintage--red"
          :disabled="!canSubmit || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '封存中…' : '封存 ⏳' }}
        </button></template
      >
    </BaseModal>

    <!-- 拆开的信件 -->
    <BaseModal v-model="showLetterModal" :title="openedCapsule?.title || '时间胶囊'" eyebrow="OPENED LETTER">
      <div v-if="openedCapsule" class="letter">
        <p class="letter__meta">书写于 {{ formatDateTime(openedCapsule.createTime) }}</p>
        <p v-if="openedCapsule.message" class="letter__content">{{ openedCapsule.message }}</p>
        <div v-if="openedCapsule.photoUrls?.length" class="letter__photos">
          <img
            v-for="(url, index) in openedCapsule.photoUrls"
            :key="index"
            :src="url"
            :alt="'照片' + (index + 1)"
            class="letter__photo"
          />
        </div>
        <p class="letter__footer">
          开启日期 {{ formatDate(openedCapsule.openDate) }} · 开启于 {{ formatDateTime(openedCapsule.openedTime)
          }}<template v-if="openedCapsule.openerNickname"> · {{ openedCapsule.openerNickname }} 拆开</template>
        </p>
      </div>
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { createCapsule, deleteCapsule, getCapsules, openCapsule } from '@/api/capsule'
import BaseModal from '@/components/BaseModal.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { useUserStore } from '@/stores/user'
import { toLocalDateInputValue } from '@/utils/date'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id || userStore.userInfo?.userId)
const loading = ref(false)
const loadError = ref(false)
const submitting = ref(false)
const showModal = ref(false)
const showLetterModal = ref(false)
const capsules = ref([])
const openedCapsule = ref(null)

const createForm = () => ({ title: '', message: '', openDate: '', photoUrls: [] })
const form = ref(createForm())

const minOpenDate = toLocalDateInputValue(new Date(Date.now() + 24 * 60 * 60 * 1000))

const waitingCapsules = computed(() => capsules.value.filter((capsule) => Number(capsule.isOpened) !== 1))
const openedCapsules = computed(() => capsules.value.filter((capsule) => Number(capsule.isOpened) === 1))
const canSubmit = computed(() => form.value.title.trim() && form.value.openDate && form.value.openDate >= minOpenDate)

function canManage(capsule) {
  return currentUserId.value != null && String(capsule.userId) === String(currentUserId.value)
}

function formatDate(value) {
  return typeof value === 'string' ? value.slice(0, 10) : ''
}

function formatDateTime(value) {
  return typeof value === 'string' ? value.slice(0, 16) : ''
}

function excerpt(message) {
  const text = String(message).replace(/\s+/g, ' ').trim()
  return text.length > 60 ? text.slice(0, 60) + '…' : text
}

async function fetchCapsules() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getCapsules()
    capsules.value = res.data || []
  } catch {
    loadError.value = true
    capsules.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = createForm()
  showModal.value = true
}

function resetEditor() {
  form.value = createForm()
}

function closeModal() {
  showModal.value = false
  resetEditor()
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    await createCapsule({
      title: form.value.title.trim(),
      message: form.value.message.trim() || null,
      openDate: form.value.openDate,
      photoUrls: form.value.photoUrls
    })
    closeModal()
    await fetchCapsules()
  } finally {
    submitting.value = false
  }
}

async function handleOpen(capsule) {
  const confirmed = await requestConfirm({
    title: '拆开胶囊',
    message: '拆开这颗胶囊吗？拆开后双方都可以看到完整内容。',
    confirmText: '拆开 ✨'
  })
  if (!confirmed) return
  const res = await openCapsule(capsule.id)
  const saved = res.data
  if (saved?.id) {
    capsules.value = capsules.value.map((item) => (item.id === capsule.id ? saved : item))
    showLetter(saved)
  } else {
    await fetchCapsules()
  }
}

function showLetter(capsule) {
  openedCapsule.value = capsule
  showLetterModal.value = true
}

async function handleDelete(capsule) {
  const confirmed = await requestConfirm({
    title: '删除胶囊',
    message: '确定要删除“' + capsule.title + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deleteCapsule(capsule.id)
  capsules.value = capsules.value.filter((item) => item.id !== capsule.id)
}

onMounted(fetchCapsules)
</script>

<style scoped>
.capsule-page {
  padding: 32px 0 60px;
}

.capsule-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.capsule-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.capsule-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.capsule-header__deco {
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
.add-capsule-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 auto 32px;
  padding: 12px 32px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 1px;
  color: rgb(var(--cream-static-rgb));
  background: linear-gradient(145deg, var(--mustard), var(--kraft-dark));
  border-radius: var(--radius-sm);
  box-shadow: 0 3px 12px rgba(var(--mustard-rgb), 0.3);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.add-capsule-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(var(--mustard-rgb), 0.4);
}

.add-icon {
  font-size: 18px;
}

/* Sections */
.capsule-section {
  margin-bottom: 36px;
}

.capsule-section__title {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
  margin-bottom: 16px;
  padding-left: 4px;
  border-left: 3px solid rgba(var(--mustard-rgb), 0.6);
}

/* Grid & cards */
.capsule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.capsule-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 24px;
  position: relative;
  transition: all var(--transition-normal);
  animation: fade-in-up 0.4s ease both;
}

.capsule-card::before {
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

.capsule-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lifted);
}

/* 到期可拆：高亮 + 脉冲 */
.capsule-card--ready {
  cursor: pointer;
  border-color: rgba(var(--mustard-rgb), 0.6);
  animation:
    fade-in-up 0.4s ease both,
    capsule-pulse 1.8s ease-in-out infinite;
}

.capsule-card--opened {
  cursor: pointer;
}

.capsule-card__stamp {
  position: absolute;
  top: 14px;
  right: 14px;
}

.capsule-card__icon {
  font-size: 32px;
  margin-bottom: 12px;
  filter: sepia(0.2);
}

.capsule-card__title {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.capsule-card__date {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.capsule-card__countdown {
  display: inline-block;
  padding: 3px 12px;
  font-size: 12px;
  font-family: var(--font-display);
  letter-spacing: 1px;
  color: var(--dusty-blue);
  background: rgba(var(--blue-rgb), 0.12);
  border: 1px solid rgba(var(--blue-rgb), 0.3);
  border-radius: 12px;
  margin-bottom: 12px;
}

.capsule-card__countdown--ready {
  color: var(--mustard);
  background: rgba(var(--mustard-rgb), 0.12);
  border-color: rgba(var(--mustard-rgb), 0.45);
}

.capsule-card__excerpt {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 10px;
}

.capsule-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid rgba(var(--brown-rgb), 0.1);
  padding-top: 12px;
}

.capsule-card__btn {
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

.capsule-card__btn--delete:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
  border-color: rgba(var(--red-rgb), 0.3);
}

/* Form */
.capsule-form .form-group {
  margin-bottom: 20px;
}

.capsule-form label {
  display: block;
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

/* Letter */
.letter {
  background: var(--letter-paper);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  padding: 24px;
}

.letter__meta {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 16px;
  letter-spacing: 1px;
}

.letter__content {
  font-family: var(--font-handwrite);
  font-size: 18px;
  color: var(--text-handwrite);
  line-height: 1.9;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  margin-bottom: 16px;
}

.letter__photos {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 16px;
}

.letter__photo {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 4px solid var(--photo-paper);
  box-shadow: var(--shadow-soft);
}

.letter__footer {
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid rgba(var(--brown-rgb), 0.1);
  padding-top: 12px;
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

@keyframes capsule-pulse {
  0%,
  100% {
    box-shadow: var(--shadow-card);
  }
  50% {
    box-shadow:
      0 0 0 6px rgba(var(--mustard-rgb), 0.18),
      var(--shadow-card);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .capsule-page {
    padding: 24px 0 48px;
  }

  .capsule-header {
    margin-bottom: 24px;
  }

  .capsule-header__title {
    font-size: 28px;
  }

  .capsule-grid {
    grid-template-columns: 1fr;
  }

  .capsule-card {
    padding: 20px;
  }

  .capsule-card__icon {
    font-size: 28px;
  }

  .capsule-card__title,
  .capsule-card__excerpt {
    overflow-wrap: break-word;
  }

  .capsule-form .input-vintage {
    font-size: 16px;
  }

  .letter__photos {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .capsule-header__title {
    font-size: 24px;
  }

  .capsule-card {
    padding: 16px;
  }

  .add-capsule-btn {
    padding: 12px 24px;
  }

  .letter {
    padding: 16px;
  }

  .letter__photos {
    gap: 6px;
  }
}
</style>
