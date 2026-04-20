import { ref, onMounted } from 'vue'
import {
  getAllOrders,
  getOrdersByStatus,
  getOrdersByCustomerName,
  updateOrderStatus,
  deleteOrder
} from '../../api/order.js'
import { getContactById } from '../../api/contact.js'
import { getLogisticsById } from '../../api/logistics.js'
import { getProductById } from '../../api/product.js'
import { showSuccess, showError, showConfirm } from '../../utils/swal.js'
import { ORDER_MESSAGES } from '../../config/messages.js'
import { ORDER_STATUS, STATUS_CLASS, STATUS_TEXT } from '../../config/orderStatus.js'

export function useOrderManager() {
  // 响应式数据
  const orders = ref([])
  const loading = ref(false)
  const filterStatus = ref('')
  const searchCustomer = ref('')

  // 详情弹窗
  const detailVisible = ref(false)
  const selectedOrder = ref(null)

  // 加载订单列表（包含联系人和物流信息）
  const loadOrders = async () => {
    loading.value = true
    try {
      const res = await getAllOrders()
      if (res.data?.orders) {
        // 为每个订单加载联系人和物流信息
        const ordersWithDetails = await Promise.all(
          res.data.orders.map(async (order) => {
            const enrichedOrder = { ...order }
            // 加载商品信息
            if (order.productId) {
              try {
                const productRes = await getProductById(order.productId)
                if (productRes.data?.data) {
                  enrichedOrder.productName = productRes.data.data.name
                }
              } catch (e) {
                console.warn('加载商品信息失败:', e)
              }
            }
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
                  enrichedOrder.logistics = logisticsRes.data.data
                }
              } catch (e) {
                console.warn('加载物流信息失败:', e)
              }
            }
            return enrichedOrder
          })
        )
        // 按时间倒序排列，新的在前面
        orders.value = ordersWithDetails.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
      }
    } catch (error) {
      console.error('加载订单失败:', error)
      showError(ORDER_MESSAGES.LOAD_FAILED)
    } finally {
      loading.value = false
    }
  }

  // 按状态筛选
  const handleStatusFilter = async () => {
    if (!filterStatus.value) {
      await loadOrders()
      return
    }
    loading.value = true
    try {
      const res = await getOrdersByStatus(filterStatus.value)
      if (res.data?.orders) {
        // 为每个订单加载联系人和物流信息
        const ordersWithDetails = await Promise.all(
          res.data.orders.map(async (order) => {
            const enrichedOrder = { ...order }
            // 加载商品信息
            if (order.productId) {
              try {
                const productRes = await getProductById(order.productId)
                if (productRes.data?.data) {
                  enrichedOrder.productName = productRes.data.data.name
                }
              } catch (e) {
                console.warn('加载商品信息失败:', e)
              }
            }
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
                  enrichedOrder.logistics = logisticsRes.data.data
                }
              } catch (e) {
                console.warn('加载物流信息失败:', e)
              }
            }
            return enrichedOrder
          })
        )
        orders.value = ordersWithDetails.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
      }
    } catch (error) {
      console.error('筛选订单失败:', error)
    } finally {
      loading.value = false
    }
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
            // 加载商品信息
            if (order.productId) {
              try {
                const productRes = await getProductById(order.productId)
                if (productRes.data?.data) {
                  enrichedOrder.productName = productRes.data.data.name
                }
              } catch (e) {
                console.warn('加载商品信息失败:', e)
              }
            }
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
                  enrichedOrder.logistics = logisticsRes.data.data
                }
              } catch (e) {
                console.warn('加载物流信息失败:', e)
              }
            }
            return enrichedOrder
          })
        )
        orders.value = ordersWithDetails.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
      }
    } catch (error) {
      console.error('搜索订单失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 判断是否显示物流信息（已发货及以上状态）
  const shouldShowTracking = (status) => {
    return status === ORDER_STATUS.SHIPPED || 
           status === ORDER_STATUS.DELIVERED || 
           status === ORDER_STATUS.RETURNED
  }

  // 状态样式
  const getStatusClass = (status) => {
    return STATUS_CLASS[status] || ''
  }

  // 状态文本
  const getStatusText = (status) => {
    return STATUS_TEXT[status] || status
  }

  // 格式化日期为年月日时分秒
  const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
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

  // 更新订单状态
  const updateStatus = async (orderId, status) => {
    try {
      const res = await updateOrderStatus(orderId, status)
      if (res.data?.message?.includes('成功')) {
        showSuccess(ORDER_MESSAGES.UPDATE_SUCCESS)
        await loadOrders()
      } else {
        showError(res.data?.message || ORDER_MESSAGES.OPERATION_FAILED)
      }
    } catch (error) {
      console.error('更新状态失败:', error)
      showError(ORDER_MESSAGES.UPDATE_FAILED)
    }
  }

  // 确认删除
  const confirmDelete = async (orderId) => {
    const result = await showConfirm(
      ORDER_MESSAGES.DELETE_CONFIRM_TITLE || '智能购物系统',
      ORDER_MESSAGES.DELETE_CONFIRM,
      ORDER_MESSAGES.CONFIRM_BUTTON,
      ORDER_MESSAGES.CANCEL_BUTTON
    )
    if (!result.isConfirmed) return

    try {
      const res = await deleteOrder(orderId)
      if (res.data?.message?.includes('成功')) {
        showSuccess(ORDER_MESSAGES.DELETE_SUCCESS)
        await loadOrders()
      } else {
        showError(res.data?.message || ORDER_MESSAGES.OPERATION_FAILED)
      }
    } catch (error) {
      console.error('删除订单失败:', error)
      showError(ORDER_MESSAGES.DELETE_FAILED)
    }
  }

  // 确认退货
  const confirmReturn = async (orderId) => {
    const result = await showConfirm(
      ORDER_MESSAGES.RETURN_CONFIRM_TITLE || '智能购物系统',
      ORDER_MESSAGES.RETURN_CONFIRM,
      ORDER_MESSAGES.CONFIRM_BUTTON,
      ORDER_MESSAGES.CANCEL_BUTTON
    )
    if (!result.isConfirmed) return

    await updateStatus(orderId, ORDER_STATUS.RETURNED)
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
    detailVisible,
    selectedOrder,
    loadOrders,
    handleStatusFilter,
    handleCustomerSearch,
    getStatusClass,
    getStatusText,
    formatDate,
    showOrderDetail,
    closeDetail,
    updateStatus,
    confirmDelete,
    confirmReturn
  }
}
