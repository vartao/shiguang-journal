/**
 * 根据后端上传约定获取缩略图地址：
 * 原图 /uploads/2026/08/07/xxx.jpg -> /uploads/2026/08/07/xxx_thumb.jpg
 * 带 签名查询参数 时保留参数（签名按不含扩展名的路径计算，缩略图可直接复用）。
 * 非静态图片（gif 等）或无法识别时返回原地址。
 * @param {string} url
 * @returns {string}
 */
export function getThumbUrl(url) {
  if (!url || typeof url !== 'string') return url
  if (url.startsWith('data:') || url.startsWith('blob:')) return url

  const queryIndex = url.indexOf('?')
  const cleanUrl = queryIndex >= 0 ? url.slice(0, queryIndex) : url
  const query = queryIndex >= 0 ? url.slice(queryIndex) : ''

  const lastSlash = cleanUrl.lastIndexOf('/')
  const dotIndex = cleanUrl.lastIndexOf('.')
  if (dotIndex <= lastSlash) return url

  const ext = cleanUrl.slice(dotIndex + 1).toLowerCase()
  if (!['jpg', 'jpeg', 'png', 'webp', 'bmp'].includes(ext)) return url

  return `${cleanUrl.slice(0, dotIndex)}_thumb.jpg${query}`
}
