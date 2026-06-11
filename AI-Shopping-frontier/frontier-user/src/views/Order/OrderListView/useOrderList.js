import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, payOrder, confirmDelivery, deleteOrder } from '@/api/order'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import { showSuccess, showError, showConfirm } from '@/utils/swal'

export function useOrderList() {
  const router = useRouter()
  const orders = ref([])
  const loading = ref(true)
  const activeFilter = ref('ALL')

  const filters = [
    { key: 'ALL', label: '全部' },
    { key: ORDER_STATUS.PENDING, label: STATUS_TEXT[ORDER_STATUS.PENDING] },
    { key: ORDER_STATUS.PAID, label: STATUS_TEXT[ORDER_STATUS.PAID] },
    { key: ORDER_STATUS.SHIPPED, label: STATUS_TEXT[ORDER_STATUS.SHIPPED] },
    { key: ORDER_STATUS.DELIVERED, label: STATUS_TEXT[ORDER_STATUS.DELIVERED] },
    { key: ORDER_STATUS.CANCELLED, label: STATUS_TEXT[ORDER_STATUS.CANCELLED] }
  ]

  const filteredOrders = computed(() => {
    if (activeFilter.value === 'ALL') return orders.value
    return orders.value.filter(o => o.orderStatus === activeFilter.value)
  })

  const loadOrders = async () => {
    loading.value = true
    try {
      const res = await getOrderList()
      orders.value = res.data || (Array.isArray(res) ? res : [])
    } catch {
      showError('加载订单失败')
    } finally {
      loading.value = false
    }
  }

  const handleViewDetail = (order) => {
    router.push(`/order/${order.orderId}`)
  }

  const handleCancel = async (order) => {
    const result = await showConfirm('确认取消', '确定要取消该订单吗？', '确认取消', '再想想')
    if (!result.isConfirmed) return
    try {
      await cancelOrder(order.orderId)
      showSuccess('订单已取消')
      await loadOrders()
    } catch {
      showError('取消失败')
    }
  }

  const payingOrder = ref(null)
  const showPaymentModal = ref(false)

  const handleDelete = async (order) => {
    const result = await showConfirm('确认删除', '删除后无法恢复，确定要删除该订单吗？', '确认删除', '再想想')
    if (!result.isConfirmed) return
    try {
      await deleteOrder(order.orderId)
      showSuccess('订单已删除')
      await loadOrders()
    } catch {
      showError('删除失败')
    }
  }

  const handlePay = async (order) => {
    payingOrder.value = order
    showPaymentModal.value = true
  }

  const onPaymentSuccess = async () => {
    showPaymentModal.value = false
    payingOrder.value = null
    await loadOrders()
  }

  const onPayLater = () => {
    showPaymentModal.value = false
    payingOrder.value = null
  }

  const handleViewLogistics = (order) => {}

  const handleConfirm = async (order) => {
    const result = await showConfirm('确认收货', '确认已收到商品？', '确认收货', '再想想')
    if (!result.isConfirmed) return
    try {
      await confirmDelivery(order.orderId)
      showSuccess('已确认收货')
      await loadOrders()
    } catch {
      showError('确认失败')
    }
  }

  const handleReview = (order) => {}

  onMounted(loadOrders)

  return {
    orders,
    loading,
    activeFilter,
    filters,
    filteredOrders,
    handleViewDetail,
    handleCancel,
    handleDelete,
    handlePay,
    handleViewLogistics,
    handleConfirm,
    handleReview,
    payingOrder,
    showPaymentModal,
    onPaymentSuccess,
    onPayLater
  }
}
