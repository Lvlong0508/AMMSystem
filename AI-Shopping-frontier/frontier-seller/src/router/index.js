import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import AppLayout from '@/layout/AppLayout.vue'
import Ship from '../views/Ship/Ship.vue'
import Login from '../views/Login/Login.vue'
import Register from '../views/Register/Register.vue'
import ShopRegister from '../views/ShopRegister/ShopRegister.vue'
import ShopList from '../views/ShopList/ShopList.vue'
import ShopProducts from '../views/ShopProducts/ShopProducts.vue'
import ShopOrders from '../views/ShopOrders/ShopOrders.vue'
import ShopEmployees from '../views/ShopEmployees/ShopEmployees.vue'
import ShopAddresses from '../views/ShopAddresses/ShopAddresses.vue'
import ShopReturns from '../views/ReturnManagement/ReturnManagement.vue'
import ShopInfo from '../views/ShopInfo/ShopInfo.vue'

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
    path: '/shop/register',
    name: 'shop-register',
    component: ShopRegister,
    meta: { public: true }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/ship' },
      { path: 'ship', name: 'ship', component: Ship },
      {
        path: 'shop/list',
        name: 'shop-list',
        component: ShopList,
        meta: { shopOwnerOnly: true }
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
        path: 'shop/:shopId/employees',
        name: 'shop-employees',
        component: ShopEmployees,
        meta: { shopOwnerOnly: true }
      },
      {
        path: 'shop/:shopId/addresses',
        name: 'shop-addresses',
        component: ShopAddresses,
        meta: { shopOwnerOnly: true }
      },
      {
        path: 'shop/:shopId/returns',
        name: 'shop-returns',
        component: ShopReturns
      },
      {
        path: 'shop/:shopId/info',
        name: 'shop-info',
        component: ShopInfo,
        meta: { shopOwnerOnly: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
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

  if (to.meta.shopOwnerOnly && !auth.isOwner) {
    next('/ship')
    return
  }

  if (auth.merchantId) await shop.initShops(auth.merchantId)

  if (to.params.shopId) {
    shop.switchShop(to.params.shopId)
  }

  next()
})

export default router
