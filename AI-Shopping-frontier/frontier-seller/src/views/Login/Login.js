import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import * as T from './Text.js'

export function useLogin() {
  const router = useRouter()
  const authStore = useAuthStore()

  const formRef = ref(null)
  const form = reactive({ username: '', password: '' })
  const loading = ref(false)

  const usernamePattern = /^[a-zA-Z0-9_]{3,20}$/

  const rules = {
    username: [
      { required: true, message: T.USERNAME_REQUIRED, trigger: 'blur' },
      { pattern: usernamePattern, message: T.USERNAME_INVALID, trigger: 'blur' }
    ],
    password: [
      { required: true, message: T.PASSWORD_REQUIRED, trigger: 'blur' }
    ]
  }

  async function handleLogin() {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    loading.value = true
    try {
      const res = await authStore.login({
        username: form.username,
        password: form.password
      })
      ElMessage.success(res.message || '登录成功')

      setTimeout(() => {
        sessionStorage.setItem('needReload', '1')
        router.push('/')
      }, 800)
    } catch (err) {
      ElMessage.error(err.message || err.error || '登录失败，请重试')
    } finally {
      loading.value = false
    }
  }

  return { T, formRef, form, loading, rules, handleLogin }
}
