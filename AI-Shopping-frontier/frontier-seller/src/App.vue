<!-- src/App.vue -->
<template>
  <div id="app" class="w-full h-screen flex flex-col">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-brand">AI-Mart-商家服务平台</div>
      <div class="nav-links">
        <router-link
          :class="['nav-link', { active: $route.path === '/ship' }]"
          to="/ship"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M13 16V6a1 1 0 00-1-1H4a1 1 0 00-1 1v10a1 1 0 001 1h1m8-1a1 1 0 01-1 1H9m4-1V8a1 1 0 011-1h2.586a1 1 0 01.707.293l3.414 3.414a1 1 0 01.293.707V16a1 1 0 01-1 1h-1m-6-1a1 1 0 001 1h1M5 17a2 2 0 104 0m-4 0a2 2 0 114 0m6 0a2 2 0 104 0m-4 0a2 2 0 114 0"></path>
          </svg>
          订单发货
        </router-link>
        <router-link
          v-if="isShopOwner"
          :class="['nav-link', { active: $route.path.startsWith('/shop') }]"
          to="/shop/list"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
          店铺管理
        </router-link>

        <!-- 用户信息和登出 -->
        <div v-if="isLoggedIn" class="user-section">
          <span class="user-name">👤 {{ merchantName }}</span>
          <button class="nav-link logout-btn" @click="handleLogout">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
            登出
          </button>
        </div>
        <router-link
          v-else
          :class="['nav-link', { active: $route.path === '/login' }]"
          to="/login"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"></path>
            <polyline points="10 17 15 12 10 7"></polyline>
            <line x1="15" y1="12" x2="3" y2="12"></line>
          </svg>
          登录
        </router-link>
      </div>
    </nav>

    <!-- 页面内容 -->
    <div class="flex-1 overflow-hidden bg-gray-50">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { merchantLogout } from './api/auth'

const router = useRouter()

// 登录状态
const isLoggedIn = computed(() => {
  return !!localStorage.getItem('satoken')
})

// 商家名称
const merchantName = computed(() => {
  const merchantInfo = localStorage.getItem('merchantInfo')
  if (merchantInfo) {
    try {
      const merchant = JSON.parse(merchantInfo)
      return merchant.nickname || merchant.username || '商家'
    } catch (e) {
      return '商家'
    }
  }
  return '商家'
})

// 是否是店长（至少有一个店铺的角色是1）
const isShopOwner = computed(() => {
  const roles = localStorage.getItem('merchantRoles')
  if (roles) {
    try {
      const roleList = JSON.parse(roles)
      return roleList.some(r => r.role === '1')
    } catch (e) {
      return false
    }
  }
  return false
})

// 登出处理
const handleLogout = async () => {
  const result = await Swal.fire({
    icon: 'question',
    title: '确认登出',
    text: '确定要退出登录吗？',
    showCancelButton: true,
    confirmButtonText: '确认登出',
    cancelButtonText: '取消',
    confirmButtonColor: '#f59e0b',
    cancelButtonColor: '#94a3b8'
  })

  if (result.isConfirmed) {
    try {
      await merchantLogout()
    } catch (e) {
      // 忽略 API 错误
    }
    // 清除本地存储
    localStorage.removeItem('satoken')
    localStorage.removeItem('merchantInfo')

    await Swal.fire({
      icon: 'success',
      title: '已登出',
      text: '期待您的再次光临',
      confirmButtonColor: '#f59e0b',
      timer: 1500,
      timerProgressBar: true
    })

    router.push('/login')
    window.location.reload()
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 导航栏样式 */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.nav-brand {
  font-size: 16px;
  font-weight: 600;
}

.nav-links {
  display: flex;
  gap: 8px;
}

.nav-link {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.icon {
  width: 16px;
  height: 16px;
}

.nav-link:hover {
  color: white;
  background: rgba(255, 255, 255, 0.15);
}

.nav-link.active {
  color: white;
  background: rgba(255, 255, 255, 0.25);
  font-weight: 500;
}

/* 用户区域样式 */
.user-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 8px;
  padding-left: 12px;
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}

.user-name {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}

.logout-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
  color: #fee2e2;
}
</style>
