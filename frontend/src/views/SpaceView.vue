<template>
  <div class="space-page">
    <div class="container">
      <header class="space-header anim-fade-in-up">
        <h1 class="space-header__title">我们的小窝</h1>
        <p class="space-header__subtitle">邀请码、头像、空间签名都在这里打理</p>
      </header>

      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在推开小窝的门...</p>
      </div>

      <template v-else>
        <!-- 空间概览 -->
        <section class="space-card anim-fade-in-up">
          <div class="space-card__banner">
            <div class="space-card__pair">
              <div class="avatar-ring">
                <img v-if="userStore.avatar" :src="userStore.avatar" alt="我" />
                <span v-else>{{ (userStore.nickname || '我').charAt(0) }}</span>
              </div>
              <span class="pair-heart">&#x2764;</span>
              <div class="avatar-ring avatar-ring--partner">
                <img v-if="partnerAvatar" :src="partnerAvatar" alt="TA" />
                <span v-else>{{ partnerInitial }}</span>
              </div>
            </div>
            <h2 class="space-card__name">{{ coupleStore.coupleName }}</h2>
            <p v-if="coupleStore.hasCouple" class="space-card__days">
              在一起 <strong>{{ coupleStore.daysTogether }}</strong> 天
              <span v-if="coupleStore.partnerName"> · 与 {{ coupleStore.partnerName }}</span>
            </p>
            <p v-if="coupleForm.announcement" class="space-card__announce">「{{ coupleForm.announcement }}」</p>
          </div>
        </section>

        <!-- 邀请码 / 绑定 -->
        <section class="panel anim-fade-in-up stagger-2">
          <h3 class="panel__title">邀请与绑定</h3>

          <div v-if="coupleStore.inviteCode" class="invite-box">
            <div class="invite-box__label">你们的邀请码</div>
            <div class="invite-box__code">
              <span>{{ coupleStore.inviteCode }}</span>
              <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="copyInvite">
                {{ copied ? '已复制' : '复制' }}
              </button>
            </div>
            <p class="panel__hint">把邀请码发给 TA，注册时填写即可加入这个小窝（最多 2 人）</p>
          </div>

          <div v-if="!hasPartner" class="bind-box">
            <label class="field-label">还是用对方的邀请码加入？</label>
            <p class="panel__hint">仅在你还没有伴侣时可用。小窝最多两人，满员后无法再绑定。</p>
            <div class="bind-row">
              <input
                v-model="bindCode"
                type="text"
                class="input-vintage"
                :class="{ 'input-error': !!bindError }"
                maxlength="12"
                placeholder="输入 6 位邀请码"
                @input="bindError = ''"
              />
              <button
                class="btn-vintage btn-vintage--red btn-vintage--small"
                :disabled="!bindCode.trim() || binding"
                @click="handleBind"
              >
                {{ binding ? '绑定中...' : '绑定' }}
              </button>
            </div>
            <p v-if="bindError" class="bind-error">{{ bindError }}</p>
          </div>
          <div v-else class="full-tip">
            <p class="panel__hint success-hint">小窝已满员 · {{ coupleStore.partnerName || 'TA' }} 已在身边</p>
            <p class="panel__hint">邀请码仍可查看，但其他人无法再加入。</p>
          </div>
        </section>

        <!-- 个人资料 -->
        <section class="panel anim-fade-in-up stagger-3">
          <h3 class="panel__title">我的资料</h3>
          <div class="form-group">
            <label class="field-label">昵称</label>
            <input
              v-model="profileForm.nickname"
              type="text"
              class="input-vintage"
              maxlength="20"
              placeholder="你的昵称"
            />
          </div>
          <div class="form-group">
            <label class="field-label">头像</label>
            <ImageUploader v-model="profileForm.avatarImages" :max-count="1" category="avatar" />
          </div>
          <div class="panel__actions">
            <button
              class="btn-vintage btn-vintage--red btn-vintage--small"
              :disabled="savingProfile"
              @click="saveProfile"
            >
              {{ savingProfile ? '保存中...' : '保存资料' }}
            </button>
          </div>
        </section>

        <!-- 空间设置 -->
        <section class="panel anim-fade-in-up stagger-4">
          <h3 class="panel__title">小窝设置</h3>
          <div class="form-group">
            <label class="field-label">空间名称</label>
            <input
              v-model="coupleForm.name"
              type="text"
              class="input-vintage"
              maxlength="50"
              placeholder="例如：我们的小窝"
            />
          </div>
          <div class="form-group">
            <label class="field-label">在一起的日子</label>
            <input v-model="coupleForm.togetherDate" type="date" class="input-vintage" />
          </div>
          <div class="form-group">
            <label class="field-label">空间签名 / 公告</label>
            <textarea
              v-model="coupleForm.announcement"
              class="input-vintage"
              rows="3"
              maxlength="200"
              placeholder="写一句只属于你们的话..."
            ></textarea>
          </div>
          <div class="form-group">
            <label class="field-label">空间封面（选填）</label>
            <ImageUploader v-model="coupleForm.coverImages" :max-count="1" category="couple" />
          </div>
          <div class="panel__actions">
            <button
              class="btn-vintage btn-vintage--red btn-vintage--small"
              :disabled="savingCouple"
              @click="saveCouple"
            >
              {{ savingCouple ? '保存中...' : '保存小窝' }}
            </button>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { updateProfile, updateCouple } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useCoupleStore } from '@/stores/couple'
import ImageUploader from '@/components/ImageUploader.vue'
import { showToast } from '@/utils/toast'

const userStore = useUserStore()
const coupleStore = useCoupleStore()

const loading = ref(true)
const copied = ref(false)
const binding = ref(false)
const bindCode = ref('')
const bindError = ref('')
const savingProfile = ref(false)
const savingCouple = ref(false)

function friendlyBindMessage(msg) {
  if (!msg) return '绑定失败，请稍后再试'
  const text = String(msg)
  if (text.includes('满员') || text.includes('住满')) {
    return '这个小窝已经住满两人啦，换一个邀请码，或让 TA 新建空间。'
  }
  if (text.includes('邀请码') && (text.includes('无效') || text.includes('不正确'))) {
    return '邀请码好像不对，请向 TA 再确认一次（注意空格）。'
  }
  if (text.includes('已经有小窝') || text.includes('无法再次') || text.includes('已在')) {
    return '你已经在一个小窝里了，不能再绑定别的邀请码。'
  }
  return text
}

const profileForm = reactive({
  nickname: '',
  avatarImages: []
})

const coupleForm = reactive({
  name: '',
  togetherDate: '',
  announcement: '',
  coverImages: []
})

const hasPartner = computed(() => {
  const c = coupleStore.coupleInfo
  if (!c) return false
  return !!(c.userId1 && c.userId2)
})

const partnerAvatar = computed(() => {
  const c = coupleStore.coupleInfo
  if (!c) return ''
  const myId = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (myId != null && String(c.userId1) === String(myId)) return c.user2Avatar || ''
  if (myId != null && String(c.userId2) === String(myId)) return c.user1Avatar || ''
  return c.user2Avatar || c.user1Avatar || ''
})

const partnerInitial = computed(() => {
  const name = coupleStore.partnerName || 'TA'
  return name.charAt(0)
})

function firstImageUrl(list) {
  if (!list || !list.length) return ''
  const item = list[0]
  const url = typeof item === 'string' ? item : item?.url
  if (!url || String(url).startsWith('blob:')) return ''
  return url
}

function fillForms() {
  profileForm.nickname = userStore.nickname || ''
  profileForm.avatarImages = userStore.avatar ? [{ url: userStore.avatar }] : []

  const c = coupleStore.coupleInfo || {}
  coupleForm.name = c.name || ''
  coupleForm.togetherDate = (c.togetherDate || c.startDate || '').toString().slice(0, 10)
  coupleForm.announcement = c.announcement || ''
  coupleForm.coverImages = c.avatar ? [{ url: c.avatar }] : []
}

async function loadData() {
  loading.value = true
  try {
    await Promise.all([userStore.fetchProfile().catch(() => null), coupleStore.fetchCoupleInfo()])
    fillForms()
  } finally {
    loading.value = false
  }
}

async function copyInvite() {
  const code = coupleStore.inviteCode
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    copied.value = true
    showToast('邀请码已复制', { duration: 2200 })
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch {
    // fallback
    const input = document.createElement('input')
    input.value = code
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    copied.value = true
    showToast('邀请码已复制', { duration: 2200 })
    setTimeout(() => {
      copied.value = false
    }, 2000)
  }
}

async function handleBind() {
  const code = bindCode.value.trim()
  if (!code) return
  if (code.length < 4) {
    bindError.value = '邀请码长度不太对，一般是 6 位'
    return
  }
  binding.value = true
  bindError.value = ''
  try {
    await coupleStore.bind(code)
    bindCode.value = ''
    await userStore.fetchProfile().catch(() => null)
    fillForms()
    showToast('绑定成功，欢迎回家', { duration: 2200 })
  } catch (err) {
    bindError.value = friendlyBindMessage(err?.message)
  } finally {
    binding.value = false
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    const avatar = firstImageUrl(profileForm.avatarImages)
    const res = await updateProfile({
      nickname: profileForm.nickname.trim(),
      avatar: avatar || undefined
    })
    if (res.data) {
      userStore.setUserInfo({
        ...userStore.userInfo,
        ...res.data,
        nickname: res.data.nickname ?? profileForm.nickname.trim(),
        avatar: res.data.avatar ?? avatar
      })
    }
    showToast('资料已保存', { duration: 2200 })
  } catch {
    // handled
  } finally {
    savingProfile.value = false
  }
}

async function saveCouple() {
  savingCouple.value = true
  try {
    const avatar = firstImageUrl(coupleForm.coverImages)
    const res = await updateCouple({
      name: coupleForm.name.trim() || undefined,
      togetherDate: coupleForm.togetherDate || undefined,
      announcement: coupleForm.announcement,
      avatar: avatar || undefined
    })
    if (res.data) {
      coupleStore.setCoupleInfo(res.data)
      fillForms()
    }
    showToast('小窝信息已更新', { duration: 2200 })
  } catch {
    // handled
  } finally {
    savingCouple.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.space-page {
  padding: 40px 0 80px;
}

.space-header {
  text-align: center;
  margin-bottom: 32px;
}

.space-header__title {
  font-family: var(--font-handwrite);
  font-size: 34px;
  color: var(--brown-deep);
  letter-spacing: 3px;
  margin-bottom: 8px;
}

.space-header__subtitle {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.space-card {
  margin-bottom: 24px;
}

.space-card__banner {
  text-align: center;
  padding: 36px 24px 28px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.space-card__pair {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 18px;
}

.avatar-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid var(--kraft-paper);
  box-shadow: var(--shadow-soft);
  background: linear-gradient(145deg, var(--kraft-light), var(--kraft-paper));
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-handwrite);
  font-size: 28px;
  color: var(--brown-deep);
}

.avatar-ring img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-ring--partner {
  border-color: var(--faded-rose);
}

.pair-heart {
  color: var(--faded-red);
  font-size: 22px;
  animation: heartbeat 2s ease-in-out infinite;
}

.space-card__name {
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: var(--brown-deep);
  margin-bottom: 6px;
}

.space-card__days {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.space-card__days strong {
  font-family: var(--font-serif);
  font-size: 20px;
  color: var(--faded-red);
  margin: 0 4px;
}

.space-card__announce {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
}

.panel {
  background: var(--gradient-paper);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  padding: 24px 28px 28px;
  margin-bottom: 20px;
}

.panel__title {
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--brown-deep);
  margin-bottom: 18px;
  padding-bottom: 10px;
  border-bottom: 1px dashed rgba(var(--brown-rgb), 0.15);
}

.panel__hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 10px;
  line-height: 1.6;
}

.success-hint {
  color: var(--olive);
}

.invite-box {
  padding: 16px;
  background: rgba(var(--white-rgb), 0.45);
  border-radius: var(--radius-md);
  border: 1px dashed rgba(var(--brown-rgb), 0.2);
  margin-bottom: 16px;
}

.invite-box__label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.invite-box__code {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.invite-box__code span {
  font-family: var(--font-serif);
  font-size: 28px;
  letter-spacing: 6px;
  color: var(--brown-deep);
  font-weight: 700;
}

.bind-box {
  margin-top: 8px;
}

.bind-row {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.bind-row .input-vintage {
  flex: 1;
}

.input-error {
  border-color: var(--faded-red) !important;
}

.bind-error {
  margin-top: 10px;
  font-size: 13px;
  color: var(--faded-red);
  line-height: 1.5;
}

.full-tip {
  margin-top: 4px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.field-label {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

.panel__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

@media (max-width: 768px) {
  .panel {
    padding: 20px 16px 24px;
  }

  .invite-box__code {
    flex-direction: column;
    align-items: flex-start;
  }

  .invite-box__code span {
    font-size: 24px;
    letter-spacing: 4px;
  }

  .bind-row {
    flex-direction: column;
  }
}
</style>
