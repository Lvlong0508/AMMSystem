import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login/LoginView/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'chat',
    component: () => import('../views/Chat/ChatView/ChatView.vue')
  },
  {
    path: '/order',
    name: 'order',
    component: () => import('../views/Order/OrderListView/OrderListView.vue')
  },
  {
    path: '/order/:id',
    name: 'orderDetail',
    component: () => import('../views/Order/OrderDetailView/OrderDetailView.vue')
  },
  {
    path: '/contact',
    name: 'contact',
    component: () => import('../views/Contact/ContactView/ContactView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.public) {
    next()
    return
  }
  const token = localStorage.getItem('satoken')
  if (!token) {
    next('/login')
    return
  }
  next()
})

router.afterEach((to) => {
  if (sessionStorage.getItem('needReload') === '1') {
    sessionStorage.removeItem('needReload')
    window.location.reload()
  }
})

export default router
