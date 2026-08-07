<template>
  <div class="countdown-card" :class="{ 'is-past': isPast }">
    <!-- 顶部装饰 -->
    <div class="countdown-card__pin"></div>

    <div class="countdown-card__body">
      <!-- 图标 -->
      <div class="countdown-card__icon">{{ icon }}</div>

      <!-- 标题 -->
      <h3 class="countdown-card__title">{{ title }}</h3>

      <!-- 日期 -->
      <p class="countdown-card__date">{{ formattedDate }}</p>

      <!-- 天数 -->
      <div class="countdown-card__number">
        <span class="number-value">{{ displayDays }}</span>
        <span class="number-unit">{{ isPast ? '天' : '天后' }}</span>
      </div>

      <!-- 状态标签 -->
      <div class="countdown-card__status">
        <span v-if="isPast" class="status-past">已过去 {{ displayDays }} 天</span>
        <span v-else-if="displayDays === 0" class="status-today">就是今天!</span>
        <span v-else class="status-future">还有 {{ displayDays }} 天</span>
      </div>
    </div>

    <!-- 底部撕页效果 -->
    <div class="countdown-card__tear"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { differenceInCalendarDays, getNextYearlyOccurrence, parseLocalDate } from '@/utils/date'

const props = defineProps({
  title: { type: String, required: true },
  date: { type: String, required: true },
  icon: { type: String, default: '💕' },
  type: { type: String, default: 'future' } // future, past, recurring
})

const targetDate = computed(() => parseLocalDate(props.date))

const dayDifference = computed(() => {
  const target = props.type === 'recurring' ? getNextYearlyOccurrence(props.date) : targetDate.value
  return differenceInCalendarDays(target, new Date())
})

const isPast = computed(() => {
  return props.type !== 'recurring' && dayDifference.value < 0
})

const displayDays = computed(() => {
  return Number.isNaN(dayDifference.value) ? 0 : Math.abs(dayDifference.value)
})

const formattedDate = computed(() => {
  const d = targetDate.value
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}.${m}.${day}`
})
</script>

<style scoped>
.countdown-card {
  position: relative;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  padding: 28px 20px 24px;
  text-align: center;
  box-shadow: var(--shadow-card);
  transition:
    transform var(--transition-normal),
    box-shadow var(--transition-normal);
}

.countdown-card:hover {
  transform: translateY(-4px) rotate(-0.5deg);
  box-shadow: var(--shadow-lifted);
}

/* 图钉 */
.countdown-card__pin {
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 16px;
  background: radial-gradient(circle at 40% 35%, var(--faded-rose), var(--faded-red));
  border-radius: 50%;
  box-shadow:
    0 2px 4px rgba(0, 0, 0, 0.3),
    inset 0 -1px 2px rgba(0, 0, 0, 0.2);
}

.countdown-card__pin::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 6px;
  background: #888;
  border-radius: 0 0 1px 1px;
}

.countdown-card__icon {
  font-size: 32px;
  margin-bottom: 8px;
  filter: sepia(0.2);
}

.countdown-card__title {
  font-family: var(--font-handwrite);
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.countdown-card__date {
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
  margin-bottom: 14px;
}

.countdown-card__number {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 10px;
}

.number-value {
  font-family: var(--font-serif);
  font-size: 42px;
  font-weight: 700;
  color: var(--faded-red);
  line-height: 1;
  text-shadow: 0 2px 4px rgba(var(--red-rgb), 0.2);
}

.is-past .number-value {
  color: var(--brown-medium);
}

.number-unit {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-secondary);
}

.countdown-card__status {
  font-size: 12px;
  letter-spacing: 1px;
}

.status-today {
  color: var(--faded-red);
  font-weight: 600;
  animation: heartbeat 1.5s ease-in-out infinite;
}

.status-future {
  color: var(--olive);
}

.status-past {
  color: var(--text-muted);
}

/* 底部撕页 */
.countdown-card__tear {
  position: absolute;
  bottom: -1px;
  left: 10%;
  right: 10%;
  height: 6px;
  background: var(--gradient-card);
  clip-path: polygon(
    0% 0%,
    5% 80%,
    10% 20%,
    15% 90%,
    20% 10%,
    25% 70%,
    30% 30%,
    35% 100%,
    40% 15%,
    45% 80%,
    50% 25%,
    55% 90%,
    60% 10%,
    65% 75%,
    70% 35%,
    75% 95%,
    80% 20%,
    85% 85%,
    90% 30%,
    95% 70%,
    100% 0%
  );
}
</style>
