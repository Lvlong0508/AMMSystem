import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerShop } from '@/api/shop'
import * as T from './Text.js'

export function useShopRegister() {
  const router = useRouter()
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
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_MSG)
        router.push('/shop/list')
      } else {
        ElMessage.error(res?.message || T.ERROR_MSG)
      }
    } catch (error) {
      ElMessage.error(error.message || T.ERROR_MSG)
    } finally {
      submitting.value = false
    }
  }

  return { T, formRef, form, submitting, rules, handleSubmit }
}
