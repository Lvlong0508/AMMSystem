<template>
  <div class="sidebar" :class="{ 'sidebar--collapsed': app.sidebarCollapsed }">
    <div class="sidebar__header">
      <span class="sidebar__logo">AI-Mart</span>
    </div>

    <el-menu
      :default-active="route.path"
      router
      :collapse="app.sidebarCollapsed"
      class="sidebar__menu"
    >
      <el-menu-item index="/ship">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 16V6a1 1 0 0 0-1-1H4a1 1 0 0 0-1 1v10l2 1m9-1a1 1 0 0 1-1 1H9m4-1V8a1 1 0 0 1 1-1h2.6a1 1 0 0 1 .7.3l3.4 3.4a1 1 0 0 1 .3.7V16a1 1 0 0 1-1 1h-1m-6-1a2 2 0 1 1-4 0m4 0a2 2 0 1 0-4 0"/></svg>
        <span>订单发货</span>
      </el-menu-item>

      <el-sub-menu v-if="auth.isOwner" index="shop">
        <template #title>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          <span>店铺管理</span>
        </template>
        <el-menu-item index="/shop/list">店铺列表</el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/products`">商品管理</el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/orders`">订单管理</el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/employees`">员工管理</el-menu-item>
        <el-menu-item v-if="shop.currentShopId" :index="`/shop/${shop.currentShopId}/addresses`">地址管理</el-menu-item>
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
import { computed } from 'vue'
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
