import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import AppLayout from '@/layout/AppLayout.vue'
import Ship from '../views/Ship/Ship.vue'
import Login from '../views/Login/Login.vue'
import Register from '../views/Register/Register.vue'

import ShopProducts from '../views/ShopProducts/ShopProducts.vue'
import ShopOrders from '../views/ShopOrders/ShopOrders.vue'
import ShopAddresses from '../views/ShopAddresses/ShopAddresses.vue'
import ShopReturns from '../views/ReturnManagement/ReturnManagement.vue'
import ShopInfo from '../views/ShopInfo/ShopInfo.vue'
import Dashboard from '../views/Dashboard/Dashboard.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: Register,
    meta: { public: true }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', name: 'home', component: Ship },
      { path: 'ship', name: 'ship', component: Ship },
      {
        path: 'shop/:shopId',
        name: 'shop-dashboard',
        component: Dashboard
      },
      {
        path: 'shop/:shopId/products',
        name: 'shop-products',
        component: ShopProducts
      },
      {
        path: 'shop/:shopId/orders',
        name: 'shop-orders',
        component: ShopOrders
      },
      {
        path: 'shop/:shopId/addresses',
        name: 'shop-addresses',
        component: ShopAddresses
      },
      {
        path: 'shop/:shopId/returns',
        name: 'shop-returns',
        component: ShopReturns
      },
      {
        path: 'shop/:shopId/info',
        name: 'shop-info',
        component: ShopInfo
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()
  const shop = useShopStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!auth.isLoggedIn) {
    next('/login')
    return
  }

  if (auth.merchantId && !shop.loaded) {
    await shop.initShop(auth.merchantId)
  }

  if (to.name === 'home') {
    if (shop.hasShop) {
      next(`/shop/${shop.currentShopId}`)
    } else {
      next('/register')
    }
    return
  }

  if (to.params.shopId) {
    shop.switchShop(to.params.shopId)
  }

  next()
})

export default router
