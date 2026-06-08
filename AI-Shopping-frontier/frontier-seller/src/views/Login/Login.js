import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import { getMyShop } from '@/api/shop'
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
      ElMessage.success(res.message || T.LOGIN_SUCCESS)

      // 查询店铺信息
      verifying.value = true
      const shopStore = useShopStore()
      shopStore.clearCurrentShop()
      await shopStore.initShop(authStore.merchantId)

      verifying.value = false

      if (shopStore.hasShop) {
        // 有店铺 → 跳转管理页
        const shopId = shopStore.currentShopId
        router.push(`/shop/${shopId}/products`)
      } else {
        // 无店铺 → 弹窗引导注册
        ElMessageBox.confirm(T.NO_SHOP_MSG, T.NO_SHOP_TITLE, {
          confirmButtonText: T.NO_SHOP_CONFIRM,
          cancelButtonText: T.NO_SHOP_CANCEL,
          type: 'warning'
        }).then(() => {
          router.push('/register')
        }).catch(() => {
          // 用户取消，留在登录页
        })
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || T.LOGIN_FAILED
      ElMessage.error(msg)
    } finally {
      loading.value = false
      verifying.value = false
    }
  }

  return { T, formRef, form, loading, verifying, rules, handleLogin }
}
