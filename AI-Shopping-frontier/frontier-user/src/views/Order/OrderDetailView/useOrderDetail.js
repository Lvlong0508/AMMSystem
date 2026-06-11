import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderById, cancelOrder, confirmDelivery, deleteOrder } from '@/api/order'
import { showSuccess, showError, showConfirm } from '@/utils/swal'

export function useOrderDetail() {
  const route = useRoute()
  const router = useRouter()
  const order = ref(null)
  const loading = ref(true)
  const showActionSheet = ref(false)
  const actionType = ref('')

  const actionTitle = computed(() => {
    if (actionType.value === 'cancel') return '确认取消订单？'
    if (actionType.value === 'confirm') return '确认收货？'
    return ''
  })

  const actionDesc = computed(() => {
    if (actionType.value === 'cancel') return '取消后无法恢复'
    if (actionType.value === 'confirm') return '请确认已收到商品'
    return ''
  })

  const actionConfirmText = computed(() => {
    if (actionType.value === 'cancel') return '确认取消'
    if (actionType.value === 'confirm') return '确认收货'
    return ''
  })

  const loadOrder = async () => {
    loading.value = true
    try {
      const orderId = route.params.id
      const res = await getOrderById(orderId)
      order.value = res.data || res
    } catch {
      showError('加载订单详情失败')
    } finally {
      loading.value = false
    }
  }

  const goBack = () => {
    router.push('/order')
  }

  const showPaymentModal = ref(false)

  const handlePay = async () => {
    showPaymentModal.value = true
  }

  const onPaymentSuccess = async () => {
    showPaymentModal.value = false
    await loadOrder()
  }

  const onPayLater = () => {
    showPaymentModal.value = false
  }

  const handleDelete = async () => {
    const confirmed = await showConfirm('确认删除', '删除后无法恢复，确定要删除该订单吗？', '确认删除', '再想想')
    if (!confirmed.isConfirmed) return
    try {
      await deleteOrder(order.value.orderId)
      showSuccess('订单已删除')
      router.push('/order')
    } catch {
      showError('删除失败')
    }
  }

  const handleViewLogistics = () => {}

  const handleReview = () => {}

  const confirmAction = async () => {
    showActionSheet.value = false
    try {
      if (actionType.value === 'cancel') {
        await cancelOrder(order.value.orderId)
        showSuccess('订单已取消')
      } else if (actionType.value === 'confirm') {
        await confirmDelivery(order.value.orderId)
        showSuccess('已确认收货')
      }
      await loadOrder()
    } catch {
      showError('操作失败')
    }
  }

  onMounted(loadOrder)

  return {
    order,
    loading,
    showActionSheet,
    actionType,
    actionTitle,
    actionDesc,
    actionConfirmText,
    goBack,
    handlePay,
    handleDelete,
    handleViewLogistics,
    handleReview,
    confirmAction,
    showPaymentModal,
    onPaymentSuccess,
    onPayLater
  }
}
