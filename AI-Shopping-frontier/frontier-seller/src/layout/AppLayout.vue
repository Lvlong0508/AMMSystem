<template>
  <div class="app-shell">
    <!-- 顶部导航栏（全宽深色背景） -->
    <AppTopBar />

    <!-- 下方主体区域 -->
    <div class="app-body">
      <!-- 左侧导航 -->
      <AppSidebar v-if="shop.currentShopId" />
      <!-- 右侧内容区 -->
      <main class="app-main">
        <div class="app-content">
          <div v-if="shop.loaded && !shop.currentShopId" class="app-empty">
            <div class="app-empty__icon">
              <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="0.8"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
            </div>
            <h2 class="app-empty__title">您还没有店铺</h2>
            <p class="app-empty__desc">创建店铺后即可管理商品和订单</p>
            <el-button type="primary" size="large" @click="goRegister" style="margin-top: 20px">前往创建</el-button>
          </div>
          <router-view v-else />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useShopStore } from '@/store/shop'
import AppSidebar from './AppSidebar.vue'
import AppTopBar from './AppTopBar.vue'

const router = useRouter()
const shop = useShopStore()

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  background: #f5f6fa;
}

.app-content {
  width: 100%;
  max-width: 1200px;
  padding: 24px;
}

.app-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
}

.app-empty__icon {
  margin-bottom: 16px;
}

.app-empty__title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px;
}

.app-empty__desc {
  font-size: 15px;
  color: #999;
  margin: 0;
}
</style>
