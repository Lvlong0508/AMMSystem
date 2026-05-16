<!-- src/App.vue -->
<template>
  <div id="app" class="w-full h-screen flex flex-col">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-brand">🛒 AI-Mart 智能购物平台</div>
      <div class="nav-links">
        <router-link 
          :class="['nav-link', { active: $route.path === '/' }]" 
          to="/"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
          </svg>
          AI 聊天
        </router-link>
        <router-link 
          :class="['nav-link', { active: $route.path === '/contact' }]" 
          to="/contact"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
          地址
        </router-link>
        <router-link 
          :class="['nav-link', { active: $route.path === '/order' }]" 
          to="/order"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"></path>
          </svg>
          订单
        </router-link>

        <!-- 用户信息和登出 -->
        <div v-if="isLoggedIn" class="user-section">
          <span class="user-name">👤 {{ userName }}</span>
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
    <div class="flex-1 overflow-auto bg-gray-50">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { userLogout } from './api/auth'

const router = useRouter()

// 登录状态
const isLoggedIn = computed(() => {
  return !!localStorage.getItem('satoken')
})

// 用户名
const userName = computed(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      return user.nickname || user.username || '用户'
    } catch (e) {
      return '用户'
    }
  }
  return '用户'
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
    confirmButtonColor: '#667eea',
    cancelButtonColor: '#94a3b8'
  })

  if (result.isConfirmed) {
    try {
      await userLogout()
    } catch (e) {
      // 忽略 API 错误
    }
    // 清除本地存储
    localStorage.removeItem('satoken')
    localStorage.removeItem('userInfo')
    
    await Swal.fire({
      icon: 'success',
      title: '已登出',
      text: '期待您的再次光临',
      confirmButtonColor: '#667eea',
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

/* 自定义滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 导航栏样式 */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
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