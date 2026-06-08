import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { useShopStore } from '@/store/shop'
import { getShopDetail } from '@/api/shop'
import * as T from './Text.js'

export function useShopList() {
  const router = useRouter()
  const auth = useAuthStore()
  const shopStore = useShopStore()
  const loading = ref(false)
  const detailVisible = ref(false)
  const detailLoading = ref(false)
  const shopDetail = ref(null)

  onMounted(async () => {
    if (auth.merchantId) {
      loading.value = true
      await shopStore.initShop(auth.merchantId)
      loading.value = false
    }
    if (shopStore.hasShop && shopStore.currentShopId) {
      router.replace(`/shop/${shopStore.currentShopId}/products`)
    }
  })

  async function showShopDetail() {
    if (!shopStore.shop) return
    detailVisible.value = true
    detailLoading.value = true
    try {
      const res = await getShopDetail(shopStore.shop.id)
      const shopData = res?.data?.shop || res?.shop || {}
      const shopInfo = res?.data?.shopInfo || res?.shopInfo || {}
      shopDetail.value = { ...shopData, ...shopInfo }
    } catch (e) {
      console.error('加载店铺详情失败:', e)
      shopDetail.value = null
    } finally {
      detailLoading.value = false
    }
  }

  function closeDetail() {
    detailVisible.value = false
    shopDetail.value = null
  }

  function enterShop() {
    if (shopStore.shop) {
      shopStore.switchShop(shopStore.shop.id)
      router.push(`/shop/${shopStore.shop.id}/products`)
    }
  }

  async function handleLogout() {
    await auth.logout()
    router.push('/login')
  }

  return { loading, detailVisible, detailLoading, shopDetail, T, showShopDetail, closeDetail, enterShop, handleLogout, auth, shopStore }
}