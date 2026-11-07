import { describe, expect, it } from 'vitest'
import { getThumbUrl } from './image'

describe('getThumbUrl', () => {
  it('改写静态图片为 _thumb.jpg', () => {
    expect(getThumbUrl('/uploads/2026/08/07/abc.jpg')).toBe('/uploads/2026/08/07/abc_thumb.jpg')
    expect(getThumbUrl('/uploads/2026/08/07/abc.webp')).toBe('/uploads/2026/08/07/abc_thumb.jpg')
  })

  it('保留签名查询参数并复用到缩略图', () => {
    const signed = '/uploads/2026/08/07/abc.jpg?exp=1767&sig=9f8e'
    expect(getThumbUrl(signed)).toBe('/uploads/2026/08/07/abc_thumb.jpg?exp=1767&sig=9f8e')
  })

  it('gif 等非静态图片返回原地址（含查询参数）', () => {
    const signed = '/uploads/2026/08/07/abc.gif?exp=1767&sig=9f8e'
    expect(getThumbUrl(signed)).toBe(signed)
  })

  it('data/blob 地址与无扩展名地址原样返回', () => {
    expect(getThumbUrl('data:image/png;base64,xxxx')).toBe('data:image/png;base64,xxxx')
    expect(getThumbUrl('blob:http://localhost/uuid')).toBe('blob:http://localhost/uuid')
    expect(getThumbUrl('/uploads/2026/08/07/noext')).toBe('/uploads/2026/08/07/noext')
  })

  it('空值安全', () => {
    expect(getThumbUrl('')).toBe('')
    expect(getThumbUrl(null)).toBe(null)
  })
})
