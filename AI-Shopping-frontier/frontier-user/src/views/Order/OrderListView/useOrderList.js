import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, payOrder, confirmDelivery, deleteOrder, submitReturnRequest, submitReturnLogistics } from '@/api/order'
import { getContactList } from '@/api/contact'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import Swal from 'sweetalert2'
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
    { key: ORDER_STATUS.RETURN_PENDING, label: STATUS_TEXT[ORDER_STATUS.RETURN_PENDING] },
    { key: ORDER_STATUS.RETURNING, label: STATUS_TEXT[ORDER_STATUS.RETURNING] },
    { key: ORDER_STATUS.RETURNED, label: STATUS_TEXT[ORDER_STATUS.RETURNED] },
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

  const handleSubmitLogistics = async (order) => {
    let contacts = []
    try {
      const res = await getContactList()
      contacts = res?.data?.contacts || res?.contacts || []
    } catch {}

    const { value: formValues } = await Swal.fire({
      title: '填写退货物流',
      html: `
        <div style="text-align:left">
          <label style="display:block;margin-bottom:4px;font-weight:500">快递单号</label>
          <input id="swal-tracking" class="swal2-input" placeholder="请输入快递单号" style="width:100%;box-sizing:border-box">
          <label style="display:block;margin:12px 0 4px;font-weight:500">选择地址</label>
          <select id="swal-contact" class="swal2-input" style="width:100%;box-sizing:border-box">
            ${contacts.map(c => `<option value="${c.id}">${c.name} ${c.phone} - ${c.address}</option>`).join('')}
          </select>
        </div>
      `,
      showCancelButton: true,
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      confirmButtonColor: '#3b82f6',
      cancelButtonColor: '#6b7280',
      preConfirm: () => {
        const tracking = document.getElementById('swal-tracking').value.trim()
        const contactId = document.getElementById('swal-contact').value
        if (!tracking) return Swal.showValidationMessage('请输入快递单号')
        if (!contactId) return Swal.showValidationMessage('请选择地址')
        return { trackingNumber: tracking, contactId: Number(contactId) }
      }
    })
    if (!formValues) return
    try {
      await submitReturnLogistics(order.orderId, formValues)
      showSuccess('退货物流已提交')
      await loadOrders()
    } catch (e) {
      showError(e?.response?.data?.message || '提交退货物流失败')
    }
  }

  onMounted(loadOrders)

  return {
    logisticsVisible,
    logisticsOrderId,
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
    handleReturn,
    handleSubmitLogistics,
    payingOrder,
    showPaymentModal,
    onPaymentSuccess,
    onPayLater
  }
}
