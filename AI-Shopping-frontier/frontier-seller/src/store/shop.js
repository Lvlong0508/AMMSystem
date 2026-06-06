import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShopByMerchant } from '@/api/shop'

export const useShopStore = defineStore('shop', () => {
  const currentShopId = ref(localStorage.getItem('currentShopId') || null)
  const shops = ref([])

  const currentShop = computed(() => shops.value.find(s => String(s.id) === String(currentShopId.value)))

  async function loadShops(merchantId) {
    const res = await getShopByMerchant(merchantId)
    if (res?.data?.shopIds) {
      shops.value = res.data.shopIds.map(id => ({ id }))
    }
  }

  function switchShop(shopId) {
    currentShopId.value = shopId
    localStorage.setItem('currentShopId', shopId)
  }

  return { currentShopId, shops, currentShop, loadShops, switchShop }
})
