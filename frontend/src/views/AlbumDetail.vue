<template>
  <div class="album-detail-page">
    <div class="container">
      <!-- 加载 -->
      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在翻开相册...</p>
      </div>

      <template v-else>
        <!-- 相册头部 -->
        <header class="detail-header anim-fade-in-up">
          <button class="detail-back" @click="goBack">&larr; 返回相册墙</button>
          <h1 class="detail-title">{{ album.name }}</h1>
          <p v-if="album.description" class="detail-desc">{{ album.description }}</p>
          <p class="detail-meta">
            共 {{ photos.length }} 张照片
            <span v-if="album.createdAt || album.createTime">
              · 创建于 {{ formatDate(album.createdAt || album.createTime) }}</span
            >
          </p>
          <div class="detail-actions">
            <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="openEdit">编辑相册</button>
            <button class="btn-vintage btn-vintage--outline btn-vintage--small danger-btn" @click="handleDeleteAlbum">
              删除相册
            </button>
          </div>
        </header>

        <!-- 上传照片 -->
        <div class="upload-section anim-fade-in-up stagger-2">
          <ImageUploader v-model="uploadImages" :max-count="20" category="album" @uploaded="handlePhotoUploaded" />
        </div>

        <!-- 照片网格 -->
        <div v-if="photos.length > 0" class="photo-grid">
          <div
            v-for="(photo, index) in photos"
            :key="photo.id || index"
            class="photo-grid__item anim-polaroid-drop"
            :class="'stagger-' + ((index % 8) + 1)"
            @click="openLightbox(index)"
          >
            <div class="photo-frame">
              <img
                :src="photo.url || photo.photoUrl || photo"
                :alt="photo.caption || photo.description || '照片'"
                loading="lazy"
              />
              <div class="photo-frame__overlay">
                <span class="zoom-icon">&#x1F50D;</span>
              </div>
              <button class="photo-delete" title="删除照片" @click.stop="handleDeletePhoto(photo, index)">
                &times;
              </button>
            </div>
            <p v-if="photo.caption" class="photo-caption">{{ photo.caption }}</p>
          </div>
        </div>

        <!-- 空照片 -->
        <div v-else class="empty-state">
          <div class="empty-state__icon">&#x1F5BC;</div>
          <h3 class="empty-state__title">相册还是空的</h3>
          <p class="empty-state__desc">上传照片，填满这页回忆吧</p>
        </div>
      </template>
    </div>

    <!-- 编辑相册 -->
    <BaseModal v-model="showEditModal" title="编辑相册">
      <div class="album-edit-form">
        <div class="form-group">
          <label class="field-label">相册名称</label>
          <input v-model="editForm.name" type="text" class="input-vintage" maxlength="50" />
        </div>
        <div class="form-group">
          <label class="field-label">相册描述</label>
          <textarea v-model="editForm.description" class="input-vintage" rows="3"></textarea>
        </div>
      </div>
      <template #footer>
        <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="showEditModal = false">取消</button>
        <button
          class="btn-vintage btn-vintage--red btn-vintage--small"
          :disabled="!editForm.name.trim() || saving"
          @click="handleSaveEdit"
        >
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </BaseModal>

    <!-- 灯箱 -->
    <LightBox
      :visible="lightboxVisible"
      :images="lightboxImages"
      :start-index="lightboxIndex"
      @close="lightboxVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { requestConfirm } from '@/composables/confirmService'
import { useRoute, useRouter } from 'vue-router'
import { getAlbumDetail, addPhoto, deletePhoto, updateAlbum, deleteAlbum } from '@/api/album'
import ImageUploader from '@/components/ImageUploader.vue'
import LightBox from '@/components/LightBox.vue'
import BaseModal from '@/components/BaseModal.vue'
import { parseLocalDate } from '@/utils/date'

const route = useRoute()
const router = useRouter()

const albumId = route.params.id
const album = ref({})
const photos = ref([])
const loading = ref(true)
const uploadImages = ref([])
const showEditModal = ref(false)
const saving = ref(false)
const editForm = reactive({
  name: '',
  description: ''
})

// 灯箱
const lightboxVisible = ref(false)
const lightboxImages = ref([])
const lightboxIndex = ref(0)

function goBack() {
  router.push('/albums')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = parseLocalDate(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function normalizePhoto(p) {
  if (!p) return p
  if (typeof p === 'string') return { url: p, photoUrl: p, caption: '' }
  return {
    ...p,
    url: p.url || p.photoUrl || '',
    photoUrl: p.photoUrl || p.url || '',
    caption: p.caption || p.description || ''
  }
}

function openLightbox(index) {
  lightboxImages.value = photos.value.map((p) => ({
    url: p.url || p.photoUrl || p,
    caption: p.caption || p.description || ''
  }))
  lightboxIndex.value = index
  lightboxVisible.value = true
}

async function fetchAlbum() {
  loading.value = true
  try {
    const res = await getAlbumDetail(albumId)
    album.value = res.data || {}
    photos.value = (res.data?.photos || []).map(normalizePhoto)
  } catch {
    album.value = {}
    photos.value = []
  } finally {
    loading.value = false
  }
}

async function handlePhotoUploaded(data) {
  try {
    const url = typeof data === 'string' ? data : data?.url || data?.photoUrl || data
    if (!url || String(url).startsWith('blob:')) return
    const res = await addPhoto(albumId, { url, caption: '' })
    photos.value.push(normalizePhoto(res.data || { url, photoUrl: url }))
  } catch {
    // 错误由拦截器提示
  }
}

async function handleDeletePhoto(photo, index) {
  const confirmed = await requestConfirm({
    title: '删除照片',
    message: '确定要删除这张照片吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  try {
    if (photo.id) {
      await deletePhoto(albumId, photo.id)
    }
    photos.value.splice(index, 1)
  } catch {
    // 保留界面状态；错误由请求拦截器提示，避免与服务端数据不一致。
  }
}

function openEdit() {
  editForm.name = album.value.name || ''
  editForm.description = album.value.description || ''
  showEditModal.value = true
}

async function handleSaveEdit() {
  if (!editForm.name.trim()) return
  saving.value = true
  try {
    const res = await updateAlbum(albumId, {
      name: editForm.name.trim(),
      description: editForm.description
    })
    album.value = { ...album.value, ...(res.data || {}), name: editForm.name.trim(), description: editForm.description }
    showEditModal.value = false
  } catch {
    // handled
  } finally {
    saving.value = false
  }
}

async function handleDeleteAlbum() {
  const confirmed = await requestConfirm({
    title: '删除相册',
    message: '确定要删除相册“' + (album.value.name || '') + '”吗？其中的照片也会一起删除。',
    confirmText: '删除'
  })
  if (!confirmed) return
  try {
    await deleteAlbum(albumId)
    router.push('/albums')
  } catch {
    // handled
  }
}

onMounted(() => {
  fetchAlbum()
})
</script>

<style scoped>
.album-detail-page {
  padding: 40px 0 80px;
}

/* 头部 */
.detail-header {
  text-align: center;
  margin-bottom: 32px;
}

.detail-back {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-muted);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  margin-bottom: 16px;
  display: inline-block;
}

.detail-back:hover {
  color: var(--text-secondary);
  background: rgba(var(--brown-rgb), 0.05);
}

.detail-title {
  font-family: var(--font-handwrite);
  font-size: 32px;
  color: var(--brown-deep);
  letter-spacing: 2px;
  margin-bottom: 8px;
}

.detail-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-style: italic;
}

.detail-meta {
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.detail-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.danger-btn:hover {
  color: var(--faded-red) !important;
  border-color: var(--faded-red) !important;
}

/* 上传区域 */
.upload-section {
  max-width: 600px;
  margin: 0 auto 40px;
}

/* 编辑相册弹窗表单 */
.album-edit-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
}

/* 照片网格 */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
  padding: 10px 0;
}

.photo-grid__item {
  cursor: pointer;
}

.photo-frame {
  position: relative;
  background: var(--photo-paper);
  padding: 8px;
  box-shadow: var(--shadow-polaroid);
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
  overflow: hidden;
}

.photo-frame:hover {
  transform: translateY(-4px) rotate(0deg);
  box-shadow: var(--shadow-lifted);
}

.photo-frame img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  filter: sepia(0.15) saturate(0.92);
  transition: filter 0.3s ease;
}

.photo-frame:hover img {
  filter: sepia(0.03) saturate(1);
}

.photo-frame__overlay {
  position: absolute;
  inset: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 26, 0.3);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.photo-frame:hover .photo-frame__overlay {
  opacity: 1;
}

.zoom-icon {
  font-size: 28px;
  filter: brightness(2);
}

.photo-delete {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 26, 0.7);
  color: #fff;
  border-radius: 50%;
  font-size: 16px;
  opacity: 0;
  transition: opacity var(--transition-fast);
  z-index: 5;
}

.photo-frame:hover .photo-delete {
  opacity: 1;
}

.photo-caption {
  margin-top: 8px;
  font-family: var(--font-handwrite);
  font-size: 14px;
  color: var(--text-secondary);
  text-align: center;
}

@media (max-width: 768px) {
  .album-detail-page {
    padding: 28px 0 64px;
  }

  .detail-header {
    margin-bottom: 24px;
  }

  .detail-back {
    padding: 10px 14px;
    margin-bottom: 12px;
  }

  .detail-title {
    font-size: 26px;
    letter-spacing: 1px;
    overflow-wrap: break-word;
  }

  .detail-actions {
    flex-wrap: wrap;
  }

  .upload-section {
    margin: 0 auto 28px;
  }

  .photo-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 16px;
  }

  /* 触屏无 hover，删除按钮常驻显示并放大到触控尺寸 */
  .photo-delete {
    width: 40px;
    height: 40px;
    font-size: 20px;
    opacity: 1;
  }

  /* 弹窗内输入框字号放大，避免 iOS 聚焦自动缩放 */
  .album-edit-form .input-vintage {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .album-detail-page {
    padding: 20px 0 56px;
  }

  .detail-title {
    font-size: 24px;
  }

  .photo-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .photo-caption {
    font-size: 13px;
  }
}
</style>
