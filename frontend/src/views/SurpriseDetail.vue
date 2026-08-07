<template>
  <div class="surprise-detail-page">
    <!-- 爱心飘浮特效容器 -->
    <div ref="heartsContainer" class="hearts-container"></div>

    <!-- 烟花特效容器 -->
    <div ref="fireworksContainer" class="fireworks-container"></div>

    <div class="container">
      <!-- 加载 -->
      <div v-if="loading" class="loading-reel">
        <div class="loading-reel__icon"></div>
        <p class="loading-reel__text">正在取出惊喜...</p>
      </div>

      <template v-else>
        <!-- 未解锁状态 -->
        <div v-if="!isUnlocked && !unlocking" class="locked-state anim-fade-in-up">
          <div class="locked-envelope">
            <div class="envelope-visual anim-sway">
              <div class="envelope-body">
                <div class="envelope-flap"></div>
                <div class="envelope-front"></div>
                <div class="envelope-lock">
                  <span class="lock-icon">&#x1F512;</span>
                </div>
              </div>
            </div>

            <h2 class="locked-title">{{ surprise.title }}</h2>
            <p class="locked-hint">{{ lockedHintText }}</p>
            <p v-if="surprise.unlockDate && unlockType === 'date'" class="locked-date">
              解锁日期：{{ formatDate(surprise.unlockDate) }}
            </p>
            <p v-else-if="surprise.hint" class="locked-date">
              {{ surprise.hint }}
            </p>

            <!-- 密码解锁 -->
            <div v-if="unlockType === 'password'" class="password-unlock">
              <input
                v-model="unlockPassword"
                type="password"
                class="input-vintage password-input"
                placeholder="输入解锁密码"
                @keyup.enter="handleUnlock"
              />
              <p v-if="unlockError" class="password-error">{{ unlockError }}</p>
              <button
                class="btn-vintage btn-vintage--red unlock-btn"
                :disabled="!unlockPassword.trim() || unlocking"
                @click="handleUnlock"
              >
                &#x1F381; 用密码拆开
              </button>
            </div>

            <!-- 日期未到 -->
            <div v-else-if="unlockType === 'date' && daysUntilUnlock > 0" class="locked-waiting">
              <span class="waiting-dots"> <span>.</span><span>.</span><span>.</span> </span>
              <p>耐心等待，惊喜值得等待</p>
            </div>

            <!-- 日期到期 / 点击解锁 -->
            <button v-else class="btn-vintage btn-vintage--red unlock-btn" :disabled="unlocking" @click="handleUnlock">
              &#x1F381; 拆开惊喜
            </button>
          </div>
        </div>

        <!-- 拆信封动画 -->
        <div v-else-if="unlocking" class="unlocking-state">
          <div class="envelope-opening" :class="{ opened: envelopeOpened }">
            <div class="opening-envelope">
              <div class="opening-flap" :class="{ open: envelopeOpened }"></div>
              <div class="opening-body">
                <div class="opening-letter" :class="{ risen: envelopeOpened }">
                  <span class="letter-heart">&#x2764;</span>
                </div>
              </div>
            </div>
          </div>
          <p class="unlocking-text">{{ envelopeOpened ? '惊喜来啦！' : '正在拆开...' }}</p>
        </div>

        <!-- 已解锁 - 展示内容 -->
        <div v-else class="revealed-state">
          <div class="revealed-card anim-pop-in">
            <!-- 顶部装饰 -->
            <div class="revealed-card__deco">
              <span class="deco-sparkle">&#x2728;</span>
              <div class="deco-line"></div>
              <span class="deco-heart">&#x2764;</span>
              <div class="deco-line"></div>
              <span class="deco-sparkle">&#x2728;</span>
            </div>

            <!-- 标题 -->
            <h1 class="revealed-title">{{ surprise.title }}</h1>

            <!-- 来自 -->
            <p class="revealed-from">
              来自 {{ surprise.fromName || surprise.creatorNickname || '爱你的人' }}
              <span v-if="surprise.createdAt || surprise.createTime">
                · {{ formatDate(surprise.createdAt || surprise.createTime) }}</span
              >
            </p>

            <!-- 封面图 -->
            <div v-if="surprise.coverImage" class="revealed-cover">
              <img :src="surprise.coverImage" :alt="surprise.title" class="photo-vintage" />
            </div>

            <!-- 内容 - 信纸风格 -->
            <div class="revealed-content">
              <div class="letter-paper">
                <p class="letter-text">{{ surprise.content }}</p>
              </div>
            </div>

            <!-- 底部签名 -->
            <div class="revealed-signature">
              <span class="signature-heart">&#x2764;</span>
              <span class="signature-text">永远爱你的人</span>
            </div>

            <!-- 操作 -->
            <div class="revealed-actions">
              <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="triggerHearts">
                &#x2764; 发送爱心
              </button>
              <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="triggerFireworks">
                &#x1F386; 放烟花
              </button>
              <button
                v-if="isCreator"
                class="btn-vintage btn-vintage--outline btn-vintage--small danger-outline"
                :disabled="deleting"
                @click="handleDelete"
              >
                {{ deleting ? '撤回中...' : '撤回惊喜' }}
              </button>
              <router-link to="/surprise" class="btn-vintage btn-vintage--small"> 返回惊喜盒 </router-link>
            </div>
          </div>
        </div>

        <!-- 未解锁时创建者也可撤回 -->
        <div v-if="!loading && isCreator && !unlocking && !isUnlocked" class="creator-bar">
          <button class="creator-delete" :disabled="deleting" @click="handleDelete">
            {{ deleting ? '撤回中...' : '撤回这个惊喜' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { requestConfirm } from '@/composables/confirmService'
import { useRoute, useRouter } from 'vue-router'
import { getSurpriseDetail, unlockSurprise, deleteSurprise } from '@/api/surprise'
import { useUserStore } from '@/stores/user'
import { differenceInCalendarDays, parseLocalDate } from '@/utils/date'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const surpriseId = route.params.id

const surprise = ref({})
const loading = ref(true)
const unlocking = ref(false)
const envelopeOpened = ref(false)
const isUnlocked = ref(false)
const unlockPassword = ref('')
const unlockError = ref('')
const deleting = ref(false)

const heartsContainer = ref(null)
const fireworksContainer = ref(null)

const isCreator = computed(() => {
  const myId = userStore.userInfo?.id ?? userStore.userInfo?.userId
  if (myId == null || surprise.value?.userId == null) return false
  return String(surprise.value.userId) === String(myId)
})

const unlockType = computed(() => {
  return surprise.value.unlockCondition || (surprise.value.unlockDate ? 'date' : 'click')
})

const daysUntilUnlock = computed(() => {
  if (unlockType.value !== 'date' || !surprise.value.unlockDate) return 0
  const days = differenceInCalendarDays(surprise.value.unlockDate, new Date())
  return Number.isNaN(days) ? 0 : days
})

const lockedHintText = computed(() => {
  if (unlockType.value === 'password') {
    return '这是一个有密码的惊喜，猜对了才能拆开哦'
  }
  if (unlockType.value === 'click') {
    return '轻轻一点，惊喜就会出现'
  }
  if (daysUntilUnlock.value > 0) {
    return `这个惊喜还要等 ${daysUntilUnlock.value} 天才能拆开哦`
  }
  return '这个惊喜已经可以拆开了！'
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = parseLocalDate(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function isUnlockedValue(data) {
  return data?.unlocked === true || data?.unlocked === 1 || data?.isUnlocked === true
}

async function fetchSurprise() {
  loading.value = true
  try {
    const res = await getSurpriseDetail(surpriseId)
    surprise.value = res.data || {}
    isUnlocked.value = isUnlockedValue(res.data)
  } catch {
    surprise.value = {}
    isUnlocked.value = false
  } finally {
    loading.value = false
  }
}

async function handleDelete() {
  const confirmed = await requestConfirm({
    title: '撤回惊喜',
    message: '确定要撤回“' + (surprise.value.title || '这个惊喜') + '”吗？撤回后对方将看不到。',
    confirmText: '撤回'
  })
  if (!confirmed) return
  deleting.value = true
  try {
    await deleteSurprise(surpriseId)
    router.push('/surprise')
  } catch {
    // handled
  } finally {
    deleting.value = false
  }
}

async function handleUnlock() {
  unlockError.value = ''
  if (unlockType.value === 'password' && !unlockPassword.value.trim()) {
    unlockError.value = '请输入解锁密码'
    return
  }

  unlocking.value = true

  try {
    const password = unlockType.value === 'password' ? unlockPassword.value.trim() : undefined
    const res = await unlockSurprise(surpriseId, password)
    if (res?.data) {
      surprise.value = { ...surprise.value, ...res.data }
    }
  } catch (err) {
    unlocking.value = false
    unlockError.value = err?.message || '解锁失败，请再试试'
    return
  }

  // 播放拆信封动画
  setTimeout(() => {
    envelopeOpened.value = true
    triggerHearts()

    setTimeout(() => {
      triggerFireworks()
    }, 800)

    setTimeout(() => {
      unlocking.value = false
      isUnlocked.value = true
      surprise.value.unlocked = 1
    }, 2000)
  }, 600)
}

// 爱心飘浮特效
function triggerHearts() {
  const container = heartsContainer.value
  if (!container) return

  const heartChars = ['❤', '💕', '💗', '💖', '💓', '♥']
  const count = 20

  for (let i = 0; i < count; i++) {
    setTimeout(() => {
      const heart = document.createElement('span')
      heart.className = 'floating-heart'
      heart.textContent = heartChars[Math.floor(Math.random() * heartChars.length)]
      heart.style.left = Math.random() * 100 + 'vw'
      heart.style.setProperty('--size', 14 + Math.random() * 20 + 'px')
      heart.style.setProperty('--sway', Math.random() * 60 - 30 + 'px')
      heart.style.setProperty('--duration', 3 + Math.random() * 3 + 's')
      container.appendChild(heart)

      setTimeout(() => heart.remove(), 6000)
    }, i * 150)
  }
}

// 烟花特效
function triggerFireworks() {
  const container = fireworksContainer.value
  if (!container) return

  const colors = ['#c1666b', '#d4a574', '#c9a227', '#7d8c5c', '#7a9bb5', '#d98e8e']

  for (let burst = 0; burst < 5; burst++) {
    setTimeout(() => {
      const x = 20 + Math.random() * 60
      const y = 15 + Math.random() * 40

      for (let i = 0; i < 12; i++) {
        const particle = document.createElement('div')
        particle.className = 'firework-particle'
        const angle = (i / 12) * Math.PI * 2
        const distance = 50 + Math.random() * 60
        const fx = Math.cos(angle) * distance
        const fy = Math.sin(angle) * distance

        particle.style.left = x + 'vw'
        particle.style.top = y + 'vh'
        particle.style.setProperty('--fx', fx + 'px')
        particle.style.setProperty('--fy', fy + 'px')
        particle.style.background = colors[Math.floor(Math.random() * colors.length)]
        particle.style.animationDelay = Math.random() * 0.2 + 's'

        container.appendChild(particle)
        setTimeout(() => particle.remove(), 1500)
      }
    }, burst * 400)
  }
}

onMounted(() => {
  fetchSurprise()
})
</script>

<style scoped>
.surprise-detail-page {
  min-height: calc(100vh - var(--nav-height));
  padding: 60px 0 80px;
  position: relative;
  overflow: hidden;
}

/* 特效容器 */
.hearts-container,
.fireworks-container {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9990;
}

/* 未解锁状态 */
.locked-state {
  display: flex;
  justify-content: center;
  padding-top: 40px;
}

.locked-envelope {
  text-align: center;
  max-width: 400px;
}

.envelope-visual {
  margin-bottom: 32px;
}

.envelope-body {
  position: relative;
  width: 180px;
  height: 120px;
  margin: 0 auto;
}

.envelope-body .envelope-flap {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-left: 90px solid transparent;
  border-right: 90px solid transparent;
  border-top: 60px solid var(--envelope-flap);
  z-index: 2;
}

.envelope-body .envelope-front {
  position: absolute;
  inset: 0;
  background: var(--envelope-back);
  border-radius: 4px;
  border: 1px solid rgba(var(--brown-rgb), 0.2);
  box-shadow: var(--shadow-card);
}

.envelope-lock {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--envelope-seal);
  border-radius: 50%;
  z-index: 3;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.3);
}

.lock-icon {
  font-size: 18px;
}

.locked-title {
  font-family: var(--font-handwrite);
  font-size: 28px;
  color: var(--brown-deep);
  margin-bottom: 12px;
}

.locked-hint {
  font-size: 15px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.locked-hint strong {
  color: var(--faded-red);
  font-size: 20px;
}

.locked-date {
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.unlock-btn {
  font-size: 16px;
  padding: 14px 36px;
  letter-spacing: 2px;
}

.password-unlock {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.password-input {
  width: 100%;
  max-width: 280px;
  text-align: center;
}

.password-error {
  font-size: 13px;
  color: var(--faded-red);
  margin: 0;
}

.locked-waiting {
  color: var(--text-muted);
  font-size: 14px;
}

.waiting-dots span {
  animation: text-fade 1.5s ease-in-out infinite;
  font-size: 24px;
}

.waiting-dots span:nth-child(2) {
  animation-delay: 0.3s;
}
.waiting-dots span:nth-child(3) {
  animation-delay: 0.6s;
}

/* 拆信封动画 */
.unlocking-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
}

.opening-envelope {
  position: relative;
  width: 220px;
  height: 150px;
  perspective: 800px;
}

.opening-flap {
  position: absolute;
  top: 0;
  left: 0;
  width: 0;
  height: 0;
  border-left: 110px solid transparent;
  border-right: 110px solid transparent;
  border-top: 75px solid var(--envelope-flap);
  transform-origin: top center;
  transition: transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 3;
}

.opening-flap.open {
  transform: rotateX(180deg);
}

.opening-body {
  position: absolute;
  inset: 0;
  background: var(--envelope-back);
  border-radius: 4px;
  border: 1px solid rgba(var(--brown-rgb), 0.2);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.opening-letter {
  position: absolute;
  top: 20px;
  left: 20px;
  right: 20px;
  bottom: 20px;
  background: var(--letter-paper);
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.opening-letter.risen {
  transform: translateY(-80px) scale(1.1);
}

.letter-heart {
  font-size: 32px;
  color: var(--faded-red);
  animation: heartbeat 1s ease-in-out infinite;
}

.unlocking-text {
  margin-top: 40px;
  font-family: var(--font-handwrite);
  font-size: 22px;
  color: var(--text-secondary);
  animation: text-fade 1.5s ease-in-out infinite;
}

/* 已解锁展示 */
.revealed-state {
  display: flex;
  justify-content: center;
}

.revealed-card {
  width: 100%;
  max-width: 640px;
  background: var(--gradient-paper);
  border: 1px solid rgba(var(--brown-rgb), 0.12);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lifted);
  padding: 48px 44px;
  position: relative;
  overflow: hidden;
}

/* 信纸纹理 */
.revealed-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    transparent,
    transparent 31px,
    rgba(var(--brown-rgb), 0.025) 31px,
    rgba(var(--brown-rgb), 0.025) 32px
  );
  pointer-events: none;
}

.revealed-card__deco {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.deco-sparkle {
  font-size: 16px;
  animation: twinkle 2s ease-in-out infinite;
}

.deco-heart {
  color: var(--faded-red);
  font-size: 16px;
  animation: heartbeat 2s ease-in-out infinite;
}

.deco-line {
  width: 50px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--kraft-paper), transparent);
}

.revealed-title {
  font-family: var(--font-handwrite);
  font-size: 32px;
  color: var(--brown-deep);
  text-align: center;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
}

.revealed-from {
  text-align: center;
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 1px;
  margin-bottom: 28px;
}

.revealed-cover {
  margin-bottom: 28px;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-soft);
}

.revealed-cover img {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
}

/* 信纸内容 */
.revealed-content {
  position: relative;
  z-index: 1;
  margin-bottom: 32px;
}

.letter-paper {
  padding: 24px;
  background: rgba(var(--letter-paper-rgb), 0.7);
  border: 1px solid rgba(var(--brown-rgb), 0.08);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-inset);
}

.letter-text {
  font-family: var(--font-serif);
  font-size: 16px;
  line-height: 2.2;
  color: var(--text-primary);
  white-space: pre-wrap;
}

/* 签名 */
.revealed-signature {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 32px;
}

.signature-heart {
  color: var(--faded-red);
  font-size: 16px;
}

.signature-text {
  font-family: var(--font-handwrite);
  font-size: 20px;
  color: var(--text-secondary);
}

/* 操作 */
.revealed-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 20px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.15);
}

.danger-outline:hover {
  color: var(--faded-red) !important;
  border-color: var(--faded-red) !important;
}

.creator-bar {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

.creator-delete {
  padding: 8px 16px;
  font-family: var(--font-display);
  font-size: 12px;
  color: var(--text-muted);
  background: transparent;
  border: 1px dashed rgba(var(--brown-rgb), 0.25);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.creator-delete:hover:not(:disabled) {
  color: var(--faded-red);
  border-color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.06);
}

.creator-delete:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .surprise-detail-page {
    padding: 40px 0 60px;
  }

  .locked-state {
    padding-top: 24px;
  }

  .locked-title {
    font-size: 24px;
    overflow-wrap: break-word;
  }

  .password-input {
    font-size: 16px;
  }

  .unlocking-text {
    font-size: 20px;
  }

  .revealed-card {
    padding: 32px 20px;
  }

  .revealed-title {
    font-size: 26px;
    overflow-wrap: break-word;
  }

  .letter-paper {
    padding: 20px 16px;
  }

  .letter-text {
    font-size: 15px;
    line-height: 2;
    overflow-wrap: break-word;
  }

  .revealed-actions .btn-vintage {
    min-height: 40px;
  }

  .creator-delete {
    min-height: 40px;
    padding: 10px 18px;
  }
}

@media (max-width: 480px) {
  .surprise-detail-page {
    padding: 32px 0 48px;
  }

  .locked-title {
    font-size: 22px;
  }

  .locked-hint {
    font-size: 14px;
  }

  .password-input {
    max-width: 100%;
  }

  .unlock-btn {
    width: 100%;
    max-width: 320px;
    padding: 13px 20px;
    letter-spacing: 1px;
  }

  .unlocking-text {
    font-size: 18px;
  }

  .revealed-card {
    padding: 24px 16px;
  }

  .revealed-title {
    font-size: 22px;
  }

  .letter-paper {
    padding: 16px 14px;
  }

  .revealed-actions {
    flex-direction: column;
    gap: 10px;
  }

  .revealed-actions .btn-vintage {
    width: 100%;
  }
}
</style>
