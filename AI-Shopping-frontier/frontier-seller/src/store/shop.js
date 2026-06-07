import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { getShopByMerchant } from "@/api/shop";

export const useShopStore = defineStore("shop", () => {
  const currentShopId = ref(
    localStorage.getItem("currentShopId") || null
  );
  const shops = ref([]);
  const loaded = ref(false);

  const currentShop = computed(() =>
    shops.value.find((s) => s.id === currentShopId.value)
  );
  const hasMultipleShops = computed(() => shops.value.length > 1);
  const hasNoShops = computed(
    () => loaded.value && shops.value.length === 0
  );

  async function initShops(merchantId) {
    if (!merchantId) return;
    loaded.value = false;
    try {
      const res = await getShopByMerchant(merchantId);
      shops.value = res?.data?.shops || res?.shops || [];
      if (shops.value.length === 1) {
        currentShopId.value = shops.value[0].id;
        localStorage.setItem("currentShopId", currentShopId.value);
      } else if (shops.value.length === 0) {
        currentShopId.value = null;
        localStorage.removeItem("currentShopId");
      }
    } catch (e) {
      console.error("初始化店铺失败:", e);
    } finally {
      loaded.value = true;
    }
  }

  function clearCurrentShop() {
    currentShopId.value = null;
    localStorage.removeItem("currentShopId");
  }

  function switchShop(shopId) {
    currentShopId.value = shopId;
    localStorage.setItem("currentShopId", shopId);
  }

  return {
    currentShopId,
    shops,
    loaded,
    currentShop,
    hasMultipleShops,
    hasNoShops,
    initShops,
    clearCurrentShop,
    switchShop,
  };
});
