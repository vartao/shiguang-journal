<template>
  <div class="image-uploader">
    <!-- 已上传图片预览 -->
    <div v-if="imageList.length > 0" class="uploader__preview-grid">
      <div v-for="(img, index) in imageList" :key="index" class="uploader__preview-item">
        <img :src="img.url || img" :alt="'图片' + (index + 1)" />
        <button class="uploader__remove-btn" title="移除" @click.stop="removeImage(index)">&times;</button>
        <div v-if="img.progress !== undefined && img.progress < 100" class="uploader__progress">
          <div class="uploader__progress-bar" :style="{ width: img.progress + '%' }"></div>
        </div>
      </div>
    </div>

    <!-- 上传区域 -->
    <div
      v-if="imageList.length < maxCount"
      class="uploader__dropzone"
      :class="{ dragging: isDragging }"
      @click="triggerFileInput"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <div class="uploader__dropzone-content">
        <span class="uploader__icon">&#x1F4F7;</span>
        <p class="uploader__text">点击或拖拽图片到这里</p>
        <p class="uploader__hint">支持 JPG、PNG、GIF，单张不超过 10MB</p>
      </div>
    </div>

    <!-- 隐藏的 file input -->
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      :multiple="maxCount > 1"
      class="uploader__input"
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { uploadImage } from '@/api/upload'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  maxCount: { type: Number, default: 9 },
  maxSize: { type: Number, default: 10 * 1024 * 1024 }, // 10MB
  category: { type: String, default: 'common' },
  accept: { type: String, default: 'image/jpeg,image/png,image/gif,image/webp' }
})

const emit = defineEmits(['update:modelValue', 'uploaded', 'error'])

const fileInput = ref(null)
const isDragging = ref(false)

const imageList = computed(() => props.modelValue)

function triggerFileInput() {
  fileInput.value?.click()
}

function handleFileSelect(e) {
  const files = Array.from(e.target.files || [])
  processFiles(files)
  e.target.value = ''
}

function handleDrop(e) {
  isDragging.value = false
  const files = Array.from(e.dataTransfer.files).filter((f) => f.type.startsWith('image/'))
  processFiles(files)
}

async function processFiles(files) {
  const remaining = props.maxCount - imageList.value.length
  const validFiles = files.slice(0, remaining)

  for (const file of validFiles) {
    if (file.size > props.maxSize) {
      emit('error', `文件 ${file.name} 超过大小限制`)
      continue
    }

    // 添加占位（本地预览）
    const localUrl = URL.createObjectURL(file)
    const newList = [...imageList.value, { url: localUrl, progress: 0 }]
    emit('update:modelValue', newList)

    try {
      const res = await uploadImage(file, props.category, (percent) => {
        const idx = newList.findIndex((item) => item.url === localUrl)
        if (idx !== -1) {
          newList[idx] = { ...newList[idx], progress: percent }
          emit('update:modelValue', [...newList])
        }
      })

      // 上传成功，替换本地 URL
      const idx = newList.findIndex((item) => item.url === localUrl)
      if (idx !== -1) {
        newList[idx] = { url: res.data?.url || res.data, progress: 100 }
        emit('update:modelValue', [...newList])
        emit('uploaded', res.data)
      }
      URL.revokeObjectURL(localUrl)
    } catch {
      // 上传失败，移除占位
      const idx = newList.findIndex((item) => item.url === localUrl)
      if (idx !== -1) {
        newList.splice(idx, 1)
        emit('update:modelValue', [...newList])
      }
      URL.revokeObjectURL(localUrl)
      emit('error', `上传失败: ${file.name}`)
    }
  }
}

function removeImage(index) {
  const newList = [...imageList.value]
  const removed = newList.splice(index, 1)[0]
  if (removed.url?.startsWith('blob:')) {
    URL.revokeObjectURL(removed.url)
  }
  emit('update:modelValue', newList)
}
</script>

<style scoped>
.image-uploader {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 预览网格 */
.uploader__preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 10px;
}

.uploader__preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 2px solid rgba(var(--brown-rgb), 0.15);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.uploader__preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: sepia(0.1);
}

.uploader__remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 26, 26, 0.7);
  color: #fff;
  border-radius: 50%;
  font-size: 14px;
  line-height: 1;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.uploader__preview-item:hover .uploader__remove-btn {
  opacity: 1;
}

.uploader__progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: rgba(0, 0, 0, 0.3);
}

.uploader__progress-bar {
  height: 100%;
  background: var(--faded-red);
  transition: width 0.3s ease;
  border-radius: 0 2px 2px 0;
}

/* 拖拽上传区 */
.uploader__dropzone {
  border: var(--border-dashed);
  border-radius: var(--radius-md);
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  background: rgba(var(--cream-rgb), 0.5);
}

.uploader__dropzone:hover {
  border-color: var(--kraft-paper);
  background: rgba(var(--kraft-rgb), 0.08);
}

.uploader__dropzone.dragging {
  border-color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.06);
  transform: scale(1.01);
}

.uploader__dropzone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.uploader__icon {
  font-size: 32px;
  filter: sepia(0.3);
  opacity: 0.7;
}

.uploader__text {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-secondary);
}

.uploader__hint {
  font-size: 12px;
  color: var(--text-muted);
}

.uploader__input {
  display: none;
}
</style>
