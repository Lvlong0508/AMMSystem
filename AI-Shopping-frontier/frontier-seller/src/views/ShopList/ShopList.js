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
  const selectedShop = ref(null)
  const shopDetail = ref(null)

  onMounted(async () => {
    if (!shopStore.loaded && auth.merchantId) {
      loading.value = true
      await shopStore.initShops(auth.merchantId)
      loading.value = false
    }
  })

  async function showShopDetail(shop) {
    selectedShop.value = shop
    detailVisible.value = true
    detailLoading.value = true
    try {
      const res = await getShopDetail(shop.id)
      shopDetail.value = res?.shop || res || null
    } catch (e) {
      console.error('加载店铺详情失败:', e)
      shopDetail.value = null
    } finally {
      detailLoading.value = false
    }
  }

  function closeDetail() {
    detailVisible.value = false
    selectedShop.value = null
    shopDetail.value = null
  }

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

  return { loading, detailVisible, detailLoading, selectedShop, shopDetail, T, showShopDetail, closeDetail, enterShop, goRegister, handleLogout, auth, shopStore }
}
