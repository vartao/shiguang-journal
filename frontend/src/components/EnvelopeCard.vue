<template>
  <div class="envelope-card" :class="{ locked: locked, opening: opening }" @click="handleClick">
    <!-- 信封主体 -->
    <div class="envelope-card__body">
      <!-- 信封背面三角 -->
      <div class="envelope-card__back-flap"></div>

      <!-- 信件内容（打开时露出） -->
      <div v-if="!locked" class="envelope-card__letter">
        <p class="letter-preview">{{ preview }}</p>
      </div>

      <!-- 信封前面 -->
      <div class="envelope-card__front">
        <div class="envelope-card__front-left"></div>
        <div class="envelope-card__front-right"></div>
        <div class="envelope-card__front-bottom"></div>
      </div>

      <!-- 封口盖 -->
      <div class="envelope-card__flap" :class="{ open: opening }">
        <div class="flap-inner"></div>
      </div>

      <!-- 蜡封/锁 -->
      <div v-if="locked" class="envelope-card__seal">
        <span class="seal-icon">&#x1F512;</span>
      </div>
      <div v-else class="envelope-card__seal wax">
        <span class="seal-icon">&#x2764;</span>
      </div>
    </div>

    <!-- 标题 -->
    <div class="envelope-card__info">
      <h4 class="envelope-card__title">{{ title }}</h4>
      <p class="envelope-card__subtitle">
        <template v-if="locked">
          <span class="unlock-hint">{{ unlockHint }}</span>
        </template>
        <template v-else>
          <span class="from-text">来自 {{ from }}</span>
        </template>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  preview: { type: String, default: '一封写给你的信...' },
  from: { type: String, default: '爱你的人' },
  locked: { type: Boolean, default: false },
  unlockHint: { type: String, default: '等待解锁中...' }
})

const emit = defineEmits(['open', 'click'])

const opening = ref(false)

function handleClick() {
  if (props.locked) {
    emit('click')
    return
  }
  opening.value = true
  setTimeout(() => {
    emit('open')
  }, 600)
}
</script>

<style scoped>
.envelope-card {
  cursor: pointer;
  transition: transform var(--transition-normal);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.envelope-card:hover {
  transform: translateY(-4px);
}

.envelope-card.locked {
  cursor: default;
}

.envelope-card.locked:hover {
  transform: translateY(-2px);
}

/* 信封主体 */
.envelope-card__body {
  position: relative;
  width: 200px;
  max-width: 100%;
  height: 140px;
  perspective: 600px;
}

/* 信封背面 */
.envelope-card__back-flap {
  position: absolute;
  inset: 0;
  background: var(--envelope-back);
  border-radius: 4px;
  border: 1px solid rgba(var(--brown-rgb), 0.2);
  box-shadow: var(--shadow-card);
}

/* 信件 */
.envelope-card__letter {
  position: absolute;
  top: 10px;
  left: 12px;
  right: 12px;
  bottom: 20px;
  background: var(--letter-paper);
  border-radius: 2px;
  padding: 14px;
  z-index: 1;
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.envelope-card.opening .envelope-card__letter {
  transform: translateY(-50px);
}

.letter-preview {
  font-family: var(--font-handwrite);
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 信封前面 */
.envelope-card__front {
  position: absolute;
  inset: 0;
  z-index: 2;
  overflow: hidden;
  border-radius: 4px;
}

.envelope-card__front-left {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-left: 100px solid var(--envelope-body);
  border-top: 70px solid transparent;
  border-bottom: 70px solid transparent;
}

.envelope-card__front-right {
  position: absolute;
  top: 0;
  right: 0;
  width: 0;
  height: 0;
  border-right: 100px solid var(--envelope-side);
  border-top: 70px solid transparent;
  border-bottom: 70px solid transparent;
}

.envelope-card__front-bottom {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 0;
  border-bottom: 80px solid var(--envelope-bottom);
  border-left: 100px solid transparent;
  border-right: 100px solid transparent;
}

/* 封口盖 */
.envelope-card__flap {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 70px;
  z-index: 3;
  transform-origin: top center;
  transition: transform 0.5s ease;
}

.envelope-card__flap.open {
  transform: rotateX(180deg);
}

.flap-inner {
  width: 0;
  height: 0;
  border-left: 100px solid transparent;
  border-right: 100px solid transparent;
  border-top: 70px solid var(--envelope-flap);
  filter: brightness(0.97);
}

/* 蜡封/锁 */
.envelope-card__seal {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 40% 35%, var(--faded-rose), var(--faded-red-dark));
  border-radius: 50%;
  z-index: 4;
  box-shadow:
    0 2px 6px rgba(0, 0, 0, 0.25),
    inset 0 -1px 3px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s ease;
}

.envelope-card:hover .envelope-card__seal {
  transform: translate(-50%, -50%) scale(1.1);
}

.envelope-card.locked .envelope-card__seal {
  background: var(--envelope-seal);
}

.envelope-card.locked:hover .envelope-card__seal {
  animation: lock-wiggle 0.4s ease-in-out;
}

.seal-icon {
  font-size: 16px;
  filter: brightness(1.2);
}

/* 信息区域 */
.envelope-card__info {
  text-align: center;
}

.envelope-card__title {
  font-family: var(--font-handwrite);
  font-size: 18px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.envelope-card__subtitle {
  font-size: 12px;
  color: var(--text-muted);
}

.unlock-hint {
  font-style: italic;
  color: var(--kraft-dark);
}

.from-text {
  color: var(--faded-red);
}

/* 小屏手机：信封整体等比缩小（约 0.85 倍），border 三角同步收窄 */
@media (max-width: 480px) {
  .envelope-card {
    gap: 10px;
  }

  .envelope-card__body {
    width: 170px;
    height: 120px;
  }

  .envelope-card__letter {
    top: 8px;
    left: 10px;
    right: 10px;
    bottom: 18px;
    padding: 12px;
  }

  .letter-preview {
    font-size: 12px;
  }

  .envelope-card__front-left {
    border-left-width: 85px;
    border-top-width: 60px;
    border-bottom-width: 60px;
  }

  .envelope-card__front-right {
    border-right-width: 85px;
    border-top-width: 60px;
    border-bottom-width: 60px;
  }

  .envelope-card__front-bottom {
    border-bottom-width: 68px;
    border-left-width: 85px;
    border-right-width: 85px;
  }

  .envelope-card__flap {
    height: 60px;
  }

  .flap-inner {
    border-left-width: 85px;
    border-right-width: 85px;
    border-top-width: 60px;
  }

  .envelope-card__seal {
    width: 32px;
    height: 32px;
  }

  .seal-icon {
    font-size: 14px;
  }

  .envelope-card__title {
    font-size: 16px;
  }
}
</style>
