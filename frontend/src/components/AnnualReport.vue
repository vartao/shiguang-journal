<template>
  <div class="annual-report">
    <div class="report-sheet">
      <!-- 封面 -->
      <section class="report-cover">
        <div class="report-cover__year">{{ currentYear }}</div>
        <h2 class="report-cover__title">我们的年度手账</h2>
        <p class="report-cover__subtitle">{{ coupleName }} · 在一起的第 {{ daysTogether }} 天</p>
        <div class="report-cover__date">生成于 {{ today }}</div>
      </section>

      <!-- 高光数字 -->
      <section class="report-page">
        <h3 class="report-page__title">
          <span class="report-page__deco">&#10022;</span>
          这一年的高光数字
          <span class="report-page__deco">&#10022;</span>
        </h3>
        <div class="report-grid">
          <div class="report-card">
            <div class="report-card__number">{{ stats.noteCount || 0 }}</div>
            <div class="report-card__label">篇笔记</div>
          </div>
          <div class="report-card">
            <div class="report-card__number">{{ stats.photoCount || 0 }}</div>
            <div class="report-card__label">张照片</div>
          </div>
          <div class="report-card">
            <div class="report-card__number">{{ stats.totalLikes || 0 }}</div>
            <div class="report-card__label">次点赞</div>
          </div>
          <div class="report-card">
            <div class="report-card__number">{{ stats.activeDays30 || 0 }}</div>
            <div class="report-card__label">近 30 天活跃</div>
          </div>
          <div class="report-card">
            <div class="report-card__number">{{ stats.longestStreak || 0 }}</div>
            <div class="report-card__label">最长连续写作</div>
          </div>
          <div class="report-card">
            <div class="report-card__number">{{ stats.surpriseCount || 0 }}</div>
            <div class="report-card__label">个惊喜</div>
          </div>
        </div>
      </section>

      <!-- 年度心情 -->
      <section v-if="topMood" class="report-page">
        <h3 class="report-page__title">
          <span class="report-page__deco">&#10022;</span>
          年度心情
          <span class="report-page__deco">&#10022;</span>
        </h3>
        <div class="report-mood">
          <div class="report-mood__emoji">{{ topMood.emoji }}</div>
          <div class="report-mood__name">{{ topMood.label }}</div>
          <div class="report-mood__desc">
            这一年里，「{{ topMood.label }}」出现了 {{ topMood.count }} 次，是我们最常记录的心情。
          </div>
        </div>
      </section>

      <!-- 结语 -->
      <section class="report-page report-page--ending">
        <div class="report-ending">
          <p class="report-ending__quote">"把日子过成胶片里的诗"</p>
          <p class="report-ending__meta">
            从 {{ startDate || '某一天' }} 开始，{{ coupleName }} 已经一起写下了 {{ daysTogether }} 天的故事。
          </p>
          <p class="report-ending__meta">下一年，继续拾光。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useCoupleStore } from '@/stores/couple'

const props = defineProps({
  stats: { type: Object, default: () => ({}) }
})

const coupleStore = useCoupleStore()

const currentYear = new Date().getFullYear()
const today = new Date().toLocaleDateString('zh-CN')

const coupleName = computed(() => coupleStore.coupleName || '我们的小窝')
const daysTogether = computed(() => coupleStore.daysTogether || 0)
const startDate = computed(() => coupleStore.startDate || '')

const moodMap = {
  happy: { emoji: '😊', label: '开心' },
  love: { emoji: '🥰', label: '甜蜜' },
  excited: { emoji: '🤩', label: '兴奋' },
  calm: { emoji: '😌', label: '平静' },
  miss: { emoji: '🥺', label: '想念' },
  funny: { emoji: '🤣', label: '搞笑' },
  sad: { emoji: '😢', label: '难过' },
  angry: { emoji: '😤', label: '生气' }
}

const topMood = computed(() => {
  const distribution = props.stats?.moodDistribution || {}
  const entries = Object.entries(distribution).sort((a, b) => b[1] - a[1])
  if (entries.length === 0) return null
  const [key, count] = entries[0]
  const info = moodMap[key] || { emoji: '💕', label: key }
  return { ...info, count }
})
</script>

<style scoped>
.annual-report {
  color: var(--text-primary);
}

.report-sheet {
  max-width: 720px;
  margin: 0 auto;
  background: var(--cream);
}

.report-cover {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 32px;
  background: linear-gradient(165deg, var(--cream), var(--cream-warm));
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  position: relative;
  overflow: hidden;
}

.report-cover::before {
  content: '';
  position: absolute;
  inset: 12px;
  border: 2px dashed rgba(var(--brown-rgb), 0.15);
  border-radius: calc(var(--radius-lg) - 8px);
  pointer-events: none;
}

.report-cover__year {
  font-family: var(--font-handwrite);
  font-size: 64px;
  color: var(--faded-red);
  line-height: 1;
  margin-bottom: 16px;
}

.report-cover__title {
  font-family: var(--font-display);
  font-size: 32px;
  color: var(--brown-deep);
  letter-spacing: 4px;
  margin-bottom: 12px;
}

.report-cover__subtitle {
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 1px;
  margin-bottom: 32px;
}

.report-cover__date {
  font-size: 12px;
  color: var(--text-muted);
}

.report-page {
  margin-top: 24px;
  padding: 32px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
}

.report-page__title {
  text-align: center;
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--brown-deep);
  letter-spacing: 2px;
  margin-bottom: 24px;
}

.report-page__deco {
  color: var(--mustard);
  margin: 0 8px;
}

.report-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.report-card {
  text-align: center;
  padding: 20px 12px;
  background: rgba(var(--cream-rgb), 0.6);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
}

.report-card__number {
  font-family: var(--font-handwrite);
  font-size: 32px;
  color: var(--brown-deep);
  line-height: 1;
  margin-bottom: 4px;
}

.report-card__label {
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.report-mood {
  text-align: center;
  padding: 24px;
}

.report-mood__emoji {
  font-size: 56px;
  margin-bottom: 8px;
}

.report-mood__name {
  font-family: var(--font-handwrite);
  font-size: 28px;
  color: var(--brown-deep);
  margin-bottom: 12px;
}

.report-mood__desc {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.report-ending {
  text-align: center;
  padding: 24px;
}

.report-ending__quote {
  font-family: var(--font-handwrite);
  font-size: 24px;
  color: var(--faded-red);
  margin-bottom: 24px;
}

.report-ending__meta {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
}

/* 打印适配 */
@media print {
  :deep(.base-modal__overlay) {
    align-items: flex-start;
    padding: 0;
    background: var(--photo-paper);
    overflow: visible;
  }

  :deep(.base-modal) {
    width: 100%;
    max-height: none;
    border: none;
    border-radius: 0;
    box-shadow: none;
    background: var(--photo-paper);
  }

  :deep(.base-modal__header),
  :deep(.base-modal__footer) {
    display: none;
  }

  :deep(.base-modal__body) {
    padding: 0;
  }

  .report-sheet {
    max-width: none;
    margin: 0;
    background: var(--photo-paper);
  }

  .report-cover,
  .report-page {
    border: none;
    box-shadow: none;
    background: var(--photo-paper);
    break-inside: avoid;
  }

  .report-cover {
    break-after: page;
    min-height: 100vh;
  }
}

@media (max-width: 768px) {
  .report-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .report-cover__year {
    font-size: 48px;
  }

  .report-cover__title {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .report-cover {
    min-height: 320px;
    padding: 32px 20px;
  }

  .report-page {
    padding: 20px;
  }

  .report-grid {
    gap: 10px;
  }

  .report-card__number {
    font-size: 26px;
  }
}
</style>
