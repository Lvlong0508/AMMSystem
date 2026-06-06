<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': app.sidebarCollapsed }">
    <div class="sidebar__header">
      <span class="sidebar__logo">AI-Mart</span>
    </div>

    <div v-if="shop.hasNoShops" class="sidebar__notice sidebar__notice--empty">
      <p>您还没有店铺</p>
      <el-button size="small" type="primary" round @click="goRegister">创建店铺</el-button>
    </div>
    <div v-else-if="shop.loaded && !shop.currentShopId" class="sidebar__notice">
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

      <!-- 商品管理 -->
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

function goRegister() {
  router.push('/shop/register')
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  background: white;
  border-right: 1px solid var(--color-border-light);
  transition: width var(--transition-fast);
  overflow: hidden;
}

.sidebar--collapsed { width: 64px; }

.sidebar__header {
  padding: var(--space-4) var(--space-5);
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--color-border-light);
}

.sidebar__logo {
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--color-primary);
  white-space: nowrap;
}

.sidebar__notice {
  padding: var(--space-4) var(--space-5);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  text-align: center;
}

.sidebar__notice--empty p {
  margin: 0 0 var(--space-3);
}

.sidebar__menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.sidebar__menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin: 1px var(--space-2);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.sidebar__menu :deep(.el-menu-item.is-active) {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 500;
  box-shadow: inset 2px 0 0 var(--color-primary);
}

.sidebar__menu :deep(.el-menu-item:hover) {
  background: var(--color-card);
}

.sidebar__menu :deep(.el-sub-menu__title) {
  height: 40px;
  line-height: 40px;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 1px var(--space-2);
  border-radius: var(--radius-md);
}

.sidebar__footer {
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-card);
}

.sidebar__user {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.sidebar__user-name {
  font-size: var(--text-sm);
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
