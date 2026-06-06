import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useShopStore } from '@/store/shop'
import { getShopByMerchant } from '@/api/shop'
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

  function formatDate(dateStr) {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('zh-CN')
  }

  function enterShop(shopId) {
    shopStore.switchShop(shopId)
    router.push(`/shop/${shopId}/products`)
  }

  onMounted(loadShops)

  function goRegister() { window.open('/shop/register', '_blank') }

  return { shops, loading, T, formatDate, enterShop, loadShops, goRegister }
}
