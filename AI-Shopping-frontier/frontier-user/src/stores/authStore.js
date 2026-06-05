import { ref } from 'vue'

export const showLogin = ref(false)

export function requireLogin() {
  const token = localStorage.getItem('satoken')
  if (!token) {
    showLogin.value = true
    return false
  }
  return true
}
