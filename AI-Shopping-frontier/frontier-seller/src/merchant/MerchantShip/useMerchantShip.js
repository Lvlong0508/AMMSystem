// src/merchant/MerchantShip/useMerchantShip.js
import { ref, onMounted, computed } from 'vue'
import {
  shipOrder
} from '../../api/order.js'
import { getAllContacts, getContactById, getShipAddressList } from '../../api/contact.js'
import { getLogisticsById } from '../../api/logistics.js'
import { getProductById } from '../../api/product.js'
import { shopApi } from '../../api/shop.js'
import { showSuccess, showError } from '../../utils/swal.js'
import { ORDER_STATUS, STATUS_CLASS, STATUS_TEXT } from '../../config/orderStatus.js'

export function useMerchantShip() {
  // 响应式数据
  const orders = ref([])
  const loading = ref(false)
  const filterStatus = ref('') // 默认显示全部订单
  const searchCustomer = ref('')

  // 店铺相关
  const shops = ref([])
  const currentShopId = ref(null)

  // 联系人列表
  const contacts = ref([])
  const contactsLoading = ref(false)

  // 详情弹窗
  const detailVisible = ref(false)
  const selectedOrder = ref(null)
  const detailLoading = ref(false)

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

  // 是否有多个店铺（需要显示店铺选择框）
  const hasMultipleShops = computed(() => shops.value.length > 1)

  // 加载店铺列表
  const loadShops = async () => {
    try {
      const res = await shopApi.list()
      if (res?.success && res?.shops) {
        shops.value = res.shops
      } else if (res?.data?.shops) {
        shops.value = res.data.shops
      }
      if (shops.value.length > 0 && !currentShopId.value) {
        currentShopId.value = shops.value[0].id
        loadOrders()
      }
    } catch (error) {
      console.error('加载店铺列表失败:', error)
    }
  }

  // 切换店铺
  const switchShop = (shopId) => {
    currentShopId.value = shopId
    loadOrders()
  }

  // 加载订单列表（强制使用shop服务API）
  const loadOrders = async () => {
    if (!currentShopId.value) {
      orders.value = []
      return
    }
    loading.value = true
    try {
      const res = await shopApi.orders(currentShopId.value)
      let orderList = res?.orders || res?.data?.orders || res?.data || []
      if (filterStatus.value) {
        orderList = orderList.filter(o => o.orderStatus === filterStatus.value)
      }
      // 按日期降序排序（最新的在前面）
      orders.value = orderList.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate))
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
    await loadOrders()
    if (searchCustomer.value.trim()) {
      const keyword = searchCustomer.value.trim().toLowerCase()
      orders.value = orders.value.filter(order => 
        order.contact?.name?.toLowerCase().includes(keyword)
      )
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

  // 加载联系人列表（店铺地址）
  const loadContacts = async () => {
    contactsLoading.value = true
    try {
      if (currentShopId.value) {
        const res = await getShipAddressList(currentShopId.value)
        if (res?.data) {
          contacts.value = res.data
          // 如果有联系人，默认选择第一个
          if (res.data.length > 0 && !shipForm.value.selectedContactId) {
            shipForm.value.selectedContactId = res.data[0].id
          }
        } else {
          contacts.value = []
        }
      } else {
        contacts.value = []
      }
    } catch (error) {
      console.error('加载联系人失败:', error)
      showError('加载联系人失败')
      contacts.value = []
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

  // 显示订单详情（异步获取完整信息）
  const showOrderDetail = async (order) => {
    selectedOrder.value = order
    detailVisible.value = true
    // 始终尝试获取最新详情（包括联系人信息）
    if (currentShopId.value) {
      detailLoading.value = true
      try {
        const res = await shopApi.orderDetail(currentShopId.value, order.orderId)
        if (res?.success && res?.order) {
          selectedOrder.value = { ...order, ...res.order }
        }
      } catch (error) {
        console.error('获取订单详情失败:', error)
      } finally {
        detailLoading.value = false
      }
    }
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
      // 调用发货接口（创建物流信息并更新订单状态为已发货）
      const res = await shipOrder(
        shipForm.value.orderId,
        shipForm.value.trackingNumber,
        shipForm.value.selectedContactId,
        shipForm.value.shippingDate
      )
      if (res?.message?.includes('成功')) {
        showSuccess('发货成功')
        closeShipDialog()
        await loadOrders()
      } else {
        showError(res?.message || '发货失败')
      }
    } catch (error) {
      console.error('发货失败:', error)
      showError('发货失败')
    } finally {
      shipping.value = false
    }
  }

  // 初始化加载
  onMounted(async () => {
    await loadShops()
    if (currentShopId.value) {
      await loadOrders()
    }
  })

  return {
    orders,
    loading,
    filterStatus,
    searchCustomer,
    pendingShipCount,
    shops,
    currentShopId,
    hasMultipleShops,
    switchShop,
    detailVisible,
    detailLoading,
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
