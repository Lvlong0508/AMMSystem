// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import MerchantShip from '../merchant/MerchantShip/MerchantShip.vue'

const routes = [
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

export default router
