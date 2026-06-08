import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { getMyShop } from "@/api/shop";

export const useShopStore = defineStore("shop", () => {
  const currentShopId = ref(
    localStorage.getItem("currentShopId") || null
  );
  const shop = ref(null);
  const loaded = ref(false);

  const hasShop = computed(() => shop.value !== null);
  const hasNoShop = computed(() => loaded.value && shop.value === null);

  async function initShop(userId) {
    if (!userId) return;
    loaded.value = false;
    try {
      const res = await getMyShop();
      shop.value = res?.data?.shop || null;
      if (shop.value && shop.value.id) {
        currentShopId.value = shop.value.id;
        localStorage.setItem("currentShopId", currentShopId.value);
      } else {
        currentShopId.value = null;
        localStorage.removeItem("currentShopId");
      }
    } catch (e) {
      console.error("初始化店铺失败:", e);
      shop.value = null;
    } finally {
      loaded.value = true;
    }
  }

  function clearCurrentShop() {
    currentShopId.value = null;
    shop.value = null;
    localStorage.removeItem("currentShopId");
  }

  function switchShop(shopId) {
    currentShopId.value = shopId;
    localStorage.setItem("currentShopId", shopId);
  }

  return {
    currentShopId,
    shop,
    loaded,
    hasShop,
    hasNoShop,
    initShop,
    clearCurrentShop,
    switchShop,
  };
});