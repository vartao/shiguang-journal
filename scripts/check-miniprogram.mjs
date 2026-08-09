#!/usr/bin/env node
// 小程序静态校验：JS 语法（node --check 等价）、JSON 合法、WXML 标签配对
// 用法：node scripts/check-miniprogram.mjs [目录，默认 miniprogram]
import { execFileSync } from 'node:child_process'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, extname } from 'node:path'

const root = process.argv[2] || 'miniprogram'
let errors = 0
let files = 0

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    if (name === 'node_modules' || name.startsWith('.')) continue
    const p = join(dir, name)
    if (statSync(p).isDirectory()) walk(p, out)
    else out.push(p)
  }
  return out
}

// WXML 里总是以 /> 自闭合的标签（其余一律要求显式闭合）
const VOID_TAGS = new Set(['import', 'include'])

function checkWxml(path) {
  const src = readFileSync(path, 'utf8')
  // 去掉注释 {{ }} 内的内容不动；仅做标签栈配对
  const tagRe = /<!--[\s\S]*?-->|<\/?([a-zA-Z][\w-]*)((?:"[^"]*"|'[^']*'|\{\{[\s\S]*?\}\}|[^<>"'])*?)\/?>/g
  const stack = []
  let m
  let line = 1
  let last = 0
  while ((m = tagRe.exec(src))) {
    line += src.slice(last, m.index).split('\n').length - 1
    last = m.index
    if (m[0].startsWith('<!--')) continue
    const [, tag, attrs] = m
    const isClose = m[0].startsWith('</')
    const selfClose = m[0].endsWith('/>') || VOID_TAGS.has(tag)
    if (isClose) {
      const top = stack.pop()
      if (top !== tag) {
        console.error(`✗ ${path}:${line} 闭合标签 </${tag}> 与栈顶 <${top ?? '空'}> 不匹配`)
        errors++
        return
      }
    } else if (!selfClose) {
      stack.push(tag)
    }
  }
  if (stack.length) {
    console.error(`✗ ${path} 存在未闭合标签: <${stack.join('> <')}>`)
    errors++
  }
}

for (const file of walk(root)) {
  const ext = extname(file)
  files++
  try {
    if (ext === '.js') {
      execFileSync(process.execPath, ['--check', file], { stdio: 'pipe' })
    } else if (ext === '.json') {
      JSON.parse(readFileSync(file, 'utf8'))
    } else if (ext === '.wxml') {
      checkWxml(file)
    }
  } catch (e) {
    console.error(`✗ ${file}: ${String(e.message || e).split('\n')[0]}`)
    errors++
  }
}

if (errors) {
  console.error(`\n共 ${errors} 处问题（检查了 ${files} 个文件）`)
  process.exit(1)
}
console.log(`✓ 小程序静态校验通过（${files} 个文件）`)
