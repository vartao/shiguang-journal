<template>
  <div class="login-page">
    <!-- 氛围层：暖光 + 胶片颗粒 + 暗角 -->
    <div class="login-atmosphere" aria-hidden="true">
      <div class="glow glow-a"></div>
      <div class="glow glow-b"></div>
      <div class="glow glow-c"></div>
      <div class="grain"></div>
      <div class="vignette"></div>
    </div>

    <div class="login-shell">
      <!-- 左侧品牌展板：桌面端展示，移动端折叠为顶部条 -->
      <aside class="login-showcase">
        <div class="showcase-inner">
          <div class="showcase-brand">
            <span class="brand-mark">🎞</span>
            <span class="brand-name">拾光手账</span>
          </div>

          <div class="showcase-hero">
            <p class="hero-kicker">for two hearts only</p>
            <h1 class="hero-title">
              把日子<br />
              冲成胶片
            </h1>
            <p class="hero-desc">
              写给彼此的时间线、相册墙、纪念日与惊喜盒子。<br />
              只属于你们两个人的私密小站。
            </p>
          </div>

          <!-- 散落的拍立得 -->
          <div class="polaroid-stack">
            <div class="polaroid p1">
              <div class="polaroid-photo ph-a"></div>
              <p class="polaroid-caption">第一次旅行</p>
            </div>
            <div class="polaroid p2">
              <div class="polaroid-photo ph-b"></div>
              <p class="polaroid-caption">雨天咖啡馆</p>
            </div>
            <div class="polaroid p3">
              <div class="polaroid-photo ph-c"></div>
              <p class="polaroid-caption">我们的日落</p>
            </div>
          </div>

          <div class="showcase-footer">
            <div class="film-sprocket"></div>
            <p class="showcase-quote">“最好的爱情，是两个人一起慢慢变好”</p>
          </div>
        </div>
      </aside>

      <!-- 右侧表单 -->
      <section class="login-panel">
        <div class="login-card anim-pop-in">
          <header class="login-card__header">
            <div class="header-mobile-brand">
              <span class="brand-mark">🎞</span>
              <span class="brand-name">拾光手账</span>
            </div>
            <p class="card-eyebrow">{{ mode === 'login' ? '欢迎回来' : '开启你们的手账' }}</p>
            <h2 class="card-title">
              {{ mode === 'login' ? '打开我们的手账' : '开始记录爱情' }}
            </h2>
            <p class="card-sub">
              {{ mode === 'login' ? '继续写下今天的心动' : '创建空间，或用邀请码加入 TA' }}
            </p>
          </header>

          <div class="login-tabs" role="tablist">
            <button
              type="button"
              class="login-tab"
              :class="{ active: mode === 'login' }"
              role="tab"
              :aria-selected="mode === 'login'"
              @click="mode = 'login'"
            >
              登录
            </button>
            <button
              type="button"
              class="login-tab"
              :class="{ active: mode === 'register' }"
              role="tab"
              :aria-selected="mode === 'register'"
              @click="mode = 'register'"
            >
              注册
            </button>
            <div class="login-tabs__indicator" :class="{ right: mode === 'register' }"></div>
          </div>

          <form class="login-form" @submit.prevent="handleSubmit">
            <div class="form-group">
              <label class="form-label" for="username">用户名</label>
              <input
                id="username"
                v-model="form.username"
                type="text"
                class="input-field"
                :class="{ error: errors.username }"
                placeholder="请输入用户名"
                autocomplete="username"
              />
              <span v-if="errors.username" class="form-error">{{ errors.username }}</span>
            </div>

            <div v-if="mode === 'register'" class="form-group">
              <label class="form-label" for="nickname">昵称</label>
              <input
                id="nickname"
                v-model="form.nickname"
                type="text"
                class="input-field"
                :class="{ error: errors.nickname }"
                placeholder="给自己取个可爱的名字"
              />
              <span v-if="errors.nickname" class="form-error">{{ errors.nickname }}</span>
            </div>

            <div v-if="mode === 'register'" class="form-group">
              <span class="form-label">我是</span>
              <div class="role-selector">
                <button
                  type="button"
                  class="role-btn"
                  :class="{ active: form.role === 'boy' }"
                  @click="form.role = 'boy'"
                >
                  <span class="role-icon">👦</span>
                  <span>男生</span>
                </button>
                <button
                  type="button"
                  class="role-btn"
                  :class="{ active: form.role === 'girl' }"
                  @click="form.role = 'girl'"
                >
                  <span class="role-icon">👧</span>
                  <span>女生</span>
                </button>
              </div>
            </div>

            <div class="form-group">
              <label class="form-label" for="password">密码</label>
              <input
                id="password"
                v-model="form.password"
                type="password"
                class="input-field"
                :class="{ error: errors.password }"
                placeholder="请输入密码"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              />
              <span v-if="errors.password" class="form-error">{{ errors.password }}</span>
            </div>

            <div v-if="mode === 'register'" class="form-group">
              <label class="form-label" for="confirmPassword">确认密码</label>
              <input
                id="confirmPassword"
                v-model="form.confirmPassword"
                type="password"
                class="input-field"
                :class="{ error: errors.confirmPassword }"
                placeholder="再输入一次密码"
                autocomplete="new-password"
              />
              <span v-if="errors.confirmPassword" class="form-error">{{ errors.confirmPassword }}</span>
            </div>

            <div v-if="mode === 'register'" class="form-group">
              <label class="form-label" for="inviteCode"> 邀请码 <span class="optional">选填</span> </label>
              <input
                id="inviteCode"
                v-model="form.inviteCode"
                type="text"
                class="input-field"
                :class="{ error: errors.inviteCode }"
                maxlength="12"
                placeholder="有 TA 的邀请码？填这里绑定"
                @input="errors.inviteCode = ''"
              />
              <p class="form-hint">不填会自动创建你们的小窝；填写后会加入 TA 的空间（最多 2 人）</p>
              <span v-if="errors.inviteCode" class="form-error">{{ errors.inviteCode }}</span>
            </div>

            <p v-if="formError" class="form-banner-error">{{ formError }}</p>

            <button type="submit" class="login-submit" :disabled="loading">
              <span v-if="loading" class="btn-loading"></span>
              <span v-else>{{
                mode === 'login' ? '进入手账' : form.inviteCode.trim() ? '加入 TA 的小窝' : '创建并进入'
              }}</span>
            </button>
          </form>

          <footer class="login-card__footer">
            <div class="footer-stamp" aria-hidden="true">✉</div>
            <p class="footer-note">
              {{ mode === 'login' ? '还没有账号？' : '已有账号？' }}
              <button type="button" class="footer-switch" @click="mode = mode === 'login' ? 'register' : 'login'">
                {{ mode === 'login' ? '去注册' : '去登录' }}
              </button>
            </p>
          </footer>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCoupleStore } from '@/stores/couple'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const coupleStore = useCoupleStore()

const mode = ref('login')
const loading = ref(false)
const formError = ref('')

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  role: 'boy',
  confirmPassword: '',
  inviteCode: ''
})

const errors = reactive({
  username: '',
  password: '',
  nickname: '',
  confirmPassword: '',
  inviteCode: ''
})

function friendlyBindMessage(msg) {
  if (!msg) return ''
  const text = String(msg)
  if (text.includes('满员') || text.includes('住满')) {
    return '这个小窝已经住满两人啦。可以让 TA 新建空间，或你不填邀请码自己建一个。'
  }
  if (text.includes('邀请码') && (text.includes('无效') || text.includes('不正确'))) {
    return '邀请码好像不对，请向 TA 再要一次（注意大小写和空格）。'
  }
  if (text.includes('已经有小窝') || text.includes('无法再次绑定') || text.includes('已在')) {
    return '你已经在一个小窝里了，不能再绑定别的邀请码。'
  }
  return text
}

function validate() {
  let valid = true
  errors.username = ''
  errors.password = ''
  errors.nickname = ''
  errors.confirmPassword = ''
  errors.inviteCode = ''
  formError.value = ''

  if (!form.username.trim()) {
    errors.username = '请输入用户名'
    valid = false
  }

  if (!form.password) {
    errors.password = '请输入密码'
    valid = false
  } else if (form.password.length < 6) {
    errors.password = '密码至少6位'
    valid = false
  }

  if (mode.value === 'register') {
    if (!form.nickname.trim()) {
      errors.nickname = '请输入昵称'
      valid = false
    }
    if (form.password !== form.confirmPassword) {
      errors.confirmPassword = '两次密码不一致'
      valid = false
    }
    if (form.inviteCode && form.inviteCode.trim().length > 0 && form.inviteCode.trim().length < 4) {
      errors.inviteCode = '邀请码长度不太对，一般是 6 位'
      valid = false
    }
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  formError.value = ''
  try {
    if (mode.value === 'login') {
      await userStore.login({
        username: form.username,
        password: form.password
      })
    } else {
      await userStore.register({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        role: form.role,
        inviteCode: form.inviteCode.trim() || undefined
      })
    }

    await coupleStore.fetchCoupleInfo()

    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (err) {
    const msg = friendlyBindMessage(err?.message || '')
    if (mode.value === 'register' && form.inviteCode.trim() && msg) {
      errors.inviteCode = msg
    } else if (msg) {
      formError.value = msg
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(ellipse 80% 60% at 15% 20%, rgba(var(--kraft-rgb), 0.22), transparent 55%),
    radial-gradient(ellipse 70% 50% at 85% 80%, rgba(var(--red-rgb), 0.14), transparent 50%),
    linear-gradient(155deg, #f7efe0 0%, #f3e4cf 45%, #efe0c8 100%);
}

/* ===== 氛围 ===== */
.login-atmosphere {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.55;
}

.glow-a {
  width: 420px;
  height: 420px;
  top: -80px;
  left: -60px;
  background: rgba(var(--kraft-rgb), 0.45);
}

.glow-b {
  width: 360px;
  height: 360px;
  bottom: -100px;
  right: 10%;
  background: rgba(var(--red-rgb), 0.28);
}

.glow-c {
  width: 280px;
  height: 280px;
  top: 40%;
  left: 35%;
  background: rgba(var(--mustard-rgb), 0.12);
}

.grain {
  position: absolute;
  inset: 0;
  opacity: 0.035;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
}

.vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 55%, rgba(var(--shadow-rgb), 0.14) 100%);
}

/* ===== 主布局：桌面左右分栏 ===== */
.login-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
}

/* ===== 左侧展示 ===== */
.login-showcase {
  position: relative;
  display: flex;
  align-items: stretch;
  padding: 40px 36px 40px 48px;
  color: rgb(var(--cream-static-rgb));
  background: linear-gradient(
    160deg,
    rgba(45, 30, 22, 0.92) 0%,
    rgba(74, 46, 34, 0.88) 55%,
    rgba(92, 55, 42, 0.9) 100%
  );
  overflow: hidden;
  isolation: isolate;
}

.login-showcase::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 25%, rgba(var(--kraft-rgb), 0.22), transparent 40%),
    radial-gradient(circle at 80% 70%, rgba(var(--red-rgb), 0.18), transparent 45%);
  z-index: 0;
}

.login-showcase::after {
  /* 胶片孔装饰 */
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
  width: 28px;
  background: repeating-linear-gradient(
    180deg,
    transparent 0 14px,
    rgba(var(--cream-static-rgb), 0.12) 14px 22px,
    transparent 22px 36px
  );
  border-left: 1px solid rgba(var(--cream-static-rgb), 0.08);
  z-index: 1;
}

.showcase-inner {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 520px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.showcase-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 48px;
}

.brand-mark {
  font-size: 22px;
  filter: sepia(0.25);
}

.brand-name {
  font-family: var(--font-handwrite);
  font-size: 28px;
  letter-spacing: 3px;
  color: #f7efe0;
}

.showcase-hero {
  margin-bottom: 36px;
}

.hero-kicker {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(var(--cream-static-rgb), 0.55);
  margin-bottom: 16px;
}

.hero-title {
  font-family: var(--font-handwrite);
  font-size: clamp(42px, 4.6vw, 64px);
  line-height: 1.15;
  letter-spacing: 4px;
  color: rgb(var(--cream-static-rgb));
  margin-bottom: 18px;
  text-shadow: 0 8px 30px rgba(0, 0, 0, 0.25);
}

.hero-desc {
  font-size: 15px;
  line-height: 1.9;
  color: rgba(var(--cream-static-rgb), 0.72);
  max-width: 36em;
}

/* 拍立得堆叠 */
.polaroid-stack {
  position: relative;
  height: 240px;
  margin: 12px 0 28px;
  flex: 1;
}

.polaroid {
  position: absolute;
  width: 148px;
  background: var(--cream);
  padding: 10px 10px 28px;
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.28),
    0 2px 6px rgba(0, 0, 0, 0.12);
  border-radius: 2px;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}

.polaroid:hover {
  z-index: 5;
  transform: rotate(0deg) translateY(-8px) scale(1.04) !important;
}

.polaroid-photo {
  width: 100%;
  height: 120px;
  border-radius: 1px;
}

.ph-a {
  background:
    linear-gradient(145deg, rgba(var(--red-rgb), 0.35), transparent 60%),
    linear-gradient(210deg, #c9a27a, var(--brown-medium) 70%, var(--brown-deep));
}

.ph-b {
  background:
    linear-gradient(160deg, rgba(var(--blue-rgb), 0.4), transparent 55%),
    linear-gradient(200deg, var(--olive-light), #6b7a4e 65%, #3d4a2a);
}

.ph-c {
  background:
    linear-gradient(180deg, rgba(var(--mustard-rgb), 0.45), transparent 50%),
    linear-gradient(220deg, #e0a878, var(--faded-red) 70%, #7a3f44);
}

.polaroid-caption {
  margin-top: 10px;
  font-family: var(--font-handwrite);
  font-size: 15px;
  color: var(--brown-deep);
  text-align: center;
  letter-spacing: 1px;
}

.p1 {
  left: 8%;
  top: 20px;
  transform: rotate(-8deg);
  z-index: 2;
}

.p2 {
  left: 34%;
  top: 0;
  transform: rotate(3deg);
  z-index: 3;
}

.p3 {
  left: 56%;
  top: 36px;
  transform: rotate(10deg);
  z-index: 1;
}

.showcase-footer {
  margin-top: auto;
  padding-top: 16px;
}

.film-sprocket {
  width: 160px;
  height: 12px;
  margin-bottom: 14px;
  opacity: 0.35;
  background: repeating-linear-gradient(90deg, rgb(var(--cream-static-rgb)) 0 10px, transparent 10px 14px);
  border-radius: 2px;
  position: relative;
}

.film-sprocket::before,
.film-sprocket::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  background: rgb(var(--cream-static-rgb));
  opacity: 0.6;
}

.film-sprocket::before {
  top: -4px;
}
.film-sprocket::after {
  bottom: -4px;
}

.showcase-quote {
  font-family: var(--font-handwrite);
  font-size: 18px;
  color: rgba(var(--cream-static-rgb), 0.7);
  letter-spacing: 1px;
}

/* ===== 右侧表单区 ===== */
.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: linear-gradient(175deg, rgba(253, 249, 240, 0.97), rgba(248, 240, 222, 0.98));
  border: 1px solid rgba(var(--brown-rgb), 0.12);
  border-radius: 16px;
  box-shadow:
    0 20px 50px rgba(var(--shadow-rgb), 0.12),
    0 4px 12px rgba(var(--shadow-rgb), 0.06),
    inset 0 1px 0 rgba(var(--white-rgb), 0.7);
  padding: 36px 36px 28px;
  position: relative;
  overflow: hidden;
}

/* 信纸横线 */
.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    transparent,
    transparent 31px,
    rgba(var(--brown-rgb), 0.035) 31px,
    rgba(var(--brown-rgb), 0.035) 32px
  );
  pointer-events: none;
}

/* 左侧装订线 */
.login-card::after {
  content: '';
  position: absolute;
  top: 18px;
  bottom: 18px;
  left: 18px;
  width: 1.5px;
  background: rgba(var(--red-rgb), 0.28);
  pointer-events: none;
}

.login-card > * {
  position: relative;
  z-index: 1;
}

.header-mobile-brand {
  display: none;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: var(--brown-deep);
}

.header-mobile-brand .brand-name {
  color: var(--brown-deep);
  font-size: 24px;
}

.card-eyebrow {
  font-family: var(--font-display);
  font-size: 12px;
  letter-spacing: 0.2em;
  color: var(--faded-red);
  margin-bottom: 8px;
}

.card-title {
  font-family: var(--font-handwrite);
  font-size: 32px;
  color: var(--brown-deep);
  letter-spacing: 2px;
  margin-bottom: 6px;
  line-height: 1.25;
}

.card-sub {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 22px;
  letter-spacing: 0.5px;
}

/* tabs */
.login-tabs {
  display: flex;
  position: relative;
  margin-bottom: 22px;
  border-bottom: 1.5px solid rgba(var(--brown-rgb), 0.1);
}

.login-tab {
  flex: 1;
  padding: 12px 8px;
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 2px;
  color: var(--text-muted);
  transition: color 0.2s ease;
}

.login-tab.active {
  color: var(--brown-deep);
}

.login-tabs__indicator {
  position: absolute;
  bottom: -1.5px;
  left: 0;
  width: 50%;
  height: 2.5px;
  background: linear-gradient(90deg, var(--faded-red), var(--faded-rose));
  border-radius: 2px;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 12px rgba(var(--red-rgb), 0.35);
}

.login-tabs__indicator.right {
  transform: translateX(100%);
}

/* form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-label {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 1px;
}

.optional {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0;
  margin-left: 4px;
}

.input-field {
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  color: var(--text-primary);
  background: rgba(var(--surface-raised-rgb), 0.72);
  border: 1.5px solid rgba(var(--brown-rgb), 0.16);
  border-radius: 8px;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.input-field::placeholder {
  color: rgba(var(--muted-rgb), 0.75);
}

.input-field:hover {
  border-color: rgba(var(--kraft-dark-rgb), 0.45);
}

.input-field:focus {
  border-color: var(--faded-red);
  background: rgba(var(--surface-raised-rgb), 0.9);
  box-shadow: 0 0 0 3px rgba(var(--red-rgb), 0.12);
}

.input-field.error {
  border-color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.04);
}

.form-error {
  font-size: 12px;
  color: var(--faded-red);
  line-height: 1.5;
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
}

.form-banner-error {
  margin: 0 0 4px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--faded-red-dark);
  background: rgba(var(--red-rgb), 0.08);
  border: 1px solid rgba(var(--red-rgb), 0.22);
  border-radius: 8px;
}

.role-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.role-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-secondary);
  background: rgba(var(--surface-raised-rgb), 0.68);
  border: 1.5px solid rgba(var(--brown-rgb), 0.16);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.role-btn:hover {
  border-color: var(--kraft-paper);
}

.role-btn.active {
  border-color: var(--faded-red);
  color: var(--faded-red-dark);
  background: rgba(var(--red-rgb), 0.08);
  box-shadow: 0 0 0 3px rgba(var(--red-rgb), 0.1);
}

.role-icon {
  font-size: 18px;
}

.login-submit {
  margin-top: 8px;
  width: 100%;
  padding: 14px 20px;
  font-family: var(--font-display);
  font-size: 16px;
  letter-spacing: 3px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--faded-red), var(--faded-red-dark));
  border: 1px solid rgba(var(--cream-rgb), 0.12);
  border-radius: 10px;
  box-shadow:
    0 8px 20px rgba(var(--red-dark-rgb), 0.28),
    inset 0 1px 0 rgba(var(--white-rgb), 0.18);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease;
}

.login-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow:
    0 12px 26px rgba(var(--red-dark-rgb), 0.35),
    inset 0 1px 0 rgba(var(--white-rgb), 0.2);
  filter: brightness(1.03);
}

.login-submit:active:not(:disabled) {
  transform: translateY(0);
}

.login-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-loading {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(var(--cream-rgb), 0.3);
  border-top-color: var(--cream);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.login-card__footer {
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px dashed rgba(var(--brown-rgb), 0.12);
  text-align: center;
}

.footer-stamp {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-bottom: 8px;
  border: 2px dashed rgba(var(--red-rgb), 0.35);
  border-radius: 4px;
  transform: rotate(-6deg);
  opacity: 0.75;
  font-size: 16px;
}

.footer-note {
  font-size: 13px;
  color: var(--text-muted);
}

.footer-switch {
  color: var(--faded-red);
  font-family: var(--font-display);
  letter-spacing: 1px;
  margin-left: 4px;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.footer-switch:hover {
  color: var(--faded-red-dark);
}

/* ===== 响应式 ===== */
@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-showcase {
    display: none;
  }

  .header-mobile-brand {
    display: flex;
  }

  .login-panel {
    padding: 28px 18px 40px;
    min-height: 100vh;
  }

  .login-card {
    padding: 28px 22px 24px;
    max-width: 440px;
  }

  .login-card::after {
    left: 12px;
  }

  .card-title {
    font-size: 28px;
  }
}

/* 平板 / 手机（展板已在 960px 断点隐藏，此处收敛表单卡片） */
@media (max-width: 768px) {
  .login-panel {
    padding: 24px 16px 32px;
  }

  .login-card {
    padding: 24px 18px 22px;
  }

  .card-title {
    font-size: 26px;
  }

  /* 避免 iOS 聚焦输入框时自动放大页面 */
  .input-field {
    font-size: 16px;
  }

  .role-btn {
    min-height: 44px;
  }

  .login-submit {
    min-height: 48px;
  }
}

/* 小屏手机 */
@media (max-width: 480px) {
  .login-panel {
    padding: 16px 12px 28px;
  }

  .login-card {
    padding: 20px 14px 18px;
    border-radius: 12px;
  }

  .login-card::after {
    left: 8px;
  }

  .header-mobile-brand {
    margin-bottom: 14px;
  }

  .header-mobile-brand .brand-name {
    font-size: 22px;
  }

  .card-title {
    font-size: 24px;
  }

  .card-sub {
    margin-bottom: 18px;
  }

  .login-tabs {
    margin-bottom: 18px;
  }

  .login-form {
    gap: 14px;
  }

  /* 角色选择按钮纵向排列，保证触控目标宽度 */
  .role-selector {
    grid-template-columns: 1fr;
  }

  .login-submit {
    letter-spacing: 2px;
  }
}

@media (min-width: 1400px) {
  .login-card {
    max-width: 440px;
    padding: 40px 40px 32px;
  }

  .hero-title {
    font-size: 68px;
  }
}
</style>
