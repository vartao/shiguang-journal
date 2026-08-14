<template>
  <teleport to="body">
    <transition name="lightbox">
      <div v-if="visible" class="lightbox" @click.self="close">
        <!-- 关闭按钮 -->
        <button class="lightbox__close" @click="close">&times;</button>

        <!-- 上一张 -->
        <button v-if="images.length > 1" class="lightbox__nav lightbox__nav--prev" @click.stop="prev">&#10094;</button>

        <!-- 图片 -->
        <div class="lightbox__content" @click.stop>
          <transition :name="slideDirection" mode="out-in">
            <div :key="currentIndex" class="lightbox__image-wrapper">
              <img
                :src="currentImage.url || currentImage"
                :alt="currentImage.caption || '照片'"
                class="lightbox__image"
              />
              <p v-if="currentImage.caption" class="lightbox__caption">
                {{ currentImage.caption }}
              </p>
            </div>
          </transition>
        </div>

        <!-- 下一张 -->
        <button v-if="images.length > 1" class="lightbox__nav lightbox__nav--next" @click.stop="next">&#10095;</button>

        <!-- 计数器 -->
        <div v-if="images.length > 1" class="lightbox__counter">{{ currentIndex + 1 }} / {{ images.length }}</div>

        <!-- 缩略图 -->
        <div v-if="images.length > 1 && images.length <= 12" class="lightbox__thumbs">
          <button
            v-for="(img, idx) in images"
            :key="idx"
            class="lightbox__thumb"
            :class="{ active: idx === currentIndex }"
            @click.stop="goTo(idx)"
          >
            <img :src="img.url || img" :alt="'缩略图' + (idx + 1)" />
          </button>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  images: { type: Array, default: () => [] },
  startIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['close', 'update:visible'])

const currentIndex = ref(props.startIndex)
const slideDirection = ref('slide-left')

const currentImage = computed(() => {
  return props.images[currentIndex.value] || {}
})

watch(
  () => props.startIndex,
  (val) => {
    currentIndex.value = val
  }
)

watch(
  () => props.visible,
  (val) => {
    if (val) {
      currentIndex.value = props.startIndex
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
  }
)

function close() {
  emit('update:visible', false)
  emit('close')
}

function prev() {
  slideDirection.value = 'slide-right'
  currentIndex.value = (currentIndex.value - 1 + props.images.length) % props.images.length
}

function next() {
  slideDirection.value = 'slide-left'
  currentIndex.value = (currentIndex.value + 1) % props.images.length
}

function goTo(idx) {
  slideDirection.value = idx > currentIndex.value ? 'slide-left' : 'slide-right'
  currentIndex.value = idx
}

function handleKeydown(e) {
  if (!props.visible) return
  switch (e.key) {
    case 'Escape':
      close()
      break
    case 'ArrowLeft':
      prev()
      break
    case 'ArrowRight':
      next()
      break
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 10001;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(20, 14, 10, 0.92);
  backdrop-filter: blur(6px);
}

.lightbox__close {
  position: absolute;
  top: 20px;
  right: 24px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: rgba(var(--cream-static-rgb), 0.8);
  background: rgba(var(--cream-static-rgb), 0.08);
  border-radius: 50%;
  transition: all var(--transition-fast);
  z-index: 10;
}

.lightbox__close:hover {
  background: rgba(var(--cream-static-rgb), 0.15);
  color: var(--text-on-dark);
  transform: rotate(90deg);
}

/* 导航按钮 */
.lightbox__nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: rgba(var(--cream-static-rgb), 0.7);
  background: rgba(var(--cream-static-rgb), 0.06);
  border-radius: 50%;
  transition: all var(--transition-fast);
  z-index: 10;
}

.lightbox__nav:hover {
  background: rgba(var(--cream-static-rgb), 0.14);
  color: var(--text-on-dark);
}

.lightbox__nav--prev {
  left: 20px;
}
.lightbox__nav--next {
  right: 20px;
}

/* 图片内容 */
.lightbox__content {
  max-width: 85vw;
  max-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox__image-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.lightbox__image {
  max-width: 85vw;
  max-height: 72vh;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.5);
  /* 轻微复古滤镜 */
  filter: sepia(0.08) saturate(0.95);
}

.lightbox__caption {
  font-family: var(--font-handwrite);
  font-size: 16px;
  color: rgba(var(--cream-static-rgb), 0.85);
  text-align: center;
  max-width: 500px;
}

/* 计数器 */
.lightbox__counter {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  font-family: var(--font-display);
  font-size: 13px;
  color: rgba(var(--cream-static-rgb), 0.6);
  letter-spacing: 2px;
}

/* 缩略图 */
.lightbox__thumbs {
  position: absolute;
  bottom: 50px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-md);
}

.lightbox__thumb {
  width: 48px;
  height: 48px;
  border-radius: 3px;
  overflow: hidden;
  opacity: 0.5;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
}

.lightbox__thumb:hover {
  opacity: 0.8;
}

.lightbox__thumb.active {
  opacity: 1;
  border-color: var(--faded-red);
}

.lightbox__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 过渡动画 */
.lightbox-enter-active {
  animation: modal-backdrop-in 0.3s ease both;
}

.lightbox-leave-active {
  animation: modal-backdrop-in 0.25s ease reverse both;
}

.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-40px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

@media (max-width: 768px) {
  .lightbox__nav {
    width: 44px;
    height: 44px;
    font-size: 20px;
    background: rgba(var(--cream-static-rgb), 0.12);
  }

  .lightbox__nav--prev {
    left: 8px;
  }
  .lightbox__nav--next {
    right: 8px;
  }

  .lightbox__close {
    top: 16px;
    right: 16px;
  }

  .lightbox__content {
    max-width: 94vw;
  }

  .lightbox__image {
    max-width: 94vw;
  }

  .lightbox__caption {
    max-width: 90vw;
    font-size: 15px;
  }

  .lightbox__thumbs {
    display: none;
  }
}

@media (max-width: 480px) {
  .lightbox__image {
    max-height: 66vh;
  }

  .lightbox__counter {
    bottom: 16px;
  }
}
</style>
