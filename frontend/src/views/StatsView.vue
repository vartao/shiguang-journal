<template>
  <div class="stats-page">
    <div class="container">
      <!-- 页面标题 -->
      <header class="stats-header anim-fade-in-up">
        <h1 class="stats-header__title">恋爱数据报告</h1>
        <p class="stats-header__subtitle">用数字记录我们的故事</p>
      </header>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在统计幸福指数...</p>
      </div>

      <template v-else-if="stats">
        <!-- 核心数据卡片 -->
        <div class="stats-hero anim-fade-in-up stagger-2">
          <div class="hero-card">
            <div class="hero-card__number">{{ coupleStore.daysTogether }}</div>
            <div class="hero-card__label">在一起的天数</div>
            <div class="hero-card__sub">从 {{ coupleStore.startDate }} 至今</div>
          </div>
        </div>

        <!-- 数据网格 -->
        <div class="stats-grid anim-fade-in-up stagger-3">
          <div class="stat-card">
            <div class="stat-card__icon">&#128221;</div>
            <div class="stat-card__number">{{ stats.noteCount || 0 }}</div>
            <div class="stat-card__label">篇笔记</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__icon">&#128247;</div>
            <div class="stat-card__number">{{ stats.photoCount || 0 }}</div>
            <div class="stat-card__label">张照片</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__icon">&#128218;</div>
            <div class="stat-card__number">{{ stats.albumCount || 0 }}</div>
            <div class="stat-card__label">本相册</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__icon">&#128150;</div>
            <div class="stat-card__number">{{ stats.totalLikes || 0 }}</div>
            <div class="stat-card__label">次点赞</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__icon">&#127881;</div>
            <div class="stat-card__number">{{ stats.anniversaryCount || 0 }}</div>
            <div class="stat-card__label">个纪念日</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__icon">&#127873;</div>
            <div class="stat-card__number">{{ stats.surpriseCount || 0 }}</div>
            <div class="stat-card__label">个惊喜</div>
          </div>
        </div>

        <!-- 趣味数据 -->
        <div class="stats-fun anim-fade-in-up stagger-4">
          <h2 class="section-title">
            <span class="section-title__deco">&#10022;</span>
            有趣的数据
            <span class="section-title__deco">&#10022;</span>
          </h2>

          <div class="fun-cards">
            <div class="fun-card">
              <div class="fun-card__value">{{ stats.activeDays30 || 0 }}</div>
              <div class="fun-card__label">近30天活跃天数</div>
              <div class="fun-card__bar">
                <div
                  class="fun-card__bar-fill"
                  :style="{ width: Math.min(100, ((stats.activeDays30 || 0) / 30) * 100) + '%' }"
                ></div>
              </div>
            </div>

            <div class="fun-card">
              <div class="fun-card__value">{{ stats.longestStreak || 0 }}</div>
              <div class="fun-card__label">最长连续写作天数</div>
              <div class="fun-card__bar">
                <div
                  class="fun-card__bar-fill fun-card__bar-fill--streak"
                  :style="{ width: Math.min(100, ((stats.longestStreak || 0) / 10) * 100) + '%' }"
                ></div>
              </div>
            </div>

            <div v-if="stats.unlockedSurpriseCount || stats.surpriseCount" class="fun-card">
              <div class="fun-card__value">{{ stats.unlockedSurpriseCount || 0 }} / {{ stats.surpriseCount || 0 }}</div>
              <div class="fun-card__label">惊喜解锁进度</div>
              <div class="fun-card__bar">
                <div
                  class="fun-card__bar-fill fun-card__bar-fill--surprise"
                  :style="{
                    width: stats.surpriseCount ? (stats.unlockedSurpriseCount / stats.surpriseCount) * 100 + '%' : '0%'
                  }"
                ></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 心情分布 -->
        <div v-if="moodEntries.length > 0" class="stats-mood anim-fade-in-up stagger-5">
          <h2 class="section-title">
            <span class="section-title__deco">&#10022;</span>
            心情分布
            <span class="section-title__deco">&#10022;</span>
          </h2>

          <div class="mood-chart">
            <div v-for="[key, count] in moodEntries" :key="key" class="mood-bar-row">
              <span class="mood-bar__emoji">{{ moods[key]?.emoji || '&#128156;' }}</span>
              <span class="mood-bar__label">{{ moods[key]?.label || key }}</span>
              <div class="mood-bar__track">
                <div
                  class="mood-bar__fill"
                  :style="{
                    width: getMoodPercent(count) + '%',
                    transitionDelay: `${moodEntries.indexOf([key, count]) * 0.1}s`
                  }"
                ></div>
              </div>
              <span class="mood-bar__count">{{ count }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStats } from '@/api/stats'
import { useCoupleStore } from '@/stores/couple'

const coupleStore = useCoupleStore()
const loading = ref(false)
const stats = ref(null)

const moods = {
  happy: { emoji: '&#128522;', label: '开心' },
  sad: { emoji: '&#128546;', label: '难过' },
  angry: { emoji: '&#128548;', label: '生气' },
  surprised: { emoji: '&#128562;', label: '惊喜' },
  tired: { emoji: '&#128555;', label: '疲惫' },
  loved: { emoji: '&#128525;', label: '被爱' },
  anxious: { emoji: '&#128560;', label: '焦虑' },
  peaceful: { emoji: '&#128524;', label: '平静' }
}

const moodEntries = computed(() => {
  if (!stats.value?.moodDistribution) return []
  return Object.entries(stats.value.moodDistribution).sort((a, b) => b[1] - a[1])
})

const maxMoodCount = computed(() => {
  if (moodEntries.value.length === 0) return 1
  return moodEntries.value[0][1] || 1
})

function getMoodPercent(count) {
  return Math.max(4, (count / maxMoodCount.value) * 100)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getStats()
    stats.value = res.data || {}
  } catch (e) {
    console.error('获取统计数据失败:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stats-page {
  padding: 32px 0 60px;
}

.stats-header {
  text-align: center;
  margin-bottom: 36px;
}

.stats-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.stats-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-muted);
  letter-spacing: 2px;
}

/* Hero Card */
.stats-hero {
  margin-bottom: 36px;
}

.hero-card {
  text-align: center;
  padding: 40px 24px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  position: relative;
}

.hero-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--paper-texture);
  opacity: 0.25;
  pointer-events: none;
}

.hero-card__number {
  font-family: var(--font-handwrite);
  font-size: 72px;
  color: var(--faded-red);
  line-height: 1;
  margin-bottom: 8px;
  text-shadow: 0 2px 4px rgba(var(--red-rgb), 0.15);
}

.hero-card__label {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--brown-deep);
  letter-spacing: 2px;
  margin-bottom: 4px;
}

.hero-card__sub {
  font-size: 13px;
  color: var(--text-muted);
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 40px;
}

.stat-card {
  text-align: center;
  padding: 28px 16px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
  transition: all var(--transition-normal);
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-card);
}

.stat-card__icon {
  font-size: 28px;
  margin-bottom: 12px;
  filter: sepia(0.2);
}

.stat-card__number {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  line-height: 1;
  margin-bottom: 4px;
}

.stat-card__label {
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* Section Title */
.section-title {
  text-align: center;
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--brown-deep);
  letter-spacing: 2px;
  margin-bottom: 24px;
}

.section-title__deco {
  color: var(--mustard);
  margin: 0 10px;
  font-size: 14px;
}

/* Fun Cards */
.stats-fun {
  margin-bottom: 40px;
}

.fun-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.fun-card {
  padding: 24px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
}

.fun-card__value {
  font-family: var(--font-handwrite);
  font-size: 32px;
  color: var(--brown-deep);
  margin-bottom: 4px;
}

.fun-card__label {
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.fun-card__bar {
  height: 8px;
  background: rgba(var(--brown-rgb), 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.fun-card__bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--kraft-paper), var(--mustard));
  border-radius: 4px;
  transition: width 1s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.fun-card__bar-fill--streak {
  background: linear-gradient(90deg, var(--olive-light), var(--olive));
}

.fun-card__bar-fill--surprise {
  background: linear-gradient(90deg, var(--faded-rose), var(--faded-red));
}

/* Mood Chart */
.stats-mood {
  margin-bottom: 40px;
}

.mood-chart {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
  padding: 24px;
}

.mood-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}

.mood-bar-row + .mood-bar-row {
  border-top: 1px solid rgba(var(--brown-rgb), 0.06);
}

.mood-bar__emoji {
  font-size: 20px;
  width: 28px;
  text-align: center;
  filter: sepia(0.15);
}

.mood-bar__label {
  font-size: 13px;
  color: var(--text-secondary);
  width: 48px;
  letter-spacing: 1px;
}

.mood-bar__track {
  flex: 1;
  height: 14px;
  background: rgba(var(--brown-rgb), 0.06);
  border-radius: 7px;
  overflow: hidden;
}

.mood-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, var(--faded-rose), var(--faded-red));
  border-radius: 7px;
  transition: width 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  min-width: 4%;
}

.mood-bar__count {
  font-family: var(--font-handwrite);
  font-size: 18px;
  color: var(--brown-deep);
  width: 32px;
  text-align: right;
}

/* Responsive */
@media (max-width: 768px) {
  .stats-page {
    padding: 24px 0 48px;
  }

  .stats-header {
    margin-bottom: 28px;
  }

  .stats-header__title {
    font-size: 28px;
  }

  .hero-card {
    padding: 32px 20px;
  }

  .hero-card__number {
    font-size: 56px;
  }

  .hero-card__label {
    font-size: 16px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 32px;
  }

  .stat-card {
    padding: 20px 12px;
  }

  .stat-card__number {
    font-size: 28px;
  }

  .section-title {
    font-size: 18px;
    margin-bottom: 20px;
  }

  .stats-fun,
  .stats-mood {
    margin-bottom: 32px;
  }

  .fun-card {
    padding: 20px;
  }

  .fun-card__value {
    font-size: 28px;
  }

  .mood-chart {
    padding: 16px;
  }

  .mood-bar-row {
    gap: 8px;
  }

  .mood-bar__emoji {
    font-size: 18px;
    width: 24px;
  }

  .mood-bar__label {
    width: 36px;
    font-size: 12px;
  }

  .mood-bar__count {
    font-size: 16px;
    width: 28px;
  }
}

@media (max-width: 480px) {
  .stats-header__title {
    font-size: 24px;
  }

  .hero-card {
    padding: 24px 16px;
  }

  .hero-card__number {
    font-size: 44px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .stat-card {
    padding: 16px 8px;
  }

  .stat-card__icon {
    font-size: 24px;
    margin-bottom: 8px;
  }

  .stat-card__number {
    font-size: 24px;
  }

  .fun-card {
    padding: 16px;
  }
}
</style>
