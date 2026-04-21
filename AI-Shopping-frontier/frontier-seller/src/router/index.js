// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import MerchantShip from '../merchant/MerchantShip/MerchantShip.vue'
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
    redirect: '/ship'
  },
  {
    path: '/ship',
    name: 'ship',
    component: MerchantShip
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
