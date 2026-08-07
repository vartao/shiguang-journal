import request from './request'

/**
 * 上传图片
 * @param {File} file
 * @param {string} category - 分类：note / album / avatar / surprise（后端当前忽略，预留）
 * @param {function} onProgress - 上传进度回调
 */
export function uploadImage(file, category = 'common', onProgress = null) {
  const formData = new FormData()
  formData.append('file', file)
  if (category) {
    formData.append('category', category)
  }

  // 后端实际路径：POST /api/upload
  return request.post('/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000,
    onUploadProgress: (progressEvent) => {
      if (onProgress && progressEvent.total) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

/**
 * 批量上传图片
 * @param {File[]} files
 * @param {string} category
 * @param {function} onProgress
 */
export async function uploadImages(files, category = 'common', onProgress = null) {
  const results = []
  for (let i = 0; i < files.length; i++) {
    const res = await uploadImage(files[i], category, (percent) => {
      if (onProgress) {
        onProgress(Math.round(((i + percent / 100) / files.length) * 100))
      }
    })
    results.push(res.data)
  }
  return results
}
