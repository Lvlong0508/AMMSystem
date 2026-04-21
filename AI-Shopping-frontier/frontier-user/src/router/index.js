// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import ChatWindow from '../components/ChatWindow/ChatWindow.vue'
import ContactManager from '../components/Contact/ContactManager.vue'
import OrderManager from '../components/OrderManager/OrderManager.vue'
import Login from '../views/Login/Login.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/',
    name: 'chat',
    component: ChatWindow
  },
  {
    path: '/contact',
    name: 'contact',
    component: ContactManager
  },
  {
    path: '/order',
    name: 'order',
    component: OrderManager
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  // 公开页面直接放行
  if (to.meta.public) {
    next()
    return
  }

  // 检查是否已登录
  const token = localStorage.getItem('satoken')
  if (!token) {
    next('/login')
    return
  }

  next()
})

export default router
