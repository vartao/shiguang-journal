<template>
  <div class="editor-page">
    <div class="container">
      <div class="editor-wrapper anim-fade-in-up">
        <!-- 信纸头部 -->
        <div class="editor-header">
          <button class="editor-back" @click="goBack">&larr; 返回</button>
          <h1 class="editor-title">
            <span class="title-icon">&#x270E;</span>
            {{ isEdit ? '编辑笔记' : '写一页新的故事' }}
          </h1>
          <p class="editor-date">{{ todayStr }}</p>
        </div>

        <!-- 表单 -->
        <form class="editor-form" @submit.prevent="handleSubmit">
          <!-- 标题 -->
          <div class="form-section">
            <input
              v-model="form.title"
              type="text"
              class="editor-title-input"
              placeholder="给这一天取个标题..."
              maxlength="50"
            />
          </div>

          <!-- 日期选择 -->
          <div class="form-section form-row">
            <div class="form-field">
              <label class="field-label">记录日期</label>
              <input v-model="form.noteDate" type="date" class="input-vintage" />
            </div>
            <div class="form-field">
              <label class="field-label">今天的心情</label>
              <div class="mood-selector">
                <button
                  v-for="(mood, key) in moods"
                  :key="key"
                  type="button"
                  class="mood-btn"
                  :class="{ active: form.mood === key }"
                  :title="mood.label"
                  @click="form.mood = key"
                >
                  <span class="mood-emoji">{{ mood.emoji }}</span>
                  <span class="mood-label">{{ mood.label }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 天气 + 置顶 -->
          <div class="form-section form-row">
            <div class="form-field">
              <label class="field-label">今天的天气</label>
              <div class="weather-selector">
                <button
                  v-for="(item, key) in weathers"
                  :key="key"
                  type="button"
                  class="weather-btn"
                  :class="{ active: form.weather === key }"
                  :title="item.label"
                  @click="form.weather = key"
                >
                  <span class="weather-emoji">{{ item.emoji }}</span>
                  <span class="weather-label">{{ item.label }}</span>
                </button>
              </div>
            </div>
            <div class="form-field">
              <label class="field-label">置顶这篇</label>
              <label class="top-switch">
                <input v-model="form.isTop" type="checkbox" />
                <span class="top-switch__ui"></span>
                <span class="top-switch__text">{{ form.isTop ? '已置顶，会优先出现在时间线' : '不置顶' }}</span>
              </label>
            </div>
          </div>

          <!-- 内容 -->
          <div class="form-section">
            <label class="field-label">写下这一刻</label>
            <textarea
              v-model="form.content"
              class="editor-textarea"
              placeholder="今天发生了什么有趣的事？想对TA说什么？&#10;&#10;慢慢写，不着急..."
              rows="10"
            ></textarea>
            <div class="textarea-footer">
              <span class="char-count">{{ form.content.length }} 字</span>
            </div>
          </div>

          <!-- 图片上传 -->
          <div class="form-section">
            <label class="field-label">附上照片</label>
            <ImageUploader v-model="form.images" :max-count="9" category="note" @error="handleUploadError" />
          </div>

          <!-- 提交 -->
          <div class="form-section form-actions">
            <button type="button" class="btn-vintage btn-vintage--outline" @click="goBack">取消</button>
            <button type="submit" class="btn-vintage btn-vintage--red" :disabled="submitting || !canSubmit">
              <span v-if="submitting" class="btn-loading"></span>
              <span v-else>{{ isEdit ? '保存修改' : '封存这页回忆' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createNote, updateNote, getNoteDetail } from '@/api/note'
import ImageUploader from '@/components/ImageUploader.vue'
import { toLocalDateInputValue } from '@/utils/date'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.query.id)
const submitting = ref(false)

const todayStr = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const moods = {
  happy: { emoji: '😊', label: '开心' },
  love: { emoji: '🥰', label: '甜蜜' },
  excited: { emoji: '🤩', label: '兴奋' },
  calm: { emoji: '😌', label: '平静' },
  miss: { emoji: '🥺', label: '想念' },
  funny: { emoji: '🤣', label: '搞笑' },
  sad: { emoji: '😢', label: '难过' },
  angry: { emoji: '😤', label: '生气' }
}

const weathers = {
  sunny: { emoji: '☀️', label: '晴天' },
  cloudy: { emoji: '⛅', label: '多云' },
  rainy: { emoji: '🌧️', label: '下雨' },
  snowy: { emoji: '❄️', label: '下雪' },
  windy: { emoji: '🌬️', label: '刮风' }
}

const form = reactive({
  title: '',
  content: '',
  mood: 'happy',
  weather: 'sunny',
  isTop: false,
  noteDate: toLocalDateInputValue(),
  images: []
})

const canSubmit = computed(() => {
  return form.title.trim() && form.content.trim()
})

function goBack() {
  router.back()
}

function handleUploadError(_msg) {
  // toast 已在 request 拦截器中处理
}

async function handleSubmit() {
  if (!canSubmit.value) return

  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      content: form.content.trim(),
      mood: form.mood,
      weather: form.weather,
      isTop: form.isTop ? 1 : 0,
      noteDate: form.noteDate,
      // 只提交已上传成功的远程 URL，过滤 blob 占位
      images: form.images
        .map((img) => (typeof img === 'string' ? img : img?.url))
        .filter((url) => url && !String(url).startsWith('blob:'))
    }

    if (isEdit.value) {
      await updateNote(route.query.id, payload)
    } else {
      await createNote(payload)
    }

    router.push('/')
  } catch {
    // 错误已处理
  } finally {
    submitting.value = false
  }
}

async function loadNote() {
  if (!route.query.id) return
  try {
    const res = await getNoteDetail(route.query.id)
    const data = res.data
    form.title = data.title || ''
    form.content = data.content || ''
    form.mood = data.mood || 'happy'
    form.weather = data.weather || 'sunny'
    form.isTop = data.isTop === 1 || data.isTop === true
    form.noteDate = data.noteDate || form.noteDate
    form.images = (data.images || []).map((url) => ({ url }))
  } catch {
    // ignore
  }
}

onMounted(() => {
  loadNote()
})
</script>

<style scoped>
.editor-page {
  padding: 40px 0 80px;
}

.editor-wrapper {
  max-width: 700px;
  margin: 0 auto;
  background: var(--gradient-paper);
  border: 1px solid rgba(var(--brown-rgb), 0.12);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lifted);
  padding: 36px 40px 44px;
  position: relative;
  overflow: hidden;
}

/* 信纸横线 */
.editor-wrapper::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    transparent,
    transparent 31px,
    rgba(var(--brown-rgb), 0.03) 31px,
    rgba(var(--brown-rgb), 0.03) 32px
  );
  pointer-events: none;
}

/* 头部 */
.editor-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.editor-back {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-muted);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.editor-back:hover {
  color: var(--text-secondary);
  background: rgba(var(--brown-rgb), 0.05);
}

.editor-title {
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: var(--brown-deep);
  flex: 1;
}

.title-icon {
  margin-right: 8px;
  filter: sepia(0.3);
}

.editor-date {
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* 表单 */
.editor-form {
  position: relative;
  z-index: 1;
}

.form-section {
  margin-bottom: 28px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

/* 标题输入 */
.editor-title-input {
  width: 100%;
  padding: 12px 4px;
  font-family: var(--font-handwrite);
  font-size: 24px;
  color: var(--text-primary);
  background: transparent;
  border: none;
  border-bottom: 2px solid rgba(var(--brown-rgb), 0.15);
  outline: none;
  transition: border-color var(--transition-fast);
}

.editor-title-input::placeholder {
  color: var(--text-muted);
  opacity: 0.6;
}

.editor-title-input:focus {
  border-bottom-color: var(--kraft-paper);
}

/* 心情选择 */
.mood-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mood-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  border: 1.5px solid transparent;
  transition: all var(--transition-fast);
}

.mood-btn:hover {
  background: rgba(var(--kraft-rgb), 0.1);
}

.mood-btn.active {
  border-color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.06);
  transform: scale(1.05);
}

.mood-emoji {
  font-size: 20px;
}

.mood-label {
  font-size: 10px;
  color: var(--text-muted);
}

.mood-btn.active .mood-label {
  color: var(--faded-red);
}

/* 天气选择 */
.weather-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.weather-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  border: 1.5px solid transparent;
  transition: all var(--transition-fast);
}

.weather-btn:hover {
  background: rgba(var(--blue-rgb), 0.1);
}

.weather-btn.active {
  border-color: var(--dusty-blue);
  background: rgba(var(--blue-rgb), 0.1);
  transform: scale(1.05);
}

.weather-emoji {
  font-size: 18px;
}

.weather-label {
  font-size: 10px;
  color: var(--text-muted);
}

.weather-btn.active .weather-label {
  color: var(--dusty-blue);
}

/* 置顶开关 */
.top-switch {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
  padding: 10px 0;
}

.top-switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.top-switch__ui {
  position: relative;
  width: 44px;
  height: 24px;
  background: rgba(var(--brown-rgb), 0.18);
  border-radius: 999px;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.top-switch__ui::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  background: var(--photo-paper);
  border-radius: 50%;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease;
}

.top-switch input:checked + .top-switch__ui {
  background: linear-gradient(145deg, var(--faded-red), var(--faded-red-dark));
}

.top-switch input:checked + .top-switch__ui::after {
  transform: translateX(20px);
}

.top-switch__text {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 文本域 */
.editor-textarea {
  width: 100%;
  padding: 16px;
  font-family: var(--font-serif);
  font-size: 15px;
  line-height: 2;
  color: var(--text-primary);
  background: rgba(var(--surface-raised-rgb), 0.7);
  border: 1.5px solid rgba(var(--brown-rgb), 0.15);
  border-radius: var(--radius-md);
  resize: vertical;
  min-height: 200px;
  outline: none;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.editor-textarea::placeholder {
  color: var(--text-muted);
  opacity: 0.6;
  font-style: italic;
}

.editor-textarea:focus {
  border-color: var(--kraft-paper);
  box-shadow: 0 0 0 3px rgba(var(--kraft-rgb), 0.15);
}

.textarea-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
}

.char-count {
  font-size: 12px;
  color: var(--text-muted);
}

/* 操作按钮 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.15);
}

.btn-loading {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(var(--cream-rgb), 0.3);
  border-top-color: var(--cream);
  border-radius: 50%;
  animation: reel-spin 0.8s linear infinite;
  display: inline-block;
}

/* 响应式 */
@media (max-width: 768px) {
  .editor-page {
    padding: 28px 0 64px;
  }

  .editor-wrapper {
    padding: 24px 20px 32px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .editor-header {
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 24px;
  }

  .editor-back {
    display: inline-flex;
    align-items: center;
    min-height: 40px;
  }

  .editor-title {
    font-size: 22px;
  }

  .editor-date {
    width: 100%;
    order: 3;
  }

  .editor-title-input {
    font-size: 20px;
  }

  /* iOS 聚焦输入框时避免页面自动放大 */
  .form-row .input-vintage,
  .editor-textarea {
    font-size: 16px;
  }

  .mood-btn,
  .weather-btn {
    min-width: 44px;
    min-height: 44px;
    justify-content: center;
  }

  .form-actions {
    flex-wrap: wrap;
  }

  /* 图片上传：触屏无 hover，移除按钮常显并加大触控目标 */
  :deep(.uploader__remove-btn) {
    opacity: 1;
    width: 28px;
    height: 28px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .editor-page {
    padding: 20px 0 56px;
  }

  .editor-wrapper {
    padding: 20px 16px 28px;
    border-radius: var(--radius-md);
  }

  .editor-title {
    font-size: 20px;
  }

  .editor-title-input {
    font-size: 18px;
  }

  .form-section {
    margin-bottom: 22px;
  }

  .mood-selector,
  .weather-selector {
    gap: 4px;
  }

  .mood-btn,
  .weather-btn {
    padding: 6px 8px;
  }

  .editor-textarea {
    min-height: 160px;
    padding: 12px;
  }

  .form-actions {
    flex-direction: column;
    gap: 12px;
  }

  .form-actions .btn-vintage {
    width: 100%;
  }

  :deep(.uploader__preview-grid) {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
  }

  :deep(.uploader__dropzone) {
    padding: 24px 16px;
  }
}
</style>
