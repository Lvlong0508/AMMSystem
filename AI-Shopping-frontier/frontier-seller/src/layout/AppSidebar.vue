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
      <template v-if="shop.currentShopId">
        <!-- 商品管理 -->
        <el-menu-item :index="`/shop/${shop.currentShopId}/products`">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
          <span>商品管理</span>
        </el-menu-item>

        <!-- 订单发货 - 独立入口 -->
        <el-menu-item index="/ship">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
          <span>订单发货</span>
        </el-menu-item>

        <!-- 订单管理（仅店主） -->
        <el-sub-menu v-if="auth.isOwner" index="order-group">
        <template #title>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
          <span>订单管理</span>
        </template>
        <el-menu-item :index="`/shop/${shop.currentShopId}/orders`">
          <span>订单列表</span>
        </el-menu-item>
        <el-menu-item :index="`/shop/${shop.currentShopId}/returns`">
          <span>退货管理</span>
        </el-menu-item>
      </el-sub-menu>

      <!-- 店铺设置（仅店主） -->
      <el-sub-menu v-if="auth.isOwner" index="shop-group">
        <template #title>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          <span>店铺设置</span>
        </template>
        <el-menu-item :index="`/shop/${shop.currentShopId}/info`">
          <span>商店信息</span>
        </el-menu-item>
        <el-menu-item :index="`/shop/${shop.currentShopId}/addresses`">
          <span>地址管理</span>
        </el-menu-item>
        <el-menu-item :index="`/shop/${shop.currentShopId}/employees`">
          <span>员工管理</span>
        </el-menu-item>
      </el-sub-menu>
      </template>
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

.sidebar__menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.sidebar__menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px var(--space-2);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
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
  height: 44px;
  line-height: 44px;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 2px var(--space-2);
  border-radius: var(--radius-md);
}


</style>
