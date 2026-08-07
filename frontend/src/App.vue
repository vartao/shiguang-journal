<template>
  <div id="app-root" :class="{ 'no-nav': isLoginPage }">
    <!-- 胶片颗粒感覆盖层 -->
    <div class="film-grain-overlay"></div>
    <!-- 暗角效果 -->
    <div class="vignette-overlay"></div>

    <NavBar v-if="!isLoginPage" />

    <main class="app-main" :class="{ 'login-main': isLoginPage }">
      <router-view v-slot="{ Component, route }">
        <transition name="page-turn" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>

    <ConfirmDialog />

    <footer v-if="!isLoginPage" class="app-footer">
      <div class="footer-film-strip"></div>
      <p class="footer-text">
        <span class="footer-heart">&hearts;</span>
        拾光手账 · 把日子过成胶片里的诗
        <span class="footer-heart">&hearts;</span>
      </p>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

const currentRoute = useRoute()
const isLoginPage = computed(() => currentRoute.name === 'Login')
</script>

<style scoped>
#app-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  padding-top: 76px;
}

.login-main {
  padding-top: 0;
}

/* 胶片颗粒覆盖层 */
.film-grain-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 9999;
  opacity: 0.04;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E");
  animation: grain-shift 0.8s steps(4) infinite;
}

/* 暗角效果 */
.vignette-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 9998;
  background: radial-gradient(ellipse at center, transparent 60%, rgba(var(--shadow-rgb), 0.12) 100%);
}

.app-footer {
  position: relative;
  padding: 28px 20px 36px;
  text-align: center;
  background: linear-gradient(to bottom, transparent, rgba(var(--brown-rgb), 0.06));
}

.footer-film-strip {
  width: 180px;
  height: 14px;
  margin: 0 auto 16px;
  background: repeating-linear-gradient(
    90deg,
    var(--film-black) 0px,
    var(--film-black) 10px,
    transparent 10px,
    transparent 14px
  );
  border-radius: 2px;
  opacity: 0.3;
  position: relative;
}

.footer-film-strip::before,
.footer-film-strip::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--film-black);
  opacity: 0.5;
  border-radius: 1px;
}

.footer-film-strip::before {
  top: -5px;
}
.footer-film-strip::after {
  bottom: -5px;
}

.footer-text {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--brown-light);
  letter-spacing: 2px;
}

.footer-heart {
  color: var(--faded-red);
  margin: 0 8px;
  display: inline-block;
  animation: heartbeat 2s ease-in-out infinite;
}

/* 页面切换动画 - 像翻相册一样 */
.page-turn-enter-active {
  animation: page-flip-in 0.45s ease-out;
}

.page-turn-leave-active {
  animation: page-flip-out 0.3s ease-in;
}

@keyframes page-flip-in {
  from {
    opacity: 0;
    transform: translateY(16px) rotateX(4deg);
  }
  to {
    opacity: 1;
    transform: translateY(0) rotateX(0);
  }
}

@keyframes page-flip-out {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-10px);
  }
}

@keyframes grain-shift {
  0% {
    transform: translate(0, 0);
  }
  25% {
    transform: translate(-2px, 2px);
  }
  50% {
    transform: translate(2px, -1px);
  }
  75% {
    transform: translate(-1px, -2px);
  }
  100% {
    transform: translate(0, 0);
  }
}

@keyframes heartbeat {
  0%,
  100% {
    transform: scale(1);
  }
  15% {
    transform: scale(1.2);
  }
  30% {
    transform: scale(1);
  }
  45% {
    transform: scale(1.15);
  }
  60% {
    transform: scale(1);
  }
}

/* 暗色模式 */
[data-theme='dark'] .film-grain-overlay {
  opacity: 0.02;
}

[data-theme='dark'] .vignette-overlay {
  background: radial-gradient(ellipse at center, transparent 60%, rgba(0, 0, 0, 0.2) 100%);
}

[data-theme='dark'] .app-footer {
  background: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.1));
}
</style>
