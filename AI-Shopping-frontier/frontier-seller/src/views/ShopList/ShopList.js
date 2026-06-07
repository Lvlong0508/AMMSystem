import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import * as T from './Text.js'

export function useShopList() {
  const router = useRouter()
  const auth = useAuthStore()
  const shopStore = useShopStore()
  const loading = ref(false)

  onMounted(async () => {
    if (!shopStore.loaded && auth.merchantId) {
      loading.value = true
      await shopStore.initShops(auth.merchantId)
      loading.value = false
    }
  })

  function enterShop(shopId) {
    shopStore.switchShop(shopId)
    router.push(`/shop/${shopId}/products`)
  }

  function goRegister() {
    window.open('/shop/register', '_blank')
  }

  async function handleLogout() {
    await auth.logout()
    router.push('/login')
  }

  return { loading, T, enterShop, goRegister, handleLogout, auth, shopStore }
}
