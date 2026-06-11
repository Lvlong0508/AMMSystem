import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderById, cancelOrder, confirmDelivery, deleteOrder, submitReturnRequest, submitReturnLogistics } from '@/api/order'
import { getContactList } from '@/api/contact'
import Swal from 'sweetalert2'
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

  const logisticsVisible = ref(false)

  const handleViewLogistics = () => {
    logisticsVisible.value = true
  }

  const handleReturn = async () => {
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
      await submitReturnRequest(order.value.orderId, { returnReason: reason })
      showSuccess('退货申请已提交，等待商家审核')
      await loadOrder()
    } catch (e) {
      showError(e?.response?.data?.message || '提交退货申请失败')
    }
  }

  const handleSubmitLogistics = async () => {
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
      await submitReturnLogistics(order.value.orderId, formValues)
      showSuccess('退货物流已提交')
      await loadOrder()
    } catch (e) {
      showError(e?.response?.data?.message || '提交退货物流失败')
    }
  }

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
    logisticsVisible,
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
    handleReturn,
    handleSubmitLogistics,
    confirmAction,
    showPaymentModal,
    onPaymentSuccess,
    onPayLater
  }
}
