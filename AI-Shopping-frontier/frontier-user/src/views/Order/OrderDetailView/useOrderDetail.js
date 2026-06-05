import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderById, cancelOrder, payOrder, confirmDelivery } from '@/api/order'
import { showSuccess, showError } from '@/utils/swal'

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
      const orderId = route.params.orderId
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

  const handlePay = async () => {
    try {
      await payOrder(order.value.orderId)
      showSuccess('支付成功')
      await loadOrder()
    } catch {
      showError('支付失败')
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
    handleViewLogistics,
    handleReview,
    confirmAction
  }
}
