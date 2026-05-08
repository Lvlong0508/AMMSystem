// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import MerchantShip from '../merchant/MerchantShip/MerchantShip.vue'
import Login from '../views/Login/Login.vue'
import ShopRegister from '../views/shop/ShopRegister.vue'
import ShopList from '../views/shop/ShopList.vue'
import ShopProducts from '../views/shop/ShopProducts.vue'
import ShopOrders from '../views/shop/ShopOrders.vue'
import ShopEmployees from '../views/shop/ShopEmployees.vue'

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
  },
  {
    path: '/shop/register',
    name: 'shop-register',
    component: ShopRegister
  },
  {
    path: '/shop/list',
    name: 'shop-list',
    component: ShopList
  },
  {
    path: '/shop/:shopId/products',
    name: 'shop-products',
    component: ShopProducts
  },
  {
    path: '/shop/:shopId/orders',
    name: 'shop-orders',
    component: ShopOrders
  },
  {
    path: '/shop/:shopId/employees',
    name: 'shop-employees',
    component: ShopEmployees
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

export default router