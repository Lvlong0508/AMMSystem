import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderListByShop } from '@/api/order'
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

  function getStatusType(status) {
    const m = { PENDING: 'info', PAID: 'warning', SHIPPED: 'primary', DELIVERED: 'success', CANCELLED: 'danger', RETURNED: 'danger' }
    return m[status] || 'info'
  }

  function getStatusText(s) { return STATUS_TEXT[s] || s }
  function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '-' }
  function formatPrice(p) { return p != null ? `¥${Number(p).toFixed(2)}` : '-' }

  function showDetail(order) { selectedOrder.value = order; detailVisible.value = true }
  function closeDetail() { detailVisible.value = false; selectedOrder.value = null }

  onMounted(() => { loadShopInfo(); loadOrders() })

  return { T, shopInfo, orders, loading, filterStatus, searchKeyword, filteredOrders, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, ORDER_STATUS }
}
