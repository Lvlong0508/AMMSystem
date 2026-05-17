// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import Swal from 'sweetalert2'
import MerchantShip from '../merchant/MerchantShip/MerchantShip.vue'
import Login from '../views/Login/Login.vue'
import ShopRegister from '../views/shop/ShopRegister.vue'
import ShopList from '../views/shop/ShopList.vue'
import ShopProducts from '../views/shop/ShopProducts.vue'
import ShopOrders from '../views/shop/ShopOrders.vue'
import ShopEmployees from '../views/shop/ShopEmployees.vue'
import ShopAddresses from '../views/shop/ShopAddresses.vue'

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
  if (to.meta.public) {
    next()
    return
  }
  const token = localStorage.getItem('satoken')
  if (!token) {
    next('/login')
    return
  }

  // 店铺管理相关页面需要店长权限
  if (to.meta.shopOwnerOnly) {
    const currentRole = localStorage.getItem('currentRole')
    if (!currentRole) {
      Swal.fire({
        icon: 'warning',
        title: '无权限',
        text: '只有店长才能访问此页面',
        confirmButtonText: '确定'
      })
      next('/ship')
      return
    }

    try {
      const role = JSON.parse(currentRole)
      if (role.role !== '1') {
        Swal.fire({
          icon: 'warning',
          title: '无权限',
          text: '只有店长才能访问此页面',
          confirmButtonText: '确定'
        })
        next('/ship')
        return
      }
    } catch (e) {
      Swal.fire({
        icon: 'warning',
        title: '无权限',
        text: '只有店长才能访问此页面',
        confirmButtonText: '确定'
      })
      next('/ship')
      return
    }
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