import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
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
  next()
})

router.afterEach((to) => {
  if (sessionStorage.getItem('needReload') === '1') {
    sessionStorage.removeItem('needReload')
    window.location.reload()
  }
})

export default router
