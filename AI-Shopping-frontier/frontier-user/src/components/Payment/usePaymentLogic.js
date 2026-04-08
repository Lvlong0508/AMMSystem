import { ref, watch } from 'vue'
import { updateOrderStatus } from '../../api/order.js'
import { ORDER_STATUS } from '../../config/orderStatus.js'

export function usePaymentLogic(props, emit, T) {
  // 响应式数据
  const selectedMethod = ref(null)
  const processing = ref(false)

  // 支付方式列表
  const paymentMethods = [
    { id: 'wechat', name: '微信支付', icon: '💚', desc: '使用微信扫码支付' },
    { id: 'alipay', name: '支付宝', icon: '💙', desc: '使用支付宝扫码支付' },
    { id: 'card', name: '银行卡', icon: '💳', desc: '使用银行卡支付' }
  ]

  // 选择支付方式
  const selectMethod = (methodId) => {
    selectedMethod.value = methodId
  }

  // 监听 visible 变化，打开时重置数据
  watch(() => props.visible, (newVal) => {
    if (newVal) {
      selectedMethod.value = null
      processing.value = false
    }
  })

  // 关闭弹窗
  const handleClose = () => {
    if (!processing.value) {
      emit('cancel')
      emit('close')
    }
  }

  // 确认支付
  const handleConfirmPayment = async () => {
    if (!selectedMethod.value || !props.orderInfo?.orderId) return

    processing.value = true
    try {
      // 调用后端更新订单状态为已支付 (PAID)
      const res = await updateOrderStatus(props.orderInfo.orderId, ORDER_STATUS.PAID)
      
      if (res.data?.message?.includes('成功')) {
        emit('success', {
          orderId: props.orderInfo.orderId,
          paymentMethod: selectedMethod.value,
          totalPrice: props.orderInfo.totalPrice
        })
        emit('close')
      } else {
        alert(res.data?.message || T.PAYMENT_FAILED)
      }
    } catch (error) {
      console.error('支付失败:', error)
      alert(error.response?.data?.message || T.PAYMENT_ERROR)
    } finally {
      processing.value = false
    }
  }

  return {
    selectedMethod,
    processing,
    paymentMethods,
    selectMethod,
    handleClose,
    handleConfirmPayment
  }
}
