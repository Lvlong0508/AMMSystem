import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderListByShop, getOrderDetail, approveReturn, confirmReturn } from '@/api/order'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useReturnManagement() {
  const route = useRoute()
  const shopId = route.params.shopId
  const orders = ref([])
  const loading = ref(false)
  const detailVisible = ref(false)
  const selectedOrder = ref(null)
  const filterStatus = ref('')

  async function loadOrders() {
    loading.value = true
    try {
      const res = await getOrderListByShop(shopId)
      let list = res?.data || res?.orders || []
      if (filterStatus.value) {
        list = list.filter(o => o.orderStatus === filterStatus.value)
      }
      orders.value = list.filter(o =>
        o.orderStatus === ORDER_STATUS.RETURNED ||
        o.orderStatus === 'RETURN_REQUESTED' ||
        o.orderStatus === 'RETURN_APPROVED'
      )
    } catch (e) {
      console.error('加载退货订单失败:', e)
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  async function handleApprove(order) {
    try {
      const res = await approveReturn(order.orderId, shopId)
      ElMessage.success(res?.message || '审核通过')
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  async function handleConfirm(order) {
    try {
      const res = await confirmReturn(order.orderId, shopId)
      ElMessage.success(res?.message || '确认成功')
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  async function showDetail(order) {
    try {
      const res = await getOrderDetail(shopId, order.orderId)
      selectedOrder.value = res?.data || order
      detailVisible.value = true
    } catch (e) {
      ElMessage.error('获取详情失败')
    }
  }

  function getStatusType(status) {
    const map = {
      RETURN_REQUESTED: 'warning',
      RETURN_APPROVED: 'primary',
      RETURNED: 'success'
    }
    return map[status] || 'info'
  }

  function getStatusText(status) { return STATUS_TEXT[status] || status }
  function formatDate(dateStr) { return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-' }
  function formatPrice(price) { return price != null ? `¥${Number(price).toFixed(2)}` : '-' }

  onMounted(loadOrders)

  return {
    T, orders, loading, detailVisible, selectedOrder, filterStatus,
    loadOrders, handleApprove, handleConfirm, showDetail,
    getStatusType, getStatusText, formatDate, formatPrice
  }
}
