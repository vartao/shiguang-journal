import request from './request'

/**
 * 获取相册列表
 */
export function getAlbums() {
  return request.get('/albums')
}

/**
 * 获取相册详情（含照片列表）
 * @param {number} id
 */
export function getAlbumDetail(id) {
  return request.get(`/albums/${id}`)
}

/**
 * 创建相册
 * @param {{ name: string, description?: string, cover?: string, coverImage?: string }} data
 */
export function createAlbum(data) {
  const payload = {
    name: data.name,
    description: data.description,
    coverImage: data.coverImage || data.cover || undefined,
    cover: data.cover || data.coverImage || undefined
  }
  return request.post('/albums', payload)
}

/**
 * 更新相册
 * @param {number} id
 * @param {{ name?: string, description?: string, cover?: string, coverImage?: string }} data
 */
export function updateAlbum(id, data) {
  return request.put(`/albums/${id}`, {
    name: data.name,
    description: data.description,
    coverImage: data.coverImage || data.cover || undefined,
    cover: data.cover || data.coverImage || undefined
  })
}

/**
 * 删除相册
 * @param {number} id
 */
export function deleteAlbum(id) {
  return request.delete(`/albums/${id}`)
}

/**
 * 添加照片到相册（已上传的 URL）
 * @param {number} albumId
 * @param {{ url: string, caption?: string, description?: string }} data
 */
export function addPhoto(albumId, data) {
  return request.post(`/albums/${albumId}/photos`, {
    url: data.url || data.photoUrl,
    photoUrl: data.url || data.photoUrl,
    caption: data.caption,
    description: data.description || data.caption
  })
}

/**
 * 删除照片
 * @param {number} albumId
 * @param {number} photoId
 */
export function deletePhoto(albumId, photoId) {
  return request.delete(`/albums/${albumId}/photos/${photoId}`)
}
