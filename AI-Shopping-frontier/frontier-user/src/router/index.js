// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import ChatWindow from '../components/ChatWindow/ChatWindow.vue'
import ContactManager from '../components/Contact/ContactManager.vue'
import OrderManager from '../components/OrderManager/OrderManager.vue'

const routes = [
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

export default router
