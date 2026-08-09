<template>
  <div class="menu-page">
    <div class="container">
      <header class="menu-header anim-fade-in-up">
        <h1 class="menu-header__title">今天吃什么</h1>
        <p class="menu-header__subtitle">把每一餐，都过成小日子</p>
        <div class="menu-header__deco">
          <span class="star s1">✦</span><span class="star s2">✧</span><span class="star s3">✦</span>
        </div>
      </header>

      <LoadingState v-if="loading" message="正在准备菜单…" />
      <ErrorState v-else-if="loadError" message="菜单暂时没有加载成功。" @retry="fetchAll" />
      <template v-else>
        <!-- 随机选菜 -->
        <section class="random-hero anim-fade-in-up stagger-2">
          <div class="category-chips">
            <button
              v-for="chip in categoryChips"
              :key="chip.value"
              class="category-chip"
              :class="{ active: activeCategory === chip.value }"
              @click="activeCategory = chip.value"
            >
              {{ chip.icon }} {{ chip.label }}
            </button>
          </div>

          <div class="random-stage" :class="{ 'random-stage--rolling': rolling }">
            <template v-if="rollResult">
              <div class="result-card">
                <p class="result-card__eyebrow">今天就吃它</p>
                <h2 class="result-card__name">{{ rollResult.name }}</h2>
                <p class="result-card__meta">
                  <span class="result-card__category">{{ categoryLabel(rollResult.category) }}</span>
                  <span v-if="rollResult.note" class="result-card__note">{{ rollResult.note }}</span>
                </p>
                <div class="result-card__meal-picker">
                  <button
                    v-for="meal in meals"
                    :key="meal.value"
                    class="category-chip"
                    :class="{ active: resultMealType === meal.value }"
                    @click="resultMealType = meal.value"
                  >
                    {{ meal.icon }} {{ meal.label }}
                  </button>
                </div>
                <div class="result-card__actions">
                  <button
                    class="btn-vintage btn-vintage--red btn-vintage--small"
                    :disabled="addingResult"
                    @click="addResultToMenu"
                  >
                    {{ addingResult ? '加入中…' : '加入菜单' }}
                  </button>
                  <button
                    class="btn-vintage btn-vintage--outline btn-vintage--small"
                    :disabled="rolling"
                    @click="handleRandom"
                  >
                    再抽一次
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="random-stage__name">{{ rolling ? rollingName || '…' : '?' }}</div>
              <p class="random-stage__hint">{{ rolling ? '翻翻找找中…' : '不知道吃什么？交给我们吧' }}</p>
            </template>
          </div>

          <button class="random-btn" :disabled="rolling" @click="handleRandom">
            <span class="random-btn__icon">🎲</span><span>{{ rolling ? '挑选中…' : '帮我决定' }}</span>
          </button>
        </section>

        <!-- 今日菜单 -->
        <section class="today-menu anim-fade-in-up stagger-3">
          <div class="today-menu__bar">
            <h2 class="section-title">今日菜单</h2>
            <div class="date-nav">
              <button class="date-nav__arrow" title="前一天" @click="shiftDay(-1)">‹</button>
              <span class="date-nav__label">{{ dateLabel }}</span>
              <button class="date-nav__arrow" title="后一天" @click="shiftDay(1)">›</button>
            </div>
          </div>

          <div class="meal-grid">
            <div v-for="meal in meals" :key="meal.value" class="meal-card">
              <h3 class="meal-card__title">{{ meal.icon }} {{ meal.label }}</h3>
              <ul v-if="menuByMeal[meal.value].length" class="meal-card__list">
                <li v-for="item in menuByMeal[meal.value]" :key="item.id" class="meal-item">
                  <span class="meal-item__name">{{ item.dishName }}</span>
                  <button class="meal-item__delete" title="移除" @click="removeMenuItem(item)">×</button>
                </li>
              </ul>
              <p v-else class="meal-card__empty">还没点菜</p>

              <div v-if="addingMeal === meal.value" class="meal-add">
                <select class="input-vintage meal-add__select" @change="onPoolPick">
                  <option value="">从菜品池选一道…</option>
                  <option v-for="dish in dishes" :key="dish.id" :value="dish.name">{{ dish.name }}</option>
                </select>
                <input
                  v-model="newDishName"
                  class="input-vintage"
                  placeholder="或手填菜名"
                  maxlength="100"
                  @keyup.enter="submitNewItem(meal.value)"
                />
                <div class="meal-add__actions">
                  <button class="btn-vintage btn-vintage--outline btn-vintage--small" @click="addingMeal = ''">
                    取消
                  </button>
                  <button
                    class="btn-vintage btn-vintage--red btn-vintage--small"
                    :disabled="!newDishName.trim() || addingItem"
                    @click="submitNewItem(meal.value)"
                  >
                    加入
                  </button>
                </div>
              </div>
              <button v-else class="meal-card__add" @click="startAdd(meal.value)">＋ 加一道</button>
            </div>
          </div>
        </section>

        <!-- 菜品池 -->
        <section class="dish-pool anim-fade-in-up stagger-4">
          <div class="dish-pool__bar">
            <h2 class="section-title">菜品池</h2>
            <button class="btn-vintage btn-vintage--red btn-vintage--small" @click="openDishModal">
              <span>🍳</span><span>添道菜</span>
            </button>
          </div>

          <template v-if="groupedDishes.length">
            <div v-for="group in groupedDishes" :key="group.value" class="dish-group">
              <h3 class="dish-group__title">{{ group.icon }} {{ group.label }}</h3>
              <div class="dish-group__items">
                <div v-for="dish in group.dishes" :key="dish.id" class="dish-chip">
                  <span class="dish-chip__name">{{ dish.name }}</span>
                  <span v-if="dish.note" class="dish-chip__note">{{ dish.note }}</span>
                  <button class="dish-chip__delete" title="删除" @click="removeDish(dish)">×</button>
                </div>
              </div>
            </div>
          </template>
          <EmptyState v-else icon="🍲" title="菜品池还是空的" message="先添几道想吃的菜，随机选菜才转得起来">
            <button class="btn-vintage btn-vintage--red" @click="openDishModal">添第一道菜</button>
          </EmptyState>
        </section>
      </template>
    </div>

    <BaseModal v-model="showDishModal" title="添道菜" eyebrow="DISH POOL" :busy="submitting" @close="resetDishForm">
      <form class="dish-form" @submit.prevent="submitDish">
        <div class="form-group">
          <label for="dish-name">菜名</label>
          <input
            id="dish-name"
            v-model="dishForm.name"
            class="input-vintage"
            placeholder="想吃什么？"
            maxlength="100"
            required
          />
        </div>
        <div class="form-group">
          <label>分类</label>
          <div class="category-chips category-chips--form">
            <button
              v-for="cat in categories"
              :key="cat.value"
              type="button"
              class="category-chip"
              :class="{ active: dishForm.category === cat.value }"
              @click="dishForm.category = cat.value"
            >
              {{ cat.icon }} {{ cat.label }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label for="dish-note">备注</label>
          <textarea
            id="dish-note"
            v-model="dishForm.note"
            class="input-vintage"
            rows="2"
            placeholder="做法、店名、口味…"
            maxlength="500"
          ></textarea>
        </div>
      </form>
      <template #footer>
        <button type="button" class="btn-vintage btn-vintage--outline" :disabled="submitting" @click="closeDishModal">
          取消
        </button>
        <button
          type="button"
          class="btn-vintage btn-vintage--red"
          :disabled="!dishForm.name.trim() || submitting"
          @click="submitDish"
        >
          {{ submitting ? '保存中…' : '加入菜品池 🍳' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { addMenuItem, createDish, deleteDish, deleteMenuItem, getDishes, getMenu, getRandomDish } from '@/api/menu'
import BaseModal from '@/components/BaseModal.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorState from '@/components/ErrorState.vue'
import LoadingState from '@/components/LoadingState.vue'
import { requestConfirm } from '@/composables/confirmService'
import { showToast } from '@/utils/toast'

const categories = [
  { value: 'breakfast', label: '早餐', icon: '🍳' },
  { value: 'lunch', label: '午餐', icon: '🍚' },
  { value: 'dinner', label: '晚餐', icon: '🌙' },
  { value: 'night', label: '宵夜', icon: '🌃' },
  { value: 'snack', label: '甜点', icon: '🍰' },
  { value: 'drink', label: '饮品', icon: '🥤' }
]
const categoryChips = [{ value: '', label: '全部', icon: '✨' }, ...categories]
const meals = [
  { value: 'breakfast', label: '早餐', icon: '🍳' },
  { value: 'lunch', label: '午餐', icon: '☀️' },
  { value: 'dinner', label: '晚餐', icon: '🌙' }
]

const loading = ref(false)
const loadError = ref(false)
const dishes = ref([])
const menuItems = ref([])
const menuDate = ref(new Date())

const activeCategory = ref('')
const rolling = ref(false)
const rollingName = ref('')
const rollResult = ref(null)
const resultMealType = ref('lunch')
const addingResult = ref(false)

const addingMeal = ref('')
const newDishName = ref('')
const addingItem = ref(false)

const showDishModal = ref(false)
const submitting = ref(false)
const dishForm = ref(createDishForm())

let rollTimer = null

function createDishForm() {
  return { name: '', category: 'lunch', note: '' }
}

function categoryLabel(value) {
  const found = categories.find((cat) => cat.value === value)
  return found ? found.label : '其他'
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const dateLabel = computed(() => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const current = menuDate.value
  const target = new Date(current.getFullYear(), current.getMonth(), current.getDate())
  const diff = Math.round((target - today) / 86400000)
  const text = formatDate(current)
  if (diff === 0) return `今天 · ${text}`
  if (diff === -1) return `昨天 · ${text}`
  if (diff === 1) return `明天 · ${text}`
  return text
})

const poolNames = computed(() => {
  if (!activeCategory.value) return dishes.value.map((dish) => dish.name)
  return dishes.value.filter((dish) => dish.category === activeCategory.value).map((dish) => dish.name)
})

const menuByMeal = computed(() => {
  const grouped = { breakfast: [], lunch: [], dinner: [] }
  for (const item of menuItems.value) {
    if (grouped[item.mealType]) grouped[item.mealType].push(item)
  }
  return grouped
})

const groupedDishes = computed(() => {
  return categories
    .map((cat) => ({ ...cat, dishes: dishes.value.filter((dish) => dish.category === cat.value) }))
    .filter((group) => group.dishes.length > 0)
})

async function fetchAll() {
  loading.value = true
  loadError.value = false
  try {
    const [dishRes, menuRes] = await Promise.all([getDishes(), getMenu({ date: formatDate(menuDate.value) })])
    dishes.value = dishRes.data || []
    menuItems.value = menuRes.data || []
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function fetchMenu() {
  const res = await getMenu({ date: formatDate(menuDate.value) })
  menuItems.value = res.data || []
}

function shiftDay(delta) {
  const next = new Date(menuDate.value)
  next.setDate(next.getDate() + delta)
  menuDate.value = next
  fetchMenu().catch(() => {})
}

async function handleRandom() {
  if (rolling.value) return
  rollResult.value = null
  const names = poolNames.value
  const params = activeCategory.value ? { category: activeCategory.value } : {}
  const apiPromise = getRandomDish(params)
    .then((res) => res.data)
    .catch(() => null)

  if (names.length) {
    rolling.value = true
    rollTimer = setInterval(() => {
      rollingName.value = names[Math.floor(Math.random() * names.length)]
    }, 90)
    await new Promise((resolve) => setTimeout(resolve, 1200 + Math.random() * 600))
    clearInterval(rollTimer)
    rollTimer = null
    rolling.value = false
  }

  const dish = await apiPromise
  if (dish) {
    rollResult.value = dish
    rollingName.value = dish.name
  } else {
    rollingName.value = ''
  }
}

async function addResultToMenu() {
  if (!rollResult.value || addingResult.value) return
  addingResult.value = true
  try {
    await addMenuItem({
      mealDate: formatDate(menuDate.value),
      mealType: resultMealType.value,
      dishName: rollResult.value.name
    })
    showToast('已加入菜单')
    await fetchMenu()
    rollResult.value = null
  } finally {
    addingResult.value = false
  }
}

function startAdd(mealType) {
  addingMeal.value = mealType
  newDishName.value = ''
}

function onPoolPick(event) {
  if (event.target.value) newDishName.value = event.target.value
}

async function submitNewItem(mealType) {
  const name = newDishName.value.trim()
  if (!name || addingItem.value) return
  addingItem.value = true
  try {
    await addMenuItem({ mealDate: formatDate(menuDate.value), mealType, dishName: name })
    addingMeal.value = ''
    newDishName.value = ''
    await fetchMenu()
  } finally {
    addingItem.value = false
  }
}

async function removeMenuItem(item) {
  const confirmed = await requestConfirm({
    title: '移除菜品',
    message: '确定要把“' + item.dishName + '”从菜单里移除吗？',
    confirmText: '移除'
  })
  if (!confirmed) return
  await deleteMenuItem(item.id)
  menuItems.value = menuItems.value.filter((entry) => entry.id !== item.id)
}

function openDishModal() {
  dishForm.value = createDishForm()
  showDishModal.value = true
}

function resetDishForm() {
  dishForm.value = createDishForm()
}

function closeDishModal() {
  showDishModal.value = false
  resetDishForm()
}

async function submitDish() {
  const name = dishForm.value.name.trim()
  if (!name || submitting.value) return
  submitting.value = true
  try {
    const res = await createDish({
      name,
      category: dishForm.value.category,
      note: dishForm.value.note.trim() || undefined
    })
    if (res.data?.id) dishes.value = [res.data, ...dishes.value]
    else {
      const listRes = await getDishes()
      dishes.value = listRes.data || []
    }
    closeDishModal()
  } finally {
    submitting.value = false
  }
}

async function removeDish(dish) {
  const confirmed = await requestConfirm({
    title: '删除菜品',
    message: '确定要删除“' + dish.name + '”吗？删除后无法恢复。',
    confirmText: '删除'
  })
  if (!confirmed) return
  await deleteDish(dish.id)
  dishes.value = dishes.value.filter((entry) => entry.id !== dish.id)
}

onMounted(fetchAll)
onBeforeUnmount(() => {
  if (rollTimer) clearInterval(rollTimer)
})
</script>

<style scoped>
.menu-page {
  padding: 32px 0 60px;
}

.menu-header {
  text-align: center;
  margin-bottom: 32px;
  position: relative;
}

.menu-header__title {
  font-family: var(--font-handwrite);
  font-size: 36px;
  color: var(--brown-deep);
  margin-bottom: 8px;
}

.menu-header__subtitle {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.menu-header__deco {
  margin-top: 12px;
}

.star {
  display: inline-block;
  color: var(--mustard);
  margin: 0 8px;
  animation: twinkle 2s ease-in-out infinite;
}

.s1 {
  font-size: 16px;
  animation-delay: 0s;
}

.s2 {
  font-size: 22px;
  animation-delay: 0.4s;
}

.s3 {
  font-size: 14px;
  animation-delay: 0.8s;
}

.section-title {
  font-family: var(--font-handwrite);
  font-size: 26px;
  color: var(--brown-deep);
}

/* 分类 chips */
.category-chips {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.category-chip {
  padding: 6px 14px;
  font-size: 13px;
  font-family: var(--font-display);
  color: var(--text-secondary);
  background: rgba(var(--kraft-rgb), 0.1);
  border: 1px solid rgba(var(--brown-rgb), 0.15);
  border-radius: 16px;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.category-chip:hover {
  background: rgba(var(--kraft-rgb), 0.2);
}

.category-chip.active {
  color: var(--cream);
  background: var(--brown-deep);
  border-color: var(--brown-deep);
}

/* 随机选菜 hero */
.random-hero {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 28px 24px 32px;
  margin-bottom: 36px;
  position: relative;
  overflow: hidden;
  text-align: center;
}

.random-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--paper-texture);
  opacity: 0.25;
  pointer-events: none;
}

.random-stage {
  margin: 24px auto;
  min-height: 170px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
}

.random-stage__name {
  font-family: var(--font-handwrite);
  font-size: 56px;
  color: var(--brown-deep);
  line-height: 1.2;
}

.random-stage--rolling .random-stage__name {
  animation: roll-pulse 0.18s ease-in-out infinite;
}

.random-stage__hint {
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
}

.random-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 44px;
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 2px;
  color: var(--cream);
  background: linear-gradient(145deg, var(--faded-red), var(--faded-red-dark));
  border: 1px solid rgba(var(--cream-static-rgb), 0.15);
  border-radius: 30px;
  box-shadow: 0 4px 16px rgba(var(--red-rgb), 0.35);
  cursor: pointer;
  transition: all var(--transition-normal);
  position: relative;
}

.random-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(var(--red-rgb), 0.45);
}

.random-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.random-btn__icon {
  font-size: 20px;
}

/* 揭晓结果卡片 */
.result-card {
  background: var(--gradient-paper);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 24px 32px;
  display: grid;
  justify-items: center;
  gap: 10px;
  animation: fade-in-up 0.4s ease both;
}

.result-card__eyebrow {
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--faded-red);
}

.result-card__name {
  font-family: var(--font-handwrite);
  font-size: 38px;
  color: var(--brown-deep);
}

.result-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-muted);
}

.result-card__category {
  padding: 2px 10px;
  background: rgba(var(--kraft-rgb), 0.15);
  border-radius: 10px;
}

.result-card__note {
  font-style: italic;
}

.result-card__meal-picker {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.result-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

/* 今日菜单 */
.today-menu {
  margin-bottom: 36px;
}

.today-menu__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.date-nav {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-nav__arrow {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  font-size: 20px;
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.12);
  border: var(--border-vintage);
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.date-nav__arrow:hover {
  background: rgba(var(--kraft-rgb), 0.25);
}

.date-nav__label {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 1px;
  min-width: 150px;
  text-align: center;
}

.meal-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.meal-card {
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.meal-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: var(--paper-texture);
  opacity: 0.2;
  pointer-events: none;
}

.meal-card__title {
  font-family: var(--font-display);
  font-size: 17px;
  color: var(--brown-deep);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px dashed rgba(var(--brown-medium-rgb), 0.25);
  position: relative;
}

.meal-card__list {
  list-style: none;
  margin: 0 0 12px;
  padding: 0;
  display: grid;
  gap: 8px;
  position: relative;
}

.meal-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(var(--kraft-rgb), 0.1);
  border: 1px solid rgba(var(--brown-rgb), 0.12);
  border-radius: var(--radius-sm);
}

.meal-item__name {
  font-size: 14px;
  color: var(--text-primary);
  overflow-wrap: break-word;
}

.meal-item__delete {
  flex: 0 0 auto;
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  font-size: 15px;
  color: var(--text-muted);
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.meal-item__delete:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.12);
}

.meal-card__empty {
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
  margin: 0 0 12px;
  position: relative;
}

.meal-card__add {
  width: 100%;
  padding: 8px;
  font-family: var(--font-display);
  font-size: 13px;
  letter-spacing: 1px;
  color: var(--text-secondary);
  background: transparent;
  border: var(--border-dashed);
  border-width: 1.5px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.meal-card__add:hover {
  color: var(--brown-deep);
  background: rgba(var(--kraft-rgb), 0.12);
}

.meal-add {
  display: grid;
  gap: 8px;
  position: relative;
}

.meal-add__select {
  appearance: auto;
}

.meal-add__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 菜品池 */
.dish-pool__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.dish-group {
  margin-bottom: 20px;
}

.dish-group__title {
  font-family: var(--font-display);
  font-size: 15px;
  color: var(--text-secondary);
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.dish-group__items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.dish-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--gradient-card);
  border: var(--border-vintage);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-soft);
}

.dish-chip__name {
  font-size: 14px;
  color: var(--text-primary);
  overflow-wrap: break-word;
}

.dish-chip__note {
  font-size: 12px;
  color: var(--text-muted);
  font-style: italic;
  max-width: 180px;
  overflow-wrap: break-word;
}

.dish-chip__delete {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  font-size: 14px;
  color: var(--text-muted);
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.dish-chip__delete:hover {
  color: var(--faded-red);
  background: rgba(var(--red-rgb), 0.12);
}

/* 弹窗表单 */
.dish-form .form-group {
  margin-bottom: 20px;
}

.dish-form label {
  display: block;
  font-family: var(--font-display);
  font-size: 13px;
  color: var(--text-secondary);
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.category-chips--form {
  justify-content: flex-start;
}

/* Animations */
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes twinkle {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(0.9);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

@keyframes roll-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.55;
    transform: scale(0.96);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .menu-page {
    padding: 24px 0 48px;
  }

  .menu-header {
    margin-bottom: 24px;
  }

  .menu-header__title {
    font-size: 28px;
  }

  .random-hero {
    padding: 22px 16px 26px;
  }

  .random-stage__name {
    font-size: 40px;
  }

  .result-card {
    padding: 20px;
  }

  .result-card__name {
    font-size: 30px;
  }

  .meal-grid {
    grid-template-columns: 1fr;
  }

  .date-nav__label {
    min-width: 0;
  }

  .category-chip {
    padding: 8px 14px;
  }

  .dish-form .input-vintage {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .menu-header__title {
    font-size: 24px;
  }

  .random-btn {
    padding: 12px 32px;
    font-size: 16px;
  }

  .random-stage__name {
    font-size: 32px;
  }

  .today-menu__bar,
  .dish-pool__bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .category-chip {
    padding: 6px 12px;
    font-size: 12px;
  }
}
</style>
