import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getReturnRequestsPending, getReturnRequestsProcessed, getOrderDetail, approveReturn, reviewReturnRequest, confirmReturn } from '@/api/order'
import * as T from './Text.js'

const RETURN_STATUS_TEXT = {
  applying: '待审核',
  agreed: '已同意',
  rejected: '已拒绝'
}

const RETURN_STATUS_TYPE = {
  applying: 'warning',
  agreed: 'success',
  rejected: 'danger'
}

export function useReturnManagement() {
  const route = useRoute()
  const shopId = route.params.shopId
  const loading = ref(false)
  const activeTab = ref('pending')
  const pendingList = ref([])
  const processedList = ref([])
  const detailVisible = ref(false)
  const selectedOrder = ref(null)

  const list = computed(() =>
    activeTab.value === 'pending' ? pendingList.value : processedList.value
  )

  async function loadOrders() {
    loading.value = true
    try {
      const [pendingRes, processedRes] = await Promise.all([
        getReturnRequestsPending(shopId),
        getReturnRequestsProcessed(shopId)
      ])
      pendingList.value = pendingRes?.data || pendingRes?.list || []
      processedList.value = processedRes?.data || processedRes?.list || []
    } catch (e) {
      console.error('加载退货请求失败:', e)
      ElMessage.error('加载失败')
    } finally {
      loading.value = false
    }
  }

  async function showDetail(item) {
    try {
      const res = await getOrderDetail(shopId, item.orderId)
      selectedOrder.value = { ...item, ...(res?.data || {}) }
    } catch {
      selectedOrder.value = item
    }
    detailVisible.value = true
  }

  function closeDetail() {
    detailVisible.value = false
    selectedOrder.value = null
  }

  async function handleApprove(item) {
    try {
      const res = await approveReturn(item.orderId, shopId)
      ElMessage.success(res?.message || '审核通过')
      closeDetail()
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  async function handleReject(item) {
    try {
      const res = await reviewReturnRequest(item.orderId, shopId, 'rejected')
      ElMessage.success(res?.message || '已拒绝')
      closeDetail()
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  async function handleConfirm(item) {
    try {
      const res = await confirmReturn(item.orderId, shopId)
      ElMessage.success(res?.message || '确认成功')
      closeDetail()
      await loadOrders()
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }

  function getStatusText(status) { return RETURN_STATUS_TEXT[status] || status }
  function getStatusType(status) { return RETURN_STATUS_TYPE[status] || 'info' }
  function formatDate(dateStr) { return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-' }

  onMounted(loadOrders)

  return {
    T, list, loading, activeTab, pendingList, processedList,
    detailVisible, selectedOrder,
    loadOrders, handleApprove, handleReject, handleConfirm, showDetail, closeDetail,
    getStatusText, getStatusType, formatDate
  }
}
