<template>
  <header class="topbar">
    <div class="topbar__left">
      <span class="topbar__logo">AI Shopping 商店控制台</span>
    </div>
    <div class="topbar__right">
      <div class="topbar__user">
        <span class="topbar__user-avatar">{{ auth.merchantName?.charAt(0)?.toUpperCase() || 'M' }}</span>
        <span class="topbar__user-name">{{ auth.merchantName }}</span>
      </div>
      <el-button class="topbar__logout-btn" @click="handleLogout">[ 退出 ]</el-button>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const auth = useAuthStore()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: #1a1a2e;
  color: #fff;
  flex-shrink: 0;
}

.topbar__left {
  display: flex;
  align-items: center;
}

.topbar__logo {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.02em;
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar__user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topbar__user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #4361ee;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.topbar__user-name {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.topbar__logout-btn {
  color: rgba(255, 255, 255, 0.7) !important;
  font-size: 13px;
  border: none !important;
  background: transparent !important;
}

.topbar__logout-btn:hover {
  color: #fff !important;
}
</style>
