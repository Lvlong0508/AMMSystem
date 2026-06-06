import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import * as T from './Text.js'

export function useLogin() {
  const router = useRouter()
  const authStore = useAuthStore()

  const formRef = ref(null)
  const form = reactive({ username: '', password: '' })
  const loading = ref(false)
  const verifying = ref(false)

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
      const shopStore = useShopStore()
      verifying.value = true
      if (authStore.merchantId) await shopStore.initShops(authStore.merchantId)
      router.push('/')
    } catch (err) {
      const msg = err.response?.data?.message || err.message || '登录失败，请重试'
      ElMessage.error(msg)
    } finally {
      loading.value = false
      verifying.value = false
    }
  }

  return { T, formRef, form, loading, verifying, rules, handleLogin }
}
