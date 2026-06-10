import { ref, computed, watch } from "vue"
import { payOrder } from "@/api/order"
import { showSuccess, showError } from "@/utils/swal"

export function usePaymentModal(props, { emit }) {
  const selectedMethod = ref("wechat")
  const paying = ref(false)
  const paid = ref(false)
  const remainingMinutes = ref(30)
  let timer = null

  const methods = [
    { key: "wechat", label: "微信支付", icon: "wechat" },
    { key: "alipay", label: "支付宝", icon: "alipay" },
    { key: "unionpay", label: "银联支付", icon: "unionpay" },
  ]

  const calcRemaining = () => {
    if (!props.orderDate) return 30
    const elapsed = Math.floor((Date.now() - new Date(props.orderDate).getTime()) / 60000)
    return Math.max(0, 30 - elapsed)
  }

  const startTimer = () => {
    stopTimer()
    timer = setInterval(() => {
      remainingMinutes.value = calcRemaining()
      if (remainingMinutes.value <= 0) { stopTimer(); emit("timeout") }
    }, 10000)
  }

  const stopTimer = () => { if (timer) { clearInterval(timer); timer = null } }

  const handlePay = async () => {
    if (paying.value || paid.value) return
    paying.value = true
    try {
      await payOrder(props.orderId)
      paid.value = true; showSuccess("支付成功"); stopTimer(); emit("pay-success")
    } catch { showError("支付失败") }
    finally { paying.value = false }
  }

  const handlePayLater = () => { stopTimer(); emit("pay-later") }

  watch(() => props.visible, (v) => {
    if (v) { selectedMethod.value = "wechat"; paying.value = false; paid.value = false; remainingMinutes.value = calcRemaining(); startTimer() }
    else { stopTimer() }
  })

  return { selectedMethod, paying, paid, methods, remainingMinutes, handlePay, handlePayLater }
}
