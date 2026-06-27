import { ref, reactive, computed } from 'vue'
import { userLogin, userRegister } from '@/api/auth'
import { showSuccess, showError } from '@/utils/swal'
import { T } from './Text'

const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/
const PHONE_PATTERN = /^1[3-9]\d{9}$/

export function useLoginCard(emit) {
  const isRegister = ref(false)
  const loading = ref(false)

  const form = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    phone: '',
    email: ''
  })

  const errors = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    phone: ''
  })

  const isFormValid = computed(() => {
    if (!form.username || !form.password) return false
    if (!USERNAME_PATTERN.test(form.username)) return false
    if (!PASSWORD_PATTERN.test(form.password)) return false
    if (isRegister.value && form.password !== form.confirmPassword) return false
    if (isRegister.value && !PHONE_PATTERN.test(form.phone)) return false
    return true
  })

  const validateUsername = () => {
    if (!form.username) { errors.username = T.USERNAME_REQUIRED; return }
    if (!USERNAME_PATTERN.test(form.username)) { errors.username = T.USERNAME_INVALID; return }
    errors.username = ''
  }

  const validatePassword = () => {
    if (!form.password) { errors.password = T.PASSWORD_REQUIRED; return }
    if (!PASSWORD_PATTERN.test(form.password)) { errors.password = T.PASSWORD_INVALID; return }
    errors.password = ''
  }

  const validateConfirmPassword = () => {
    if (form.password !== form.confirmPassword) { errors.confirmPassword = T.CONFIRM_MISMATCH; return }
    errors.confirmPassword = ''
  }

  const validatePhone = () => {
    if (!form.phone) { errors.phone = T.PHONE_INVALID; return }
    if (!PHONE_PATTERN.test(form.phone)) { errors.phone = T.PHONE_INVALID; return }
    errors.phone = ''
  }

  const handleSubmit = async () => {
    validateUsername()
    validatePassword()
    if (isRegister.value) {
      validateConfirmPassword()
      validatePhone()
    }
    if (!isFormValid.value) return

    loading.value = true
    try {
      const data = isRegister.value
        ? { username: form.username, password: form.password, phone: form.phone, email: form.email || undefined }
        : { username: form.username, password: form.password }

      const res = isRegister.value ? await userRegister(data) : await userLogin(data)

      if (res.token) {
        localStorage.setItem('satoken', res.token)
        if (res.userInfo) localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
        await showSuccess(res.message || '成功')
        emit('logged-in')
      } else {
        showError(res.message || '操作失败')
      }
    } catch (e) {
      const msg = e.response?.data?.message || e.message || '网络错误'
      showError(msg)
    } finally {
      loading.value = false
    }
  }

  const toggleMode = () => {
    isRegister.value = !isRegister.value
    form.username = ''
    form.password = ''
    form.confirmPassword = ''
    form.phone = ''
    form.email = ''
    errors.username = ''
    errors.password = ''
    errors.confirmPassword = ''
    errors.phone = ''
  }

  return {
    form,
    errors,
    isRegister,
    loading,
    isFormValid,
    validateUsername,
    validatePassword,
    validateConfirmPassword,
    validatePhone,
    handleSubmit,
    toggleMode
  }
}
