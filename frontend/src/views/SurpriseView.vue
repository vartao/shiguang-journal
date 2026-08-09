<template>
  <div class="surprise-page">
    <div class="container">
      <!-- 页面标题 -->
      <header class="surprise-header anim-fade-in-up">
        <div class="header-deco">
          <span class="sparkle anim-twinkle">&#x2728;</span>
        </div>
        <h1 class="surprise-header__title">惊喜盒子</h1>
        <p class="surprise-header__subtitle">那些藏在时光里的小心意，等待被拆开</p>
      </header>

      <!-- 操作 -->
      <div class="surprise-toolbar anim-fade-in-up stagger-2">
        <button class="btn-vintage btn-vintage--red btn-vintage--small" @click="showCreateModal = true">
          <span>&#x1F381;</span> 准备一个惊喜
        </button>
      </div>

      <!-- 加载 -->
      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在寻找惊喜...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="surprises.length === 0" class="empty-state">
        <div class="empty-state__icon">&#x1F381;</div>
        <h3 class="empty-state__title">还没有惊喜</h3>
        <p class="empty-state__desc">给TA准备一个特别的惊喜吧，可以是一封信、一段话、或者一个约定</p>
        <button class="btn-vintage btn-vintage--red" @click="showCreateModal = true">准备惊喜</button>
      </div>

      <!-- 惊喜列表 - 信封样式 -->
      <div v-else class="surprise-grid">
        <div
          v-for="(item, index) in surprises"
          :key="item.id"
          class="surprise-item anim-polaroid-drop"
          :class="'stagger-' + ((index % 8) + 1)"
        >
          <EnvelopeCard
            :title="item.title"
            :preview="item.content ? item.content.slice(0, 60) + '...' : item.hint || '一封写给你的信...'"
            :from="item.fromName || item.creatorNickname || '爱你的人'"
            :locked="!isItemUnlocked(item)"
            :unlock-hint="getUnlockHint(item)"
            @click="handleEnvelopeClick(item)"
            @open="goToDetail(item.id)"
          />
          <button
            v-if="isMySurprise(item)"
            class="surprise-delete"
            title="删除这个惊喜"
            @click.stop="handleDelete(item)"
          >
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- 创建惊喜模态框 -->
    <BaseModal v-model="showCreateModal" title="&#x1F381; 准备惊喜">
      <div class="surprise-form">
        <div class="form-group">
          <label class="field-label">惊喜标题</label>
          <input
            v-model="newSurprise.title"
            type="text"
            class="input-vintage"
            placeholder="例如：给你的一封情书、100天纪念..."
          />
        </div>
        <div class="form-group">
          <label class="field-label">惊喜内容</label>
          <textarea
            v-model="newSurprise.content"
            class="input-vintage surprise-textarea"
            rows="6"
            placeholder="写下你想对TA说的话...&#10;这封信会在解锁那天被TA看到"
          ></textarea>
        </div>
        <div class="form-group">
          <label class="field-label">解锁方式</label>
          <div class="type-selector">
            <button
              type="button"
              class="type-btn"
              :class="{ active: newSurprise.unlockCondition === 'date' }"
              @click="newSurprise.unlockCondition = 'date'"
            >
              到期解锁
            </button>
            <button
              type="button"
              class="type-btn"
              :class="{ active: newSurprise.unlockCondition === 'password' }"
              @click="newSurprise.unlockCondition = 'password'"
            >
              密码解锁
            </button>
            <button
              type="button"
              class="type-btn"
              :class="{ active: newSurprise.unlockCondition === 'click' }"
              @click="newSurprise.unlockCondition = 'click'"
            >
              点击解锁
            </button>
          </div>
        </div>
        <div v-if="newSurprise.unlockCondition === 'date'" class="form-group">
          <label class="field-label">解锁日期</label>
          <input v-model="newSurprise.unlockDate" type="date" class="input-vintage" :min="todayStr" />
          <span class="field-hint">到了这一天，TA就能拆开这个惊喜啦</span>
        </div>
        <div v-if="newSurprise.unlockCondition === 'password'" class="form-group">
          <label class="field-label">解锁密码</label>
          <input
            v-model="newSurprise.unlockPassword"
            type="text"
            class="input-vintage"
            placeholder="设一个只有你们知道的暗号"
          />
        </div>
        <div class="form-group">
          <label class="field-label">提示语（选填）</label>
          <input v-model="newSurprise.hint" type="text" class="input-vintage" placeholder="未解锁时展示的小提示" />
        </div>
        <div class="form-group">
          <label class="field-label">封面图片（选填）</label>
          <ImageUploader v-model="newSurprise.coverImages" :max-count="1" category="surprise" />
        </div>
      </div>
      <template #footer>
        <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="showCreateModal = false">
          取消
        </button>
        <button
          class="btn-vintage btn-vintage--red btn-vintage--small"
          :disabled="!canCreate || creating"
          @click="handleCreate"
        >
          {{ creating ? '包装中...' : '封好这个惊喜' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { requestConfirm } from '@/composables/confirmService'
import { useRouter } from 'vue-router'
import { getSurprises, createSurprise, deleteSurprise } from '@/api/surprise'
import { useUserStore } from '@/stores/user'
import EnvelopeCard from '@/components/EnvelopeCard.vue'
import BaseModal from '@/components/BaseModal.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import { differenceInCalendarDays, toLocalDateInputValue } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()

const surprises = ref([])
const loading = ref(true)
const showCreateModal = ref(false)
const creating = ref(false)

const todayStr = toLocalDateInputValue()

const newSurprise = reactive({
  title: '',
  content: '',
  unlockCondition: 'date',
  unlockDate: '',
  unlockPassword: '',
  hint: '',
  coverImages: []
})

const canCreate = computed(() => {
  if (!newSurprise.title.trim() || !newSurprise.content.trim()) return false
  if (newSurprise.unlockCondition === 'date') return !!newSurprise.unlockDate
  if (newSurprise.unlockCondition === 'password') return !!newSurprise.unlockPassword.trim()
  return true
})

function isItemUnlocked(item) {
  return item.unlocked === true || item.unlocked === 1 || item.isUnlocked === true
}

function getUnlockHint(item) {
  if (isItemUnlocked(item)) return '已拆开'
  if (item.unlockCondition === 'password') return '需要密码解锁'
  if (item.unlockCondition === 'click') return '点击即可拆开'
  if (!item.unlockDate) return '等待解锁中...'
  const diff = differenceInCalendarDays(item.unlockDate, new Date())
  if (Number.isNaN(diff)) return '等待解锁中...'
  if (diff <= 0) return '已经可以解锁啦！'
  if (diff === 1) return '明天就能拆开了'
  return `还有 ${diff} 天解锁`
}

function handleEnvelopeClick(item) {
  // 详情页负责解锁交互
  goToDetail(item.id)
}

function goToDetail(id) {
  router.push(`/surprise/${id}`)
}

function isMySurprise(item) {
  const myId = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (myId == null || item?.userId == null) return false
  return String(item.userId) === String(myId)
}

async function handleDelete(item) {
  const confirmed = await requestConfirm({
    title: '撤回惊喜',
    message: '确定要撤回“' + item.title + '”这个惊喜吗？',
    confirmText: '撤回'
  })
  if (!confirmed) return
  try {
    await deleteSurprise(item.id)
    surprises.value = surprises.value.filter((s) => s.id !== item.id)
  } catch {
    // handled
  }
}

async function fetchSurprises() {
  loading.value = true
  try {
    const res = await getSurprises()
    surprises.value = res.data || []
  } catch {
    surprises.value = []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!canCreate.value) return
  creating.value = true
  try {
    await createSurprise({
      title: newSurprise.title.trim(),
      content: newSurprise.content.trim(),
      unlockCondition: newSurprise.unlockCondition,
      unlockDate: newSurprise.unlockCondition === 'date' ? newSurprise.unlockDate : undefined,
      unlockPassword: newSurprise.unlockCondition === 'password' ? newSurprise.unlockPassword.trim() : undefined,
      hint: newSurprise.hint.trim() || undefined,
      coverImage: newSurprise.coverImages[0]?.url || ''
    })
    showCreateModal.value = false
    newSurprise.title = ''
    newSurprise.content = ''
    newSurprise.unlockCondition = 'date'
    newSurprise.unlockDate = ''
    newSurprise.unlockPassword = ''
    newSurprise.hint = ''
    newSurprise.coverImages = []
    await fetchSurprises()
  } catch {
    // handled
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  fetchSurprises()
})
</script>

<style scoped>
.surprise-page {
  padding: 40px 0 80px;
}

.surprise-header {
  text-align: center;
  margin-bottom: 28px;
}

.header-deco {
  margin-bottom: 12px;
}

.sparkle {
  font-size: 28px;
  display: inline-block;
}

.surprise-header__title {
  font-family: var(--font-handwrite);
  font-size: 34px;
  color: var(--brown-deep);
  letter-spacing: 3px;
  margin-bottom: 8px;
}

.surprise-header__subtitle {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

.surprise-toolbar {
  display: flex;
  justify-content: center;
  margin-bottom: 44px;
}

/* 惊喜网格 */
.surprise-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 40px;
  padding: 20px 0;
}

.surprise-item {
  width: 220px;
  position: relative;
}

.surprise-delete {
  display: block;
  width: 100%;
  margin-top: 10px;
  padding: 6px;
  font-size: 12px;
  font-family: var(--font-display);
  color: var(--text-muted);
  background: transparent;
  border: 1px dashed rgba(var(--brown-rgb), 0.2);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.surprise-delete:hover {
  color: var(--faded-red);
  border-color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.06);
}

/* 创建惊喜弹窗 */
.surprise-form {
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

.field-hint {
  font-size: 11px;
  color: var(--text-muted);
  font-style: italic;
}

.type-selector {
  display: flex;
  gap: 8px;
}

.type-btn {
  flex: 1;
  padding: 8px 10px;
  font-size: 12px;
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

.surprise-textarea {
  resize: vertical;
  min-height: 120px;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .surprise-page {
    padding: 28px 0 60px;
  }

  .surprise-header__title {
    font-size: 28px;
  }

  .surprise-toolbar {
    margin-bottom: 32px;
  }

  .surprise-grid {
    gap: 28px;
  }

  /* EnvelopeCard 信封主体固定 200px 宽，item 保持不小于 200px 避免信封溢出 */
  .surprise-item {
    width: 200px;
  }

  .surprise-delete {
    padding: 10px 6px;
  }

  .surprise-form {
    gap: 14px;
  }

  .input-vintage {
    font-size: 16px;
  }

  .type-btn {
    padding: 12px 10px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .surprise-header__title {
    font-size: 24px;
  }

  .surprise-grid {
    gap: 20px;
  }
}
</style>
