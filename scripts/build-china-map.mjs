#!/usr/bin/env node
/**
 * 构建中国省级地图 SVG 路径数据（frontend/src/components/chinaMapData.js）
 *
 * 数据源：阿里云 DataV 省级 GeoJSON（https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json）
 * 处理：等距投影到 viewBox 0 0 1000 780 → Douglas-Peucker 抽稀 → 输出 SVG path
 * 用法：node scripts/build-china-map.mjs
 */
import { writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const SOURCE_URL = 'https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json'
// Douglas-Peucker 容差（单位：度），抽稀后总点数控制在几千
const TOLERANCE = 0.08
const VIEW_W = 1000
const VIEW_H = 780
const PADDING = 10

const outFile = join(dirname(fileURLToPath(import.meta.url)), '../frontend/src/components/chinaMapData.js')

/** Douglas-Peucker 抽稀（在经纬度空间上进行） */
function simplify(points, tolerance) {
  if (points.length <= 4) return points
  const keep = new Array(points.length).fill(false)
  keep[0] = keep[points.length - 1] = true
  const stack = [[0, points.length - 1]]
  while (stack.length) {
    const [first, last] = stack.pop()
    if (last - first < 2) continue
    const [x1, y1] = points[first]
    const [x2, y2] = points[last]
    const dx = x2 - x1
    const dy = y2 - y1
    const lenSq = dx * dx + dy * dy
    let maxDist = -1
    let index = -1
    for (let i = first + 1; i < last; i++) {
      const [px, py] = points[i]
      // 点到线段距离的平方
      let t = lenSq === 0 ? 0 : ((px - x1) * dx + (py - y1) * dy) / lenSq
      t = Math.max(0, Math.min(1, t))
      const ddx = px - (x1 + t * dx)
      const ddy = py - (y1 + t * dy)
      const dist = ddx * ddx + ddy * ddy
      if (dist > maxDist) {
        maxDist = dist
        index = i
      }
    }
    if (Math.sqrt(maxDist) > tolerance) {
      keep[index] = true
      stack.push([first, index], [index, last])
    }
  }
  const result = points.filter((_, i) => keep[i])
  // 环形至少保留 4 个点，否则退回首尾加点
  return result.length >= 4 ? result : [points[0], points[Math.floor(points.length / 2)], points[points.length - 1], points[0]]
}

/** 等距投影：x = (lng - minLng) * scale，y = (maxLat - lat) * scale */
function makeProjector(features) {
  let minLng = Infinity
  let maxLng = -Infinity
  let minLat = Infinity
  let maxLat = -Infinity
  const visit = (coords) => {
    for (const [lng, lat] of coords) {
      if (lng < minLng) minLng = lng
      if (lng > maxLng) maxLng = lng
      if (lat < minLat) minLat = lat
      if (lat > maxLat) maxLat = lat
    }
  }
  for (const f of features) {
    const geom = f.geometry
    if (!geom) continue
    if (geom.type === 'Polygon') geom.coordinates.forEach(visit)
    else if (geom.type === 'MultiPolygon') geom.coordinates.forEach((poly) => poly.forEach(visit))
  }
  const scale = Math.min((VIEW_W - PADDING * 2) / (maxLng - minLng), (VIEW_H - PADDING * 2) / (maxLat - minLat))
  // 内容在 viewBox 中居中
  const offsetX = (VIEW_W - (maxLng - minLng) * scale) / 2
  const offsetY = (VIEW_H - (maxLat - minLat) * scale) / 2
  return ([lng, lat]) => [offsetX + (lng - minLng) * scale, offsetY + (maxLat - lat) * scale]
}

function ringToPath(ring, project) {
  const parts = ring.map((pt, i) => {
    const [x, y] = project(pt).map((v) => Math.round(v * 10) / 10)
    return `${i === 0 ? 'M' : 'L'}${x},${y}`
  })
  return parts.join('') + 'Z'
}

const res = await fetch(SOURCE_URL)
if (!res.ok) throw new Error(`下载失败: HTTP ${res.status}`)
const geojson = await res.json()
const features = (geojson.features || []).filter((f) => f.geometry && f.properties?.name)
const project = makeProjector(features)

let totalPoints = 0
const provinces = []
for (const f of features) {
  const geom = f.geometry
  const polygons = geom.type === 'Polygon' ? [geom.coordinates] : geom.type === 'MultiPolygon' ? geom.coordinates : []
  if (!polygons.length) continue
  let d = ''
  for (const poly of polygons) {
    for (const ring of poly) {
      const simplified = simplify(ring, TOLERANCE)
      totalPoints += simplified.length
      d += ringToPath(simplified, project)
    }
  }
  provinces.push({ name: f.properties.name, adcode: f.properties.adcode, d })
}
provinces.sort((a, b) => String(a.adcode).localeCompare(String(b.adcode)))

const lines = provinces.map((p) => `  { name: ${JSON.stringify(p.name)}, adcode: ${p.adcode}, d: '${p.d}' }`)
const content = `// 本文件由 scripts/build-china-map.mjs 自动生成，请勿手工编辑
// 数据源：阿里云 DataV 省级 GeoJSON（100000_full.json），等距投影到 viewBox 0 0 1000 780
// prettier-ignore
export default [
${lines.join(',\n')}
]
`

await writeFile(outFile, content, 'utf8')
const kb = (Buffer.byteLength(content) / 1024).toFixed(1)
console.log(`省份数量: ${provinces.length}`)
console.log(`抽稀后总点数: ${totalPoints}`)
console.log(`输出文件: ${outFile} (${kb} KB)`)
