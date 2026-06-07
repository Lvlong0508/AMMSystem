import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { registerShop } from '@/api/shop'
import * as T from './Text.js'

export function useShopRegister() {
  const router = useRouter()
  const authStore = useAuthStore()
  const formRef = ref(null)
  const submitting = ref(false)
  const form = reactive({ name: '', description: '' })

  const rules = {
    name: [
      { required: true, message: T.NAME_REQUIRED, trigger: 'blur' },
      { max: 50, message: '名称不超过 50 字', trigger: 'blur' }
    ],
    description: [
      { max: 200, message: '简介不超过 200 字', trigger: 'blur' }
    ]
  }

  async function handleSubmit() {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    submitting.value = true
    try {
      const res = await registerShop({ name: form.name, description: form.description })
      if (res.data?.id) {
        ElMessage.success(T.SUCCESS_MSG)
        router.push('/shop/select')
      } else {
        ElMessage.error(res.data?.message || T.ERROR_MSG)
      }
    } catch (error) {
      ElMessage.error(error.message || T.ERROR_MSG)
    } finally {
      submitting.value = false
    }
  }

  function handleLogout() {
    authStore.logout()
    router.push('/login')
  }

  return { T, formRef, form, submitting, rules, handleSubmit, handleLogout }
}
