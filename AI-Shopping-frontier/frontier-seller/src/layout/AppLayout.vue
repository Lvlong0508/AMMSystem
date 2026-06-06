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

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useShopStore } from '@/store/shop'
import { useAuthStore } from '@/store/auth'
import AppSidebar from './AppSidebar.vue'
import AppTopBar from './AppTopBar.vue'

const route = useRoute()
const shop = useShopStore()
const auth = useAuthStore()

function refreshShopsOnVisible() {
  if (document.visibilityState === 'visible' && shop.hasNoShops) {
    if (auth.merchantId) shop.initShops(auth.merchantId)
  }
}

onMounted(() => document.addEventListener('visibilitychange', refreshShopsOnVisible))
onUnmounted(() => document.removeEventListener('visibilitychange', refreshShopsOnVisible))

const showNoShopDialog = ref(false)

watch(() => shop.hasNoShops, (val) => {
  showNoShopDialog.value = val && !route.path.startsWith('/shop/register')
}, { immediate: true })

watch(() => route.path, () => {
  showNoShopDialog.value = shop.hasNoShops && !route.path.startsWith('/shop/register')
})

function goRegister() {
  window.open('/shop/register', '_blank')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 0;
  overflow: hidden;
  background: var(--color-bg);
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
}

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

</style>

<style>
.no-shop-dialog__body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--space-4) 0;
}

.no-shop-dialog__icon {
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-5);
}

.no-shop-dialog__title {
  font-size: var(--text-xl);
  font-weight: 600;
  margin: 0 0 var(--space-2);
  color: var(--color-text);
}

.no-shop-dialog__desc {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin: 0;
}
</style>
