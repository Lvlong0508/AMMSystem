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
          :class="['nav-link', { active: $route.path.startsWith('/shop') }]" 
          to="/shop/list"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
          </svg>
          店铺管理
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
</style>
