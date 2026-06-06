import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShopByMerchant } from '@/api/shop'

export const useShopStore = defineStore('shop', () => {
  const currentShopId = ref(localStorage.getItem('currentShopId') || null)
  const shops = ref([])
  const loaded = ref(false)

  const currentShop = computed(() => shops.value.find(s => String(s.id) === String(currentShopId.value)))
  const hasMultipleShops = computed(() => shops.value.length > 1)

  async function initShops(merchantId) {
    if (!merchantId) return
    loaded.value = false
    try {
      const res = await getShopByMerchant(merchantId)
      const shopIds = res?.data?.shopIds || res?.shopIds || []
      shops.value = shopIds.map(id => ({ id, name: `店铺 ${id}` }))
      if (shops.value.length > 0 && !currentShopId.value) {
        currentShopId.value = String(shops.value[0].id)
        localStorage.setItem('currentShopId', currentShopId.value)
      }
      if (shops.value.length === 1) {
        currentShopId.value = String(shops.value[0].id)
        localStorage.setItem('currentShopId', currentShopId.value)
      }
    } catch (e) {
      console.error('初始化店铺失败:', e)
    } finally {
      loaded.value = true
    }
  }

  function switchShop(shopId) {
    currentShopId.value = String(shopId)
    localStorage.setItem('currentShopId', String(shopId))
  }

  return { currentShopId, shops, loaded, currentShop, hasMultipleShops, initShops, switchShop }
})
