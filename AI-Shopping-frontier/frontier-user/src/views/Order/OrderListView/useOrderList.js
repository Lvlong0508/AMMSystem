import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, payOrder, confirmDelivery, deleteOrder, submitReturnRequest } from '@/api/order'
import { ORDER_STATUS } from '@/config/orderStatus'
import Swal from 'sweetalert2'
import { showSuccess, showError, showConfirm } from '@/utils/swal'

export function useOrderList() {
  const router = useRouter()
  const orders = ref([])
  const loading = ref(true)
  const activeFilter = ref('ALL')
  const searchQuery = ref('')

  const filters = [
    { key: 'ALL', label: '全部' },
    { key: ORDER_STATUS.PENDING, label: '待支付' },
    { key: ORDER_STATUS.PAID, label: '待发货' },
    { key: 'COMPLETED', label: '已完成' }
  ]

  const filteredOrders = computed(() => {
    let result = orders.value

    if (activeFilter.value === 'COMPLETED') {
      result = result.filter(o =>
        [ORDER_STATUS.SHIPPED, ORDER_STATUS.DELIVERED, ORDER_STATUS.RETURNED].includes(o.orderStatus)
      )
    } else if (activeFilter.value !== 'ALL') {
      result = result.filter(o => o.orderStatus === activeFilter.value)
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.trim().toLowerCase()
      result = result.filter(o =>
        (o.productName && o.productName.toLowerCase().includes(q)) ||
        (o.shopName && o.shopName.toLowerCase().includes(q))
      )
    }

    return result
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
  const logisticsVisible = ref(false)
  const logisticsOrderId = ref('')

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

  const handleViewLogistics = (order) => {
    logisticsOrderId.value = order.orderId
    logisticsVisible.value = true
  }

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

  const handleReturn = async (order) => {
    const { value: reason } = await Swal.fire({
      title: '退货申请',
      input: 'textarea',
      inputLabel: '请输入退货原因',
      inputPlaceholder: '请简要描述退货原因...',
      showCancelButton: true,
      confirmButtonText: '提交申请',
      cancelButtonText: '取消',
      confirmButtonColor: '#3b82f6',
      cancelButtonColor: '#6b7280',
      inputValidator: (value) => {
        if (!value) return '请输入退货原因'
      }
    })
    if (!reason) return
    try {
      await submitReturnRequest(order.orderId, { returnReason: reason })
      showSuccess('退货申请已提交，等待商家审核')
      await loadOrders()
    } catch (e) {
      showError(e?.response?.data?.message || '提交退货申请失败')
    }
  }

  const handleAfterSale = () => {
    router.push('/after-sales')
  }

  onMounted(loadOrders)

  return {
    orders,
    loading,
    activeFilter,
    searchQuery,
    filters,
    filteredOrders,
    handleViewDetail,
    handleAfterSale,
    handleCancel,
    handleDelete,
    handlePay,
    handleViewLogistics,
    handleConfirm,
    handleReturn,
    logisticsVisible,
    logisticsOrderId,
    payingOrder,
    showPaymentModal,
    onPaymentSuccess,
    onPayLater
  }
}
