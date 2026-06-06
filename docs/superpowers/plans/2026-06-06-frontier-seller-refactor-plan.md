# Frontier Seller 商家端重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 frontier-seller 商家端进行全面的视觉/架构重构，按代码规格说明书每个页面拆分为 4 文件结构，引入 Pinia 状态管理，搭建轻量极简风格组件库

**Architecture:** Vue 3 + Vite + Tailwind CSS + Pinia。采用渐进式重构策略（方案 A）：先搭骨架（Phase 1），再逐页迁移（Phase 2-3），最后收尾（Phase 4）

**Tech Stack:** Vue 3 (Composition API), Vite, Tailwind CSS 3, Pinia, Axios, SweetAlert2

**参考文档:** `docs/superpowers/specs/2026-06-06-frontier-seller-refactor-design.md`

---

## Phase 1: 骨架搭建

### Task 1: 创建设计令牌与全局样式

**Files:**
- Create: `src/styles/variables.css`
- Create: `src/styles/base.css`
- Create: `src/styles/utilities.css`
- Modify: `src/main.js`
- Delete: `src/style.css`

- [ ] **Step 1: 创建 `src/styles/variables.css`**

```css
:root {
  --color-primary: oklch(0.55 0.15 250);
  --color-primary-hover: oklch(0.5 0.15 250);
  --color-surface: oklch(0.99 0 0);
  --color-card: oklch(0.97 0.01 240);
  --color-border: oklch(0.92 0.01 240);
  --color-text: oklch(0.2 0.02 240);
  --color-text-secondary: oklch(0.55 0.03 240);
  --color-text-tertiary: oklch(0.7 0.02 240);
  --color-accent: oklch(0.65 0.2 30);
  --color-success: oklch(0.6 0.18 150);
  --color-danger: oklch(0.55 0.2 25);
  --color-sidebar: oklch(0.25 0.02 240);

  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --font-display: 'Inter', sans-serif;

  --text-xs: 0.75rem;
  --text-sm: 0.875rem;
  --text-base: 1rem;
  --text-lg: 1.25rem;
  --text-xl: 1.5rem;
  --text-2xl: 2rem;

  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-12: 3rem;

  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;
  --radius-xl: 12px;

  --shadow-sm: 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md: 0 2px 8px rgba(0,0,0,0.06);
  --shadow-lg: 0 8px 24px rgba(0,0,0,0.08);
}
```

- [ ] **Step 2: 创建 `src/styles/base.css`**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

*, *::before, *::after {
  box-sizing: border-box;
}

body {
  font-family: var(--font-sans);
  font-size: var(--text-base);
  color: var(--color-text);
  background: var(--color-surface);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

- [ ] **Step 3: 创建 `src/styles/utilities.css`**

```css
.text-secondary { color: var(--color-text-secondary); }
.text-tertiary { color: var(--color-text-tertiary); }
```

- [ ] **Step 4: 更新 `src/main.js`**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/base.css'
import './styles/variables.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 5: 删除旧文件 `src/style.css`**

在确认无引用后删除。

---

### Task 2: 创建 Pinia Store

**Files:**
- Create: `src/store/auth.js`
- Create: `src/store/shop.js`
- Create: `src/store/app.js`

- [ ] **Step 1: 创建 `src/store/auth.js`**

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { merchantLogin, merchantLogout } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('satoken') || '')
  const merchantInfo = ref(JSON.parse(localStorage.getItem('merchantInfo') || 'null'))
  const currentRole = ref(JSON.parse(localStorage.getItem('currentRole') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isOwner = computed(() => currentRole.value?.role === '1')
  const merchantName = computed(() => merchantInfo.value?.username || '')

  async function login(credentials) {
    const res = await merchantLogin(credentials)
    if (res.token) {
      token.value = res.token
      merchantInfo.value = res.merchantInfo
      localStorage.setItem('satoken', res.token)
      localStorage.setItem('merchantInfo', JSON.stringify(res.merchantInfo))
    }
    return res
  }

  async function logout() {
    try {
      await merchantLogout()
    } finally {
      token.value = ''
      merchantInfo.value = null
      currentRole.value = null
      localStorage.removeItem('satoken')
      localStorage.removeItem('merchantInfo')
      localStorage.removeItem('currentRole')
      localStorage.removeItem('merchantRoles')
    }
  }

  function setRole(role) {
    currentRole.value = role
    localStorage.setItem('currentRole', JSON.stringify(role))
  }

  return { token, merchantInfo, currentRole, isLoggedIn, isOwner, merchantName, login, logout, setRole }
})
```

- [ ] **Step 2: 创建 `src/store/shop.js`**

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShopByMerchant } from '@/api/shop'

export const useShopStore = defineStore('shop', () => {
  const currentShopId = ref(localStorage.getItem('currentShopId') || null)
  const shops = ref([])

  const currentShop = computed(() => shops.value.find(s => s.id === currentShopId.value))

  async function loadShops(merchantId) {
    const res = await getShopByMerchant(merchantId)
    if (res?.data?.shopIds) {
      shops.value = res.data.shopIds.map(id => ({ id }))
    }
  }

  function switchShop(shopId) {
    currentShopId.value = shopId
    localStorage.setItem('currentShopId', shopId)
  }

  return { currentShopId, shops, currentShop, loadShops, switchShop }
})
```

- [ ] **Step 3: 创建 `src/store/app.js`**

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, toggleSidebar }
})
```

---

### Task 3: 创建布局组件

**Files:**
- Create: `src/layout/AppLayout.vue`
- Create: `src/layout/AppLayout.css`
- Create: `src/layout/AppSidebar.vue`
- Create: `src/layout/AppSidebar.css`
- Create: `src/layout/AppTopBar.vue`
- Create: `src/layout/AppTopBar.css`

- [ ] **Step 1: 创建 `src/layout/AppLayout.vue`**

```vue
<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="app-main">
      <AppTopBar />
      <main class="app-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import AppSidebar from './AppSidebar.vue'
import AppTopBar from './AppTopBar.vue'
</script>

<style src="./AppLayout.css"></style>
```

- [ ] **Step 2: 创建 `src/layout/AppLayout.css`**

```css
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-surface);
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
}
```

- [ ] **Step 3: 创建 `src/layout/AppSidebar.vue`**

侧边栏，包含：
- Logo/品牌标识
- 导航菜单（订单发货、店铺管理 → 子菜单：商品/订单/员工/地址）
- 底部：用户信息 + 退出按钮
- 折叠功能（使用 useAppStore 的 sidebarCollapsed）
- 高亮当前路由

导航链接配置（硬编码）：
```
/ship           → "订单发货"（图标: 📦）
/shop/list      → "店铺管理"（图标: 🏪，店长可见）
  /shop/:shopId/products   → "商品管理"
  /shop/:shopId/orders     → "订单管理"
  /shop/:shopId/employees  → "员工管理"
  /shop/:shopId/addresses  → "地址管理"
```

- [ ] **Step 4: 创建 `src/layout/AppTopBar.vue`**

顶栏包含：
- 左侧：侧边栏折叠按钮 + 面包屑导航
- 右侧：店铺选择器（从 shop store 读取） + 用户头像/名称

---

### Task 4: 创建核心组件（上）

**Files:**
- Create: `src/components/AppButton.vue`
- Create: `src/components/AppCard.vue`
- Create: `src/components/AppModal.vue`
- Create: `src/components/AppBadge.vue`
- Create: `src/components/AppEmpty.vue`

- [ ] **Step 1: 创建 `src/components/AppButton.vue`**

```vue
<template>
  <button
    class="app-btn"
    :class="[`app-btn--${variant}`, `app-btn--${size}`, { 'app-btn--loading': loading }]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="app-btn__spinner" />
    <slot />
  </button>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | ghost | danger
  size: { type: String, default: 'md' },         // sm | md | lg
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
})
defineEmits(['click'])
</script>

<style scoped>
.app-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: none;
  border-radius: var(--radius-md);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  white-space: nowrap;
}
.app-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.app-btn--sm { padding: var(--space-1) var(--space-3); font-size: var(--text-sm); }
.app-btn--md { padding: var(--space-2) var(--space-4); font-size: var(--text-sm); }
.app-btn--lg { padding: var(--space-3) var(--space-6); font-size: var(--text-base); }
.app-btn--primary { background: var(--color-primary); color: white; }
.app-btn--primary:hover:not(:disabled) { background: var(--color-primary-hover); }
.app-btn--secondary { background: var(--color-card); color: var(--color-text); border: 1px solid var(--color-border); }
.app-btn--secondary:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.app-btn--ghost { background: transparent; color: var(--color-text-secondary); }
.app-btn--ghost:hover:not(:disabled) { background: var(--color-card); color: var(--color-text); }
.app-btn--danger { background: var(--color-danger); color: white; }
.app-btn__spinner {
  width: 14px; height: 14px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
```

- [ ] **Step 2: 创建 `src/components/AppCard.vue`**

```vue
<template>
  <div class="app-card" :class="{ 'app-card--hoverable': hoverable }" :style="{ padding: padding }">
    <slot />
  </div>
</template>

<script setup>
defineProps({
  hoverable: { type: Boolean, default: false },
  padding: { type: String, default: 'var(--space-6)' }
})
</script>

<style scoped>
.app-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s;
}
.app-card--hoverable:hover {
  box-shadow: var(--shadow-md);
}
</style>
```

- [ ] **Step 3: 创建 `src/components/AppModal.vue`**

弹窗组件：遮罩层 + 居中对话框 + 标题 + 内容 slot + 底部按钮。
支持 `visible` prop 控制显示隐藏，`@close` 事件。
`confirmText` / `cancelText` prop 自定义按钮文字。
ESC 键和点击遮罩关闭。

- [ ] **Step 4: 创建 `src/components/AppBadge.vue`**

```vue
<template>
  <span class="app-badge" :class="`app-badge--${variant}`">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'default' } // default | success | warning | danger | info
})
</script>

<style scoped>
.app-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: var(--text-xs);
  font-weight: 600;
  line-height: 1.4;
}
.app-badge--default { background: oklch(0.93 0.01 240); color: var(--color-text-secondary); }
.app-badge--success { background: oklch(0.92 0.05 150); color: oklch(0.4 0.12 150); }
.app-badge--warning { background: oklch(0.95 0.08 85); color: oklch(0.5 0.15 85); }
.app-badge--danger  { background: oklch(0.93 0.06 25);  color: oklch(0.45 0.18 25); }
.app-badge--info    { background: oklch(0.92 0.04 250); color: oklch(0.4 0.1 250); }
</style>
```

- [ ] **Step 5: 创建 `src/components/AppEmpty.vue`**

```vue
<template>
  <div class="app-empty">
    <span class="app-empty__icon">{{ icon }}</span>
    <p class="app-empty__title">{{ title }}</p>
    <p v-if="description" class="app-empty__desc">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '📦' },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.app-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-6);
  text-align: center;
}
.app-empty__icon { font-size: 2.5rem; margin-bottom: var(--space-4); }
.app-empty__title { font-size: var(--text-base); color: var(--color-text); font-weight: 600; margin: 0; }
.app-empty__desc { font-size: var(--text-sm); color: var(--color-text-secondary); margin: var(--space-2) 0 0; }
</style>
```

---

### Task 5: 创建核心组件（下）

**Files:**
- Create: `src/components/AppSearch.vue`
- Create: `src/components/AppDropdown.vue`
- Create: `src/components/index.js`

- [ ] **Step 1: 创建 `src/components/AppSearch.vue`**

搜索输入框，含图标和清除按钮。v-model 双向绑定，`@search` 事件（回车时触发），`placeholder` prop。

- [ ] **Step 2: 创建 `src/components/AppDropdown.vue`**

下拉菜单，`items` prop（数组，{label, value}），`v-model` 双向绑定，`placeholder` prop。

- [ ] **Step 3: 创建 `src/components/index.js`**

```js
export { default as AppButton } from './AppButton.vue'
export { default as AppCard } from './AppCard.vue'
export { default as AppModal } from './AppModal.vue'
export { default as AppBadge } from './AppBadge.vue'
export { default as AppEmpty } from './AppEmpty.vue'
export { default as AppSearch } from './AppSearch.vue'
export { default as AppDropdown } from './AppDropdown.vue'
```

---

### Task 6: 更新 App.vue 和路由

**Files:**
- Modify: `src/App.vue`
- Create: `src/App.css`
- Create: `src/composables/useAuth.js`
- Create: `src/composables/useShop.js`
- Modify: `src/router/index.js`

- [ ] **Step 1: 重写 `src/App.vue`**

```vue
<template>
  <router-view />
</template>

<script setup>
</script>

<style src="./App.css"></style>
```

- [ ] **Step 2: 创建 `src/App.css`**

```css
#app {
  width: 100vw;
  height: 100vh;
}
```

- [ ] **Step 3: 创建 `src/composables/useAuth.js`**

```js
import { useAuthStore } from '@/store/auth'
import { storeToRefs } from 'pinia'

export function useAuth() {
  const store = useAuthStore()
  const { isLoggedIn, isOwner, merchantName, currentRole } = storeToRefs(store)
  return { ...store, isLoggedIn, isOwner, merchantName, currentRole }
}
```

- [ ] **Step 4: 创建 `src/composables/useShop.js`**

```js
import { useShopStore } from '@/store/shop'
import { storeToRefs } from 'pinia'

export function useShop() {
  const store = useShopStore()
  const { currentShopId, shops, currentShop } = storeToRefs(store)
  return { ...store, currentShopId, shops, currentShop }
}
```

- [ ] **Step 5: 重写 `src/router/index.js`**

路由保持不变，但替换守卫逻辑：
- 使用 `useAuthStore` 替代 localStorage 直接检查
- `shopOwnerOnly` 页面的权限检查使用 `useAuthStore().isOwner`
- 布局路由：公开路由（/login）使用无布局，其他路由使用 AppLayout

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import AppLayout from '@/layout/AppLayout.vue'
import Login from '@/views/Login/Login.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/ship' },
      { path: 'ship', name: 'ship', component: () => import('@/views/Ship/Ship.vue') },
      { path: 'shop/register', name: 'shop-register', component: () => import('@/views/ShopRegister/ShopRegister.vue'), meta: { shopOwnerOnly: true } },
      { path: 'shop/list', name: 'shop-list', component: () => import('@/views/ShopList/ShopList.vue'), meta: { shopOwnerOnly: true } },
      { path: 'shop/:shopId/products', name: 'shop-products', component: () => import('@/views/ShopProducts/ShopProducts.vue') },
      { path: 'shop/:shopId/orders', name: 'shop-orders', component: () => import('@/views/ShopOrders/ShopOrders.vue') },
      { path: 'shop/:shopId/employees', name: 'shop-employees', component: () => import('@/views/ShopEmployees/ShopEmployees.vue'), meta: { shopOwnerOnly: true } },
      { path: 'shop/:shopId/addresses', name: 'shop-addresses', component: () => import('@/views/ShopAddresses/ShopAddresses.vue'), meta: { shopOwnerOnly: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.public) return next()
  if (!auth.isLoggedIn) return next('/login')
  if (to.meta.shopOwnerOnly && !auth.isOwner) {
    Swal.fire({ title: '权限不足', text: '仅店长可操作', icon: 'warning', confirmButtonText: '确定' })
    return next('/ship')
  }
  next()
})

export default router
```

注意：路由守卫中的 Swal 需要在文件顶部 `import Swal from 'sweetalert2'`。

---

## Phase 2: 核心页面迁移

### Task 7: Login 页面 → 4 文件结构

**Files:**
- Create: `src/views/Login/Login.vue`
- Create: `src/views/Login/Login.js`
- Create: `src/views/Login/Login.css`
- Create: `src/views/Login/Text.js`
- Delete: `src/views/Login/Login.vue`（旧单文件）

- [ ] **Step 1: 创建 `src/views/Login/Text.js`**

```js
export const LOGIN_TITLE = '商家登录'
export const LOGIN_SUBTITLE = 'AI-Mart 商家服务平台'
export const USERNAME_LABEL = '商家账号'
export const PASSWORD_LABEL = '密码'
export const USERNAME_PLACEHOLDER = '请输入商家账号'
export const PASSWORD_PLACEHOLDER = '请输入密码'
export const SUBMIT_TEXT = '登录'
export const SUBMIT_LOADING = '登录中...'
export const HINT_TEXT = '测试账号：merchant001 / 123456'
export const ERR_USERNAME_REQUIRED = '账号不能为空'
export const ERR_USERNAME_INVALID = '账号格式不正确'
export const ERR_PASSWORD_REQUIRED = '密码不能为空'
export const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/
```

- [ ] **Step 2: 创建 `src/views/Login/Login.js`**

```js
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { showError } from '@/utils/swal'
import {
  USERNAME_PATTERN,
  ERR_USERNAME_REQUIRED,
  ERR_USERNAME_INVALID
} from './Text'

export function useLogin() {
  const router = useRouter()
  const auth = useAuthStore()

  const loading = ref(false)
  const showPassword = ref(false)

  const form = reactive({
    username: '',
    password: ''
  })

  const errors = reactive({
    username: '',
    password: ''
  })

  const isFormValid = computed(() => {
    return form.username && form.password && USERNAME_PATTERN.test(form.username)
  })

  function validateUsername() {
    errors.username = ''
    if (!form.username) {
      errors.username = ERR_USERNAME_REQUIRED
      return
    }
    if (!USERNAME_PATTERN.test(form.username)) {
      errors.username = ERR_USERNAME_INVALID
    }
  }

  async function handleLogin() {
    validateUsername()
    if (!isFormValid.value) return

    loading.value = true
    try {
      const res = await auth.login({
        username: form.username,
        password: form.password
      })
      if (res.token) {
        sessionStorage.setItem('needReload', '1')
        router.push('/')
      } else {
        showError(res.message || '登录失败')
      }
    } catch (e) {
      showError(e.response?.data?.message || e.message || '网络错误，请稍后重试')
    } finally {
      loading.value = false
    }
  }

  return {
    form, errors, loading, showPassword,
    isFormValid, validateUsername, handleLogin
  }
}
```

- [ ] **Step 3: 创建 `src/views/Login/Login.vue`**

模板从旧 Login.vue 提取，但：
- 使用 Text.js 的文本常量替换硬编码字符串
- 使用 Login.js 导出的 useLogin() composable
- 使用 AppButton 组件替代原生 `<button>`
- 样式精简

```vue
<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">{{ LOGIN_TITLE }}</h1>
        <p class="login-subtitle">{{ LOGIN_SUBTITLE }}</p>
      </div>

      <div class="login-form">
        <div class="field">
          <label class="field__label">{{ USERNAME_LABEL }}</label>
          <input
            v-model="form.username"
            :placeholder="USERNAME_PLACEHOLDER"
            class="field__input"
            :class="{ 'field__input--error': errors.username }"
            @blur="validateUsername"
          />
          <span v-if="errors.username" class="field__error">{{ errors.username }}</span>
        </div>

        <div class="field">
          <label class="field__label">{{ PASSWORD_LABEL }}</label>
          <div class="field__password">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              :placeholder="PASSWORD_PLACEHOLDER"
              class="field__input"
            />
            <button class="field__toggle" @click="showPassword = !showPassword" type="button">
              {{ showPassword ? '🙈' : '🙉' }}
            </button>
          </div>
        </div>

        <AppButton
          class="login-submit"
          :loading="loading"
          :disabled="!isFormValid"
          @click="handleLogin"
        >
          {{ loading ? SUBMIT_LOADING : SUBMIT_TEXT }}
        </AppButton>

        <p class="login-hint">{{ HINT_TEXT }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { AppButton } from '@/components'
import { useLogin } from './Login'
import {
  LOGIN_TITLE, LOGIN_SUBTITLE,
  USERNAME_LABEL, PASSWORD_LABEL,
  USERNAME_PLACEHOLDER, PASSWORD_PLACEHOLDER,
  SUBMIT_TEXT, SUBMIT_LOADING, HINT_TEXT
} from './Text'

const {
  form, errors, loading, showPassword,
  isFormValid, validateUsername, handleLogin
} = useLogin()
</script>

<style src="./Login.css" scoped></style>
```

- [ ] **Step 4: 创建 `src/views/Login/Login.css`**

登录页样式：居中卡片布局，琥珀色渐变色背景（保持原有品牌色），简洁的表单样式。

- [ ] **Step 5: 删除旧的 `src/views/Login/Login.vue`**

---

### Task 8: Ship 页面 → 4 文件结构

**Files:**
- Create: `src/views/Ship/Ship.vue`
- Create: `src/views/Ship/Ship.js`
- Create: `src/views/Ship/Ship.css`
- Create: `src/views/Ship/Text.js`
- Delete: `src/merchant/MerchantShip/` 整个目录

- [ ] **Step 1: 创建 `src/views/Ship/Text.js`**

从 `src/merchant/MerchantShip/Text.js` 复制并整理所有文本常量。

- [ ] **Step 2: 创建 `src/views/Ship/Ship.js`**

从 `src/merchant/MerchantShip/useMerchantShip.js` 迁移逻辑到 `Ship.js`，导出 `useShip()` composable。
主要变化：
- 使用 `useAuthStore` 替代 localStorage 获取 merchantInfo
- 使用 `useShopStore` 替代本地 shops 管理
- 使用新 API 导入（已在之前更新过）

- [ ] **Step 3: 创建 `src/views/Ship/Ship.vue`**

从 `MerchantShip.vue` 迁移模板，使用新的组件和文本常量。

- [ ] **Step 4: 创建 `src/views/Ship/Ship.css`**

从 `MerchantShip.css` 迁移样式，精简优化。

- [ ] **Step 5: 删除 `src/merchant/MerchantShip/` 目录**

---

### Task 9: ShopList 页面 → 4 文件结构

**Files:**
- Create: `src/views/ShopList/ShopList.vue`
- Create: `src/views/ShopList/ShopList.js`
- Create: `src/views/ShopList/ShopList.css`
- Create: `src/views/ShopList/Text.js`
- Delete: `src/views/shop/ShopList.vue`

- [ ] **Step 1: 创建 Text.js**

提取所有 UI 文本。

- [ ] **Step 2: 创建 ShopList.js**

从现有 ShopList.vue 提取逻辑：
- 使用 `useShopStore` 替代直接调 API
- `loadShops()` 使用 `useAuthStore` 获取 merchantId

- [ ] **Step 3: 创建 ShopList.vue**

使用 AppCard、AppButton、AppEmpty 等组件。

- [ ] **Step 4: 创建 ShopList.css**

- [ ] **Step 5: 删除旧文件**

---

### Task 10: ShopRegister 页面 → 4 文件结构

**Files:**
- Create: `src/views/ShopRegister/ShopRegister.vue`
- Create: `src/views/ShopRegister/ShopRegister.js`
- Create: `src/views/ShopRegister/ShopRegister.css`
- Create: `src/views/ShopRegister/Text.js`
- Delete: `src/views/shop/ShopRegister.vue`

步骤与 Task 7-9 相同：提取文本 → 提取逻辑 → 新建模板 → 新建样式 → 删除旧文件。

---

## Phase 3: 管理页面迁移

### Task 11: ShopProducts 页面 → 4 文件结构

**Files:**
- Create: `src/views/ShopProducts/ShopProducts.vue`
- Create: `src/views/ShopProducts/ShopProducts.js`
- Create: `src/views/ShopProducts/ShopProducts.css`
- Create: `src/views/ShopProducts/Text.js`
- Delete: `src/views/shop/ShopProducts.vue`

注意：商品列表加载（`loadProducts`）当前没有合适的后端接口。保持现状，使用 `getShopDetail` 作为占位。

### Task 12: ShopOrders 页面 → 4 文件结构

**Files:**
- Create: `src/views/ShopOrders/ShopOrders.vue`
- Create: `src/views/ShopOrders/ShopOrders.js`
- Create: `src/views/ShopOrders/ShopOrders.css`
- Create: `src/views/ShopOrders/Text.js`
- Delete: `src/views/shop/ShopOrders.vue`

### Task 13: ShopEmployees 页面 → 4 文件结构

**Files:**
- Create: `src/views/ShopEmployees/ShopEmployees.vue`
- Create: `src/views/ShopEmployees/ShopEmployees.js`
- Create: `src/views/ShopEmployees/ShopEmployees.css`
- Create: `src/views/ShopEmployees/Text.js`
- Delete: `src/views/shop/ShopEmployees.vue`

### Task 14: ShopAddresses 页面 → 4 文件结构

**Files:**
- Create: `src/views/ShopAddresses/ShopAddresses.vue`
- Create: `src/views/ShopAddresses/ShopAddresses.js`
- Create: `src/views/ShopAddresses/ShopAddresses.css`
- Create: `src/views/ShopAddresses/Text.js`
- Delete: `src/views/shop/ShopAddresses.vue`

---

## Phase 4: 收尾清理

### Task 15: 全局清理与验证

**Files:**
- Delete: `src/views/shop/` 目录（确认所有文件已迁移后）
- Modify: 全局检查

- [ ] **Step 1: 检查所有 import 路径**

确认所有 `@/views/xxx/xxx` 路径指向新的 4 文件结构。

- [ ] **Step 2: 检查路由懒加载**

确认路由配置中的 `() => import()` 路径全部更新为新路径。

- [ ] **Step 3: 删除空目录**

```bash
Remove-Item -LiteralPath "src/views/shop" -Recurse -ErrorAction SilentlyContinue
Remove-Item -LiteralPath "src/merchant" -Recurse -ErrorAction SilentlyContinue
```

- [ ] **Step 4: 安装依赖并测试**

```bash
npm install
npm run build
```

确保构建无错误。

- [ ] **Step 5: 运行 dev server 手动验证**

```bash
npm run dev
```

验证所有页面可正常访问。
