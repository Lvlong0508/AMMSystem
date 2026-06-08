# 商家端独立店铺选择流程 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将店铺选择从侧边栏剥离为独立全屏页面，用户必须先选店才能进入功能页，切换店铺需回到选择页。

**Architecture:** 新增独立路由 `/shop/select`（无 AppLayout 包裹），路由守卫拦截未选店铺的访问，`initShops` 不再多店铺时自动选中，复用 ShopList.vue 改造为全屏选择页。

**Tech Stack:** Vue 3 + Vue Router 5 + Element Plus 2 + Pinia 3

---

### Task 1: Store - initShops 去掉自动选中

**Files:**
- Modify: `frontier-seller/src/store/shop.js`

- [ ] **Step 1: 修改 initShops 逻辑**

当前 `initShops` 在多店铺时会自动选中第一个。改为：仅单店铺时自动选中，多店铺时仅加载列表不设置 `currentShopId`。

```js
async function initShops(merchantId) {
    if (!merchantId) return;
    loaded.value = false;
    try {
      const res = await getShopByMerchant(merchantId);
      const shopIds = res?.data?.shopIds || res?.shopIds || [];
      shops.value = shopIds.map((id) => ({ id, name: `店铺 ${id}` }));
      if (shops.value.length === 1) {
        currentShopId.value = shops.value[0].id;
        localStorage.setItem("currentShopId", currentShopId.value);
      } else if (shops.value.length === 0) {
        currentShopId.value = null;
        localStorage.removeItem("currentShopId");
      }
      } catch (e) {
      console.error("初始化店铺失败:", e);
    } finally {
      loaded.value = true;
    }
}

function clearCurrentShop() {
    currentShopId.value = null;
    localStorage.removeItem("currentShopId");
}
```

同时在 `return` 中添加 `clearCurrentShop`。

- [ ] **Step 2: 提交**

```bash
git add frontier-seller/src/store/shop.js
git commit -m "feat(shop-store): 多店铺时不再自动选中第一个，添加 clearCurrentShop"
```

---

### Task 2: Router - 添加 /shop/select 路由并更新守卫

**Files:**
- Modify: `frontier-seller/src/router/index.js`

- [ ] **Step 1: 添加 /shop/select 路由，移除 /shop/list 子路由**

在 `/login` 同级（AppLayout 外部）添加 `/shop/select` 为公开路由。从 AppLayout 子路由中移除 `shop/list`。将首页 `/` 改为直接重定向到 `/ship`。

```js
import ShopSelectPage from '@/views/ShopList/ShopList.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: Register,
    meta: { public: true }
  },
  {
    path: '/shop/register',
    name: 'shop-register',
    component: ShopRegister,
    meta: { public: true }
  },
  {
    path: '/shop/select',
    name: 'shop-select',
    component: ShopSelectPage,
    meta: { public: true }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', name: 'home', component: Ship },
      { path: 'ship', name: 'ship', component: Ship },
      {
        path: 'shop/:shopId/products',
        name: 'shop-products',
        component: ShopProducts
      },
      {
        path: 'shop/:shopId/orders',
        name: 'shop-orders',
        component: ShopOrders
      },
      {
        path: 'shop/:shopId/employees',
        name: 'shop-employees',
        component: ShopEmployees,
        meta: { shopOwnerOnly: true }
      },
      {
        path: 'shop/:shopId/addresses',
        name: 'shop-addresses',
        component: ShopAddresses,
        meta: { shopOwnerOnly: true }
      },
      {
        path: 'shop/:shopId/returns',
        name: 'shop-returns',
        component: ShopReturns
      },
      {
        path: 'shop/:shopId/info',
        name: 'shop-info',
        component: ShopInfo,
        meta: { shopOwnerOnly: true }
      }
    ]
  }
]
```

- [ ] **Step 2: 更新路由守卫**

```js
router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore()
  const shop = useShopStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!auth.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.shopOwnerOnly && !auth.isOwner) {
    next('/ship')
    return
  }

  if (auth.merchantId) await shop.initShops(auth.merchantId)

  if (!shop.currentShopId && shop.loaded && !shop.hasNoShops) {
    next('/shop/select')
    return
  }

  if (to.params.shopId && to.params.shopId !== shop.currentShopId) {
    next('/shop/select')
    return
  }

  if (to.name === 'home') {
    if (shop.hasNoShops) {
      next()
      return
    }
    if (shop.currentShopId) {
      next(`/shop/${shop.currentShopId}/products`)
      return
    }
    next('/shop/select')
    return
  }

  if (to.params.shopId) {
    shop.switchShop(to.params.shopId)
  }

  next()
})
```

- [ ] **Step 3: 提交**

```bash
git add frontier-seller/src/router/index.js
git commit -m "feat(router): 添加 /shop/select 独立路由，更新导航守卫"
```

---

### Task 3: ShopList 页面改造为全屏选择页

**Files:**
- Modify: `frontier-seller/src/views/ShopList/ShopList.vue`
- Modify: `frontier-seller/src/views/ShopList/ShopList.js`
- Modify: `frontier-seller/src/views/ShopList/ShopList.css`
- Modify: `frontier-seller/src/views/ShopList/Text.js`

- [ ] **Step 1: 修改 Text.js 添加新文本**

```js
export const PAGE_TITLE = '我的店铺'
export const BTN_REFRESH = '刷新'
export const BTN_CREATE = '创建店铺'
export const STATUS_ACTIVE = '营业中'
export const STATUS_INACTIVE = '未营业'
export const STATUS_CLOSED = '已关闭'
export const EMPTY_TEXT = '暂无店铺'
export const EMPTY_DESC = '创建一个店铺来管理你的商品和订单'
export const BTN_CREATE_NOW = '创建一个店铺'
export const LABEL_DESC = '店铺简介'
export const LABEL_ADDRESS = '地址'
export const LABEL_PHONE = '电话'
export const LABEL_HOURS = '营业时间'
export const LABEL_CREATED = '创建时间'
export const BTN_PRODUCTS = '商品管理'
export const BTN_ORDERS = '订单管理'
export const BTN_EMPLOYEES = '员工管理'
export const BTN_ADDRESSES = '地址管理'
export const BTN_ENTER = '进入管理'
export const SELECT_TITLE = '请选择要管理的店铺'
export const BTN_LOGOUT = '退出登录'
```

- [ ] **Step 2: 修改 ShopList.js**

去掉 `onMounted(loadShops)`（店铺数据由路由守卫加载），添加 auth store 获取用户信息，添加 logout 函数。

```js
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import * as T from './Text.js'

export function useShopList() {
  const router = useRouter()
  const auth = useAuthStore()
  const shopStore = useShopStore()
  const shops = ref(shopStore.shops)
  const loading = ref(false)

  async function loadShops() {
    loading.value = true
    try {
      const merchantId = localStorage.getItem('merchantId')
      if (!merchantId) return
      const { getShopByMerchant } = await import('@/api/shop')
      const res = await getShopByMerchant(merchantId)
      const shopIds = res?.data?.shopIds || res?.shopIds || []
      shops.value = shopIds.map(id => ({ id, name: `店铺 ${id}` }))
    } catch (error) {
      console.error('加载店铺失败:', error)
      ElMessage.error('加载店铺失败')
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    if (shopStore.shops.length > 0) {
      shops.value = shopStore.shops
    } else {
      loadShops()
    }
  })

  function enterShop(shopId) {
    shopStore.switchShop(shopId)
    router.push(`/shop/${shopId}/products`)
  }

  function goRegister() {
    window.open('/shop/register', '_blank')
  }

  async function handleLogout() {
    await auth.logout()
    router.push('/login')
  }

  return { shops, loading, T, enterShop, loadShops, goRegister, handleLogout, auth }
}
```

- [ ] **Step 3: 修改 ShopList.vue**

改为全屏独立布局，顶部显示用户信息，主体为店铺选择卡片，底部为退出登录。

```html
<template>
  <div class="shop-select-page">
    <div class="shop-select-page__header">
      <div class="shop-select-page__user">
        <el-avatar :size="36">{{ auth.merchantName?.charAt(0)?.toUpperCase() || 'M' }}</el-avatar>
        <span class="shop-select-page__username">{{ auth.merchantName }}</span>
      </div>
    </div>

    <div class="shop-select-page__body">
      <h1 class="shop-select-page__title">{{ T.SELECT_TITLE }}</h1>

      <div v-loading="loading" class="shop-list__grid">
        <el-card
          v-for="shop in shops"
          :key="shop.id"
          shadow="hover"
          class="shop-card"
          @click="enterShop(shop.id)"
        >
          <div class="shop-card__header">
            <el-avatar :size="48" class="shop-card__avatar">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
            </el-avatar>
            <div class="shop-card__info">
              <h3 class="shop-card__name">{{ shop.name || `店铺 ${shop.id}` }}</h3>
              <span class="shop-card__id">ID: {{ shop.id }}</span>
            </div>
          </div>
          <div class="shop-card__footer">
            <el-button type="primary" size="default" @click.stop="enterShop(shop.id)">
              {{ T.BTN_ENTER }}
            </el-button>
          </div>
        </el-card>
      </div>

      <el-empty v-if="!loading && shops.length === 0" :description="T.EMPTY_TEXT">
        <el-button type="primary" @click="goRegister">{{ T.BTN_CREATE_NOW }}</el-button>
      </el-empty>
    </div>

    <div class="shop-select-page__footer">
      <el-button type="primary" @click="goRegister">{{ T.BTN_CREATE }}</el-button>
      <el-button @click="handleLogout">{{ T.BTN_LOGOUT }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { useShopList } from './ShopList.js'
const { shops, loading, T, enterShop, goRegister, handleLogout, auth } = useShopList()
</script>

<style scoped src="./ShopList.css"></style>
```

- [ ] **Step 4: 修改 ShopList.css**

添加全屏居中布局样式，保留原有卡片样式。

```css
.shop-select-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg, #f5f7fa);
}

.shop-select-page__header {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-4) var(--space-6);
}

.shop-select-page__user {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.shop-select-page__username {
  font-size: var(--text-base);
  color: var(--color-text);
}

.shop-select-page__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-8) var(--space-6);
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.shop-select-page__title {
  font-size: var(--text-2xl, 24px);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-8);
  text-align: center;
}

.shop-select-page__footer {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-6);
}

.shop-list__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-4);
  width: 100%;
}

.shop-card {
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.shop-card:hover {
  transform: translateY(-2px);
}

.shop-card__header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.shop-card__avatar {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.shop-card__info {
  flex: 1;
  min-width: 0;
}

.shop-card__name {
  font-size: var(--text-base);
  font-weight: 600;
  margin: 0 0 var(--space-1);
  color: var(--color-text);
}

.shop-card__id {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
}

.shop-card__footer {
  display: flex;
  justify-content: flex-end;
}
```

- [ ] **Step 5: 提交**

```bash
git add frontier-seller/src/views/ShopList/
git commit -m "feat(shop-select): ShopList 改造为全屏独立选择页"
```

---

### Task 4: Sidebar - 移除店铺列表菜单项

**Files:**
- Modify: `frontier-seller/src/layout/AppSidebar.vue`

- [ ] **Step 1: 移除"店铺列表" el-menu-item**

删除以下代码块（约第 18-21 行）：

```html
      <!-- 店铺列表 - 切换店铺 -->
      <el-menu-item v-if="!shop.hasNoShops" index="/shop/list">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
        <span>店铺列表</span>
      </el-menu-item>
```

同时更新 `v-if="shop.currentShopId"` 不需要条件（因为无法进入此布局时如果没有选中店铺）。

- [ ] **Step 2: 提交**

```bash
git add frontier-seller/src/layout/AppSidebar.vue
git commit -m "feat(sidebar): 移除店铺列表导航项"
```

---

### Task 5: TopBar - 添加切换店铺按钮，调整面包屑

**Files:**
- Modify: `frontier-seller/src/layout/AppTopBar.vue`

- [ ] **Step 1: 添加切换店铺按钮，移除店铺列表相关面包屑**

在右侧添加"切换店铺"按钮，移除面包屑中 `/shop/list` 和 "店铺管理" 相关项。

```html
<template>
  <header class="topbar">
    <div class="topbar__left">
      <el-button text @click="app.toggleSidebar">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </el-button>
      <el-breadcrumb v-if="!shop.hasNoShops" separator="/">
        <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="i">
          <router-link v-if="i < breadcrumbs.length - 1" :to="crumb.path">{{ crumb.label }}</router-link>
          <span v-else>{{ crumb.label }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="topbar__right">
      <el-button text @click="switchShop">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
        <span>切换店铺</span>
      </el-button>

      <div class="topbar__user">
        <el-avatar :size="34">{{ auth.merchantName?.charAt(0)?.toUpperCase() || 'M' }}</el-avatar>
        <span class="topbar__user-name">{{ auth.merchantName }}</span>
        <el-button text type="danger" @click="handleLogout">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          <span>退出</span>
        </el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useAppStore } from '@/store/app'
import { useShopStore } from '@/store/shop'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()
const shop = useShopStore()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}

function switchShop() {
  shop.clearCurrentShop()
  router.push('/shop/select')
}

const breadcrumbMap = {
  '/ship': '订单发货'
}

const breadcrumbs = computed(() => {
  const path = route.path
  const crumbs = []

  if (breadcrumbMap[path]) {
    crumbs.push({ label: breadcrumbMap[path], path: '' })
  } else if (path.startsWith('/shop/')) {
    const pageMap = {
      'products': '商品管理',
      'orders': '订单管理',
      'employees': '员工管理',
      'addresses': '地址管理',
      'returns': '退货管理',
      'info': '商店信息'
    }
    const segment = path.split('/').pop()
    if (pageMap[segment]) {
      crumbs.push({ label: pageMap[segment], path: '' })
    }
  }

  return crumbs
})
</script>
```

- [ ] **Step 2: 提交**

```bash
git add frontier-seller/src/layout/AppTopBar.vue
git commit -m "feat(topbar): 添加切换店铺按钮，调整面包屑"
```

---

### Task 6: AppLayout - 调整空店铺逻辑

**Files:**
- Modify: `frontier-seller/src/layout/AppLayout.vue`

- [ ] **Step 1: 调整 AppLayout 空店铺处理**

当前 AppLayout 在 `hasNoShops` 时隐藏侧边栏并显示空状态。这个逻辑基本保持不变，但需要移除 `watch` 中的 dialog 弹出逻辑（因为现在没有店铺时用户会停留在首页，不需要 dialog 弹窗了）。

简化 AppLayout，移除 dialog 和相关逻辑：

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
          <el-button type="primary" size="large" round @click="goRegister" style="margin-top: var(--space-4)">前往创建</el-button>
        </div>
        <router-view v-else />
      </div>
    </el-main>
  </div>
</template>

<script setup>
import { useShopStore } from '@/store/shop'
import { useAuthStore } from '@/store/auth'
import AppSidebar from './AppSidebar.vue'
import AppTopBar from './AppTopBar.vue'

const shop = useShopStore()
const auth = useAuthStore()

function goRegister() {
  window.open('/shop/register', '_blank')
}
</script>
```

样式保持不变。

- [ ] **Step 2: 提交**

```bash
git add frontier-seller/src/layout/AppLayout.vue
git commit -m "feat(layout): 简化空店铺处理，移除弹窗"
```

---

### Task 7: 验证

- [ ] **Step 1: 检查项目是否能正常启动**

```bash
cd frontier-seller
npm run dev
```

确认编译无报错。

- [ ] **Step 2: 流程测试**

验证以下流程：
1. 未登录 → 访问任意页面 → 重定向到 /login
2. 登录后无店铺 → 进入 AppLayout 显示空状态
3. 登录后单店铺 → 自动选中 → 进入 /shop/:shopId/products
4. 登录后多店铺 → 跳转 /shop/select → 选择店铺 → 进入功能页
5. 功能页点击「切换店铺」→ 跳转 /shop/select
6. 退出登录 → 回到 /login
