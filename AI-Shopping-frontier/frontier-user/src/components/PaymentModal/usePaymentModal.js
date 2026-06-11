import { ref, computed, watch } from "vue"
import { payOrder } from "@/api/order"
import { showSuccess, showError } from "@/utils/swal"

export function usePaymentModal(props, { emit }) {
  const selectedMethod = ref("wechat")
  const paying = ref(false)
  const paid = ref(false)

  const methods = [
    { key: "wechat", label: "微信支付", icon: "wechat" },
    { key: "alipay", label: "支付宝", icon: "alipay" },
    { key: "unionpay", label: "银联支付", icon: "unionpay" },
  ]

  const handlePay = async () => {
    if (paying.value || paid.value) return
    paying.value = true
    try {
      await payOrder(props.orderId)
      paid.value = true; showSuccess("支付成功"); emit("pay-success")
    } catch { showError("支付失败") }
    finally { paying.value = false }
  }

  const handlePayLater = () => { emit("pay-later") }

  watch(() => props.visible, (v) => {
    if (v) { selectedMethod.value = "wechat"; paying.value = false; paid.value = false }
  })

  const productImage = computed(() => props.order?.imageUrl || props.order?.productImageUrl || null)

  return { selectedMethod, paying, paid, methods, handlePay, handlePayLater, productImage }
}
