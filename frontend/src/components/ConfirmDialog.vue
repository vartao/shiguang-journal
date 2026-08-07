<template>
  <transition name="confirm-fade">
    <div v-if="confirmState.open" class="confirm-overlay" @click.self="cancelConfirm">
      <section
        class="confirm-dialog"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="descriptionId"
        @keydown.esc="cancelConfirm"
      >
        <div class="confirm-dialog__pin" aria-hidden="true"></div>
        <p class="confirm-dialog__eyebrow">拾光提醒</p>
        <h2 :id="titleId">{{ confirmState.title }}</h2>
        <p :id="descriptionId" class="confirm-dialog__message">{{ confirmState.message }}</p>
        <div class="confirm-dialog__actions">
          <button
            type="button"
            class="btn btn-ghost"
            data-testid="cancel-action"
            :disabled="confirmState.busy"
            @click="cancelConfirm"
          >
            {{ confirmState.cancelText }}
          </button>
          <button
            type="button"
            class="btn"
            :class="confirmState.danger ? 'btn-danger' : 'btn-primary'"
            data-testid="confirm-action"
            :disabled="confirmState.busy"
            @click="resolveConfirm(true)"
          >
            {{ confirmState.busy ? '处理中…' : confirmState.confirmText }}
          </button>
        </div>
      </section>
    </div>
  </transition>
</template>

<script setup>
import { cancelConfirm, confirmState, resolveConfirm } from '@/composables/confirmService'

const titleId = 'global-confirm-title'
const descriptionId = 'global-confirm-description'
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 12000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(47, 32, 24, 0.5);
  backdrop-filter: blur(5px);
}

.confirm-dialog {
  position: relative;
  width: min(420px, 100%);
  padding: 34px 30px 28px;
  overflow: hidden;
  color: var(--brown-deep);
  text-align: center;
  background: linear-gradient(165deg, var(--cream), var(--cream-warm));
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lifted);
}

.confirm-dialog::before {
  position: absolute;
  inset: 10px;
  pointer-events: none;
  content: '';
  border: 1px dashed rgba(var(--brown-medium-rgb), 0.22);
  border-radius: calc(var(--radius-lg) - 6px);
}

.confirm-dialog__pin {
  position: absolute;
  top: 11px;
  left: 50%;
  width: 12px;
  height: 12px;
  background: var(--faded-red);
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(60, 30, 20, 0.35);
  transform: translateX(-50%);
}

.confirm-dialog__eyebrow {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-size: 12px;
  letter-spacing: 3px;
}

.confirm-dialog h2 {
  position: relative;
  margin: 0;
  font-family: var(--font-display);
  font-size: 24px;
}

.confirm-dialog__message {
  position: relative;
  margin: 16px 0 26px;
  color: var(--text-secondary);
  line-height: 1.75;
  white-space: pre-line;
}

.confirm-dialog__actions {
  position: relative;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.confirm-dialog__actions .btn {
  min-width: 96px;
  min-height: 44px;
}

.btn-danger {
  color: #fff;
  background: var(--faded-red);
  border-color: var(--faded-red);
}

.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 0.2s ease;
}

.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}

@media (max-width: 480px) {
  .confirm-dialog {
    padding: 32px 22px 24px;
  }

  .confirm-dialog__actions {
    flex-direction: column-reverse;
  }

  .confirm-dialog__actions .btn {
    width: 100%;
  }
}
</style>
