<template>
  <div class="app-shell">
    <div class="app-layout" :class="{ 'app-layout--sidebar-collapsed': sidebarCollapsed }" :style="{ '--sidebar-width': sidebarCollapsed ? '72px' : '240px' }">
      <aside class="sidebar" :class="{ 'sidebar--collapsed': sidebarCollapsed, 'sidebar--has-history': activeRoute.startsWith('/chat') }">
        <div class="sidebar-brand">
          <span>{{ sidebarCollapsed ? 'AS' : text.brand }}</span>
          <div class="sidebar-toggle" @click.stop="toggleSidebar">
            <svg v-if="sidebarCollapsed" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div v-if="activeRoute.startsWith('/chat')" class="sidebar-item sidebar-item--new-chat" title="新对话" @click="handleNewChat">
            <span class="sidebar-item__inner" data-label="新对话">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              <span>{{ text.newChatTitle }}</span>
            </span>
          </div>

          <div class="sidebar-nav-divider"></div>

          <router-link to="/" class="sidebar-item" :class="{ active: activeRoute.startsWith('/chat') }" :title="text.nav.shopping">
            <span class="sidebar-item__inner" :data-label="text.nav.shopping">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
              </svg>
              <span>{{ text.nav.shopping }}</span>
            </span>
          </router-link>

          <a class="sidebar-item" :class="{ active: activeRoute.startsWith('/order') }" @click="handleNavClick('/order')" :title="text.nav.orders">
            <span class="sidebar-item__inner" :data-label="text.nav.orders">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
                <rect x="9" y="3" width="6" height="4" rx="1" />
                <line x1="9" y1="12" x2="15" y2="12" />
                <line x1="9" y1="16" x2="13" y2="16" />
              </svg>
              <span>{{ text.nav.orders }}</span>
            </span>
          </a>
        </nav>

        <div v-if="activeRoute.startsWith('/chat')" class="sidebar-history">
          <div class="sidebar-history__header">回话记录</div>
          <div
            v-for="s in sessionList"
            :key="s.id"
            class="sidebar-history__item"
            :class="{ active: activeSessionId === s.id }"
            :title="s.title"
            @click="handleSessionClick(s.id)"
          >
            <span>{{ s.title }}</span>
          </div>
        </div>

        <div class="sidebar-footer">
          <template v-if="isLoggedIn">
            <div class="sidebar-user" :data-label="userDisplayName" @mouseenter="showUserMenu = true" @mouseleave="showUserMenu = false">
              <div class="sidebar-user-avatar">
                <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M20 21a8 8 0 1 0-16 0" />
                </svg>
              </div>
              <div v-if="!sidebarCollapsed" class="sidebar-user-name">{{ userDisplayName }}</div>
              <Transition name="menu-fade">
                <div v-if="showUserMenu && !sidebarCollapsed" class="sidebar-user-menu">
                  <div class="sidebar-user-menu__item" @click="goToProfile">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="8" r="4" />
                      <path d="M20 21a8 8 0 1 0-16 0" />
                    </svg>
                    <span>{{ text.user.profile }}</span>
                  </div>
                  <div class="sidebar-user-menu__item" @click="goToAddress">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                      <circle cx="12" cy="10" r="3" />
                    </svg>
                    <span>{{ text.nav.address }}</span>
                  </div>
                  <div class="sidebar-user-menu__divider"></div>
                  <div class="sidebar-user-menu__item sidebar-user-menu__item--danger" @click="handleLogout">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                      <polyline points="16 17 21 12 16 7" />
                      <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                    <span>{{ text.user.logout }}</span>
                  </div>
                </div>
              </Transition>
            </div>
          </template>
          <template v-else>
            <div class="sidebar-login-trigger" data-label="登录" @click="handleLogin">
              <div class="sidebar-user-avatar">
                <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="8" r="4" />
                  <path d="M20 21a8 8 0 1 0-16 0" />
                </svg>
              </div>
              <span v-if="!sidebarCollapsed" class="sidebar-login-text">{{ text.login }}</span>
            </div>
          </template>
        </div>
      </aside>

      <main class="main-content">
        <router-view v-slot="{ Component, route }">
          <transition :name="route.meta.transition || 'fade'" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>

    <LoginCard v-if="showLogin" @logged-in="onLoggedIn" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { text } from './Text'
import { useAppLayout } from './useAppLayout'
import LoginCard from '@/components/LoginCard/LoginCard.vue'
import { showLogin } from '@/stores/authStore'
import { sessionList, activeSessionId, clearActiveSession, switchSession } from '@/stores/chatStore'
import { useRouter } from 'vue-router'

const { isLoggedIn, activeRoute, handleLogout } = useAppLayout()
const router = useRouter()
const showUserMenu = ref(false)
const sidebarCollapsed = ref(false)

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const userDisplayName = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info.username || info.id || '未知用户'
  } catch {
    return '未知用户'
  }
})

const handleLogin = () => {
  showLogin.value = true
}

const handleNavClick = (to) => {
  if (isLoggedIn.value) {
    router.push(to)
  } else {
    showLogin.value = true
  }
}

const goToProfile = () => {
  showUserMenu.value = false
  router.push('/profile')
}

const goToAddress = () => {
  showUserMenu.value = false
  router.push('/contact')
}

const onLoggedIn = () => {
  window.location.reload()
}

const handleNewChat = () => {
  clearActiveSession()
}

const handleSessionClick = (id) => {
  switchSession(id)
}
</script>

<style>
@import './AppLayout.css';
</style>
