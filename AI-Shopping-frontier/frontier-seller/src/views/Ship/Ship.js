import { ref, onMounted, computed } from "vue"
import { ElMessage } from "element-plus"
import { getShipmentList, getOrderDetail, shipOrder } from "@/api/order"
import { getShipDefaultAddress } from "@/api/contact"
import { useShopStore } from "@/store/shop"
import { ORDER_STATUS, STATUS_TEXT } from "@/config/orderStatus"
import * as T from "./Text.js"

export function useShip() {
  const shopStore = useShopStore()
  const orders = ref([])
  const loading = ref(false)
  const searchKeyword = ref("")

  const contacts = ref([])
  const contactsLoading = ref(false)

  const detailVisible = ref(false)
  const selectedOrder = ref(null)
  const detailLoading = ref(false)

  const shipVisible = ref(false)
  const shipForm = ref({
    orderId: "",
    trackingNumber: "",
    shippingDate: "",
    selectedContactId: null,
  })
  const shipping = ref(false)

  const pendingShipCount = computed(() => orders.value.length)

  const filteredOrders = computed(() => {
    if (!searchKeyword.value.trim()) return orders.value
    const keyword = searchKeyword.value.trim().toLowerCase()
    return orders.value.filter(
      (o) =>
        (o.contactName && o.contactName.toLowerCase().includes(keyword)) ||
        o.orderId.toLowerCase().includes(keyword)
    )
  })

  async function loadOrders() {
    if (!shopStore.currentShopId) {
      orders.value = []
      return
    }
    loading.value = true
    try {
      const res = await getShipmentList(shopStore.currentShopId)
      orders.value = (res?.data || []).sort(
        (a, b) => new Date(b.orderDate) - new Date(a.orderDate)
      )
    } catch (error) {
      console.error("加载待发货订单失败:", error)
      ElMessage.error("加载待发货订单失败")
      orders.value = []
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {}

  function getStatusType(status) {
    const map = {
      PENDING: "info",
      PAID: "warning",
      SHIPPED: "primary",
      DELIVERED: "success",
      CANCELLED: "danger",
      RETURNED: "danger",
    }
    return map[status] || "info"
  }

  function getStatusText(status) {
    return STATUS_TEXT[status] || status
  }

  function formatPrice(price) {
    return price != null ? `¥${Number(price).toFixed(2)}` : "-"
  }

  function formatDate(dateStr) {
    return dateStr ? new Date(dateStr).toLocaleString("zh-CN") : "-"
  }

  async function loadContacts() {
    contactsLoading.value = true
    try {
      const res = await getShipDefaultAddress()
      if (res?.data) {
        contacts.value = Array.isArray(res.data) ? res.data : [res.data]
        if (contacts.value.length > 0 && !shipForm.value.selectedContactId) {
          shipForm.value.selectedContactId = contacts.value[0].id
        }
      } else {
        contacts.value = []
      }
    } catch (error) {
      console.error("加载联系人失败", error)
      ElMessage.error("加载联系人失败")
      contacts.value = []
    } finally {
      contactsLoading.value = false
    }
  }

  async function showOrderDetail(order) {
    selectedOrder.value = order
    detailVisible.value = true
    if (shopStore.currentShopId) {
      detailLoading.value = true
      try {
        const res = await getOrderDetail(shopStore.currentShopId, order.orderId)
        if (res?.data)
          selectedOrder.value = { ...order, ...res.data }
      } catch (error) {
        console.error("获取订单详情失败:", error)
      } finally {
        detailLoading.value = false
      }
    }
  }

  function closeDetail() {
    detailVisible.value = false
    selectedOrder.value = null
  }

  async function showShipDialog(order) {
    shipForm.value = {
      orderId: order.orderId,
      trackingNumber: "",
      shippingDate: new Date().toISOString().slice(0, 16),
      selectedContactId: null,
    }
    selectedOrder.value = order
    shipVisible.value = true
    await loadContacts()
  }

  function closeShipDialog() {
    shipVisible.value = false
  }

  async function handleShip() {
    if (!shipForm.value.trackingNumber.trim()) {
      ElMessage.warning("请输入物流单号")
      return
    }
    if (!shipForm.value.selectedContactId) {
      ElMessage.warning("请选择发货地址")
      return
    }
    shipping.value = true
    try {
      const res = await shipOrder(shipForm.value.orderId, {
        trackingNumber: shipForm.value.trackingNumber,
        contactId: shipForm.value.selectedContactId,
        shippingDate: shipForm.value.shippingDate || undefined,
        shopId: shopStore.currentShopId,
      })
      if (res?.message?.includes("成功")) {
        ElMessage.success("发货成功")
        closeShipDialog()
        await loadOrders()
      } else {
        ElMessage.error(res?.message || "发货失败")
      }
    } catch (error) {
      console.error("发货失败:", error)
      ElMessage.error("发货失败")
    } finally {
      shipping.value = false
    }
  }

  onMounted(async () => {
    if (shopStore.currentShopId) await loadOrders()
  })

  return {
    T,
    orders: filteredOrders,
    loading,
    searchKeyword,
    pendingShipCount,
    contacts,
    contactsLoading,
    detailVisible,
    detailLoading,
    selectedOrder,
    shipVisible,
    shipForm,
    shipping,
    ORDER_STATUS,
    loadOrders,
    handleSearch,
    getStatusType,
    getStatusText,
    formatPrice,
    formatDate,
    showOrderDetail,
    closeDetail,
    showShipDialog,
    closeShipDialog,
    handleShip,
  }
}
