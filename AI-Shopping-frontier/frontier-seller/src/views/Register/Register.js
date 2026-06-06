import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantRegister } from '@/api/auth'
import * as T from './Text.js'

export function useRegister() {
  const router = useRouter()
  const formRef = ref(null)
  const submitting = ref(false)
  const form = reactive({ username: '', phone: '', password: '', confirmPassword: '' })

  const validateConfirm = (_rule, value, callback) => {
    if (value !== form.password) {
      callback(new Error(T.CONFIRM_MISMATCH))
    } else {
      callback()
    }
  }

  const rules = {
    username: [
      { required: true, message: T.USERNAME_REQUIRED, trigger: 'blur' },
      { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: T.USERNAME_INVALID, trigger: 'blur' }
    ],
    phone: [
      { required: true, message: T.PHONE_REQUIRED, trigger: 'blur' },
      { pattern: /^1\d{10}$/, message: T.PHONE_INVALID, trigger: 'blur' }
    ],
    password: [
      { required: true, message: T.PASSWORD_REQUIRED, trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: T.CONFIRM_REQUIRED, trigger: 'blur' },
      { validator: validateConfirm, trigger: 'blur' }
    ]
  }

  async function handleRegister() {
    if (!formRef.value) return
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return

    submitting.value = true
    try {
      const res = await merchantRegister({
        username: form.username,
        phone: form.phone,
        password: form.password
      })
      ElMessage.success(res.message || T.SUCCESS_MSG)
      router.push('/login')
    } catch (err) {
      const msg = err.response?.data?.message || err.message || T.ERROR_MSG
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  }

  return { T, formRef, form, submitting, rules, handleRegister }
}
