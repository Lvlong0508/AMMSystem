<template>
  <header class="topbar">
    <div class="topbar__left">
      <el-button text @click="app.toggleSidebar">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="i">
          <router-link v-if="i < breadcrumbs.length - 1" :to="crumb.path">{{ crumb.label }}</router-link>
          <span v-else>{{ crumb.label }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="topbar__right">
      <el-select v-if="shop.shops.length > 1" v-model="selectedShopId" size="small" @change="onShopChange">
        <el-option v-for="s in shop.shops" :key="s.id" :label="`店铺 ${s.id}`" :value="s.id" />
      </el-select>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useShopStore } from '@/store/shop'

const route = useRoute()
const app = useAppStore()
const shop = useShopStore()

const selectedShopId = ref(shop.currentShopId)

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
        'addresses': '地址管理'
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

watch(() => shop.currentShopId, (val) => {
  selectedShopId.value = val
})

function onShopChange() {
  shop.switchShop(selectedShopId.value)
}
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-4);
  background: white;
  border-bottom: 1px solid var(--color-border-light);
  min-height: 48px;
}

.topbar__left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.topbar__right {
  display: flex;
  align-items: center;
}
</style>
