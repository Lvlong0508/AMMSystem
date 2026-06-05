<template>
  <div class="app-shell">
    <div class="app-layout">
      <aside class="sidebar">
        <div class="sidebar-brand">{{ text.brand }}</div>
        <nav class="sidebar-nav">
          <router-link to="/" class="sidebar-item" :class="{ active: activeRoute.startsWith('/chat') }">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
            </svg>
            <span>{{ text.nav.shopping }}</span>
          </router-link>

          <router-link to="/order" class="sidebar-item" :class="{ active: activeRoute.startsWith('/order') }">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2" />
              <rect x="9" y="3" width="6" height="4" rx="1" />
              <line x1="9" y1="12" x2="15" y2="12" />
              <line x1="9" y1="16" x2="13" y2="16" />
            </svg>
            <span>{{ text.nav.orders }}</span>
          </router-link>

          <router-link to="/contact" class="sidebar-item" :class="{ active: activeRoute === '/contact' }">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
              <circle cx="12" cy="10" r="3" />
            </svg>
            <span>{{ text.nav.address }}</span>
          </router-link>
        </nav>

        <div class="sidebar-footer">
          <template v-if="isLoggedIn">
            <div class="sidebar-user-avatar">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="8" r="4" />
                <path d="M20 21a8 8 0 1 0-16 0" />
              </svg>
            </div>
            <button class="sidebar-logout" @click="handleLogout">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
              <span>{{ text.user.logout }}</span>
            </button>
          </template>
        </div>
      </aside>

      <main class="main-content">
        <router-view />
      </main>
    </div>

    <LoginCard v-if="!checking && !isLoggedIn" @logged-in="onLoggedIn" />
  </div>
</template>

<script setup>
import { text } from './Text'
import { useAppLayout } from './useAppLayout'
import LoginCard from '@/components/LoginCard/LoginCard.vue'

const { isLoggedIn, activeRoute, checking, handleLogout } = useAppLayout()

const onLoggedIn = () => {
  window.location.reload()
}
</script>

<style>
@import './AppLayout.css';
</style>
