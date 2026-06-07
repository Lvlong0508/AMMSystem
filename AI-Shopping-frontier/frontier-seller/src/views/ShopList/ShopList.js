import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import * as T from './Text.js'

export function useShopList() {
  const router = useRouter()
  const auth = useAuthStore()
  const shopStore = useShopStore()
  const shops = ref(shopStore.shops)
  const loading = ref(false)

  async function loadShops() {
    loading.value = true
    try {
      const merchantId = localStorage.getItem('merchantId')
      if (!merchantId) return
      const { getShopByMerchant } = await import('@/api/shop')
      const res = await getShopByMerchant(merchantId)
      const shopIds = res?.data?.shopIds || res?.shopIds || []
      shops.value = shopIds.map(id => ({ id, name: `店铺 ${id}` }))
    } catch (error) {
      console.error('加载店铺失败:', error)
      ElMessage.error('加载店铺失败')
    } finally {
      loading.value = false
    }
  }

  onMounted(() => {
    if (shopStore.shops.length > 0) {
      shops.value = shopStore.shops
    } else {
      loadShops()
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

  return { shops, loading, T, enterShop, goRegister, handleLogout, auth }
}
