import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userGetProfile, userUpdateProfile } from '@/api/auth'
import { showSuccess, showError } from '@/utils/swal'

export function useProfileView() {
  const router = useRouter()

  const profile = ref({})
  const editing = ref(false)
  const saving = ref(false)
  const formNickname = ref('')
  const formPhone = ref('')
  const formEmail = ref('')

  const loadProfile = async () => {
    try {
      const res = await userGetProfile()
      profile.value = res
    } catch {
      try {
        const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
        profile.value = local
      } catch {}
    }
  }

  const startEdit = () => {
    formNickname.value = profile.value.nickname || ''
    formPhone.value = profile.value.phone || ''
    formEmail.value = profile.value.email || ''
    editing.value = true
  }

  const cancelEdit = () => {
    editing.value = false
  }

  const saveProfile = async () => {
    const email = formEmail.value.trim()
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showError('邮箱格式不正确')
      return
    }
    const data = {
      nickname: formNickname.value,
      phone: formPhone.value,
      email
    }
    if (
      data.nickname === (profile.value.nickname || '') &&
      data.phone === (profile.value.phone || '') &&
      data.email === (profile.value.email || '')
    ) {
      editing.value = false
      return
    }
    saving.value = true
    try {
      const res = await userUpdateProfile(data)
      Object.assign(profile.value, res)
      showSuccess('个人信息已更新')
      editing.value = false
      const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
      if (res.nickname) local.nickname = res.nickname
      if (res.phone) local.phone = res.phone
      if (res.email) local.email = res.email
      localStorage.setItem('userInfo', JSON.stringify(local))
    } catch {
      showError('更新失败')
    } finally {
      saving.value = false
    }
  }

  const goBack = () => {
    router.back()
  }

  onMounted(loadProfile)

  return {
    profile,
    editing,
    saving,
    formNickname,
    formPhone,
    formEmail,
    startEdit,
    cancelEdit,
    saveProfile,
    goBack
  }
}
