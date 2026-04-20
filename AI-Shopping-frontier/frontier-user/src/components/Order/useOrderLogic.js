import { ref, computed, watch } from 'vue'
import { placeOrder } from '../../api/order.js'
import { getAllContacts } from '../../api/contact.js'

export function useOrderLogic(props, emit) {
  // 响应式数据
  const quantity = ref(1)
  const submitting = ref(false)
  const contacts = ref([])
  const loadingContacts = ref(false)
  const selectedContact = ref(null)

  // 计算属性
  const maxStock = computed(() => props.product?.stock || 1)

  const subtotal = computed(() => {
    if (!props.product) return 0
    return props.product.price * quantity.value
  })

  const totalPrice = computed(() => subtotal.value)

  const isValid = computed(() => {
    return selectedContact.value &&
      quantity.value >= 1 &&
      quantity.value <= maxStock.value
  })

  // 获取地址列表
  const loadContacts = async () => {
    loadingContacts.value = true
    try {
      const res = await getAllContacts()
      // 支持不同的返回数据结构
      const data = res.data?.data || res.data || []
      contacts.value = Array.isArray(data) ? data : []
      // 如果有地址，默认选择第一个
      if (contacts.value.length > 0 && !selectedContact.value) {
        selectedContact.value = contacts.value[0]
      }
    } catch (error) {
      console.error('获取地址列表失败:', error)
      contacts.value = []
    } finally {
      loadingContacts.value = false
    }
  }

  // 选择地址
  const selectContact = (contact) => {
    selectedContact.value = contact
  }

  // 监听 visible 变化，打开时重置数据并加载地址
  watch(() => props.visible, (newVal) => {
    if (newVal) {
      quantity.value = 1
      selectedContact.value = null
      loadContacts()
    }
  })

  // 方法
  const parseTags = (tags) => {
    if (!tags) return []
    if (Array.isArray(tags)) return tags
    return []
  }

  const increaseQty = () => {
    if (quantity.value < maxStock.value) {
      quantity.value++
    }
  }

  const decreaseQty = () => {
    if (quantity.value > 1) {
      quantity.value--
    }
  }

  const validateQuantity = () => {
    let qty = parseInt(quantity.value) || 1
    if (qty < 1) qty = 1
    if (qty > maxStock.value) qty = maxStock.value
    quantity.value = qty
  }

  const handleClose = () => {
    if (!submitting.value) {
      emit('close')
    }
  }

  const handleSubmit = async () => {
    if (!isValid.value || submitting.value) return

    submitting.value = true
    try {
      const res = await placeOrder(props.product.id, quantity.value, selectedContact.value.id)
      if (res.data?.orderId) {
        // 创建订单成功，触发支付流程
        emit('order-created', {
          orderId: res.data.orderId,
          product: props.product,
          quantity: quantity.value,
          totalPrice: totalPrice.value,
          contact: selectedContact.value
        })
      } else {
        alert(res.data?.message || '下单失败')
      }
    } catch (error) {
      console.error('下单错误:', error)
      alert(error.response?.data?.message || '下单失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  }

  return {
    quantity,
    submitting,
    contacts,
    loadingContacts,
    selectedContact,
    maxStock,
    subtotal,
    totalPrice,
    isValid,
    parseTags,
    increaseQty,
    decreaseQty,
    validateQuantity,
    handleClose,
    handleSubmit,
    selectContact
  }
}
