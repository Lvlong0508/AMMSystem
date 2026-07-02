import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAfterSaleList, submitReturnLogistics, getMerchantReturnAddress, deleteReturnRequest } from '@/api/order'
import { getContactList } from '@/api/contact'
import { showSuccess, showError } from '@/utils/swal'
import Swal from 'sweetalert2'

export function useAfterSale() {
  const router = useRouter()
  const list = ref([])
  const loading = ref(true)
  const showReturnLogisticsModal = ref(false)
  const currentItem = ref(null)
  const shopReturnName = ref('')
  const shopReturnAddress = ref('')
  const shopReturnPhone = ref('')
  const contacts = ref([])
  const loadingAddress = ref(false)

  const goBack = () => {
    router.push('/order')
  }

  const loadData = async () => {
    loading.value = true
    try {
      const res = await getAfterSaleList()
      list.value = res.data || (Array.isArray(res) ? res : [])
    } catch {
      showError('加载售后信息失败')
    } finally {
      loading.value = false
    }
  }

  const showSubmitLogisticsBtn = (item) => {
    return item?.returnStatus === 'agreed' && !item.logisticsId
  }

  const showDeleteBtn = (item) => {
    return item?.returnStatus === 'rejected' || item?.orderStatus === 'RETURNED'
  }

  const getReturnStatusTag = (item) => {
    if (!item?.returnStatus) return ''
    if (item.orderStatus === 'RETURNING') return '退货中'
    if (item.orderStatus === 'RETURNED') return '已退货'
    const map = { applying: '审核中', agreed: '已同意', rejected: '已拒绝' }
    return map[item.returnStatus] || ''
  }

  const getReturnStatusClass = (item) => {
    if (!item?.returnStatus) return ''
    if (item.orderStatus === 'RETURNING') return 'returning'
    if (item.orderStatus === 'RETURNED') return 'returned'
    return item.returnStatus
  }

  const handleSubmitLogistics = async (item) => {
    currentItem.value = item
    showReturnLogisticsModal.value = true
    loadingAddress.value = true
    try {
      const [addrData, contactData] = await Promise.all([
        getMerchantReturnAddress(item.shopId),
        getContactList()
      ])
      shopReturnName.value = addrData?.name || ''
      shopReturnAddress.value = addrData?.address || ''
      shopReturnPhone.value = addrData?.phone || ''
      contacts.value = contactData?.contacts || []
    } catch {
      shopReturnName.value = ''
      shopReturnAddress.value = ''
      shopReturnPhone.value = ''
      contacts.value = []
    } finally {
      loadingAddress.value = false
    }
  }

  const onLogisticsSubmit = async (logisticsData) => {
    if (!currentItem.value) return
    try {
      await submitReturnLogistics(currentItem.value.orderId, logisticsData)
      showSuccess('退货物流已提交')
      showReturnLogisticsModal.value = false
      currentItem.value = null
      await loadData()
    } catch (e) {
      showError(e?.response?.data?.message || '提交退货物流失败')
    }
  }

  const handleDeleteReturn = async (item) => {
    const { isConfirmed } = await Swal.fire({
      title: '确认删除',
      text: '删除后将无法恢复，确定要删除该退货申请吗？',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    if (!isConfirmed) return
    try {
      await deleteReturnRequest(item.orderId)
      showSuccess('退货申请已删除')
      await loadData()
    } catch (e) {
      showError(e?.response?.data?.message || '删除退货申请失败')
    }
  }

  const detailItem = ref(null)
  const showDetailModal = ref(false)

  const openDetail = (item) => {
    detailItem.value = item
    showDetailModal.value = true
  }

  const closeDetail = () => {
    showDetailModal.value = false
  }

  onMounted(loadData)

  return {
    list,
    loading,
    goBack,
    showSubmitLogisticsBtn,
    showDeleteBtn,
    getReturnStatusTag,
    getReturnStatusClass,
    handleDeleteReturn,
    handleSubmitLogistics,
    onLogisticsSubmit,
    showReturnLogisticsModal,
    shopReturnName,
    shopReturnAddress,
    shopReturnPhone,
    contacts,
    loadingAddress,
    detailItem,
    showDetailModal,
    openDetail,
    closeDetail
  }
}
