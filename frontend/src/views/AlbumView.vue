<template>
  <div class="album-page">
    <div class="container">
      <!-- 页面标题 -->
      <header class="album-header anim-fade-in-up">
        <h1 class="album-header__title">相册墙</h1>
        <p class="album-header__subtitle">那些被定格的瞬间，散落在时光里</p>
      </header>

      <!-- 操作栏 -->
      <div class="album-toolbar anim-fade-in-up stagger-2">
        <button class="btn-vintage btn-vintage--red btn-vintage--small" @click="showCreateModal = true">
          <span>+</span> 新建相册
        </button>
      </div>

      <!-- 加载 -->
      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在整理照片...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="albums.length === 0" class="empty-state">
        <div class="empty-state__icon">&#x1F4F8;</div>
        <h3 class="empty-state__title">还没有相册</h3>
        <p class="empty-state__desc">创建第一个相册，把美好的照片收藏起来</p>
        <button class="btn-vintage btn-vintage--red" @click="showCreateModal = true">创建相册</button>
      </div>

      <!-- 相册墙 - 拍立得散落效果 -->
      <div v-else class="album-wall">
        <div
          v-for="(album, index) in albums"
          :key="album.id"
          class="album-wall__item anim-polaroid-drop"
          :class="'stagger-' + ((index % 8) + 1)"
          :style="{ '--rotate-to': getScatterRotation(index) + 'deg' }"
        >
          <PolaroidCard
            :src="album.cover || album.coverImage || (album.photos && album.photos[0])"
            :caption="album.name"
            :date="album.createdAt || album.createTime"
            :rotation="getScatterRotation(index)"
            :show-tape="index % 2 === 0"
            :tape-position="index % 4 === 0 ? 'top' : 'left'"
            @click="goToAlbum(album.id)"
          />
          <div class="album-wall__count">
            <span>&#x1F4F7;</span> {{ album.photoCount || (album.photos ? album.photos.length : 0) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 创建相册模态框 -->
    <BaseModal v-model="showCreateModal" title="新建相册" :busy="creating">
      <div class="album-form">
        <div class="form-group">
          <label class="field-label">相册名称</label>
          <input
            v-model="newAlbum.name"
            type="text"
            class="input-vintage"
            placeholder="例如：第一次旅行、日常碎片..."
          />
        </div>
        <div class="form-group">
          <label class="field-label">相册描述</label>
          <textarea
            v-model="newAlbum.description"
            class="input-vintage"
            rows="3"
            placeholder="这个相册里装着什么回忆？"
          ></textarea>
        </div>
        <div class="form-group">
          <label class="field-label">封面照片</label>
          <ImageUploader v-model="newAlbum.coverImages" :max-count="1" category="album" />
        </div>
      </div>
      <template #footer>
        <div class="album-form__actions">
          <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="showCreateModal = false">
            取消
          </button>
          <button
            class="btn-vintage btn-vintage--red btn-vintage--small"
            :disabled="!newAlbum.name.trim() || creating"
            @click="handleCreate"
          >
            {{ creating ? '创建中...' : '创建相册' }}
          </button>
        </div>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAlbums, createAlbum } from '@/api/album'
import PolaroidCard from '@/components/PolaroidCard.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import BaseModal from '@/components/BaseModal.vue'

const router = useRouter()

const albums = ref([])
const loading = ref(true)
const showCreateModal = ref(false)
const creating = ref(false)

const newAlbum = reactive({
  name: '',
  description: '',
  coverImages: []
})

function getScatterRotation(index) {
  const rotations = [-3, 2, -1.5, 3.5, -2.5, 1, -4, 2.5]
  return rotations[index % rotations.length]
}

function goToAlbum(id) {
  router.push(`/albums/${id}`)
}

async function fetchAlbums() {
  loading.value = true
  try {
    const res = await getAlbums()
    albums.value = (res.data || []).map(normalizeAlbum)
  } catch {
    albums.value = []
  } finally {
    loading.value = false
  }
}

function normalizeAlbum(album) {
  return {
    ...album,
    cover: album.cover || album.coverImage || '',
    coverImage: album.coverImage || album.cover || '',
    createdAt: album.createdAt || album.createTime || ''
  }
}

async function handleCreate() {
  if (!newAlbum.name.trim()) return
  creating.value = true
  try {
    const coverUrl = newAlbum.coverImages[0]?.url || ''
    await createAlbum({
      name: newAlbum.name.trim(),
      description: newAlbum.description,
      cover: coverUrl,
      coverImage: coverUrl
    })
    showCreateModal.value = false
    newAlbum.name = ''
    newAlbum.description = ''
    newAlbum.coverImages = []
    await fetchAlbums()
  } catch {
    // handled
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  fetchAlbums()
})
</script>

<style scoped>
.album-page {
  padding: 40px 0 80px;
}

.album-header {
  text-align: center;
  margin-bottom: 28px;
}

.album-header__title {
  font-family: var(--font-handwrite);
  font-size: 34px;
  color: var(--brown-deep);
  letter-spacing: 3px;
  margin-bottom: 8px;
}

.album-header__subtitle {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.album-toolbar {
  display: flex;
  justify-content: center;
  margin-bottom: 40px;
}

/* 相册墙 */
.album-wall {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 32px;
  padding: 20px 0;
}

.album-wall__item {
  width: 220px;
  max-width: 100%;
  position: relative;
}

.album-wall__count {
  text-align: center;
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

/* 新建相册表单 */
.album-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  letter-spacing: 1px;
}

.album-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-left: auto;
}

@media (max-width: 768px) {
  .album-page {
    padding: 28px 0 64px;
  }

  .album-header {
    margin-bottom: 20px;
  }

  .album-header__title {
    font-size: 28px;
    letter-spacing: 2px;
  }

  .album-toolbar {
    margin-bottom: 28px;
  }

  .album-wall {
    gap: 24px;
  }

  .album-wall__item {
    width: 160px;
  }

  .album-form .input-vintage {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .album-page {
    padding: 20px 0 56px;
  }

  .album-header__title {
    font-size: 26px;
  }

  .album-header__subtitle {
    font-size: 13px;
  }

  .album-wall {
    gap: 16px;
  }

  .album-wall__item {
    width: 150px;
  }
}
</style>
