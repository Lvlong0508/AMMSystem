import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderListByShop, getOrderDetail, shipOrder } from '@/api/order'
import { getAddressList } from '@/api/contact'
import { getShopDetail } from '@/api/shop'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useShopOrders() {
  const route = useRoute()
  const shopId = computed(() => route.params.shopId)
  const shopInfo = ref(null)
  const orders = ref([])
  const loading = ref(false)
  const filterStatus = ref('')
  const searchKeyword = ref('')

  const detailVisible = ref(false)
  const selectedOrder = ref(null)

  const shipVisible = ref(false)
  const shipFormRef = ref(null)
  const shipForm = ref({ trackingNumber: '', selectedContactId: null })
  const shipping = ref(false)
  const contacts = ref([])
  const contactsLoading = ref(false)
  const trackingRule = [
    { required: true, message: '请输入物流单号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9\-]{6,20}$/, message: '物流单号格式不正确（6-20位字母、数字或连字符）', trigger: 'blur' },
  ]

  const filteredOrders = computed(() => {
    let r = orders.value
    if (filterStatus.value) r = r.filter(o => o.orderStatus === filterStatus.value)
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase()
      r = r.filter(o => o.orderId?.toLowerCase().includes(kw) || o.productName?.toLowerCase().includes(kw))
    }
    return r
  })

  async function loadShopInfo() {
    try { const res = await getShopDetail(shopId.value); if (res?.data) shopInfo.value = res.data } catch (e) { console.error(e) }
  }

  async function loadOrders() {
    loading.value = true
    try {
      const res = await getOrderListByShop(shopId.value)
      orders.value = res?.data ? (Array.isArray(res.data) ? res.data : []) : res?.orders || []
    } catch (e) {
      ElMessage.error('加载失败')
      orders.value = []
    } finally { loading.value = false }
  }

  async function showDetail(order) {
    try {
      const res = await getOrderDetail(shopId.value, order.orderId)
      selectedOrder.value = res?.data || order
    } catch {
      selectedOrder.value = order
    }
    detailVisible.value = true
  }

  function closeDetail() {
    detailVisible.value = false
    selectedOrder.value = null
  }

  function handleShipFromDetail(order) {
    shipForm.value = { trackingNumber: '', selectedContactId: null }
    selectedOrder.value = order
    shipVisible.value = true
    detailVisible.value = false
    loadContacts()
  }

  async function loadContacts() {
    contactsLoading.value = true
    try {
      const res = await getAddressList()
      const list = res?.data?.addresses || []
      contacts.value = list.filter(a => a.addressType === 1)
      if (contacts.value.length > 0 && !shipForm.value.selectedContactId) {
        shipForm.value.selectedContactId = (contacts.value.find(c => c.isDefault === 1) || contacts.value[0]).id
      }
    } catch {
      contacts.value = []
    } finally {
      contactsLoading.value = false
    }
  }

  async function handleShip(order) {
    shipForm.value = { trackingNumber: '', selectedContactId: null }
    selectedOrder.value = order
    shipVisible.value = true
    await loadContacts()
  }

  async function confirmShip() {
    if (!shipFormRef.value) return
    try { await shipFormRef.value.validate() } catch { return }
    shipping.value = true
    try {
      const res = await shipOrder(selectedOrder.value.orderId, {
        trackingNumber: shipForm.value.trackingNumber,
        contactId: shipForm.value.selectedContactId,
        shopId: shopId.value,
      })
      if (res?.message?.includes('成功') || res?.code === 200) {
        ElMessage.success('发货成功')
        shipVisible.value = false
        await loadOrders()
      } else {
        ElMessage.error(res?.message || '发货失败')
      }
    } catch {
      ElMessage.error('发货失败')
    } finally {
      shipping.value = false
    }
  }

  function getStatusType(status) {
    const m = { PENDING: 'info', PAID: 'warning', SHIPPED: 'primary', DELIVERED: 'success', CANCELLED: 'danger', RETURNED: 'danger' }
    return m[status] || 'info'
  }

  function getStatusText(s) { return STATUS_TEXT[s] || s }
  function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '-' }
  function formatPrice(p) { return p != null ? `¥${Number(p).toFixed(2)}` : '-' }

  onMounted(() => { loadShopInfo(); loadOrders() })

  return { T, shopInfo, orders, loading, filterStatus, searchKeyword, filteredOrders, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, handleShip, confirmShip, ORDER_STATUS, STATUS_TEXT, shipVisible, shipFormRef, shipForm, shipping, contacts, contactsLoading, trackingRule }
}
