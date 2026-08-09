<template>
  <div class="feedback-state">
    <div class="feedback-state__icon" aria-hidden="true">{{ icon }}</div>
    <h3>{{ title }}</h3>
    <p>{{ message }}</p>
    <slot />
  </div>
</template>
<script setup>
defineProps({
  icon: { type: String, default: '✦' },
  title: { type: String, default: '这里还空着' },
  message: { type: String, default: '留下第一份属于你们的记录吧。' }
})
</script>
<style scoped>
.feedback-state {
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 48px 32px;
  color: var(--text-secondary);
  text-align: center;
  max-width: 560px;
  margin: 0 auto;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
}
.feedback-state__icon {
  color: var(--mustard);
  font-size: 38px;
}
h3 {
  margin: 4px 0 0;
  color: var(--brown-deep);
  font-family: var(--font-display);
  font-size: 22px;
}
p {
  max-width: 440px;
  margin: 0;
  line-height: 1.7;
}
</style>
