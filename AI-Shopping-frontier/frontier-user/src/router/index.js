import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'chat',
    meta: { transition: 'fade' },
    component: () => import('../views/Chat/ChatView/ChatView.vue')
  },
  {
    path: '/order',
    name: 'order',
    meta: { transition: 'fade' },
    component: () => import('../views/Order/OrderListView/OrderListView.vue')
  },
  {
    path: '/order/:id',
    name: 'orderDetail',
    meta: { transition: 'slide-left' },
    component: () => import('../views/Order/OrderDetailView/OrderDetailView.vue')
  },
  {
    path: '/contact',
    name: 'contact',
    meta: { transition: 'slide-up' },
    component: () => import('../views/Contact/ContactView/ContactView.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    meta: { transition: 'scale' },
    component: () => import('../views/Profile/ProfileView/ProfileView.vue')
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.afterEach((to) => {
  if (sessionStorage.getItem('needReload') === '1') {
    sessionStorage.removeItem('needReload')
    window.location.reload()
  }
})

export default router
