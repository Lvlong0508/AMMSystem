import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { userLogin, userRegister } from '@/api/auth'
import { showSuccess, showError } from '@/utils/swal'

export function useLoginView() {
  const router = useRouter()
  const isRegister = ref(false)
  const loading = ref(false)

  const form = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    phone: ''
  })

  const errors = reactive({
    username: '',
    password: '',
    confirmPassword: ''
  })

  const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/
  const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/

  const isFormValid = computed(() => {
    if (!form.username || !form.password) return false
    if (!USERNAME_PATTERN.test(form.username)) return false
    if (!PASSWORD_PATTERN.test(form.password)) return false
    if (isRegister.value && form.password !== form.confirmPassword) return false
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

  const handleSubmit = async () => {
    validateUsername()
    validatePassword()
    if (isRegister.value) validateConfirmPassword()
    if (!isFormValid.value) return

    loading.value = true
    try {
      const data = isRegister.value
        ? { username: form.username, password: form.password, phone: form.phone }
        : { username: form.username, password: form.password }

      const res = isRegister.value ? await userRegister(data) : await userLogin(data)

      if (res.token) {
        localStorage.setItem('satoken', res.token)
        if (res.userInfo) localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
        await showSuccess(res.message || '成功')
        sessionStorage.setItem('needReload', '1')
        router.push('/')
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
    errors.username = ''
    errors.password = ''
    errors.confirmPassword = ''
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
    handleSubmit,
    toggleMode
  }
}
