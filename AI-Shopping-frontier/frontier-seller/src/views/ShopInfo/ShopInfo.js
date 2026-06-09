import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShopDetail, updateShop, closeShop, openShop } from '@/api/shop'
import { parseAddress, buildAddressString } from '@/utils/region'
import * as T from './Text.js'

export function useShopInfo() {
  const route = useRoute()
  const shopId = route.params.shopId
  const loading = ref(false)
  const saving = ref(false)
  const toggling = ref(false)
  const shopStatus = ref(null)

  const form = reactive({
    name: '',
    description: '',
    phone: '',
    region: [],
    addressDetail: '',
    address: '',
    businessHours: ''
  })

  async function loadShopInfo() {
    loading.value = true
    try {
      const res = await getShopDetail(shopId)
      const shopData = res?.data?.shop || res?.shop || {}
      const shopInfo = res?.data?.shopInfo || res?.shopInfo || {}
      const shop = { ...shopData, ...shopInfo }
      form.name = shop.name || ''
      form.description = shop.description || ''
      form.phone = shop.phone || ''
      form.address = shop.address || ''
      const parsed = parseAddress(shop.address || '')
      form.region = parsed.region
      form.addressDetail = parsed.detail
      form.businessHours = shop.businessHours || ''
      shopStatus.value = shop.status
    } catch (e) {
      console.error('加载商店信息失败:', e)
      ElMessage.error(T.LOAD_FAILED)
    } finally {
      loading.value = false
    }
  }

  async function handleSave() {
    saving.value = true
    try {
      const payload = {
        name: form.name,
        description: form.description,
        phone: form.phone,
        address: form.region.length > 0 ? buildAddressString(form.region, form.addressDetail) : (form.addressDetail || ''),
        businessHours: form.businessHours
      }
      const res = await updateShop(shopId, payload)
      ElMessage.success(res?.message || T.SAVE_SUCCESS)
    } catch (e) {
      ElMessage.error(T.SAVE_FAILED)
    } finally {
      saving.value = false
    }
  }

  async function handleToggleStatus() {
    toggling.value = true
    try {
      if (shopStatus.value === 1) {
        await closeShop(shopId)
        shopStatus.value = 0
      } else {
        await openShop(shopId)
        shopStatus.value = 1
      }
      ElMessage.success(T.STATUS_TOGGLED)
    } catch (e) {
      ElMessage.error('操作失败')
    } finally {
      toggling.value = false
    }
  }

  onMounted(loadShopInfo)

  return { T, form, loading, saving, toggling, shopStatus, handleSave, handleToggleStatus }
}
