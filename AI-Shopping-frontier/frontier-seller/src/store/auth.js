import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { merchantLogin, merchantLogout } from '@/api/auth'

function safeParse(val) {
  try { return JSON.parse(val) } catch { return null }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('satoken') || '')
  const merchantInfo = ref(safeParse(localStorage.getItem('merchantInfo')))
  const merchantId = ref(localStorage.getItem('merchantId') || null)
  const isLoggedIn = computed(() => !!token.value)
  const merchantName = computed(() => merchantInfo.value?.username || '')

  async function login(credentials) {
    const res = await merchantLogin(credentials)
    if (res.data?.token) {
      token.value = res.data.token
      merchantInfo.value = res.data.merchantInfo
      merchantId.value = res.data?.merchantInfo?.id
      localStorage.setItem('satoken', res.data.token)
      localStorage.setItem('merchantInfo', JSON.stringify(res.data.merchantInfo))
      localStorage.setItem('merchantId', merchantId.value)
    }
    return res
  }

  async function logout() {
    try {
      await merchantLogout()
    } finally {
      token.value = ''
      merchantInfo.value = null
      merchantId.value = null
      localStorage.removeItem('satoken')
      localStorage.removeItem('merchantInfo')
      localStorage.removeItem('merchantId')
      localStorage.removeItem('merchantRoles')
      localStorage.removeItem('currentShopId')
    }
  }

  return { token, merchantInfo, merchantId, isLoggedIn, merchantName, login, logout }
})
