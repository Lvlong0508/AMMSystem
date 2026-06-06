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
  const currentRole = ref(safeParse(localStorage.getItem('currentRole')))

  const isLoggedIn = computed(() => !!token.value)
  const isOwner = computed(() => currentRole.value?.role === '1')
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
      if (res.data?.role) {
        const roleObj = { role: res.data.role, shopId: null }
        currentRole.value = roleObj
        localStorage.setItem('currentRole', JSON.stringify(roleObj))
      }
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
      currentRole.value = null
      localStorage.removeItem('satoken')
      localStorage.removeItem('merchantInfo')
      localStorage.removeItem('merchantId')
      localStorage.removeItem('currentRole')
      localStorage.removeItem('merchantRoles')
    }
  }

  function setRole(role) {
    currentRole.value = role
    localStorage.setItem('currentRole', JSON.stringify(role))
  }

  return { token, merchantInfo, merchantId, currentRole, isLoggedIn, isOwner, merchantName, login, logout, setRole }
})
