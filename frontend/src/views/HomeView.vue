<template>
  <div class="home-page">
    <div class="container">
      <!-- 页面标题 -->
      <header class="home-header anim-fade-in-up">
        <div class="home-header__deco">
          <FilmStrip :frame-count="6" class="home-film-strip" />
        </div>
        <h1 class="home-header__title">我们的时光胶卷</h1>
        <p class="home-header__subtitle">每一帧都是心动的瞬间 · 已记录 {{ notes.length }} 个日子</p>
      </header>

      <!-- 写笔记快捷入口 -->
      <div class="quick-write anim-fade-in-up stagger-2">
        <router-link to="/write" class="quick-write__link">
          <span class="quick-write__icon">&#x270E;</span>
          <span>今天有什么想记录的？</span>
          <span class="quick-write__arrow">&rarr;</span>
        </router-link>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar anim-fade-in-up stagger-2">
        <div class="search-bar__inner">
          <span class="search-bar__icon">&#128269;</span>
          <input
            v-model="searchKeyword"
            class="search-bar__input"
            placeholder="搜索笔记标题或内容..."
            @keyup.enter="handleSearch"
            @input="onSearchInput"
          />
          <button v-if="searchKeyword" class="search-bar__clear" @click="clearSearch">&times;</button>
        </div>
      </div>

      <!-- 搜索结果提示 -->
      <div v-if="isSearchMode" class="search-result-hint anim-fade-in-up">
        <span>搜索「{{ searchKeyword }}」找到 {{ notes.length }} 条结果</span>
        <button class="search-result-hint__clear" @click="clearSearch">清除搜索</button>
      </div>

      <!-- 心情筛选 -->
      <div v-if="!isSearchMode" class="mood-filter anim-fade-in-up stagger-2">
        <button class="mood-chip" :class="{ active: !activeMood }" @click="filterByMood('')">全部</button>
        <button
          v-for="(mood, key) in moods"
          :key="key"
          class="mood-chip"
          :class="{ active: activeMood === key }"
          @click="filterByMood(key)"
        >
          <span>{{ mood.emoji }}</span>
          <span>{{ mood.label }}</span>
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在冲洗照片...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="notes.length === 0" class="empty-state">
        <div class="empty-state__icon">&#x1F4F7;</div>
        <h3 class="empty-state__title">{{ activeMood ? '没有这种心情的记录' : '胶卷还是空白的' }}</h3>
        <p class="empty-state__desc">
          {{ activeMood ? '换个心情标签看看，或者写一篇新的' : '写下第一篇笔记，开始记录你们的故事吧' }}
        </p>
        <router-link to="/write" class="btn-vintage btn-vintage--red">
          {{ activeMood ? '写一篇笔记' : '写第一篇笔记' }}
        </router-link>
      </div>

      <!-- 时间线 -->
      <div v-else class="timeline">
        <div class="timeline__axis"></div>

        <article
          v-for="(note, index) in notes"
          :key="note.id"
          class="timeline__item"
          :class="{ 'timeline__item--right': index % 2 === 1 }"
        >
          <div class="timeline__dot">
            <span class="dot-inner">{{ getMoodEmoji(note.mood) }}</span>
          </div>

          <div
            class="note-card anim-polaroid-drop"
            :class="{ 'note-card--top': note.isTop === 1 || note.isTop === true }"
            :style="{ '--rotate-to': getRotation(index) + 'deg' }"
          >
            <div v-if="index % 3 === 0 || note.isTop === 1 || note.isTop === true" class="note-card__tape"></div>
            <div v-if="note.isTop === 1 || note.isTop === true" class="note-card__top-badge">置顶</div>

            <div class="note-card__date">
              <span class="date-day">{{ formatDay(note.noteDate || note.createdAt || note.createTime) }}</span>
              <span class="date-month">{{ formatMonth(note.noteDate || note.createdAt || note.createTime) }}</span>
            </div>

            <div v-if="note.images && note.images.length > 0" class="note-card__images">
              <div class="note-card__image-main" @click="openLightbox(note.images, 0)">
                <img :src="note.images[0]" :alt="note.title" class="photo-vintage" loading="lazy" />
                <div v-if="note.images.length > 1" class="image-count">+{{ note.images.length - 1 }}</div>
              </div>
            </div>

            <div class="note-card__content">
              <h3 class="note-card__title">{{ note.title }}</h3>
              <p class="note-card__text">{{ note.content }}</p>

              <div class="note-card__meta">
                <div class="note-card__tags">
                  <span class="tag-vintage">{{ getMoodLabel(note.mood) }}</span>
                  <span v-if="note.weather" class="tag-weather">
                    {{ getWeatherEmoji(note.weather) }} {{ getWeatherLabel(note.weather) }}
                  </span>
                </div>
                <span class="note-card__author">{{ note.authorName || note.authorNickname || '我们' }}</span>
              </div>
            </div>

            <div class="note-card__actions">
              <button class="action-btn" :class="{ liked: note.liked }" @click="handleLike(note)">
                <span>{{ note.liked ? '&#x2764;' : '&#x2661;' }}</span>
                <span v-if="(note.likes || note.likeCount || 0) > 0">{{ note.likes ?? note.likeCount }}</span>
              </button>
              <template v-if="isMyNote(note)">
                <button class="action-btn" title="编辑" @click="editNote(note)">
                  <span>&#x270E;</span>
                  <span>编辑</span>
                </button>
                <button class="action-btn action-btn--danger" title="删除" @click="handleDelete(note)">
                  <span>&#x1F5D1;</span>
                  <span>删除</span>
                </button>
              </template>
            </div>
          </div>
        </article>

        <div v-if="hasMore" class="timeline__load-more">
          <button class="btn-vintage btn-vintage--outline" :disabled="loadingMore" @click="loadMore">
            <span v-if="loadingMore">冲洗中...</span>
            <span v-else>翻开更多回忆</span>
          </button>
        </div>
      </div>
    </div>

    <LightBox
      :visible="lightboxVisible"
      :images="lightboxImages"
      :start-index="lightboxIndex"
      @close="lightboxVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { requestConfirm } from '@/composables/confirmService'
import { useRouter } from 'vue-router'
import { getNotes, likeNote, deleteNote, searchNotes } from '@/api/note'
import { useUserStore } from '@/stores/user'
import FilmStrip from '@/components/FilmStrip.vue'
import LightBox from '@/components/LightBox.vue'
import { parseLocalDate } from '@/utils/date'

const router = useRouter()
const userStore = useUserStore()

const notes = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(false)
const page = ref(1)
const activeMood = ref('')
const searchKeyword = ref('')
const isSearchMode = ref(false)
let searchTimer = null

function handleSearch() {
  if (!searchKeyword.value.trim()) {
    clearSearch()
    return
  }
  isSearchMode.value = true
  page.value = 1
  fetchNotes()
}

function onSearchInput() {
  clearTimeout(searchTimer)
  if (!searchKeyword.value.trim()) {
    clearSearch()
    return
  }
  searchTimer = setTimeout(handleSearch, 400)
}

function clearSearch() {
  searchKeyword.value = ''
  isSearchMode.value = false
  page.value = 1
  fetchNotes()
}

const lightboxVisible = ref(false)
const lightboxImages = ref([])
const lightboxIndex = ref(0)

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

function getWeatherEmoji(weather) {
  return weathers[weather]?.emoji || '🌤️'
}

function getWeatherLabel(weather) {
  return weathers[weather]?.label || weather
}

function isMyNote(note) {
  const myId = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (myId == null || note?.userId == null) return false
  return String(note.userId) === String(myId)
}

function editNote(note) {
  router.push({ path: '/write', query: { id: note.id } })
}

async function handleDelete(note) {
  const confirmed = await requestConfirm({
    title: '删除回忆',
    message: '确定要撕掉这页回忆吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  try {
    await deleteNote(note.id)
    notes.value = notes.value.filter((n) => n.id !== note.id)
  } catch {
    // 错误由拦截器提示
  }
}

function filterByMood(mood) {
  activeMood.value = mood || ''
  page.value = 1
  fetchNotes()
}

function buildNoteParams(pageNum) {
  const params = { page: pageNum, size: 10, pageNum, pageSize: 10 }
  if (activeMood.value) params.mood = activeMood.value
  return params
}

function getMoodEmoji(mood) {
  return moods[mood]?.emoji || '💕'
}

function getMoodLabel(mood) {
  return moods[mood]?.label || '记录'
}

function getRotation(index) {
  const rotations = [-1.5, 1, -0.8, 1.5, -1.2, 0.8]
  return rotations[index % rotations.length]
}

function formatDay(dateStr) {
  if (!dateStr) return ''
  const d = parseLocalDate(dateStr)
  if (Number.isNaN(d.getTime())) return ''
  return String(d.getDate()).padStart(2, '0')
}

function formatMonth(dateStr) {
  if (!dateStr) return ''
  const d = parseLocalDate(dateStr)
  if (Number.isNaN(d.getTime())) return ''
  const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
  return months[d.getMonth()]
}

function openLightbox(images, index) {
  lightboxImages.value = images.map((url) => (typeof url === 'string' ? { url } : url))
  lightboxIndex.value = index
  lightboxVisible.value = true
}

function extractList(data) {
  if (!data) return []
  if (Array.isArray(data)) return data
  return data.records || data.list || []
}

function updateHasMore(data) {
  if (!data || Array.isArray(data)) {
    hasMore.value = false
    return
  }
  if (data.pages != null) {
    hasMore.value = page.value < data.pages
  } else if (data.hasMore != null) {
    hasMore.value = !!data.hasMore
  } else {
    hasMore.value = false
  }
}

async function fetchNotes() {
  loading.value = true
  try {
    let res
    if (isSearchMode.value && searchKeyword.value.trim()) {
      res = await searchNotes(searchKeyword.value.trim(), { page: page.value, size: 10 })
    } else {
      res = await getNotes(buildNoteParams(page.value))
    }
    const data = res.data
    notes.value = extractList(data)
    updateHasMore(data)
  } catch {
    notes.value = []
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  page.value++
  try {
    const res = await getNotes(buildNoteParams(page.value))
    const data = res.data
    const newNotes = extractList(data)
    notes.value.push(...newNotes)
    updateHasMore(data)
  } catch {
    hasMore.value = false
    page.value--
  } finally {
    loadingMore.value = false
  }
}

async function handleLike(note) {
  try {
    const res = await likeNote(note.id)
    const data = res.data || {}
    const likeCount = data.likeCount ?? data.likes
    note.liked = data.liked != null ? !!data.liked : !note.liked
    if (likeCount != null) {
      note.likes = likeCount
      note.likeCount = likeCount
    } else {
      note.likes = note.liked
        ? (note.likes || note.likeCount || 0) + 1
        : Math.max(0, (note.likes || note.likeCount || 1) - 1)
      note.likeCount = note.likes
    }
  } catch {
    // 错误由拦截器提示
  }
}

onMounted(() => {
  fetchNotes()
})
</script>

<style scoped>
.home-page {
  padding: 40px 0 60px;
}

.home-header {
  text-align: center;
  margin-bottom: 36px;
}

.home-header__deco {
  margin-bottom: 20px;
  opacity: 0.5;
}

.home-film-strip {
  max-width: 420px;
  margin: 0 auto;
}

.home-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  letter-spacing: 3px;
  margin-bottom: 8px;
}

.home-header__subtitle {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.quick-write {
  margin-bottom: 20px;
}

.mood-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 32px;
  justify-content: center;
}

.mood-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-secondary);
  background: rgba(var(--white-rgb), 0.55);
  border: 1.5px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 999px;
  transition: all var(--transition-fast);
}

.mood-chip:hover {
  border-color: var(--kraft-paper);
  color: var(--brown-deep);
}

.mood-chip.active {
  border-color: var(--faded-red);
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.08);
  box-shadow: 0 2px 8px rgba(var(--red-rgb), 0.12);
}

.quick-write__link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: var(--gradient-card);
  border: var(--border-dashed);
  border-radius: var(--radius-md);
  color: var(--text-muted);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  text-decoration: none;
  transition: all var(--transition-fast);
}

.quick-write__link:hover {
  border-color: var(--kraft-paper);
  color: var(--text-secondary);
  box-shadow: var(--shadow-soft);
  transform: translateY(-1px);
}

.quick-write__icon {
  font-size: 18px;
  color: var(--faded-red);
}

.quick-write__arrow {
  margin-left: auto;
  transition: transform var(--transition-fast);
}

.quick-write__link:hover .quick-write__arrow {
  transform: translateX(4px);
}

/* Search Bar */
.search-bar {
  margin-bottom: 16px;
}

.search-bar__inner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  background: rgba(var(--cream-rgb), 0.8);
  border: 1.5px solid rgba(var(--brown-rgb), 0.2);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-inset);
  transition: all var(--transition-fast);
}

.search-bar__inner:focus-within {
  border-color: var(--kraft-paper);
  box-shadow:
    var(--shadow-inset),
    0 0 0 3px rgba(var(--kraft-rgb), 0.15);
}

.search-bar__icon {
  font-size: 16px;
  opacity: 0.5;
  flex-shrink: 0;
}

.search-bar__input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: var(--text-primary);
  font-family: var(--font-serif);
}

.search-bar__input::placeholder {
  color: var(--text-muted);
  font-style: italic;
}

.search-bar__clear {
  font-size: 18px;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.search-bar__clear:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.1);
}

.search-result-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.1);
  border-radius: var(--radius-sm);
}

.search-result-hint__clear {
  padding: 4px 12px;
  font-size: 12px;
  font-family: var(--font-display);
  color: var(--faded-red);
  border: 1px solid rgba(var(--red-rgb), 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.search-result-hint__clear:hover {
  background: rgba(var(--red-rgb), 0.1);
}

.timeline {
  position: relative;
  padding: 20px 0;
}

.timeline__axis {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  background: linear-gradient(to bottom, transparent, var(--kraft-paper) 5%, var(--kraft-paper) 95%, transparent);
  transform: translateX(-50%);
}

.timeline__item {
  position: relative;
  width: 50%;
  padding: 0 40px 48px 0;
}

.timeline__item--right {
  margin-left: 50%;
  padding: 0 0 48px 40px;
}

.timeline__dot {
  position: absolute;
  top: 12px;
  right: -18px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--cream);
  border: 2px solid var(--kraft-paper);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(var(--shadow-rgb), 0.12);
  z-index: 2;
}

.timeline__item--right .timeline__dot {
  right: auto;
  left: -18px;
}

.dot-inner {
  font-size: 16px;
}

.note-card {
  background: var(--photo-paper);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-polaroid);
  overflow: hidden;
  position: relative;
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
}

.note-card--top {
  box-shadow: 0 8px 28px rgba(var(--red-rgb), 0.2);
}

.note-card:hover {
  transform: rotate(0deg) translateY(-3px) !important;
  box-shadow: var(--shadow-lifted);
}

.note-card__top-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  padding: 3px 10px;
  font-family: var(--font-display);
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--faded-red), var(--faded-red-dark));
  border-radius: 999px;
  box-shadow: 0 2px 8px rgba(var(--red-rgb), 0.35);
}

.note-card__tape {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(-2deg);
  width: 60px;
  height: 20px;
  background: rgba(var(--kraft-rgb), 0.5);
  z-index: 3;
  border-radius: 1px;
}

.note-card__date {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 10px;
  background: rgba(26, 26, 26, 0.7);
  border-radius: 3px;
  z-index: 2;
  backdrop-filter: blur(2px);
}

.date-day {
  font-family: var(--font-serif);
  font-size: 20px;
  font-weight: 700;
  color: var(--cream);
  line-height: 1.1;
}

.date-month {
  font-family: var(--font-display);
  font-size: 10px;
  color: rgba(var(--cream-static-rgb), 0.7);
  letter-spacing: 2px;
}

.note-card__images {
  position: relative;
}

.note-card__image-main {
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.note-card__image-main img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.note-card__image-main:hover img {
  transform: scale(1.03);
}

.image-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 3px 8px;
  font-size: 12px;
  color: var(--cream);
  background: rgba(26, 26, 26, 0.6);
  border-radius: 10px;
}

.note-card__content {
  padding: 18px 20px 12px;
}

.note-card__title {
  font-family: var(--font-handwrite);
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 8px;
  line-height: 1.4;
}

.note-card__text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.note-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.12);
}

.note-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.tag-weather {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 8px;
  font-size: 11px;
  color: var(--dusty-blue);
  background: rgba(var(--blue-rgb), 0.12);
  border-radius: 999px;
}

.note-card__author {
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.note-card__actions {
  padding: 8px 20px 14px;
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 14px;
  color: var(--text-muted);
  border-radius: 12px;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: rgba(var(--red-rgb), 0.08);
  color: var(--faded-red);
}

.action-btn.liked {
  color: var(--faded-red);
}

.action-btn.liked span:first-child {
  animation: heartbeat 0.6s ease;
}

.action-btn--danger:hover {
  background: rgba(var(--red-rgb), 0.12);
  color: var(--faded-red-dark);
}

.timeline__load-more {
  text-align: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .home-page {
    padding: 24px 0 48px;
  }

  .home-header {
    margin-bottom: 24px;
  }

  .home-header__title {
    font-size: 28px;
  }

  .home-film-strip {
    max-width: 320px;
  }

  .quick-write__link {
    padding: 14px 18px;
  }

  .search-bar__inner {
    padding: 12px 16px;
  }

  .search-bar__input {
    font-size: 16px;
  }

  .search-bar__clear {
    min-width: 40px;
    min-height: 40px;
    padding: 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  .search-result-hint {
    flex-wrap: wrap;
    gap: 8px;
  }

  .mood-filter {
    margin-bottom: 24px;
  }

  .mood-chip {
    padding: 8px 14px;
    min-height: 36px;
  }

  .timeline__axis {
    left: 20px;
  }

  .timeline__item,
  .timeline__item--right {
    width: 100%;
    margin-left: 0;
    padding: 0 0 36px 52px;
  }

  .timeline__dot,
  .timeline__item--right .timeline__dot {
    left: 2px;
    right: auto;
  }

  .note-card__image-main img {
    height: 180px;
  }

  .note-card__content {
    padding: 16px 16px 10px;
  }

  .note-card__title {
    font-size: 18px;
  }

  .note-card__text {
    overflow-wrap: break-word;
  }

  .note-card__meta {
    flex-wrap: wrap;
  }

  .note-card__actions {
    padding: 6px 16px 12px;
    gap: 8px;
  }

  .action-btn {
    min-height: 40px;
    padding: 8px 12px;
  }
}

@media (max-width: 480px) {
  .home-page {
    padding: 16px 0 40px;
  }

  .home-header__deco {
    display: none;
  }

  .home-header__title {
    font-size: 24px;
    letter-spacing: 2px;
  }

  .home-header__subtitle {
    font-size: 13px;
  }

  .quick-write__link {
    padding: 12px 16px;
    font-size: 13px;
    gap: 10px;
  }

  .mood-filter {
    gap: 6px;
  }

  .mood-chip {
    padding: 6px 10px;
    font-size: 11px;
  }

  .timeline {
    padding: 12px 0;
  }

  .timeline__axis {
    left: 16px;
  }

  .timeline__item,
  .timeline__item--right {
    padding: 0 0 28px 44px;
  }

  .timeline__dot,
  .timeline__item--right .timeline__dot {
    left: 0;
    width: 32px;
    height: 32px;
  }

  .dot-inner {
    font-size: 14px;
  }

  .note-card__date {
    top: 10px;
    left: 10px;
    padding: 4px 8px;
  }

  .date-day {
    font-size: 17px;
  }

  .note-card__image-main img {
    height: 150px;
  }

  .note-card__content {
    padding: 14px 14px 8px;
  }

  .note-card__title {
    font-size: 17px;
  }

  .note-card__text {
    font-size: 13px;
  }

  .note-card__actions {
    padding: 4px 12px 10px;
    gap: 4px;
  }

  .action-btn {
    padding: 8px 10px;
    font-size: 13px;
  }
}
</style>
