<template>
  <div class="feedback-state" role="status" aria-live="polite">
    <span class="feedback-state__spinner" aria-hidden="true"></span>
    <p>{{ message }}</p>
  </div>
</template>
<script setup>
defineProps({ message: { type: String, default: '正在翻找回忆…' } })
</script>
<style scoped>
.feedback-state {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 48px 20px;
  color: var(--text-muted);
  text-align: center;
}
.feedback-state__spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(var(--brown-medium-rgb), 0.18);
  border-top-color: var(--faded-red);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
