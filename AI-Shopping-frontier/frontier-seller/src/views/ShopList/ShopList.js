import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useShopStore } from '@/store/shop'
import { getShopByMerchant, getShopDetail } from '@/api/shop'
import * as T from './Text.js'

export function useShopList() {
  const router = useRouter()
  const shopStore = useShopStore()
  const shops = ref([])
  const loading = ref(false)

  async function loadShops() {
    loading.value = true
    try {
      const merchantId = localStorage.getItem('merchantId')
      if (!merchantId) return
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

  function getStatusText(status) {
    if (status === 1) return T.STATUS_ACTIVE
    if (status === 0) return T.STATUS_INACTIVE
    if (status === -1) return T.STATUS_CLOSED
    return T.STATUS_INACTIVE
  }

  function getStatusType(status) {
    if (status === 1) return 'success'
    if (status === 0) return 'info'
    if (status === -1) return 'danger'
    return 'info'
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('zh-CN')
  }

  function goToProducts(shopId) { router.push(`/shop/${shopId}/products`) }
  function goToOrders(shopId) { router.push(`/shop/${shopId}/orders`) }
  function goToEmployees(shopId) { router.push(`/shop/${shopId}/employees`) }
  function goToAddresses(shopId) { router.push(`/shop/${shopId}/addresses`) }

  function enterShop(shopId) {
    shopStore.switchShop(shopId)
    router.push('/ship')
  }

  onMounted(loadShops)

  function goRegister() { window.open('/shop/register', '_blank') }

  return { shops, loading, T, getStatusText, getStatusType, formatDate, goToProducts, goToOrders, goToEmployees, goToAddresses, enterShop, loadShops, goRegister }
}
