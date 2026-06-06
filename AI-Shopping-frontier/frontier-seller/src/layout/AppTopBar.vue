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

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-5);
  background: white;
  border-bottom: 1px solid var(--color-border-light);
  min-height: 56px;
}

.topbar__left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.topbar__user {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.topbar__user-name {
  font-size: var(--text-base);
  color: var(--color-text);
}

:deep(.el-breadcrumb) {
  font-size: var(--text-base);
}
</style>
