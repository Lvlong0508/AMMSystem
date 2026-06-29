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
        <router-link :to="`/shop/${shop.currentShopId}`" class="sidebar__item" :class="{ 'sidebar__item--active': route.path === `/shop/${shop.currentShopId}` }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          <span v-show="!app.sidebarCollapsed">概览</span>
        </router-link>

        <router-link :to="`/shop/${shop.currentShopId}/products`" class="sidebar__item" :class="{ 'sidebar__item--active': route.path.includes('/products') }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
          <span v-show="!app.sidebarCollapsed">商品管理</span>
        </router-link>

        <router-link to="/ship" class="sidebar__item" :class="{ 'sidebar__item--active': route.path === '/ship' }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
          <span v-show="!app.sidebarCollapsed">订单发货</span>
        </router-link>

        <div class="sidebar__group">
          <div class="sidebar__group-title" :class="{ 'sidebar__group-title--expanded': expandedGroups.has('orders') }" @click="toggleGroup('orders')" v-show="!app.sidebarCollapsed">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>
            <span>订单管理</span>
            <svg class="sidebar__arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </div>
          <div class="sidebar__sub-items" v-show="expandedGroups.has('orders') && !app.sidebarCollapsed">
            <router-link :to="`/shop/${shop.currentShopId}/orders`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/orders') }">
              <span>订单列表</span>
            </router-link>
            <router-link :to="`/shop/${shop.currentShopId}/returns`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/returns') }">
              <span>退货管理</span>
            </router-link>
          </div>
        </div>

        <router-link :to="`/shop/${shop.currentShopId}/knowledge`" class="sidebar__item" :class="{ 'sidebar__item--active': route.path.includes('/knowledge') }">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/><path d="M12 6v7"/><path d="M9 9h6"/></svg>
          <span v-show="!app.sidebarCollapsed">知识库管理</span>
        </router-link>

        <div class="sidebar__group">
          <div class="sidebar__group-title" :class="{ 'sidebar__group-title--expanded': expandedGroups.has('settings') }" @click="toggleGroup('settings')" v-show="!app.sidebarCollapsed">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
            <span>店铺设置</span>
            <svg class="sidebar__arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </div>
          <div class="sidebar__sub-items" v-show="expandedGroups.has('settings') && !app.sidebarCollapsed">
            <router-link :to="`/shop/${shop.currentShopId}/info`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/info') }">
              <span>商店信息</span>
            </router-link>
            <router-link :to="`/shop/${shop.currentShopId}/addresses`" class="sidebar__sub-item" :class="{ 'sidebar__item--active': route.path.includes('/addresses') }">
              <span>地址管理</span>
            </router-link>
          </div>
        </div>
      </template>
    </nav>
  </aside>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useShopStore } from '@/store/shop'
import { useAppStore } from '@/store/app'

const route = useRoute()
const shop = useShopStore()
const app = useAppStore()
const expandedGroups = reactive(new Set())

function toggleGroup(name) {
  if (expandedGroups.has(name)) {
    expandedGroups.delete(name)
  } else {
    expandedGroups.add(name)
  }
}

watch(() => route.path, (path) => {
  if (path.includes('/orders') || path.includes('/returns')) {
    expandedGroups.add('orders')
  }
  if (path.includes('/info') || path.includes('/addresses')) {
    expandedGroups.add('settings')
  }
})

watch(() => app.sidebarCollapsed, (collapsed) => {
  if (collapsed) {
    expandedGroups.clear()
  }
})
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
  cursor: pointer;
  user-select: none;
  border-radius: 8px;
  transition: all 0.15s;
}

.sidebar__group-title:hover {
  background: #f0f2ff;
  color: #4361ee;
}

.sidebar__group-title--expanded {
  color: #4361ee;
}

.sidebar__arrow {
  margin-left: auto;
  transition: transform 0.2s;
}

.sidebar__group-title--expanded .sidebar__arrow {
  transform: rotate(90deg);
}

.sidebar__sub-items {
  display: flex;
  flex-direction: column;
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
