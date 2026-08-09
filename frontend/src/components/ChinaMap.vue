<template>
  <div class="china-map">
    <svg viewBox="0 0 1000 780" role="img" aria-label="中国地图点亮图">
      <path
        v-for="province in provinces"
        :key="province.adcode"
        class="china-map__province"
        :class="provinceClass(province.name)"
        :d="province.d"
        @click="handleSelect(province.name)"
      >
        <title>{{ tooltip(province.name) }}</title>
      </path>
    </svg>
    <div class="china-map__legend">
      <span class="legend-item"><i class="legend-swatch legend-swatch--visited"></i>已去过</span>
      <span class="legend-item"><i class="legend-swatch legend-swatch--planned"></i>想去</span>
      <span class="legend-item"><i class="legend-swatch"></i>未点亮</span>
    </div>
  </div>
</template>

<script setup>
import chinaMapData from './chinaMapData'

const props = defineProps({
  // 省名 → { visited, planned }
  summary: { type: Object, default: () => ({}) },
  selected: { type: String, default: '' }
})
const emit = defineEmits(['select'])

const provinces = chinaMapData

function provinceClass(name) {
  const stat = props.summary[name]
  return {
    'china-map__province--visited': stat?.visited > 0,
    'china-map__province--planned': !(stat?.visited > 0) && stat?.planned > 0,
    'china-map__province--selected': props.selected === name
  }
}

function tooltip(name) {
  const stat = props.summary[name]
  if (!stat) return name
  const parts = []
  if (stat.visited > 0) parts.push(`去过 ${stat.visited} 次`)
  if (stat.planned > 0) parts.push(`想去 ${stat.planned} 处`)
  return parts.length ? `${name}：${parts.join('，')}` : name
}

function handleSelect(name) {
  // 再点已选省份则取消筛选
  emit('select', props.selected === name ? '' : name)
}
</script>

<style scoped>
.china-map {
  width: 100%;
}

.china-map svg {
  display: block;
  width: 100%;
  height: auto;
}

.china-map__province {
  fill: rgba(var(--kraft-rgb), 0.15);
  stroke: rgba(var(--brown-rgb), 0.25);
  stroke-width: 1;
  cursor: pointer;
  transition:
    fill var(--transition-fast),
    stroke var(--transition-fast);
}

.china-map__province:hover {
  fill: rgba(var(--kraft-rgb), 0.35);
  stroke: rgba(var(--brown-rgb), 0.5);
}

.china-map__province--visited {
  fill: rgba(var(--olive-rgb), 0.55);
  stroke: rgba(var(--olive-rgb), 0.8);
}

.china-map__province--visited:hover {
  fill: var(--olive);
}

.china-map__province--planned {
  fill: rgba(var(--mustard-rgb), 0.35);
  stroke: rgba(var(--mustard-rgb), 0.7);
  stroke-dasharray: 4 3;
}

.china-map__province--planned:hover {
  fill: rgba(var(--mustard-rgb), 0.55);
}

.china-map__province--selected {
  stroke: var(--faded-red);
  stroke-width: 2.5;
  stroke-dasharray: none;
}

.china-map__legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  background: rgba(var(--kraft-rgb), 0.15);
  border: 1px solid rgba(var(--brown-rgb), 0.25);
}

.legend-swatch--visited {
  background: rgba(var(--olive-rgb), 0.55);
  border-color: rgba(var(--olive-rgb), 0.8);
}

.legend-swatch--planned {
  background: rgba(var(--mustard-rgb), 0.35);
  border-color: rgba(var(--mustard-rgb), 0.7);
  border-style: dashed;
}

@media (max-width: 480px) {
  .china-map__legend {
    gap: 12px;
    font-size: 11px;
  }
}
</style>
