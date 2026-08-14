<template>
  <div ref="mapRoot" class="china-map" @pointerleave="hideTooltip">
    <svg viewBox="0 0 1000 780" role="img" aria-label="中国地图点亮图">
      <path
        v-for="province in provinces"
        :key="province.adcode"
        class="china-map__province"
        :class="provinceClass(province.name)"
        :d="province.d"
        tabindex="0"
        role="button"
        :aria-label="tooltip(province.name)"
        @pointerenter="showTooltip($event, province.name)"
        @pointermove="moveTooltip"
        @focus="showTooltipFromFocus($event, province.name)"
        @blur="hideTooltip"
        @keydown.enter.prevent="handleSelect(province.name)"
        @keydown.space.prevent="handleSelect(province.name)"
        @click="handleSelect(province.name)"
      />
    </svg>
    <div
      v-show="hoveredProvince"
      class="china-map__tooltip"
      :class="tooltipClasses"
      :style="tooltipStyle"
      role="tooltip"
      aria-live="polite"
    >
      <strong>{{ hoveredProvince }}</strong>
      <span v-if="hoveredSummary">{{ hoveredSummary }}</span>
      <span v-if="hoveredCities.length">城市：{{ hoveredCityText }}</span>
    </div>
    <div class="china-map__legend">
      <span class="legend-item"><i class="legend-swatch legend-swatch--visited"></i>已去过</span>
      <span class="legend-item"><i class="legend-swatch legend-swatch--planned"></i>想去</span>
      <span class="legend-item"><i class="legend-swatch"></i>未点亮</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import chinaMapData from './chinaMapData'

const props = defineProps({
  // 省名 -> { visited, planned, cities }
  summary: { type: Object, default: () => ({}) },
  selected: { type: String, default: '' }
})
const emit = defineEmits(['select'])

const provinces = chinaMapData
const mapRoot = ref(null)
const hoveredProvince = ref('')
const tooltipPosition = ref({ x: 0, y: 0, alignRight: false, placeBelow: false })

const hoveredSummary = computed(() => {
  if (!hoveredProvince.value) return ''
  const stat = props.summary[hoveredProvince.value]
  if (!stat) return ''
  const parts = []
  if (stat.visited > 0) parts.push(`去过 ${stat.visited} 次`)
  if (stat.planned > 0) parts.push(`想去 ${stat.planned} 处`)
  return parts.join(' · ')
})

const hoveredCities = computed(() => {
  if (!hoveredProvince.value) return []
  return props.summary[hoveredProvince.value]?.cities || []
})

const hoveredCityText = computed(() => {
  const visibleCities = hoveredCities.value.slice(0, 6)
  const remaining = hoveredCities.value.length - visibleCities.length
  return `${visibleCities.join(' · ')}${remaining > 0 ? ` 等 ${hoveredCities.value.length} 个城市` : ''}`
})

const tooltipStyle = computed(() => ({
  left: `${tooltipPosition.value.x}px`,
  top: `${tooltipPosition.value.y}px`
}))

const tooltipClasses = computed(() => ({
  'china-map__tooltip--right': tooltipPosition.value.alignRight,
  'china-map__tooltip--below': tooltipPosition.value.placeBelow
}))

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

function updateTooltipPosition(clientX, clientY) {
  const rect = mapRoot.value?.getBoundingClientRect()
  if (!rect) return

  const x = Math.max(8, Math.min(clientX - rect.left, rect.width - 8))
  const y = Math.max(8, Math.min(clientY - rect.top, rect.height - 8))
  tooltipPosition.value = {
    x,
    y,
    alignRight: x > rect.width * 0.72,
    placeBelow: y < 72
  }
}

function showTooltip(event, name) {
  hoveredProvince.value = name
  updateTooltipPosition(event.clientX, event.clientY)
}

function moveTooltip(event) {
  updateTooltipPosition(event.clientX, event.clientY)
}

function showTooltipFromFocus(event, name) {
  hoveredProvince.value = name
  const rect = event.currentTarget.getBoundingClientRect()
  updateTooltipPosition(rect.left + rect.width / 2, rect.top + rect.height / 2)
}

function hideTooltip() {
  hoveredProvince.value = ''
}

function handleSelect(name) {
  // 再点已选省份则取消筛选
  emit('select', props.selected === name ? '' : name)
}
</script>

<style scoped>
.china-map {
  width: 100%;
  position: relative;
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

.china-map__province:focus-visible {
  outline: none;
  stroke: var(--faded-red);
  stroke-width: 3;
}

.china-map__tooltip {
  position: absolute;
  z-index: 5;
  display: grid;
  gap: 2px;
  min-width: 88px;
  max-width: min(240px, calc(100% - 16px));
  padding: 8px 11px;
  color: var(--text-on-dark);
  background: rgba(var(--tooltip-bg-rgb), 0.94);
  border: 1px solid rgba(var(--cream-static-rgb), 0.18);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-card);
  pointer-events: none;
  transform: translate(12px, calc(-100% - 10px));
  backdrop-filter: blur(6px);
}

.china-map__tooltip::after {
  content: '';
  position: absolute;
  left: -5px;
  bottom: 8px;
  width: 8px;
  height: 8px;
  background: rgba(var(--tooltip-bg-rgb), 0.94);
  border-left: 1px solid rgba(var(--cream-static-rgb), 0.18);
  border-bottom: 1px solid rgba(var(--cream-static-rgb), 0.18);
  transform: rotate(45deg);
}

.china-map__tooltip strong {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
}

.china-map__tooltip span {
  color: rgba(var(--cream-static-rgb), 0.72);
  font-size: 11px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.china-map__tooltip--right {
  transform: translate(calc(-100% - 12px), calc(-100% - 10px));
}

.china-map__tooltip--right::after {
  right: -5px;
  left: auto;
  border: 0;
  border-top: 1px solid rgba(var(--cream-static-rgb), 0.18);
  border-right: 1px solid rgba(var(--cream-static-rgb), 0.18);
}

.china-map__tooltip--below {
  transform: translate(12px, 10px);
}

.china-map__tooltip--below::after {
  top: 8px;
  bottom: auto;
}

.china-map__tooltip--below.china-map__tooltip--right {
  transform: translate(calc(-100% - 12px), 10px);
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
