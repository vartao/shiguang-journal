<template>
  <div class="export-page">
    <div class="container">
      <!-- 页面标题 -->
      <header class="export-header anim-fade-in-up no-print">
        <h1 class="export-header__title">导出手账</h1>
        <p class="export-header__subtitle">把写下的日子装订成册 · 打印时在对话框里选择「另存为 PDF」即可导出文件</p>
      </header>

      <!-- 导出选项 -->
      <section class="export-controls card-vintage anim-fade-in-up stagger-2 no-print">
        <div class="control-group">
          <span class="control-label">时间范围</span>
          <div class="range-picker">
            <button
              v-for="option in rangeOptions"
              :key="option.value"
              class="range-chip"
              :class="{ active: range === option.value }"
              @click="range = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
        <div class="control-group">
          <span class="control-label">导出内容</span>
          <label class="image-toggle">
            <input v-model="includeImages" type="checkbox" />
            <span>包含照片（拍立得样式）</span>
          </label>
        </div>
        <button class="btn-vintage btn-vintage--red export-generate" :disabled="loading" @click="generatePreview">
          <span v-if="loading">正在整理手账页…</span>
          <span v-else>生成预览</span>
        </button>
      </section>

      <!-- 加载 / 失败 / 空状态 -->
      <LoadingState v-if="loading" message="正在整理手账页…" />
      <ErrorState v-else-if="loadError" message="笔记暂时没有加载成功。" @retry="generatePreview" />
      <EmptyState
        v-else-if="generated && notes.length === 0"
        icon="📖"
        title="这个范围还没有笔记"
        message="换个时间范围试试，或者先去写一篇吧"
      />

      <!-- 预览与打印 -->
      <template v-else-if="generated">
        <div class="export-toolbar anim-fade-in-up no-print">
          <span class="export-toolbar__info">共 {{ notes.length }} 篇 · {{ rangeLabel }}</span>
          <button class="btn-vintage btn-vintage--red" @click="handlePrint">
            <span>&#x1F5A8;</span>
            <span>打印 / 导出 PDF</span>
          </button>
        </div>

        <div class="export-sheet-wrap">
          <div class="export-sheet">
            <!-- 封面 -->
            <section class="sheet-cover">
              <p class="sheet-cover__eyebrow">OUR JOURNAL</p>
              <h2 class="sheet-cover__title">拾光手账</h2>
              <div class="sheet-cover__divider">✦</div>
              <p class="sheet-cover__meta">共 {{ notes.length }} 篇笔记 · {{ rangeLabel }}</p>
              <p class="sheet-cover__meta">导出于 {{ exportDate }}</p>
            </section>

            <!-- 每篇笔记一页手账 -->
            <article v-for="note in notes" :key="note.id" class="sheet-note">
              <header class="sheet-note__header">
                <span class="sheet-note__date">{{ formatFullDate(note) }}</span>
                <span class="sheet-note__tags">
                  <span class="tag-vintage">{{ getMoodEmoji(note.mood) }} {{ getMoodLabel(note.mood) }}</span>
                  <span v-if="note.weather" class="tag-weather">
                    {{ getWeatherEmoji(note.weather) }} {{ getWeatherLabel(note.weather) }}
                  </span>
                </span>
              </header>
              <h3 class="sheet-note__title">{{ note.title }}</h3>
              <p class="sheet-note__content">{{ note.content }}</p>
              <div v-if="includeImages && note.images && note.images.length" class="sheet-note__photos">
                <figure v-for="(url, index) in note.images" :key="index" class="polaroid">
                  <img :src="url" :alt="note.title" class="photo-vintage" loading="lazy" />
                </figure>
              </div>
              <footer class="sheet-note__footer">{{ note.authorName || note.authorNickname || '我们' }}</footer>
            </article>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { getNotes } from '@/api/note'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import {
  filterNotesByRange,
  getNoteDate,
  getRangeLabel,
  RANGE_OPTIONS,
  sortNotesByDateAsc
} from '@/features/export/exportFilter'
import { parseLocalDate } from '@/utils/date'

const PAGE_SIZE = 100

// 心情/天气映射与首页保持一致
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

const rangeOptions = RANGE_OPTIONS
const range = ref('all')
const includeImages = ref(true)
const loading = ref(false)
const loadError = ref(false)
const generated = ref(false)
const notes = ref([])
const exportDate = ref('')

const rangeLabel = computed(() => getRangeLabel(range.value))

function getMoodEmoji(mood) {
  return moods[mood]?.emoji || '💕'
}

function getMoodLabel(mood) {
  return moods[mood]?.label || '记录'
}

function getWeatherEmoji(weather) {
  return weathers[weather]?.emoji || '🌤️'
}

function getWeatherLabel(weather) {
  return weathers[weather]?.label || weather
}

function formatFullDate(note) {
  const date = parseLocalDate(getNoteDate(note))
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
}

// 分页循环拉取全部笔记
async function fetchAllNotes() {
  const all = []
  let page = 1
  let hasNext = true
  while (hasNext) {
    const res = await getNotes({ page, size: PAGE_SIZE })
    const data = res.data
    const list = Array.isArray(data) ? data : data?.records || data?.list || []
    all.push(...list)
    if (!data || Array.isArray(data)) {
      hasNext = false
    } else if (data.pages != null) {
      hasNext = page < data.pages
    } else {
      // 没有总页数信息时，拿满一页才认为还有下一页
      hasNext = list.length === PAGE_SIZE
    }
    page++
  }
  return all
}

async function generatePreview() {
  if (loading.value) return
  loading.value = true
  loadError.value = false
  try {
    const all = await fetchAllNotes()
    notes.value = filterNotesByRange(sortNotesByDateAsc(all), range.value)
    exportDate.value = formatFullDate({ noteDate: new Date() })
    generated.value = true
  } catch {
    // 错误提示由请求拦截器统一处理
    loadError.value = true
    notes.value = []
  } finally {
    loading.value = false
  }
}

function handlePrint() {
  window.print()
}
</script>

<style scoped>
.export-page {
  padding: 32px 0 60px;
}

.export-header {
  text-align: center;
  margin-bottom: 28px;
}

.export-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.export-header__subtitle {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* 导出选项 */
.export-controls {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
  margin-bottom: 32px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.control-label {
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-secondary);
}

.range-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.range-chip {
  padding: 8px 18px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.1);
  border: 1px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 18px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.range-chip:hover {
  background: rgba(var(--kraft-rgb), 0.2);
}

.range-chip.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

.image-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
}

.image-toggle input {
  width: 16px;
  height: 16px;
  accent-color: var(--faded-red);
  cursor: pointer;
}

.export-generate {
  align-self: flex-start;
}

/* 工具条 */
.export-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.export-toolbar__info {
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-muted);
}

/* 纸张预览 */
.export-sheet-wrap {
  display: flex;
  justify-content: center;
}

.export-sheet {
  width: 100%;
  max-width: 720px;
  padding: 48px 44px;
  background: var(--gradient-paper);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lifted);
}

/* 封面 */
.sheet-cover {
  text-align: center;
  padding: 72px 16px;
}

.sheet-cover__eyebrow {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 6px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.sheet-cover__title {
  font-family: var(--font-handwrite);
  font-size: 44px;
  letter-spacing: 6px;
  color: var(--brown-deep);
  margin-bottom: 20px;
}

.sheet-cover__divider {
  font-size: 18px;
  color: var(--mustard);
  margin-bottom: 20px;
}

.sheet-cover__meta {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  line-height: 2;
  color: var(--text-secondary);
}

/* 单篇笔记 */
.sheet-note {
  padding: 28px 0;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.25);
}

.sheet-note__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.sheet-note__date {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  color: var(--text-muted);
}

.sheet-note__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-weather {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 4px 12px;
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--dusty-blue);
  background: rgba(var(--blue-rgb), 0.12);
  border-radius: 20px;
}

.sheet-note__title {
  font-family: var(--font-handwrite);
  font-size: 24px;
  line-height: 1.4;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.sheet-note__content {
  font-size: 15px;
  line-height: 1.9;
  color: var(--text-secondary);
  white-space: pre-wrap;
  overflow-wrap: break-word;
}

/* 拍立得小图 */
.sheet-note__photos {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 16px;
}

.polaroid {
  padding: 8px 8px 28px;
  background: var(--photo-paper);
  border-radius: 2px;
  box-shadow: var(--shadow-polaroid);
}

.polaroid img {
  width: 130px;
  height: 130px;
  object-fit: cover;
}

.sheet-note__footer {
  margin-top: 14px;
  text-align: right;
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--text-muted);
}

/* 响应式 */
@media (max-width: 768px) {
  .export-page {
    padding: 24px 0 48px;
  }

  .export-header__title {
    font-size: 28px;
  }

  .export-controls {
    padding: 20px;
  }

  .export-sheet {
    padding: 32px 20px;
  }

  .sheet-cover {
    padding: 48px 12px;
  }

  .sheet-cover__title {
    font-size: 32px;
    letter-spacing: 4px;
  }

  .polaroid img {
    width: 110px;
    height: 110px;
  }
}

@media (max-width: 480px) {
  .export-header__title {
    font-size: 24px;
  }

  .export-sheet {
    padding: 24px 16px;
  }

  .sheet-cover__title {
    font-size: 26px;
  }

  .sheet-note__title {
    font-size: 20px;
  }

  .export-generate,
  .export-toolbar .btn-vintage {
    width: 100%;
  }

  .export-toolbar {
    justify-content: center;
  }
}

/* 打印样式：白纸黑字、按页切分 */
@media print {
  .export-page {
    padding: 0;
  }

  .export-sheet-wrap {
    display: block;
  }

  .export-sheet {
    /* 打印时强制浅色纸张配色，暗色模式下也保证可读 */
    --text-primary: #3d2b1f;
    --text-secondary: #5f4a3a;
    --text-muted: #6b5f52;
    --brown-deep: #5c3d2e;
    --brown-rgb: 92, 61, 46;
    --kraft-rgb: 212, 165, 116;
    --blue-rgb: 90, 120, 145;
    --shadow-rgb: 60, 35, 15;

    width: 100%;
    max-width: none;
    padding: 0;
    background: var(--photo-paper);
    border-radius: 0;
    box-shadow: none;
  }

  .sheet-cover {
    padding: 60mm 0;
    break-after: page;
  }

  .sheet-note {
    break-inside: avoid;
  }

  /* 重要底色与照片在打印时保留 */
  .tag-vintage,
  .tag-weather,
  .polaroid {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .polaroid {
    box-shadow: none;
    border: 1px solid rgba(var(--brown-rgb), 0.2);
  }
}
</style>
