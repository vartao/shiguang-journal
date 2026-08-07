<template>
  <teleport to="body">
    <transition name="comment-fade">
      <div v-if="visible" class="comment-overlay" @click.self="close">
        <transition name="comment-slide">
          <div v-if="visible" class="comment-drawer">
            <div class="comment-drawer__header">
              <span class="comment-drawer__title">笔记评论</span>
              <button class="comment-drawer__close" aria-label="关闭" @click="close">&times;</button>
            </div>

            <div ref="bodyRef" class="comment-drawer__body">
              <div v-if="loading" class="comment-status">正在加载评论...</div>
              <div v-else-if="comments.length === 0" class="comment-status comment-status--empty">
                还没有评论，来说点什么吧 💬
              </div>
              <ul v-else class="comment-list">
                <li v-for="item in comments" :key="item.id" class="comment-item">
                  <img
                    class="comment-item__avatar"
                    :src="item.authorAvatar || placeholderAvatar"
                    :alt="item.authorNickname || '头像'"
                  />
                  <div class="comment-item__main">
                    <div class="comment-item__meta">
                      <span class="comment-item__name">{{ item.authorNickname || '匿名' }}</span>
                      <span class="comment-item__time">{{ formatTime(item.createTime) }}</span>
                    </div>
                    <p class="comment-item__content">{{ item.content }}</p>
                  </div>
                  <button v-if="isMyComment(item)" class="comment-item__delete" title="删除" @click="remove(item)">
                    删除
                  </button>
                </li>
              </ul>
            </div>

            <div class="comment-drawer__footer">
              <input
                v-model="draft"
                class="comment-input"
                type="text"
                maxlength="500"
                placeholder="写下你的评论..."
                @keyup.enter="submit"
              />
              <button class="comment-submit" :disabled="!draft.trim() || submitting" @click="submit">
                {{ submitting ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </transition>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getComments, addComment, deleteComment } from '@/api/noteComment'

const props = defineProps({
  visible: { type: Boolean, default: false },
  note: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['close', 'update-count'])

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const comments = ref([])
const draft = ref('')
const bodyRef = ref(null)

const placeholderAvatar =
  'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 64 64%22%3E%3Ccircle cx=%2232%22 cy=%2232%22 r=%2232%22 fill=%22%23ddd%22/%3E%3Ctext x=%2232%22 y=%2238%22 text-anchor=%22middle%22 font-size=%2228%22 fill=%22%23999%22%3E%F0%9F%98%8A%3C/text%3E%3C/svg%3E'

function currentUserId() {
  return userStore.userInfo?.id ?? userStore.userInfo?.userId
}

function isMyComment(item) {
  const myId = currentUserId()
  if (myId == null || item?.userId == null) return false
  return String(item.userId) === String(myId)
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

async function loadComments() {
  if (!props.note?.id) return
  loading.value = true
  try {
    const res = await getComments(props.note.id)
    comments.value = Array.isArray(res.data) ? res.data : res.data?.data || []
    emit('update-count', props.note.id, comments.value.length)
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function submit() {
  const content = draft.value.trim()
  if (!content || !props.note?.id) return
  submitting.value = true
  try {
    await addComment(props.note.id, content)
    draft.value = ''
    await loadComments()
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  } finally {
    submitting.value = false
  }
}

async function remove(item) {
  if (!item?.id) return
  try {
    await deleteComment(item.id)
    await loadComments()
  } catch {
    // 错误由拦截器提示
  }
}

function close() {
  emit('close')
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      draft.value = ''
      loadComments()
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  }
)
</script>

<style scoped>
.comment-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(var(--shadow-rgb), 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.comment-drawer {
  width: 100%;
  max-width: 560px;
  max-height: 75vh;
  background: var(--photo-paper);
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -8px 32px rgba(var(--shadow-rgb), 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.comment-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px dashed rgba(var(--brown-rgb), 0.15);
}

.comment-drawer__title {
  font-family: var(--font-handwrite);
  font-size: 18px;
  color: var(--brown-deep);
}

.comment-drawer__close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--text-muted);
  background: rgba(var(--brown-rgb), 0.08);
  border-radius: 50%;
  transition: all var(--transition-fast);
}

.comment-drawer__close:hover {
  background: rgba(var(--red-rgb), 0.12);
  color: var(--faded-red);
}

.comment-drawer__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  -webkit-overflow-scrolling: touch;
}

.comment-status {
  text-align: center;
  padding: 36px 16px;
  font-size: 14px;
  color: var(--text-muted);
}

.comment-status--empty {
  color: var(--text-secondary);
}

.comment-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.comment-item__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: rgba(var(--brown-rgb), 0.08);
}

.comment-item__main {
  flex: 1;
  min-width: 0;
  background: rgba(var(--cream-rgb), 0.65);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.comment-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-item__name {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--brown-deep);
}

.comment-item__time {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.comment-item__content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-item__delete {
  flex-shrink: 0;
  padding: 4px 8px;
  font-size: 12px;
  color: var(--faded-red);
  border: 1px solid rgba(var(--red-rgb), 0.25);
  border-radius: 10px;
  background: transparent;
  transition: all var(--transition-fast);
}

.comment-item__delete:hover {
  background: rgba(var(--red-rgb), 0.1);
}

.comment-drawer__footer {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px 16px 16px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.15);
  background: var(--photo-paper);
}

.comment-input {
  flex: 1;
  min-width: 0;
  padding: 10px 14px;
  font-size: 14px;
  color: var(--text-primary);
  background: rgba(var(--cream-rgb), 0.8);
  border: 1.5px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 999px;
  outline: none;
  font-family: var(--font-serif);
}

.comment-input:focus {
  border-color: var(--kraft-paper);
  box-shadow: 0 0 0 3px rgba(var(--kraft-rgb), 0.12);
}

.comment-submit {
  flex-shrink: 0;
  padding: 10px 18px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--cream);
  background: var(--faded-red);
  border-radius: 999px;
  transition: all var(--transition-fast);
}

.comment-submit:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.comment-submit:not(:disabled):hover {
  background: var(--faded-red-dark);
}

.comment-fade-enter-active,
.comment-fade-leave-active {
  transition: opacity 0.25s ease;
}

.comment-fade-enter-from,
.comment-fade-leave-to {
  opacity: 0;
}

.comment-slide-enter-active,
.comment-slide-leave-active {
  transition: transform 0.28s cubic-bezier(0.25, 1, 0.5, 1);
}

.comment-slide-enter-from,
.comment-slide-leave-to {
  transform: translateY(100%);
}

@media (max-width: 768px) {
  .comment-drawer {
    max-width: 100%;
    max-height: 80vh;
  }

  .comment-drawer__footer {
    padding-bottom: calc(12px + env(safe-area-inset-bottom, 0px));
  }
}
</style>
