<template>
  <transition name="modal-fade">
    <div v-if="modelValue" class="base-modal__overlay" data-testid="modal-overlay" @click.self="requestClose">
      <section class="base-modal" role="dialog" aria-modal="true" :aria-labelledby="titleId">
        <header v-if="title || $slots.header" class="base-modal__header">
          <slot name="header">
            <div>
              <p v-if="eyebrow" class="base-modal__eyebrow">{{ eyebrow }}</p>
              <h2 :id="titleId">{{ title }}</h2>
            </div>
          </slot>
          <button type="button" class="base-modal__close" aria-label="关闭" :disabled="busy" @click="requestClose">
            ×
          </button>
        </header>
        <div class="base-modal__body"><slot /></div>
        <footer v-if="$slots.footer" class="base-modal__footer"><slot name="footer" /></footer>
      </section>
    </div>
  </transition>
</template>

<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  eyebrow: { type: String, default: '' },
  busy: { type: Boolean, default: false },
  closeOnOverlay: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'close'])
const titleId = 'base-modal-title'

function requestClose(event) {
  if (props.busy) return
  if (event?.currentTarget?.dataset?.testid === 'modal-overlay' && !props.closeOnOverlay) return
  emit('update:modelValue', false)
  emit('close')
}
function handleKeydown(event) {
  if (props.modelValue && event.key === 'Escape') requestClose()
}
onMounted(() => window.addEventListener('keydown', handleKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
.base-modal__overlay {
  position: fixed;
  inset: 0;
  z-index: 11000;
  display: grid;
  place-items: center;
  padding: 20px;
  overflow-y: auto;
  background: rgba(44, 30, 22, 0.48);
  backdrop-filter: blur(5px);
}
.base-modal {
  width: min(620px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  background: linear-gradient(165deg, var(--cream), var(--cream-warm));
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lifted);
}
.base-modal__header,
.base-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px;
}
.base-modal__header {
  border-bottom: 1px dashed rgba(var(--brown-medium-rgb), 0.25);
}
.base-modal__header h2 {
  margin: 0;
  color: var(--brown-deep);
  font-family: var(--font-display);
  font-size: 24px;
}
.base-modal__eyebrow {
  margin: 0 0 4px;
  color: var(--faded-red);
  font-size: 12px;
  letter-spacing: 2px;
}
.base-modal__close {
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  color: var(--text-muted);
  font-size: 28px;
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  place-items: center;
}
.base-modal__close:hover {
  background: rgba(var(--brown-medium-rgb), 0.08);
}
.base-modal__body {
  padding: 26px;
}
.base-modal__footer {
  border-top: 1px dashed rgba(var(--brown-medium-rgb), 0.25);
}
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
@media (max-width: 600px) {
  .base-modal__overlay {
    align-items: end;
    padding: 10px;
  }
  .base-modal {
    max-height: calc(100vh - 20px);
    border-radius: 18px 18px 10px 10px;
  }
  .base-modal__header,
  .base-modal__footer {
    padding: 18px;
  }
  .base-modal__body {
    padding: 20px 18px;
  }
}
</style>
