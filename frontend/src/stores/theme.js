import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('ql_theme') === 'dark')

  function applyTheme() {
    document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('ql_theme', isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  function init() {
    // 首次访问：跟随系统偏好
    if (!localStorage.getItem('ql_theme')) {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      isDark.value = prefersDark
    }
    applyTheme()
  }

  return { isDark, toggle, init }
})
