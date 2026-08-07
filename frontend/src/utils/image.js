/**
 * 根据后端上传约定获取缩略图地址：
 * 原图 /uploads/2026/08/07/xxx.jpg -> /uploads/2026/08/07/xxx_thumb.jpg
 * 非静态图片（gif 等）或无法识别时返回原地址。
 * @param {string} url
 * @returns {string}
 */
export function getThumbUrl(url) {
  if (!url || typeof url !== 'string') return url
  if (url.startsWith('data:') || url.startsWith('blob:')) return url

  const lastSlash = url.lastIndexOf('/')
  const dotIndex = url.lastIndexOf('.')
  if (dotIndex <= lastSlash) return url

  const ext = url.slice(dotIndex + 1).toLowerCase()
  if (!['jpg', 'jpeg', 'png', 'webp', 'bmp'].includes(ext)) return url

  return `${url.slice(0, dotIndex)}_thumb.jpg`
}
