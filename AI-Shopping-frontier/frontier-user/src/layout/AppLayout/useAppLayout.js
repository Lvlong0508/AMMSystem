import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { userLogout } from '../../api/auth'
import { text } from './Text'

export function useAppLayout() {
  const route = useRoute()
  const router = useRouter()

  const isLoggedIn = computed(() => {
    return !!localStorage.getItem('satoken')
  })

  const activeRoute = computed(() => route.path)

  const handleLogout = async () => {
    const result = await Swal.fire({
      title: text.user.logoutTitle,
      text: text.user.logoutConfirm,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: text.user.confirm,
      cancelButtonText: text.user.cancel,
      confirmButtonColor: '#2563eb',
      cancelButtonColor: '#94a3b8'
    })

    if (result.isConfirmed) {
      try {
        await userLogout()
      } catch (e) {}
      localStorage.removeItem('satoken')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
  }

  return {
    isLoggedIn,
    activeRoute,
    handleLogout
  }
}
