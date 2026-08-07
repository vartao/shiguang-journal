<template>
  <div class="feedback-state" role="alert">
    <div class="feedback-state__icon" aria-hidden="true">!</div>
    <h3>{{ title }}</h3>
    <p>{{ message }}</p>
    <button type="button" class="btn btn-secondary" @click="$emit('retry')">重新试试</button>
  </div>
</template>
<script setup>
defineProps({
  title: { type: String, default: '暂时没有翻到这一页' },
  message: { type: String, default: '网络可能走神了，请稍后重试。' }
})
defineEmits(['retry'])
</script>
<style scoped>
.feedback-state {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 48px 20px;
  color: var(--text-muted);
  text-align: center;
}
.feedback-state__icon {
  display: grid;
  width: 38px;
  height: 38px;
  color: var(--faded-red);
  font-weight: 700;
  border: 2px solid currentColor;
  border-radius: 50%;
  place-items: center;
}
h3 {
  margin: 4px 0 0;
  color: var(--brown-deep);
  font-family: var(--font-display);
  font-size: 22px;
}
p {
  max-width: 440px;
  margin: 0 0 8px;
  line-height: 1.7;
}
</style>
