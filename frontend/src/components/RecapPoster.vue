<template>
  <BaseModal :model-value="visible" title="回忆海报" eyebrow="RECAP POSTER" @close="$emit('close')">
    <div class="poster-wrap">
      <canvas ref="canvasRef" class="poster-canvas" :width="WIDTH" :height="HEIGHT"></canvas>
    </div>
    <template #footer>
      <button type="button" class="btn-vintage btn-vintage--outline" @click="draw">重新生成</button>
      <button type="button" class="btn-vintage btn-vintage--red" @click="download">保存图片</button>
    </template>
  </BaseModal>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import request from '@/api/request'
import { useCoupleStore } from '@/stores/couple'

const props = defineProps({
  visible: { type: Boolean, default: false },
  stats: { type: Object, default: null }
})
defineEmits(['close'])

const WIDTH = 750
const HEIGHT = 1200

const coupleStore = useCoupleStore()
const canvasRef = ref(null)

const palette = {
  paper: '#faf3e0',
  paperDeep: '#f0e2c8',
  ink: '#3d2b1f',
  secondary: '#6b5344',
  muted: '#7d6a56',
  red: '#c1666b',
  olive: '#7d8c5c',
  mustard: '#c9a227',
  line: 'rgba(92, 61, 46, 0.18)'
}

const FONT_HAND = '"Ma Shan Zheng", "STXingkai", "KaiTi", "楷体", cursive'
const FONT_SERIF = '"Noto Serif SC", "Songti SC", "SimSun", serif'

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      await nextTick()
      draw()
    }
  }
)

async function fetchTravelSummary() {
  try {
    const res = await request.get('/travels/map')
    const list = res.data || []
    const visited = list.filter((p) => (p.visited || 0) > 0)
    const planned = list.filter((p) => (p.planned || 0) > 0 && !(p.visited > 0))
    return { provinces: visited.length, planned: planned.length }
  } catch {
    return { provinces: 0, planned: 0 }
  }
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

async function draw() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const travel = await fetchTravelSummary()
  const stats = props.stats || {}
  const today = new Date()
  const dateStr = `${today.getFullYear()}.${String(today.getMonth() + 1).padStart(2, '0')}.${String(today.getDate()).padStart(2, '0')}`

  // 纸面底
  ctx.fillStyle = palette.paper
  ctx.fillRect(0, 0, WIDTH, HEIGHT)
  // 淡纹理横线
  ctx.strokeStyle = 'rgba(92, 61, 46, 0.05)'
  ctx.lineWidth = 1
  for (let y = 40; y < HEIGHT; y += 36) {
    ctx.beginPath()
    ctx.moveTo(0, y)
    ctx.lineTo(WIDTH, y)
    ctx.stroke()
  }
  // 边框
  ctx.strokeStyle = palette.line
  ctx.lineWidth = 2
  ctx.strokeRect(24, 24, WIDTH - 48, HEIGHT - 48)
  ctx.strokeRect(32, 32, WIDTH - 64, HEIGHT - 64)

  let y = 110

  // 报头
  ctx.textAlign = 'center'
  ctx.fillStyle = palette.muted
  ctx.font = `20px ${FONT_SERIF}`
  ctx.fillText('拾 光 手 账 · 回 忆 海 报', WIDTH / 2, y)
  y += 66
  ctx.fillStyle = palette.ink
  ctx.font = `56px ${FONT_HAND}`
  ctx.fillText(coupleStore.coupleName || '我们的小窝', WIDTH / 2, y)
  y += 34
  ctx.fillStyle = palette.secondary
  ctx.font = `20px ${FONT_SERIF}`
  const range = coupleStore.startDate ? `${coupleStore.startDate} 至今` : ''
  ctx.fillText(`${range} · 生成于 ${dateStr}`, WIDTH / 2, y)
  y += 30

  // 分隔线
  ctx.strokeStyle = palette.line
  ctx.beginPath()
  ctx.moveTo(90, y)
  ctx.lineTo(WIDTH - 90, y)
  ctx.stroke()
  y += 78

  // 在一起天数（主角数字）
  ctx.fillStyle = palette.red
  ctx.font = `700 120px ${FONT_SERIF}`
  ctx.fillText(String(coupleStore.daysTogether || 0), WIDTH / 2, y)
  y += 42
  ctx.fillStyle = palette.secondary
  ctx.font = `22px ${FONT_SERIF}`
  ctx.fillText('天 · 我们在一起', WIDTH / 2, y)
  y += 80

  // 数据格子（两行三列）
  const cells = [
    { num: stats.noteCount || 0, label: '篇笔记' },
    { num: stats.photoCount || 0, label: '张照片' },
    { num: stats.albumCount || 0, label: '本相册' },
    { num: stats.totalLikes || 0, label: '次点赞' },
    { num: travel.provinces, label: '个点亮的省份' },
    { num: stats.unlockedSurpriseCount || 0, label: '个拆开的惊喜' }
  ]
  const cellW = (WIDTH - 120 - 32) / 3
  const cellH = 150
  cells.forEach((cell, i) => {
    const col = i % 3
    const row = Math.floor(i / 3)
    const x = 60 + col * (cellW + 16)
    const cy = y + row * (cellH + 16)
    ctx.fillStyle = palette.paperDeep
    roundRect(ctx, x, cy, cellW, cellH, 12)
    ctx.fill()
    ctx.strokeStyle = palette.line
    ctx.lineWidth = 1.5
    ctx.stroke()
    ctx.fillStyle = palette.ink
    ctx.font = `700 44px ${FONT_SERIF}`
    ctx.fillText(String(cell.num), x + cellW / 2, cy + 66)
    ctx.fillStyle = palette.muted
    ctx.font = `19px ${FONT_SERIF}`
    ctx.fillText(cell.label, x + cellW / 2, cy + 106)
  })
  y += cellH * 2 + 16 + 64

  // 心情一句话（模板文案）
  const moods = stats.moodDistribution || {}
  const topMood = Object.entries(moods).sort((a, b) => b[1] - a[1])[0]
  const lines = []
  if (topMood) lines.push(`这些日子里，最多的心情是「${topMood[0]}」。`)
  if (travel.provinces > 0)
    lines.push(
      `我们一起点亮了 ${travel.provinces} 个省份${travel.planned ? `，还想去 ${travel.planned} 个地方` : ''}。`
    )
  if ((stats.longestStreak || 0) > 0) lines.push(`最长连续记录了 ${stats.longestStreak} 天，把日子过成了坚持。`)
  if (!lines.length) lines.push('故事才刚刚开始，慢慢写。')
  ctx.fillStyle = palette.secondary
  ctx.font = `22px ${FONT_SERIF}`
  lines.forEach((line) => {
    ctx.fillText(line, WIDTH / 2, y)
    y += 40
  })

  // 底部签名
  ctx.fillStyle = palette.muted
  ctx.font = `18px ${FONT_SERIF}`
  ctx.fillText('♡ 把日子过成胶片里的诗 ♡', WIDTH / 2, HEIGHT - 70)
  ctx.font = `16px ${FONT_SERIF}`
  ctx.fillText('拾光手账', WIDTH / 2, HEIGHT - 42)
}

function download() {
  const canvas = canvasRef.value
  if (!canvas) return
  const a = document.createElement('a')
  a.href = canvas.toDataURL('image/png')
  a.download = `回忆海报-${Date.now()}.png`
  a.click()
}
</script>

<style scoped>
.poster-wrap {
  display: flex;
  justify-content: center;
}

.poster-canvas {
  width: 100%;
  max-width: 360px;
  height: auto;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-card);
}
</style>
