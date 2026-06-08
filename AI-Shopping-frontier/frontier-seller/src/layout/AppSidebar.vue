<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': app.sidebarCollapsed }">
    <div class="sidebar__header">
      <button class="sidebar__toggle" @click="app.toggleSidebar">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </button>
      <span v-show="!app.sidebarCollapsed" class="sidebar__title">导航菜单</span>
    </div>

    <nav class="sidebar__menu">
      <template v-if="shop.currentShopId">
        <router-link :to="`/shop/${shop.currentShopId}/products`" class="sidebar__item" :class="{ 'sidebar__item--active': route.path.includes('/products') }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
          <span v-show="!app.sidebarCollapsed">商品管理</span>
        </router-link>

        <router-link to="/ship" class="sidebar__item" :class="{ 'sidebar__item--active': route.path === '/ship' }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
          <span v-show="!app.sidebarCollapsed">订单发货</span>
        </router-link>

        <div class="sidebar__group">
          <div class="sidebar__group-title" v-show="!app.sidebarCollapsed">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
            <span>订单管理</span>
          </div>
          <router-link :to="`/shop/${shop.currentShopId}/orders`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/orders') }">
            <span v-show="!app.sidebarCollapsed">订单列表</span>
          </router-link>
          <router-link :to="`/shop/${shop.currentShopId}/returns`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/returns') }">
            <span v-show="!app.sidebarCollapsed">退货管理</span>
          </router-link>
        </div>

        <div class="sidebar__group">
          <div class="sidebar__group-title" v-show="!app.sidebarCollapsed">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
            <span>店铺设置</span>
          </div>
          <router-link :to="`/shop/${shop.currentShopId}/info`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/info') }">
            <span v-show="!app.sidebarCollapsed">商店信息</span>
          </router-link>
          <router-link :to="`/shop/${shop.currentShopId}/addresses`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/addresses') }">
            <span v-show="!app.sidebarCollapsed">地址管理</span>
          </router-link>
        </div>
      </template>
    </nav>
  </aside>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useShopStore } from '@/store/shop'
import { useAppStore } from '@/store/app'

const route = useRoute()
const shop = useShopStore()
const app = useAppStore()
</script>

<style scoped>
.sidebar {
  width: 200px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  transition: width 0.2s;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar--collapsed { width: 56px; }

.sidebar__header {
  height: 48px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.sidebar__toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #666;
  display: flex;
  align-items: center;
}

.sidebar__toggle:hover { color: #333; }

.sidebar__title {
  font-size: 14px;
  font-weight: 600;
  color: #999;
  white-space: nowrap;
}

.sidebar__menu {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.sidebar__item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 44px;
  padding: 0 16px;
  margin: 2px 8px;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s;
}

.sidebar__item:hover {
  background: #f0f2ff;
  color: #4361ee;
}

.sidebar__item--active {
  background: #eef0ff;
  color: #4361ee;
  font-weight: 600;
}

.sidebar__group {
  margin: 4px 0;
}

.sidebar__group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 16px;
  margin: 2px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #999;
  white-space: nowrap;
}

.sidebar__sub-item {
  display: flex;
  align-items: center;
  height: 38px;
  padding: 0 16px 0 42px;
  margin: 1px 8px;
  border-radius: 8px;
  font-size: 13px;
  color: #555;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s;
}

.sidebar__sub-item:hover {
  background: #f0f2ff;
  color: #4361ee;
}

.sidebar__sub-item.sidebar__item--active {
  background: #eef0ff;
  color: #4361ee;
  font-weight: 600;
}
</style>
