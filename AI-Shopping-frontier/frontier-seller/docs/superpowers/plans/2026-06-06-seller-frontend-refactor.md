# Seller Frontend Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor login flow with verifying state + fix no-shop UX (hide sidebar, empty state, dialog opens new tab)

**Architecture:** Login page transitions to a full-screen verifying state after auth success, runs `initShops()` in parallel, then navigates to AppLayout. AppLayout conditionally renders sidebar/empty state based on `hasNoShops`.

**Tech Stack:** Vue 3 (Composition API + `<script setup>`), Vue Router 4, Pinia, Element Plus

---

### Task 1: Add verifying state to Login.js

**Files:**
- Modify: `src/views/Login/Login.js`

- [ ] **Step 1: Add verifying ref + shop init logic**

```js
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import * as T from './Text.js'

export function useLogin() {
  const router = useRouter()
  const authStore = useAuthStore()
  const shopStore = useShopStore()

  const formRef = ref(null)
  const form = reactive({ username: '', password: '' })
  const loading = ref(false)
  const verifying = ref(false)

  const usernamePattern = /^[a-zA-Z0-9_]{3,20}$/

  const rules = {
    username: [
      { required: true, message: T.USERNAME_REQUIRED, trigger: 'blur' },
      { pattern: usernamePattern, message: T.USERNAME_INVALID, trigger: 'blur' }
    ],
    password: [
      { required: true, message: T.PASSWORD_REQUIRED, trigger: 'blur' }
    ]
  }

  async function handleLogin() {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
      const res = await authStore.login({
        username: form.username,
        password: form.password
      })
      ElMessage.success(res.message || '登录成功')

      verifying.value = true
      await shopStore.initShops(authStore.merchantInfo?.id || authStore.merchantId)
      router.push('/')
    } catch (err) {
      const msg = err.response?.data?.message || err.message || '登录失败，请重试'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  }

  return { T, formRef, form, loading, verifying, rules, handleLogin }
}
```

### Task 2: Update Login.vue with verifying overlay

**Files:**
- Modify: `src/views/Login/Login.vue`

- [ ] **Step 1: Update template to support verifying state**

```vue
<template>
  <div class="login-page">
    <transition name="el-fade-in-linear">
      <div v-if="verifying" class="login-verifying">
        <div class="login-verifying__spinner"></div>
        <h2 class="login-verifying__title">{{ T.BRAND_NAME }}</h2>
        <p class="login-verifying__text">{{ T.VERIFYING }}</p>
      </div>
    </transition>

    <el-card v-show="!verifying" class="login-card" shadow="always">
      <div class="login-brand">
        <svg class="login-brand__icon" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="oklch(0.9 0.02 240)"/>
          <path d="M16 30V20l8 6 8-6v10" stroke="oklch(0.5 0.1 240)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <h1 class="login-brand__title">{{ T.PAGE_TITLE }}</h1>
        <p class="login-brand__subtitle">{{ T.BRAND_NAME }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item :label="T.USERNAME_LABEL" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>

        <el-form-item :label="T.PASSWORD_LABEL" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="login-btn"
            round
          >
            {{ loading ? T.LOGIN_LOADING : T.LOGIN_BUTTON }}
          </el-button>
        </el-form-item>
      </el-form>

    </el-card>
  </div>
</template>

<script setup>
import { useLogin } from './Login.js'

const { T, formRef, form, loading, verifying, rules, handleLogin } = useLogin()
</script>

<style scoped src="./Login.css"></style>
```

### Task 3: Add verifying styles to Login.css

**Files:**
- Modify: `src/views/Login/Login.css`

- [ ] **Step 1: Append verifying overlay styles**

```css
.login-verifying {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.login-verifying__spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border-light);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: var(--space-6);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-verifying__title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-3);
}

.login-verifying__text {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin: 0;
}
```

### Task 4: Add verifying i18n text

**Files:**
- Modify: `src/views/Login/Text.js`

- [ ] **Step 1: Add VERIFYING constant**

```js
export const PAGE_TITLE = '商家登录'
export const BRAND_NAME = 'AI-Mart 商家服务平台'
export const USERNAME_LABEL = '账号'
export const PASSWORD_LABEL = '密码'
export const LOGIN_BUTTON = '登录'
export const LOGIN_LOADING = '登录中...'
export const VERIFYING = '正在验证店铺信息…'
export const USERNAME_REQUIRED = '请输入账号'
export const USERNAME_INVALID = '账号为 3-20 位字母、数字或下划线'
export const PASSWORD_REQUIRED = '请输入密码'
```

### Task 5: AppLayout - conditional sidebar + empty state

**Files:**
- Modify: `src/layout/AppLayout.vue`

- [ ] **Step 1: Update template to hide sidebar and show empty state when no shops**

```vue
<template>
  <div class="app-layout">
    <AppSidebar v-if="!shop.hasNoShops" />

    <el-main class="app-main">
      <AppTopBar />
      <div class="app-content">

        <div v-if="shop.hasNoShops" class="app-empty">
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="app-empty__icon"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          <h2 class="app-empty__title">您还没有店铺</h2>
          <p class="app-empty__desc">创建店铺后即可管理商品和订单</p>
        </div>
        <router-view v-else />

      </div>
    </el-main>

    <el-dialog
      v-model="showNoShopDialog"
      :close-on-click-modal="false"
      :show-close="false"
      width="420px"
      align-center
      top="30vh"
      destroy-on-close
    >
      <div class="no-shop-dialog__body">
        <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" class="no-shop-dialog__icon"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
        <h2 class="no-shop-dialog__title">您还没有店铺</h2>
        <p class="no-shop-dialog__desc">创建店铺后即可管理商品和订单</p>
      </div>
      <template #footer>
        <el-button type="primary" size="large" round @click="goRegister" style="width: 100%">前往创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 2: Append empty state + no-shop dialog styles to scoped + unscoped blocks**

Append to `<style scoped>`:
```css
.app-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-16) var(--space-6);
}

.app-empty__icon {
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-6);
}

.app-empty__title {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-2);
}

.app-empty__desc {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin: 0;
}
```

### Task 6: AppSidebar - remove dead hasNoShops code

**Files:**
- Modify: `src/layout/AppSidebar.vue`

- [ ] **Step 1: Remove the no-shop notice block and goRegister function**

Remove the `v-if="shop.hasNoShops"` block (lines 7-10) and the `v-else-if` notice (lines 11-13). Remove the `goRegister` function and unused `useRouter` import.

```vue
<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': app.sidebarCollapsed }">
    <div class="sidebar__header">
      <span class="sidebar__logo">AI-Mart</span>
    </div>

    <div v-if="shop.loaded && !shop.currentShopId" class="sidebar__notice">
      请先选择店铺
    </div>

    <el-menu
      :default-active="route.path"
      router
      :collapse="app.sidebarCollapsed"
      class="sidebar__menu"
    >
      <el-sub-menu v-if="!shop.hasNoShops" index="order-group">
        <template #title>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
          <span>订单管理</span>
        </template>
        <el-menu-item index="/ship">
          <span>订单发货</span>
        </el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/orders`">
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/returns`">
          <span>退货管理</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/products`">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        <span>商品管理</span>
      </el-menu-item>

      <el-sub-menu v-if="auth.isOwner" index="shop-group">
        <template #title>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          <span>店铺管理</span>
        </template>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/info`">
          <span>商店信息</span>
        </el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/addresses`">
          <span>地址管理</span>
        </el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/employees`">
          <span>员工管理</span>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import { useAppStore } from '@/store/app'

const route = useRoute()
const auth = useAuthStore()
const shop = useShopStore()
const app = useAppStore()
</script>
```

### Task 7: Build verification

- [ ] **Step 1: Run build to verify no errors**

Run: `npm run build`
Expected: `built in X.XXs`
