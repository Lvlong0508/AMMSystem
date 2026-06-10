import { ref, nextTick, watch, onMounted } from 'vue'
import { newChatCounter } from '@/stores/chatStore'
import { sendMessage } from '@/api/chat'
import { CHAT_VIEW_TEXT } from './Text'
import { requireLogin } from '@/stores/authStore'
import { showError, showInfo } from '@/utils/swal'

export function useChatView() {
  const messages = ref([])
  const loading = ref(false)
  const inputText = ref('')
  const inputRef = ref(null)
  const messagesRef = ref(null)
  const selectedProduct = ref(null)
  const showOrderModal = ref(false)
  const showPaymentModal = ref(false)
  const placedOrderId = ref('')

  watch(newChatCounter, () => {
    messages.value = []
  })

  const scrollToBottom = async () => {
    await nextTick()
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  }

  const handleSend = async () => {
    const text = inputText.value.trim()
    if (!text || loading.value) return
    if (!requireLogin()) return

    messages.value.push({ role: 'user', text })
    inputText.value = ''
    loading.value = true
    await scrollToBottom()

    try {
      const res = await sendMessage(text)
      const reply = {
        role: 'ai',
        text: res.message || '',
        products: res.data?.type === 'product' ? res.data.products : null
      }
      messages.value.push(reply)
    } catch {
      showError(CHAT_VIEW_TEXT.ERROR_TEXT)
    } finally {
      loading.value = false
      await scrollToBottom()
    }
  }

  const handleViewDetail = (product) => {
    const info = [
      `名称: ${product.name}`,
      `价格: ¥${product.price?.toFixed(2)}`,
      `库存: ${product.stock ?? '-'}`,
      `描述: ${product.description || '暂无描述'}`
    ].join('\n')
    showInfo(info, '知道了')
  }

  const handleBuyNow = (product) => {
    selectedProduct.value = product
    showOrderModal.value = true
  }

  const onOrderPlaced = (orderId) => {
    showOrderModal.value = false
    placedOrderId.value = orderId
    showPaymentModal.value = true
  }

  const onPaymentSuccess = () => {
    showPaymentModal.value = false
    placedOrderId.value = ''
    selectedProduct.value = null
  }

  const onPayLater = () => {
    showPaymentModal.value = false
    placedOrderId.value = ''
    selectedProduct.value = null
  }

  onMounted(() => {
    if (inputRef.value?.inputEl) inputRef.value.inputEl.focus()
  })

  return {
    messages,
    loading,
    inputText,
    inputRef,
    messagesRef,
    handleSend,
    handleViewDetail,
    handleBuyNow,
    showOrderModal,
    showPaymentModal,
    selectedProduct,
    placedOrderId,
    onOrderPlaced,
    onPaymentSuccess,
    onPayLater
  }
}