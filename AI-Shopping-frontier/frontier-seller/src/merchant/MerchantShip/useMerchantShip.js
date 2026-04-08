// src/merchant/MerchantShip/useMerchantShip.js
import { ref, onMounted, computed } from 'vue'
import {
  getAllOrders,
  getOrdersByStatus,
  getOrdersByCustomerName,
  updateOrderStatus,
  shipOrder
} from '../../api/order.js'
import { getAllContacts, getContactById } from '../../api/contact.js'
import { getLogisticsById } from '../../api/logistics.js'
import { showSuccess, showError } from '../../utils/swal.js'
import { ORDER_STATUS, STATUS_CLASS, STATUS_TEXT } from '../../config/orderStatus.js'

export function useMerchantShip() {
  // 响应式数据
  const orders = ref([])
  const loading = ref(false)
  const filterStatus = ref('') // 默认显示全部订单
  const searchCustomer = ref('')

  // 联系人列表
  const contacts = ref([])
  const contactsLoading = ref(false)

  // 详情弹窗
  const detailVisible = ref(false)
  const selectedOrder = ref(null)

  // 发货弹窗
  const shipVisible = ref(false)
  const shipForm = ref({
    orderId: '',
    trackingNumber: '',
    shippingDate: '',
    selectedContactId: null  // 选中的发货联系人ID
  })
  const shipping = ref(false)

  // 待发货订单数量
  const pendingShipCount = computed(() => {
    return orders.value.filter(o => o.orderStatus === ORDER_STATUS.PAID).length
  })

  // 加载订单列表（包含联系人和物流信息）
  const loadOrders = async () => {
    loading.value = true
    try {
      let res
      if (filterStatus.value) {
        res = await getOrdersByStatus(filterStatus.value)
      } else {
        res = await getAllOrders()
      }
      if (res.data?.orders) {
        // 为每个订单加载联系人和物流信息
        const ordersWithDetails = await Promise.all(
          res.data.orders.map(async (order) => {
            const enrichedOrder = { ...order }
            // 加载联系人信息
            if (order.contactId) {
              try {
                const contactRes = await getContactById(order.contactId)
                if (contactRes.data?.data) {
                  enrichedOrder.contact = contactRes.data.data
                }
              } catch (e) {
                console.warn('加载联系人信息失败:', e)
              }
            }
            // 加载物流信息
            if (order.logisticsId) {
              try {
                const logisticsRes = await getLogisticsById(order.logisticsId)
                if (logisticsRes.data?.data) {
                  const logistics = logisticsRes.data.data
                  enrichedOrder.logistics = logistics
                  // 尝试加载发货人（物流中的联系人）信息，失败不影响物流信息显示
                  if (logistics.contactId) {
                    try {
                      const shipperRes = await getContactById(logistics.contactId)
                      if (shipperRes.data?.data) {
                        logistics.shipper = shipperRes.data.data
                      }
                    } catch (shipperErr) {
                      console.warn('加载发货人信息失败:', shipperErr)
                    }
                  }
                }
              } catch (e) {
                console.warn('加载物流信息失败:', e)
              }
            }
            return enrichedOrder
          })
        )
        orders.value = ordersWithDetails
      }
    } catch (error) {
      console.error('加载订单失败:', error)
      showError('加载订单失败')
    } finally {
      loading.value = false
    }
  }

  // 按状态筛选
  const handleStatusFilter = async () => {
    await loadOrders()
  }

  // 按客户搜索
  const handleCustomerSearch = async () => {
    if (!searchCustomer.value.trim()) {
      await loadOrders()
      return
    }
    loading.value = true
    try {
      const res = await getOrdersByCustomerName(searchCustomer.value.trim())
      if (res.data?.orders) {
        // 为每个订单加载联系人和物流信息
        const ordersWithDetails = await Promise.all(
          res.data.orders.map(async (order) => {
            const enrichedOrder = { ...order }
            if (order.contactId) {
              try {
                const contactRes = await getContactById(order.contactId)
                if (contactRes.data?.data) {
                  enrichedOrder.contact = contactRes.data.data
                }
              } catch (e) {
                console.warn('加载联系人信息失败:', e)
              }
            }
            if (order.logisticsId) {
              try {
                const logisticsRes = await getLogisticsById(order.logisticsId)
                if (logisticsRes.data?.data) {
                  const logistics = logisticsRes.data.data
                  // 加载发货人（物流中的联系人）信息
                  if (logistics.contactId) {
                    const shipperRes = await getContactById(logistics.contactId)
                    if (shipperRes.data?.data) {
                      logistics.shipper = shipperRes.data.data
                    }
                  }
                  enrichedOrder.logistics = logistics
                }
              } catch (e) {
                console.warn('加载物流信息失败:', e)
              }
            }
            return enrichedOrder
          })
        )
        // 显示待发货、已发货和已退货的订单
        orders.value = ordersWithDetails.filter(o => 
          o.orderStatus === ORDER_STATUS.PAID || 
          o.orderStatus === ORDER_STATUS.SHIPPED ||
          o.orderStatus === ORDER_STATUS.RETURNED
        )
      }
    } catch (error) {
      console.error('搜索订单失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 状态样式
  const getStatusClass = (status) => {
    return STATUS_CLASS[status] || ''
  }

  // 状态文本
  const getStatusText = (status) => {
    return STATUS_TEXT[status] || status
  }

  // 加载联系人列表
  const loadContacts = async () => {
    contactsLoading.value = true
    try {
      const res = await getAllContacts()
      if (res.data?.data) {
        contacts.value = res.data.data
      }
    } catch (error) {
      console.error('加载联系人失败:', error)
      showError('加载联系人失败')
    } finally {
      contactsLoading.value = false
    }
  }

  // 格式化日期
  const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN')
  }

  // 显示订单详情
  const showOrderDetail = (order) => {
    selectedOrder.value = order
    detailVisible.value = true
  }

  // 关闭详情
  const closeDetail = () => {
    detailVisible.value = false
    selectedOrder.value = null
  }

  // 显示发货弹窗
  const showShipDialog = async (order) => {
    shipForm.value.orderId = order.orderId
    shipForm.value.trackingNumber = ''
    // 使用当前日期时间，格式为datetime-local需要的 YYYY-MM-DDTHH:mm
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    const hours = String(now.getHours()).padStart(2, '0')
    const minutes = String(now.getMinutes()).padStart(2, '0')
    shipForm.value.shippingDate = `${year}-${month}-${day}T${hours}:${minutes}`
    shipForm.value.selectedContactId = null
    selectedOrder.value = order
    shipVisible.value = true
    // 加载联系人列表
    await loadContacts()
  }

  // 关闭发货弹窗
  const closeShipDialog = () => {
    shipVisible.value = false
    shipForm.value = { orderId: '', trackingNumber: '', selectedContactId: null }
  }

  // 处理发货
  const handleShip = async () => {
    if (!shipForm.value.trackingNumber.trim()) {
      showError('请输入物流单号')
      return
    }
    if (!shipForm.value.selectedContactId) {
      showError('请选择发货联系人')
      return
    }

    // 根据选中的ID获取联系人信息
    const selectedContact = contacts.value.find(c => c.id === shipForm.value.selectedContactId)
    if (!selectedContact) {
      showError('选择的联系人无效')
      return
    }

    shipping.value = true
    try {
      // 调用发货接口（创建物流信息并更新订单）
      const res = await shipOrder(
        shipForm.value.orderId,
        shipForm.value.trackingNumber,
        shipForm.value.selectedContactId,
        shipForm.value.shippingDate
      )
      if (res.data?.message?.includes('成功')) {
        // 更新状态为已发货
        await updateOrderStatus(shipForm.value.orderId, ORDER_STATUS.SHIPPED)
        showSuccess('发货成功')
        closeShipDialog()
        await loadOrders()
      } else {
        showError(res.data?.message || '发货失败')
      }
    } catch (error) {
      console.error('发货失败:', error)
      showError('发货失败')
    } finally {
      shipping.value = false
    }
  }

  // 初始化加载
  onMounted(() => {
    loadOrders()
  })

  return {
    orders,
    loading,
    filterStatus,
    searchCustomer,
    pendingShipCount,
    detailVisible,
    selectedOrder,
    shipVisible,
    shipForm,
    shipping,
    contacts,
    contactsLoading,
    loadOrders,
    handleStatusFilter,
    handleCustomerSearch,
    getStatusClass,
    getStatusText,
    formatDate,
    showOrderDetail,
    closeDetail,
    showShipDialog,
    closeShipDialog,
    handleShip
  }
}
