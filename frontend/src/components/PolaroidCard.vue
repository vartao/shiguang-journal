<template>
  <div
    class="polaroid-card"
    :style="{ '--rotation': rotation + 'deg' }"
    @mouseenter="hovered = true"
    @mouseleave="hovered = false"
  >
    <!-- 胶带装饰 -->
    <div v-if="showTape" class="polaroid-card__tape" :class="'tape-' + tapePosition"></div>

    <!-- 图片区域 -->
    <div class="polaroid-card__image" @click="$emit('click')">
      <img v-if="src" :src="src" :alt="caption || '照片'" class="polaroid-card__img" loading="lazy" />
      <div v-else class="polaroid-card__placeholder">
        <span>&#x1F3DE;</span>
      </div>
      <!-- 日期角标 -->
      <div v-if="date" class="polaroid-card__date-badge">
        {{ formattedDate }}
      </div>
    </div>

    <!-- 手写文字区域 -->
    <div class="polaroid-card__caption">
      <p class="polaroid-card__text">{{ caption || '...' }}</p>
      <span v-if="author" class="polaroid-card__author">— {{ author }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { parseLocalDate } from '@/utils/date'

const props = defineProps({
  src: { type: String, default: '' },
  caption: { type: String, default: '' },
  author: { type: String, default: '' },
  date: { type: String, default: '' },
  rotation: { type: Number, default: 0 },
  showTape: { type: Boolean, default: false },
  tapePosition: { type: String, default: 'top' } // top, left, right
})

defineEmits(['click'])

const hovered = ref(false)

const formattedDate = computed(() => {
  if (!props.date) return ''
  const d = parseLocalDate(props.date)
  const month = d.getMonth() + 1
  const day = d.getDate()
  const year = String(d.getFullYear()).slice(2)
  return `${month}.${day}.'${year}`
})
</script>

<style scoped>
.polaroid-card {
  background: var(--photo-paper);
  padding: 12px 12px 16px;
  box-shadow: var(--shadow-polaroid);
  transform: rotate(var(--rotation));
  transition:
    transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1),
    box-shadow 0.3s ease;
  position: relative;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.polaroid-card:hover {
  transform: rotate(0deg) scale(1.04) translateY(-6px);
  box-shadow: var(--shadow-lifted);
  z-index: 10;
}

/* 胶带 */
.polaroid-card__tape {
  position: absolute;
  width: 70px;
  height: 24px;
  background: rgba(var(--kraft-rgb), 0.55);
  z-index: 5;
  transform: rotate(-2deg);
  border-radius: 1px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.tape-top {
  top: -10px;
  left: 50%;
  margin-left: -35px;
}

.tape-left {
  top: 20px;
  left: -20px;
  transform: rotate(-45deg);
}

.tape-right {
  top: 20px;
  right: -20px;
  transform: rotate(45deg);
}

/* 图片区域 */
.polaroid-card__image {
  position: relative;
  overflow: hidden;
  background: var(--cream-dark);
  aspect-ratio: 1 / 1;
}

.polaroid-card__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: sepia(0.18) saturate(0.92) contrast(1.02);
  transition:
    filter 0.4s ease,
    transform 0.5s ease;
}

.polaroid-card:hover .polaroid-card__img {
  filter: sepia(0.05) saturate(1);
  transform: scale(1.03);
}

.polaroid-card__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 160px;
  font-size: 40px;
  opacity: 0.4;
  background: linear-gradient(145deg, var(--cream-dark), var(--cream));
}

/* 日期角标 */
.polaroid-card__date-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 3px 8px;
  font-family: var(--font-display);
  font-size: 11px;
  color: var(--cream);
  background: rgba(26, 26, 26, 0.65);
  border-radius: 2px;
  letter-spacing: 0.5px;
  backdrop-filter: blur(2px);
}

/* 文字区域 */
.polaroid-card__caption {
  padding: 12px 4px 2px;
  text-align: center;
}

.polaroid-card__text {
  font-family: var(--font-handwrite);
  font-size: 16px;
  color: var(--text-handwrite);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.polaroid-card__author {
  display: block;
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
}
</style>
