import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import Ship from '../views/Ship/Ship.vue'
import Login from '../views/Login/Login.vue'
import ShopRegister from '../views/ShopRegister/ShopRegister.vue'
import ShopList from '../views/ShopList/ShopList.vue'
import ShopProducts from '../views/ShopProducts/ShopProducts.vue'
import ShopOrders from '../views/ShopOrders/ShopOrders.vue'
import ShopEmployees from '../views/ShopEmployees/ShopEmployees.vue'
import ShopAddresses from '../views/ShopAddresses/ShopAddresses.vue'

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
    component: Ship
  },
  {
    path: '/shop/register',
    name: 'shop-register',
    component: ShopRegister,
    meta: { shopOwnerOnly: true }
  },
  {
    path: '/shop/list',
    name: 'shop-list',
    component: ShopList,
    meta: { shopOwnerOnly: true }
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
    component: ShopEmployees,
    meta: { shopOwnerOnly: true }
  },
  {
    path: '/shop/:shopId/addresses',
    name: 'shop-addresses',
    component: ShopAddresses,
    meta: { shopOwnerOnly: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

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

  next()
})

router.afterEach(() => {
  if (sessionStorage.getItem('needReload') === '1') {
    sessionStorage.removeItem('needReload')
    window.location.reload()
  }
})

export default router
