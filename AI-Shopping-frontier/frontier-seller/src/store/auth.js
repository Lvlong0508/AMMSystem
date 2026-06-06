import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { merchantLogin, merchantLogout } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('satoken') || '')
  const merchantInfo = ref(JSON.parse(localStorage.getItem('merchantInfo') || 'null'))
  const currentRole = ref(JSON.parse(localStorage.getItem('currentRole') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isOwner = computed(() => currentRole.value?.role === '1')
  const merchantName = computed(() => merchantInfo.value?.username || '')

  async function login(credentials) {
    const res = await merchantLogin(credentials)
    if (res.data?.token) {
      token.value = res.data.token
      merchantInfo.value = res.data.merchantInfo
      localStorage.setItem('satoken', res.data.token)
      localStorage.setItem('merchantInfo', JSON.stringify(res.data.merchantInfo))
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
      currentRole.value = null
      localStorage.removeItem('satoken')
      localStorage.removeItem('merchantInfo')
      localStorage.removeItem('currentRole')
      localStorage.removeItem('merchantRoles')
    }
  }

  function setRole(role) {
    currentRole.value = role
    localStorage.setItem('currentRole', JSON.stringify(role))
  }

  return { token, merchantInfo, currentRole, isLoggedIn, isOwner, merchantName, login, logout, setRole }
})
