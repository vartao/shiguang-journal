<template>
  <div class="board-page">
    <div class="container">
      <header class="board-header anim-fade-in-up">
        <h1 class="board-header__title">留言板</h1>
        <p class="board-header__subtitle">悄悄话，只说给你听</p>
        <div class="board-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <div class="board-panel anim-fade-in-up stagger-2">
        <div ref="listRef" class="board-list">
          <LoadingState v-if="loading" message="正在翻阅悄悄话…" />
          <template v-else-if="loadError">
            <ErrorState message="悄悄话暂时没有加载成功。" @retry="fetchMessages" />
          </template>
          <EmptyState
            v-else-if="!messages.length"
            icon="💬"
            title="还没有悄悄话"
            message="说第一句吧，只有你们两个人看得到"
          />
          <template v-else>
            <template v-for="item in timeline" :key="item.key">
              <div v-if="item.type === 'divider'" class="time-divider">
                <span>{{ item.text }}</span>
              </div>
              <div v-else class="msg-row" :class="{ 'msg-row--mine': isMine(item.message) }">
                <img
                  v-if="item.showMeta"
                  class="msg-row__avatar"
                  :src="item.message.avatar || placeholderAvatar"
                  :alt="item.message.nickname || '头像'"
                />
                <span v-else class="msg-row__avatar-spacer"></span>
                <div class="msg-row__main">
                  <span v-if="item.showMeta" class="msg-row__name">{{ item.message.nickname || '匿名' }}</span>
                  <div
                    class="bubble"
                    :class="{ 'bubble--mine': isMine(item.message), 'bubble--active': activeId === item.message.id }"
                    @click="toggleActive(item.message)"
                  >
                    <p class="bubble__text">{{ item.message.content }}</p>
                    <button
                      v-if="isMine(item.message)"
                      class="bubble__delete"
                      title="删除"
                      aria-label="删除留言"
                      @click.stop="handleDelete(item.message)"
                    >
                      &times;
                    </button>
                  </div>
                  <span class="msg-row__time">{{ formatTime(item.message.createTime) }}</span>
                </div>
              </div>
            </template>
          </template>
        </div>

        <div class="board-input">
          <textarea
            ref="inputRef"
            v-model="draft"
            class="board-input__field"
            rows="1"
            maxlength="500"
            placeholder="写一句悄悄话…"
            @keydown.enter.exact.prevent="handleSend"
            @input="autoGrow"
          ></textarea>
          <button class="board-input__send" :disabled="!draft.trim() || sending" @click="handleSend">
            {{ sending ? '…' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { createMessage, deleteMessage, getMessages } from '@/api/message'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { useUserStore } from '@/stores/user'

const DIVIDER_GAP = 10 * 60 * 1000

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id || userStore.userInfo?.userId)
const loading = ref(false)
const loadError = ref(false)
const sending = ref(false)
const messages = ref([])
const draft = ref('')
const activeId = ref(null)
const listRef = ref(null)
const inputRef = ref(null)

const placeholderAvatar =
  'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 64 64%22%3E%3Ccircle cx=%2232%22 cy=%2232%22 r=%2232%22 fill=%22%23ddd%22/%3E%3Ctext x=%2232%22 y=%2238%22 text-anchor=%22middle%22 font-size=%2228%22 fill=%22%23999%22%3E%F0%9F%98%8A%3C/text%3E%3C/svg%3E'

// 组装时间线：时间分隔条 + 相邻同一人消息合并头像/昵称
const timeline = computed(() => {
  const items = []
  let prev = null
  messages.value.forEach((message, index) => {
    const needDivider = !prev || timeOf(message) - timeOf(prev) > DIVIDER_GAP
    if (needDivider) {
      items.push({ type: 'divider', key: 'divider-' + (message.id ?? index), text: formatDivider(message.createTime) })
    }
    const showMeta = needDivider || !prev || String(prev.userId) !== String(message.userId)
    items.push({ type: 'message', key: 'msg-' + (message.id ?? index), message, showMeta })
    prev = message
  })
  return items
})

function timeOf(message) {
  const t = new Date(message.createTime).getTime()
  return Number.isNaN(t) ? 0 : t
}

function isMine(message) {
  return currentUserId.value != null && String(message.userId) === String(currentUserId.value)
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${hour}:${minute}`
}

function formatDivider(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const now = new Date()
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return `今天 ${hour}:${minute}`
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  if (d.getFullYear() === now.getFullYear()) return `${month}-${day} ${hour}:${minute}`
  return `${d.getFullYear()}-${month}-${day} ${hour}:${minute}`
}

function scrollToBottom() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function fetchMessages() {
  loading.value = true
  loadError.value = false
  try {
    const res = await getMessages()
    // 后端按时间倒序返回，页面按正序展示（最新在底部）
    messages.value = (res.data || []).slice().reverse()
    scrollToBottom()
  } catch {
    loadError.value = true
    messages.value = []
  } finally {
    loading.value = false
  }
}

function toggleActive(message) {
  if (!isMine(message)) return
  activeId.value = activeId.value === message.id ? null : message.id
}

function autoGrow() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  sending.value = true
  try {
    const res = await createMessage({ content })
    draft.value = ''
    autoGrow()
    if (res.data?.id) {
      messages.value = [...messages.value, res.data]
    } else {
      await fetchMessages()
    }
    scrollToBottom()
  } finally {
    sending.value = false
  }
}

async function handleDelete(message) {
  const confirmed = await requestConfirm({
    title: '删除留言',
    message: '确定要删除这条悄悄话吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deleteMessage(message.id)
  messages.value = messages.value.filter((item) => item.id !== message.id)
  activeId.value = null
}

onMounted(fetchMessages)
</script>

<style scoped>
.board-page {
  padding: 32px 0 60px;
}

.board-header {
  text-align: center;
  margin-bottom: 24px;
}

.board-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.board-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.board-header__deco {
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
}

.s2 {
  font-size: 22px;
  animation-delay: 0.4s;
}

.s3 {
  font-size: 14px;
  animation-delay: 0.8s;
}

/* 面板 */
.board-panel {
  max-width: 720px;
  margin: 0 auto;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.board-list {
  flex: 1;
  height: calc(100vh - 380px);
  min-height: 320px;
  max-height: 560px;
  overflow-y: auto;
  padding: 20px 16px 8px;
  -webkit-overflow-scrolling: touch;
}

/* 时间分隔条 */
.time-divider {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}

.time-divider span {
  padding: 3px 12px;
  font-size: 11px;
  color: var(--text-muted);
  background: rgba(var(--kraft-rgb), 0.15);
  border-radius: 12px;
  letter-spacing: 1px;
}

/* 消息行 */
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 4px;
}

.msg-row--mine {
  flex-direction: row-reverse;
}

.msg-row__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: rgba(var(--brown-rgb), 0.08);
}

.msg-row__avatar-spacer {
  width: 36px;
  flex-shrink: 0;
}

.msg-row__main {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 75%;
  min-width: 0;
  margin-bottom: 10px;
}

.msg-row--mine .msg-row__main {
  align-items: flex-end;
}

.msg-row__name {
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0 4px 4px;
}

.msg-row__time {
  font-size: 11px;
  color: var(--text-muted);
  margin: 4px 4px 0;
}

/* 气泡 */
.bubble {
  position: relative;
  padding: 10px 14px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: 14px 14px 14px 4px;
  box-shadow: 0 2px 8px rgba(var(--brown-rgb), 0.08);
}

.bubble--mine {
  background: rgba(var(--olive-rgb), 0.15);
  border-color: rgba(var(--olive-rgb), 0.3);
  border-radius: 14px 14px 4px 14px;
  cursor: pointer;
}

.bubble__text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  word-break: break-word;
  white-space: pre-wrap;
}

.bubble__delete {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--cream);
  background: var(--faded-red);
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(var(--shadow-rgb), 0.25);
  opacity: 0;
  pointer-events: none;
  transition: opacity var(--transition-fast);
}

.bubble--mine:hover .bubble__delete,
.bubble--active .bubble__delete {
  opacity: 1;
  pointer-events: auto;
}

.bubble__delete:hover {
  background: var(--faded-red-dark);
}

/* 输入区 */
.board-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.15);
}

.board-input__field {
  flex: 1;
  min-width: 0;
  padding: 10px 14px;
  font-size: 14px;
  font-family: var(--font-serif);
  line-height: 1.5;
  color: var(--text-primary);
  background: rgba(var(--cream-rgb), 0.8);
  border: 1.5px solid rgba(var(--brown-rgb), 0.15);
  border-radius: var(--radius-md);
  outline: none;
  resize: none;
  max-height: 120px;
}

.board-input__field:focus {
  border-color: var(--kraft-paper);
  box-shadow: 0 0 0 3px rgba(var(--kraft-rgb), 0.12);
}

.board-input__send {
  flex-shrink: 0;
  padding: 10px 20px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  color: var(--cream);
  background: var(--faded-red);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.board-input__send:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.board-input__send:not(:disabled):hover {
  background: var(--faded-red-dark);
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

@media (max-width: 768px) {
  .board-page {
    padding: 24px 0 48px;
  }

  .board-header {
    margin-bottom: 16px;
  }

  .board-header__title {
    font-size: 28px;
  }

  .board-list {
    height: calc(100vh - 340px);
    min-height: 280px;
  }

  .msg-row__main {
    max-width: 82%;
  }

  .board-input {
    padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  }
}

@media (max-width: 480px) {
  .board-header__title {
    font-size: 24px;
  }

  .board-list {
    padding: 16px 10px 8px;
  }

  .msg-row__avatar,
  .msg-row__avatar-spacer {
    width: 32px;
    height: 32px;
  }

  .msg-row__avatar-spacer {
    height: auto;
  }

  .board-input__send {
    padding: 10px 14px;
  }
}
</style>
