# 商家端流程重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构商家端登录后流程，引入全局店铺上下文，重构导航为两级分组，新增退货管理和商店信息页面

**Architecture:** 将店铺上下文从 Ship.vue 本地管理提升到全局 Pinia store，App.vue 初始化时自动加载 shops，路由守卫同步 shopId，侧栏按角色+店铺状态控制显隐

**Tech Stack:** Vue 3 (Composition API) + Pinia + Vue Router + Element Plus

**核心流程保障：** 本计划确保登录 → 角色判定 → 店铺加载 → 全局上下文 → 导航展示 → 现有功能正常工作的核心链路可运行。新页面先创建路由骨架和占位内容。

---

### Task 1: 增强 shop store — 全局上下文单例

**Files:**
- Modify: `src/store/shop.js` (entire file rewrite)

- [ ] **Step 1: Rewrite shop store with global initialization**

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShopByMerchant } from '@/api/shop'

export const useShopStore = defineStore('shop', () => {
  const currentShopId = ref(localStorage.getItem('currentShopId') || null)
  const shops = ref([])
  const loaded = ref(false)

  const currentShop = computed(() => shops.value.find(s => String(s.id) === String(currentShopId.value)))
  const hasMultipleShops = computed(() => shops.value.length > 1)

  async function initShops(merchantId) {
    if (!merchantId) return
    loaded.value = false
    try {
      const res = await getShopByMerchant(merchantId)
      const shopIds = res?.data?.shopIds || res?.shopIds || []
      shops.value = shopIds.map(id => ({ id, name: `店铺 ${id}` }))
      if (shops.value.length > 0 && !currentShopId.value) {
        currentShopId.value = String(shops.value[0].id)
        localStorage.setItem('currentShopId', currentShopId.value)
      }
      if (shops.value.length === 1) {
        currentShopId.value = String(shops.value[0].id)
        localStorage.setItem('currentShopId', currentShopId.value)
      }
    } catch (e) {
      console.error('初始化店铺失败:', e)
    } finally {
      loaded.value = true
    }
  }

  function switchShop(shopId) {
    currentShopId.value = String(shopId)
    localStorage.setItem('currentShopId', String(shopId))
  }

  return { currentShopId, shops, loaded, currentShop, hasMultipleShops, initShops, switchShop }
})
```

- [ ] **Step 2: Verify store structure**

思考确认：
- `initShops` 在 `shops.length === 1` 时自动设 `currentShopId` — 员工直达
- `shops.length > 1` 时保留用户已有选择 — 店主自由切换
- `switchShop` 联动 `localStorage` 持久化

---

### Task 2: 增强 auth store — 登录时设置 role

**Files:**
- Modify: `src/store/auth.js`

- [ ] **Step 1: Add role setup in login function**

```js
// 在 login 函数中，token 设置之后追加：
if (res.data?.role) {
  const roleObj = { role: res.data.role, shopId: null }
  currentRole.value = roleObj
  localStorage.setItem('currentRole', JSON.stringify(roleObj))
}
```

完整 `login` 函数修改后：

```js
async function login(credentials) {
  const res = await merchantLogin(credentials)
  if (res.data?.token) {
    token.value = res.data.token
    merchantInfo.value = res.data.merchantInfo
    localStorage.setItem('satoken', res.data.token)
    localStorage.setItem('merchantInfo', JSON.stringify(res.data.merchantInfo))
    if (res.data?.role) {
      const roleObj = { role: res.data.role, shopId: null }
      currentRole.value = roleObj
      localStorage.setItem('currentRole', JSON.stringify(roleObj))
    }
  }
  return res
}
```

---

### Task 3: 改造 App.vue — 登录后自动初始化 shops

**Files:**
- Modify: `src/App.vue`

- [ ] **Step 1: Add shop initialization on mount**

在 `<script setup>` 中追加：

```js
import { onMounted } from 'vue'
import { useShopStore } from '@/store/shop'

const shopStore = useShopStore()

onMounted(() => {
  if (authStore.isLoggedIn && authStore.merchantInfo?.id) {
    shopStore.initShops(authStore.merchantInfo.id)
  }
})
```

- [ ] **Step 2: Watch auth state changes**

```js
import { watch } from 'vue'

watch(() => authStore.isLoggedIn, (loggedIn) => {
  if (loggedIn && authStore.merchantInfo?.id) {
    shopStore.initShops(authStore.merchantInfo.id)
  }
})
```

完整 `script setup` 改为：

```vue
<script setup>
import { onMounted, watch } from 'vue'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import AppLayout from '@/layout/AppLayout.vue'

const authStore = useAuthStore()
const shopStore = useShopStore()

onMounted(() => {
  if (authStore.isLoggedIn && authStore.merchantInfo?.id) {
    shopStore.initShops(authStore.merchantInfo.id)
  }
})

watch(() => authStore.isLoggedIn, (loggedIn) => {
  if (loggedIn && authStore.merchantInfo?.id) {
    shopStore.initShops(authStore.merchantInfo.id)
  }
})
</script>
```

---

### Task 4: 增强路由守卫 + 添加新路由

**Files:**
- Modify: `src/router/index.js`

- [ ] **Step 1: Add new route imports**

```js
import ShopReturns from '../views/ReturnManagement/ReturnManagement.vue'
import ShopInfo from '../views/ShopInfo/ShopInfo.vue'
```

- [ ] **Step 2: Add new routes**

在 `routes` 数组中追加：

```js
{
  path: '/shop/:shopId/returns',
  name: 'shop-returns',
  component: ShopReturns
},
{
  path: '/shop/:shopId/info',
  name: 'shop-info',
  component: ShopInfo,
  meta: { shopOwnerOnly: true }
}
```

- [ ] **Step 3: Enhance beforeEach guard**

在 `router.beforeEach` 中追加 shopId 同步逻辑：

```js
router.beforeEach((to, from, next) => {
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

  // 同步 shopId 路由参数到 store
  if (to.params.shopId) {
    shop.switchShop(to.params.shopId)
  }

  next()
})
```

---

### Task 5: 重构 AppSidebar.vue — 两级分组菜单

**Files:**
- Modify: `src/layout/AppSidebar.vue` (template + script + style)

- [ ] **Step 1: Rewrite template**

```vue
<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': app.sidebarCollapsed }">
    <div class="sidebar__header">
      <span class="sidebar__logo">AI-Mart</span>
    </div>

    <div v-if="!shop.currentShopId" class="sidebar__notice">
      请先选择店铺
    </div>

    <el-menu
      :default-active="route.path"
      router
      :collapse="app.sidebarCollapsed"
      class="sidebar__menu"
    >
      <!-- 订单管理分组 -->
      <el-sub-menu index="order-group">
        <template #title>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
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

      <!-- 商品管理分组 -->
      <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/products`">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        <span>商品管理</span>
      </el-menu-item>

      <!-- 店铺管理分组（仅店主） -->
      <el-sub-menu v-if="auth.isOwner" index="shop-group">
        <template #title>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
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

    <div class="sidebar__footer">
      <div class="sidebar__user">
        <el-avatar :size="28">{{ auth.merchantName?.charAt(0)?.toUpperCase() || 'M' }}</el-avatar>
        <span v-show="!app.sidebarCollapsed" class="sidebar__user-name">{{ auth.merchantName }}</span>
      </div>
      <el-button text type="danger" size="small" @click="handleLogout">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        <span v-show="!app.sidebarCollapsed">退出</span>
      </el-button>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Update script — 移除旧的店铺列表入口，改用全局 shop store**

```vue
<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import { useAppStore } from '@/store/app'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const shop = useShopStore()
const app = useAppStore()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>
```

- [ ] **Step 3: Add notice style**

在 `<style>` 中追加：

```css
.sidebar__notice {
  padding: var(--space-4) var(--space-5);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  text-align: center;
}
```

---

### Task 6: 增强 AppTopBar.vue — 店铺选择器联动全局 store

**Files:**
- Modify: `src/layout/AppTopBar.vue`

- [ ] **Step 1: Update to use global shop store instead of local ref**

```vue
<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useShopStore } from '@/store/shop'

const route = useRoute()
const app = useAppStore()
const shop = useShopStore()

const selectedShopId = computed({
  get: () => shop.currentShopId,
  set: (val) => { if (val) shop.switchShop(val) }
})

const breadcrumbMap = {
  '/ship': '订单发货',
  '/shop/list': '店铺列表',
  '/shop/register': '创建店铺'
}

const breadcrumbs = computed(() => {
  const path = route.path
  const crumbs = []

  if (path.startsWith('/shop/')) {
    crumbs.push({ label: '店铺管理', path: '/shop/list' })
    const shopId = route.params.shopId
    if (shopId) {
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
    } else if (breadcrumbMap[path]) {
      crumbs.push({ label: breadcrumbMap[path], path: '' })
    }
  } else if (breadcrumbMap[path]) {
    crumbs.push({ label: breadcrumbMap[path], path: '' })
  }

  return crumbs
})
</script>
```

- [ ] **Step 2: Update template — 移除未使用的 watch 和 onShopChange**

保留 `el-select` 绑定到新的 `selectedShopId` computed：

```vue
<el-select v-if="shop.hasMultipleShops" v-model="selectedShopId" size="small" style="width: 140px">
  <el-option v-for="s in shop.shops" :key="s.id" :label="`店铺 ${s.id}`" :value="s.id" />
</el-select>
```

---

### Task 7: 改造 Ship.vue — 移除非全局的店铺管理逻辑

**Files:**
- Modify: `src/views/Ship/Ship.js`
- Modify: `src/views/Ship/Ship.vue`

- [ ] **Step 1: Rewrite Ship.js — 移除本地 shops/currentShopId，改用 shopStore**

```js
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { shipOrder, getOrderListByShop, getOrderDetail } from '@/api/order'
import { getShipDefaultAddress } from '@/api/contact'
import { useShopStore } from '@/store/shop'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useShip() {
  const shopStore = useShopStore()
  const orders = ref([])
  const loading = ref(false)
  const filterStatus = ref('')
  const searchCustomer = ref('')

  const contacts = ref([])
  const contactsLoading = ref(false)

  const detailVisible = ref(false)
  const selectedOrder = ref(null)
  const detailLoading = ref(false)

  const shipVisible = ref(false)
  const shipForm = ref({
    orderId: '',
    trackingNumber: '',
    shippingDate: '',
    selectedContactId: null
  })
  const shipping = ref(false)

  const pendingShipCount = computed(() =>
    orders.value.filter(o => o.orderStatus === ORDER_STATUS.PAID).length
  )

  async function loadOrders() {
    if (!shopStore.currentShopId) {
      orders.value = []
      return
    }
    loading.value = true
    try {
      const res = await getOrderListByShop(shopStore.currentShopId)
      let orderList = res?.data || res?.orders || []
      if (filterStatus.value) {
        orderList = orderList.filter(o => o.orderStatus === filterStatus.value)
      }
      orders.value = orderList.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
    } catch (error) {
      console.error('加载订单失败:', error)
      ElMessage.error('加载订单失败')
    } finally {
      loading.value = false
    }
  }

  async function handleSearch() {
    await loadOrders()
    if (searchCustomer.value.trim()) {
      const keyword = searchCustomer.value.trim().toLowerCase()
      orders.value = orders.value.filter(order =>
        order.contact?.name?.toLowerCase().includes(keyword)
      )
    }
  }

  function getStatusType(status) {
    const map = {
      PENDING: 'info', PAID: 'warning', SHIPPED: 'primary',
      DELIVERED: 'success', CANCELLED: 'danger', RETURNED: 'danger'
    }
    return map[status] || 'info'
  }

  function getStatusText(status) { return STATUS_TEXT[status] || status }
  function formatPrice(price) { return price != null ? `¥${Number(price).toFixed(2)}` : '-' }
  function formatDate(dateStr) { return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-' }

  function getContactText(contact) {
    return contact ? `${contact.name} ${contact.phone}` : '-'
  }

  async function loadContacts() {
    contactsLoading.value = true
    try {
      const res = await getShipDefaultAddress()
      if (res?.data) {
        contacts.value = Array.isArray(res.data) ? res.data : [res.data]
        if (contacts.value.length > 0 && !shipForm.value.selectedContactId) {
          shipForm.value.selectedContactId = contacts.value[0].id
        }
      } else {
        contacts.value = []
      }
    } catch (error) {
      console.error('加载联系人失败:', error)
      ElMessage.error('加载联系人失败')
      contacts.value = []
    } finally {
      contactsLoading.value = false
    }
  }

  async function showOrderDetail(order) {
    selectedOrder.value = order
    detailVisible.value = true
    if (shopStore.currentShopId) {
      detailLoading.value = true
      try {
        const res = await getOrderDetail(shopStore.currentShopId, order.orderId)
        if (res?.data) selectedOrder.value = { ...order, ...res.data }
      } catch (error) {
        console.error('获取订单详情失败:', error)
      } finally {
        detailLoading.value = false
      }
    }
  }

  function closeDetail() {
    detailVisible.value = false
    selectedOrder.value = null
  }

  async function showShipDialog(order) {
    shipForm.value = {
      orderId: order.orderId,
      trackingNumber: '',
      shippingDate: new Date().toISOString().slice(0, 16),
      selectedContactId: null
    }
    selectedOrder.value = order
    shipVisible.value = true
    await loadContacts()
  }

  function closeShipDialog() { shipVisible.value = false }

  async function handleShip() {
    if (!shipForm.value.trackingNumber.trim()) {
      ElMessage.warning('请输入物流单号')
      return
    }
    if (!shipForm.value.selectedContactId) {
      ElMessage.warning('请选择发货地址')
      return
    }
    shipping.value = true
    try {
      const res = await shipOrder(shipForm.value.orderId, {
        trackingNumber: shipForm.value.trackingNumber,
        contactId: shipForm.value.selectedContactId,
        shippingDate: shipForm.value.shippingDate || undefined
      })
      if (res?.message?.includes('成功')) {
        ElMessage.success('发货成功')
        closeShipDialog()
        await loadOrders()
      } else {
        ElMessage.error(res?.message || '发货失败')
      }
    } catch (error) {
      console.error('发货失败:', error)
      ElMessage.error('发货失败')
    } finally {
      shipping.value = false
    }
  }

  onMounted(async () => {
    if (shopStore.currentShopId) await loadOrders()
  })

  return {
    T, orders, loading, filterStatus, searchCustomer, pendingShipCount,
    contacts, contactsLoading,
    detailVisible, detailLoading, selectedOrder, shipVisible, shipForm, shipping,
    ORDER_STATUS, loadOrders, handleSearch, getStatusType, getStatusText,
    formatPrice, formatDate, getContactText, showOrderDetail, closeDetail,
    showShipDialog, closeShipDialog, handleShip
  }
}
```

- [ ] **Step 2: Update Ship.vue template — 移除本地 shops 和 switchShop 相关**

移除：
- `v-if="hasMultipleShops"` 的店铺下拉框（已移到 AppTopBar）
- `currentShopId` 相关绑定

---

### Task 8: 创建退货管理页面（骨架）

**Files:**
- Create: `src/views/ReturnManagement/ReturnManagement.vue`
- Create: `src/views/ReturnManagement/ReturnManagement.js`
- Create: `src/views/ReturnManagement/Text.js`
- Create: `src/views/ReturnManagement/ReturnManagement.css`

- [ ] **Step 1: Create Text.js**

```js
export const PAGE_TITLE = '退货管理'
export const BTN_REFRESH = '刷新'
export const EMPTY_TEXT = '暂无退货订单'
export const LABEL_ORDER_ID = '订单编号'
export const LABEL_DATE = '下单时间'
export const LABEL_PRODUCT = '商品'
export const LABEL_QUANTITY = '数量'
export const LABEL_TOTAL = '总价'
export const LABEL_STATUS = '状态'
export const LABEL_ACTIONS = '操作'
export const BTN_DETAIL = '详情'
export const BTN_APPROVE = '审核通过'
export const BTN_CONFIRM = '确认退货'
export const LABEL_TRACKING = '物流单号'
export const DIALOG_DETAIL = '订单详情'
```

- [ ] **Step 2: Create ReturnManagement.js**

```js
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderListByShop, getOrderDetail, approveReturn, confirmReturn } from '@/api/order'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useReturnManagement() {
  const route = useRoute()
  const shopId = route.params.shopId
  const orders = ref([])
  const loading = ref(false)
  const detailVisible = ref(false)
  const selectedOrder = ref(null)
  const filterStatus = ref('')

  async function loadOrders() {
    loading.value = true
    try {
      const res = await getOrderListByShop(shopId)
      let list = res?.data || res?.orders || []
      if (filterStatus.value) {
        list = list.filter(o => o.orderStatus === filterStatus.value)
      }
      orders.value = list.filter(o =>
        o.orderStatus === ORDER_STATUS.RETURNED ||
        o.orderStatus === 'RETURN_REQUESTED' ||
        o.orderStatus === 'RETURN_APPROVED'
      )
    } catch (e) {
      console.error('加载退货订单失败:', e)
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  async function handleApprove(order) {
    try {
      const res = await approveReturn(order.orderId, shopId)
      ElMessage.success(res?.message || '审核通过')
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  async function handleConfirm(order) {
    try {
      const res = await confirmReturn(order.orderId, shopId)
      ElMessage.success(res?.message || '确认成功')
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  async function showDetail(order) {
    try {
      const res = await getOrderDetail(shopId, order.orderId)
      selectedOrder.value = res?.data || order
      detailVisible.value = true
    } catch (e) {
      ElMessage.error('获取详情失败')
    }
  }

  function getStatusType(status) {
    const map = {
      RETURN_REQUESTED: 'warning',
      RETURN_APPROVED: 'primary',
      RETURNED: 'success'
    }
    return map[status] || 'info'
  }

  function getStatusText(status) { return STATUS_TEXT[status] || status }
  function formatDate(dateStr) { return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-' }
  function formatPrice(price) { return price != null ? `¥${Number(price).toFixed(2)}` : '-' }

  onMounted(loadOrders)

  return {
    T, orders, loading, detailVisible, selectedOrder, filterStatus,
    loadOrders, handleApprove, handleConfirm, showDetail,
    getStatusType, getStatusText, formatDate, formatPrice
  }
}
```

- [ ] **Step 3: Create ReturnManagement.vue**

```vue
<template>
  <div class="return-management">
    <div class="return-management__toolbar">
      <h2 class="return-management__title">{{ T.PAGE_TITLE }}</h2>
      <el-button size="small" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="orders" v-loading="loading" stripe border size="small" style="width: 100%">
        <el-table-column prop="orderId" :label="T.LABEL_ORDER_ID" min-width="160" />
        <el-table-column :label="T.LABEL_DATE" min-width="160">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_PRODUCT" min-width="120">
          <template #default="{ row }">{{ row.productName || `商品 #${row.productId}` }}</template>
        </el-table-column>
        <el-table-column prop="quantity" :label="T.LABEL_QUANTITY" width="80" />
        <el-table-column :label="T.LABEL_TOTAL" width="100">
          <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_STATUS" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" size="small">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="T.LABEL_ACTIONS" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">{{ T.BTN_DETAIL }}</el-button>
            <el-button v-if="row.orderStatus === 'RETURN_REQUESTED'" text type="warning" size="small" @click="handleApprove(row)">{{ T.BTN_APPROVE }}</el-button>
            <el-button v-if="row.orderStatus === 'RETURN_APPROVED'" text type="success" size="small" @click="handleConfirm(row)">{{ T.BTN_CONFIRM }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && orders.length === 0" :description="T.EMPTY_TEXT" />
    </el-card>
  </div>
</template>

<script setup>
import { useReturnManagement } from './ReturnManagement.js'
const { T, orders, loading, detailVisible, selectedOrder, filterStatus, loadOrders, handleApprove, handleConfirm, showDetail, getStatusType, getStatusText, formatDate, formatPrice } = useReturnManagement()
</script>

<style scoped src="./ReturnManagement.css"></style>
```

- [ ] **Step 4: Create ReturnManagement.css**

```css
.return-management__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.return-management__title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
}
```

---

### Task 9: 创建商店信息管理页面（骨架）

**Files:**
- Create: `src/views/ShopInfo/ShopInfo.vue`
- Create: `src/views/ShopInfo/ShopInfo.js`
- Create: `src/views/ShopInfo/Text.js`
- Create: `src/views/ShopInfo/ShopInfo.css`

- [ ] **Step 1: Create Text.js**

```js
export const PAGE_TITLE = '商店信息'
export const BTN_SAVE = '保存修改'
export const BTN_SAVING = '保存中...'
export const BTN_OPEN = '开店'
export const BTN_CLOSE = '关店'
export const LABEL_NAME = '商店名称'
export const LABEL_DESC = '商店简介'
export const LABEL_PHONE = '联系电话'
export const LABEL_ADDRESS = '地址'
export const LABEL_HOURS = '营业时间'
export const PLACEHOLDER_NAME = '请输入商店名称'
export const PLACEHOLDER_DESC = '请输入商店简介'
export const PLACEHOLDER_PHONE = '请输入联系电话'
export const PLACEHOLDER_ADDRESS = '请输入商店地址'
export const PLACEHOLDER_HOURS = '例如：09:00-22:00'
export const LOAD_FAILED = '加载商店信息失败'
export const SAVE_SUCCESS = '保存成功'
export const SAVE_FAILED = '保存失败'
export const STATUS_OPENED = '营业中'
export const STATUS_CLOSED = '已关闭'
export const STATUS_TOGGLED = '状态切换成功'
```

- [ ] **Step 2: Create ShopInfo.js**

```js
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShopDetail, updateShop, closeShop, openShop } from '@/api/shop'
import * as T from './Text.js'

export function useShopInfo() {
  const route = useRoute()
  const shopId = route.params.shopId
  const loading = ref(false)
  const saving = ref(false)
  const toggling = ref(false)
  const shopStatus = ref(null)

  const form = reactive({
    name: '',
    description: '',
    phone: '',
    address: '',
    businessHours: ''
  })

  async function loadShopInfo() {
    loading.value = true
    try {
      const res = await getShopDetail(shopId)
      const shop = res?.data?.shop || res?.data || {}
      form.name = shop.name || ''
      form.description = shop.description || ''
      form.phone = shop.phone || ''
      form.address = shop.address || ''
      form.businessHours = shop.businessHours || ''
      shopStatus.value = shop.status
    } catch (e) {
      console.error('加载商店信息失败:', e)
      ElMessage.error(T.LOAD_FAILED)
    } finally {
      loading.value = false
    }
  }

  async function handleSave() {
    saving.value = true
    try {
      const res = await updateShop(shopId, { ...form })
      ElMessage.success(res?.message || T.SAVE_SUCCESS)
    } catch (e) {
      ElMessage.error(T.SAVE_FAILED)
    } finally {
      saving.value = false
    }
  }

  async function handleToggleStatus() {
    toggling.value = true
    try {
      if (shopStatus.value === 1) {
        await closeShop(shopId)
        shopStatus.value = 0
      } else {
        await openShop(shopId)
        shopStatus.value = 1
      }
      ElMessage.success(T.STATUS_TOGGLED)
    } catch (e) {
      ElMessage.error('操作失败')
    } finally {
      toggling.value = false
    }
  }

  onMounted(loadShopInfo)

  return { T, form, loading, saving, toggling, shopStatus, handleSave, handleToggleStatus }
}
```

- [ ] **Step 3: Create ShopInfo.vue**

```vue
<template>
  <div class="shop-info">
    <div class="shop-info__toolbar">
      <h2 class="shop-info__title">{{ T.PAGE_TITLE }}</h2>
      <el-tag v-if="shopStatus === 1" type="success" size="small">{{ T.STATUS_OPENED }}</el-tag>
      <el-tag v-else-if="shopStatus === 0" type="danger" size="small">{{ T.STATUS_CLOSED }}</el-tag>
    </div>

    <el-card v-loading="loading" shadow="never">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :placeholder="T.PLACEHOLDER_NAME" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="T.PLACEHOLDER_DESC" :maxlength="500" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE">
          <el-input v-model="form.phone" :placeholder="T.PLACEHOLDER_PHONE" :maxlength="20" />
        </el-form-item>
        <el-form-item :label="T.LABEL_ADDRESS">
          <el-input v-model="form.address" :placeholder="T.PLACEHOLDER_ADDRESS" :maxlength="200" />
        </el-form-item>
        <el-form-item :label="T.LABEL_HOURS">
          <el-input v-model="form.businessHours" :placeholder="T.PLACEHOLDER_HOURS" :maxlength="50" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            {{ saving ? T.BTN_SAVING : T.BTN_SAVE }}
          </el-button>
          <el-button :type="shopStatus === 1 ? 'danger' : 'success'" :loading="toggling" @click="handleToggleStatus">
            {{ shopStatus === 1 ? T.BTN_CLOSE : T.BTN_OPEN }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { useShopInfo } from './ShopInfo.js'
const { T, form, loading, saving, toggling, shopStatus, handleSave, handleToggleStatus } = useShopInfo()
</script>

<style scoped src="./ShopInfo.css"></style>
```

- [ ] **Step 4: Create ShopInfo.css**

```css
.shop-info__toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.shop-info__title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
}
```

---

### Task 10: 扩展 orderStatus 配置

**Files:**
- Modify: `src/config/orderStatus.js`

- [ ] **Step 1: Add return-related order status constants**

```js
RETURN_REQUESTED: 'RETURN_REQUESTED',
RETURN_APPROVED: 'RETURN_APPROVED',
```

在 `STATUS_TEXT` 中追加：

```js
[ORDER_STATUS.RETURN_REQUESTED]: '退货申请中',
[ORDER_STATUS.RETURN_APPROVED]: '退货审核通过',
```

在 `STATUS_CLASS` 中追加：

```js
[ORDER_STATUS.RETURN_REQUESTED]: 'status-return-requested',
[ORDER_STATUS.RETURN_APPROVED]: 'status-return-approved',
```

---

### Task 11: 调整 ShopList.vue — 新增"进入管理"按钮

**Files:**
- Modify: `src/views/ShopList/ShopList.js`

- [ ] **Step 1: Add enterManagement function**

在 `goToAddresses` 之后追加：

```js
function enterShop(shopId) {
  shopStore.switchShop(shopId)
  router.push('/ship')
}
```

返回中追加 `enterShop`。

- [ ] **Step 2: Update template — 在卡片操作区添加"进入管理"按钮**

在 `<script setup>` 中导入 shop store：

```js
import { useShopStore } from '@/store/shop'
const shopStore = useShopStore()
```

在 `goToAddresses` 按钮后追加：

```html
<el-button size="small" type="primary" @click="enterShop(shop.id)">进入管理</el-button>
```

---

### Task 12: 配置 api/index.js 导出新增页面需要的函数

**Files:**
- Verify: `src/api/index.js` 是否已包含 `approveReturn`、`confirmReturn`

- [ ] **Step 1: Verify or add missing exports**

```js
// Order
export { getOrderListByShop, getOrderDetail, shipOrder, approveReturn, confirmReturn } from './order'
```

---

## 自审查

**Spec coverage check:**
- Task 1 → 第3节 shop store 增强
- Task 2 → 第2.2节 角色设置
- Task 3 → 第3.2节 App.vue 初始化
- Task 4 → 第3.3节 路由守卫 + 第7节 路由表
- Task 5 → 第4节 导航栏重构
- Task 6 → 第3节 店铺选择器
- Task 7 → 第5.1节 Ship.vue 改造
- Task 8 → 第6.1节 退货管理
- Task 9 → 第6.3节 商店信息
- Task 10 → 第6.2节 退货状态
- Task 11 → 第5.2节 ShopList 改造
- Task 12 → 第1节 API 导出

**Placeholder scan:** 所有步骤均包含完整代码，无 TBD/TODO。

**Type consistency:** shopStore.switchShop 统一接收 shopId，内部转为 String 存储，所有调用处保持一致。
