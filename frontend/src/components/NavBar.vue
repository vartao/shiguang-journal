<template>
  <nav class="navbar">
    <div class="navbar__inner container">
      <!-- Logo -->
      <router-link to="/" class="navbar__logo">
        <span class="navbar__logo-icon">&#x1F4F7;</span>
        <span class="navbar__logo-text">拾光手账</span>
      </router-link>

      <!-- 导航链接 - 桌面端 -->
      <div class="navbar__links" :class="{ open: menuOpen }">
        <router-link
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="navbar__link"
          :class="{ active: isActive(link.path) }"
          @click="menuOpen = false"
        >
          <span class="navbar__link-icon">{{ link.icon }}</span>
          <span class="navbar__link-text">{{ link.label }}</span>
        </router-link>
      </div>

      <!-- 右侧操作 -->
      <div class="navbar__actions">
        <!-- 暗色模式切换 -->
        <button
          class="navbar__theme-btn"
          :title="themeStore.isDark ? '切换亮色' : '切换暗色'"
          @click="themeStore.toggle()"
        >
          <span v-if="themeStore.isDark">&#9788;</span>
          <span v-else>&#9790;</span>
        </button>

        <router-link to="/write" class="navbar__write-btn">
          <span class="write-icon">&#9998;</span>
          <span class="write-text">写笔记</span>
        </router-link>

        <div class="navbar__user" @click="toggleUserMenu">
          <div class="navbar__avatar">
            <img v-if="userStore.avatar" :src="userStore.avatar" alt="头像" />
            <span v-else class="avatar-placeholder">{{ userStore.nickname.charAt(0) }}</span>
          </div>
          <span class="navbar__username">{{ userStore.nickname }}</span>

          <!-- 用户下拉菜单 -->
          <transition name="dropdown">
            <div v-if="userMenuOpen" class="navbar__dropdown">
              <div class="dropdown__header">
                <span class="dropdown__name">{{ userStore.nickname }}</span>
                <span v-if="coupleStore.hasCouple" class="dropdown__couple">
                  {{ coupleStore.coupleName }}
                </span>
              </div>
              <div class="dropdown__divider"></div>
              <router-link to="/space" class="dropdown__item" @click="userMenuOpen = false">
                <span>&#x1F3E0;</span> 我们的小窝
              </router-link>
              <button class="dropdown__item" @click="handleLogout"><span>&#x1F6AA;</span> 退出登录</button>
            </div>
          </transition>
        </div>

        <!-- 移动端菜单按钮 -->
        <button class="navbar__hamburger" @click="menuOpen = !menuOpen">
          <span :class="{ open: menuOpen }"></span>
        </button>
      </div>
    </div>

    <!-- 底部装饰线 - 木质纹理 -->
    <div class="navbar__wood-border"></div>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCoupleStore } from '@/stores/couple'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const userStore = useUserStore()
const coupleStore = useCoupleStore()
const themeStore = useThemeStore()

const menuOpen = ref(false)
const userMenuOpen = ref(false)

const navLinks = [
  { path: '/', label: '时间线', icon: '🎞️' },
  { path: '/albums', label: '相册墙', icon: '📸' },
  { path: '/anniversary', label: '纪念日', icon: '💑' },
  { path: '/surprise', label: '惊喜盒', icon: '🎁' },
  { path: '/wishes', label: '心愿池', icon: '✨' },
  { path: '/stats', label: '数据', icon: '📊' },
  { path: '/space', label: '小窝', icon: '🏠' }
]

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

function handleLogout() {
  userMenuOpen.value = false
  userStore.logout()
}

function handleClickOutside(e) {
  if (!e.target.closest('.navbar__user')) {
    userMenuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: var(--gradient-nav);
  border-bottom: 1px solid rgba(var(--brown-rgb), 0.15);
  box-shadow: 0 2px 16px rgba(var(--shadow-rgb), 0.08);
}

[data-theme='dark'] .navbar {
  border-bottom-color: rgba(180, 160, 130, 0.1);
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.2);
}

[data-theme='dark'] .navbar__dropdown {
  background: linear-gradient(160deg, #2a241e, #252018);
}

.navbar__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--nav-height);
}

/* Logo */
.navbar__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  transition: transform var(--transition-fast);
}

.navbar__logo:hover {
  transform: scale(1.03);
}

.navbar__logo-icon {
  font-size: 24px;
  filter: sepia(0.3);
}

.navbar__logo-text {
  font-family: var(--font-handwrite);
  font-size: 24px;
  color: var(--brown-deep);
  letter-spacing: 2px;
}

/* 导航链接 */
.navbar__links {
  display: flex;
  align-items: center;
  gap: 6px;
}

.navbar__link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 1px;
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  position: relative;
}

.navbar__link::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 60%;
  height: 2px;
  background: var(--faded-red);
  border-radius: 1px;
  transition: transform var(--transition-normal);
}

.navbar__link:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.12);
}

.navbar__link.active {
  color: var(--brown-deep);
  font-weight: 500;
}

.navbar__link.active::after {
  transform: translateX(-50%) scaleX(1);
}

.navbar__link-icon {
  font-size: 16px;
  filter: sepia(0.2);
}

/* 暗色模式切换按钮 */
.navbar__theme-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 18px;
  color: var(--text-secondary);
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.navbar__theme-btn:hover {
  background: rgba(var(--kraft-rgb), 0.12);
  color: var(--mustard);
  transform: rotate(15deg);
}

/* 右侧操作 */
.navbar__actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.navbar__write-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--faded-red), var(--faded-red-dark));
  border-radius: var(--radius-sm);
  text-decoration: none;
  box-shadow: 0 2px 8px rgba(var(--red-rgb), 0.3);
  transition: all var(--transition-fast);
}

.navbar__write-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(var(--red-rgb), 0.4);
  color: var(--cream);
}

.write-icon {
  font-size: 14px;
}

/* 用户区域 */
.navbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
  position: relative;
}

.navbar__user:hover {
  background: rgba(var(--kraft-rgb), 0.12);
}

.navbar__avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--kraft-paper);
  box-shadow: 0 1px 4px rgba(var(--shadow-rgb), 0.15);
}

.navbar__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: sepia(0.15);
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(145deg, var(--kraft-light), var(--kraft-paper));
  font-family: var(--font-handwrite);
  font-size: 16px;
  color: var(--brown-deep);
}

.navbar__username {
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 下拉菜单 */
.navbar__dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  min-width: 180px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lifted);
  padding: 12px;
  z-index: 100;
}

.dropdown__header {
  padding: 4px 8px 10px;
}

.dropdown__name {
  display: block;
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-primary);
}

.dropdown__couple {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.dropdown__divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(var(--brown-rgb), 0.2), transparent);
  margin: 6px 0;
}

.dropdown__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 8px;
  font-size: 14px;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  text-decoration: none;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  box-sizing: border-box;
}

.dropdown__item:hover {
  background: rgba(var(--red-rgb), 0.08);
  color: var(--faded-red);
}

/* 木质底边 */
.navbar__wood-border {
  height: 4px;
  background: linear-gradient(
    90deg,
    var(--kraft-dark) 0%,
    var(--kraft-paper) 15%,
    var(--kraft-light) 30%,
    var(--kraft-paper) 50%,
    var(--kraft-dark) 70%,
    var(--kraft-paper) 85%,
    var(--kraft-light) 100%
  );
  opacity: 0.6;
}

/* 汉堡菜单 */
.navbar__hamburger {
  display: none;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
}

.navbar__hamburger span,
.navbar__hamburger span::before,
.navbar__hamburger span::after {
  display: block;
  width: 20px;
  height: 2px;
  background: var(--brown-deep);
  border-radius: 1px;
  transition: all 0.3s ease;
  position: relative;
}

.navbar__hamburger span::before,
.navbar__hamburger span::after {
  content: '';
  position: absolute;
  left: 0;
}

.navbar__hamburger span::before {
  top: -6px;
}
.navbar__hamburger span::after {
  top: 6px;
}

.navbar__hamburger span.open {
  background: transparent;
}

.navbar__hamburger span.open::before {
  transform: rotate(45deg);
  top: 0;
}

.navbar__hamburger span.open::after {
  transform: rotate(-45deg);
  top: 0;
}

/* 下拉动画 */
.dropdown-enter-active {
  animation: modal-content-in 0.25s ease both;
}

.dropdown-leave-active {
  animation: modal-content-in 0.2s ease reverse both;
}

/* 响应式 */
@media (max-width: 768px) {
  .navbar__links {
    position: fixed;
    top: var(--nav-height);
    left: 0;
    right: 0;
    flex-direction: column;
    padding: 16px;
    background: var(--gradient-nav);
    border-bottom: 1px solid rgba(var(--brown-rgb), 0.15);
    box-shadow: 0 8px 24px rgba(var(--shadow-rgb), 0.12);
    transform: translateY(-100%);
    opacity: 0;
    pointer-events: none;
    transition: all 0.3s ease;
  }

  .navbar__links.open {
    transform: translateY(0);
    opacity: 1;
    pointer-events: all;
  }

  .navbar__link {
    padding: 12px 16px;
    width: 100%;
    justify-content: center;
  }

  .navbar__hamburger {
    display: flex;
  }

  .navbar__username {
    display: none;
  }

  .write-text {
    display: none;
  }

  .navbar__write-btn {
    padding: 8px 12px;
  }
}
</style>
