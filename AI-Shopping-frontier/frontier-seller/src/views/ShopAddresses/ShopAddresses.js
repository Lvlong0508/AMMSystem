import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAddressList, createAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/contact'
import { parseAddress, buildAddressString } from '@/utils/region'
import * as T from './Text.js'

export function useShopAddresses() {
  const route = useRoute()
  const shopId = ref(route.params.shopId)
  const addresses = ref([])
  const loading = ref(false)
  const activeTab = ref(1)
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const editingId = ref(null)
  const form = ref({ addressType: 1, name: '', phone: '', region: [], addressDetail: '', isDefault: 0 })

  const shippingAddresses = computed(() => addresses.value.filter(a => a.addressType === 1))
  const returnAddresses = computed(() => addresses.value.filter(a => a.addressType === 2))
  const currentAddresses = computed(() => activeTab.value === 1 ? shippingAddresses.value : returnAddresses.value)

  async function loadAddresses() {
    loading.value = true
    try {
      const res = await getAddressList()
      addresses.value = res?.data?.addresses || []
    } catch (e) {
      ElMessage.error('加载失败')
      addresses.value = []
    } finally { loading.value = false }
  }

  function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '-' }

  function showAddDialog() {
    isEdit.value = false; editingId.value = null
    form.value = { addressType: activeTab.value, name: '', phone: '', region: [], addressDetail: '', isDefault: 0 }
    dialogVisible.value = true
  }

  function showEditDialog(addr) {
    isEdit.value = true; editingId.value = addr.id
    const parsed = parseAddress(addr.address || '')
    form.value = {
      addressType: addr.addressType,
      name: addr.name,
      phone: addr.phone,
      region: parsed.region,
      addressDetail: parsed.detail,
      isDefault: addr.isDefault || 0
    }
    dialogVisible.value = true
  }

  function closeDialog() { dialogVisible.value = false }

  function validate() {
    if (!form.value.name.trim()) { ElMessage.warning(T.NAME_REQUIRED); return false }
    if (!form.value.phone.trim()) { ElMessage.warning(T.PHONE_REQUIRED); return false }
    if (!form.value.region || form.value.region.length === 0) { ElMessage.warning(T.REGION_REQUIRED); return false }
    return true
  }

  async function handleSubmit() {
    if (!validate()) return
    submitting.value = true
    try {
      const payload = {
        addressType: form.value.addressType,
        name: form.value.name,
        phone: form.value.phone,
        address: buildAddressString(form.value.region, form.value.addressDetail),
        isDefault: form.value.isDefault
      }
      const res = isEdit.value
        ? await updateAddress(editingId.value, payload)
        : await createAddress(payload)
      if (res?.message?.includes('成功')) {
        ElMessage.success(isEdit.value ? T.SUCCESS_EDIT : T.SUCCESS_ADD)
        closeDialog()
        await loadAddresses()
      } else {
        ElMessage.error(res?.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    } finally { submitting.value = false }
  }

  async function handleDelete(addr) {
    try { await ElMessageBox.confirm(T.CONFIRM_DELETE, { type: 'warning' }) } catch { return }
    try {
      const res = await deleteAddress(addr.id)
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_DELETE)
        await loadAddresses()
      } else { ElMessage.error(res?.message || '删除失败') }
    } catch (e) { ElMessage.error('删除失败') }
  }

  async function handleSetDefault(addr) {
    try {
      const res = await setDefaultAddress(addr.id)
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_DEFAULT)
        await loadAddresses()
      } else { ElMessage.error(res?.message || '设置失败') }
    } catch (e) { ElMessage.error('设置失败') }
  }

  onMounted(loadAddresses)

  return { T, addresses, loading, activeTab, dialogVisible, isEdit, submitting, form, shippingAddresses, returnAddresses, currentAddresses, loadAddresses, formatDate, showAddDialog, showEditDialog, closeDialog, handleSubmit, handleDelete, handleSetDefault }
}
