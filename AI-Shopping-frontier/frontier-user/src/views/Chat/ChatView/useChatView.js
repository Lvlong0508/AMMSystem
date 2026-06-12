import { ref, nextTick, watch, onMounted } from 'vue'
import { sessionList, activeSessionId, loadSessions, switchSession } from '@/stores/chatStore'
import { createSession, sendMessage, getSessionMessages } from '@/api/chat'
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
  const placedOrder = ref(null)

  watch(activeSessionId, async (newId) => {
    if (!newId) {
      messages.value = []
      return
    }
    if (loading.value) return
    try {
      const msgs = await getSessionMessages(newId)
      if (loading.value) return
      messages.value = msgs
    } catch {
      if (!loading.value) {
        messages.value = []
      }
    }
    if (!loading.value) {
      await scrollToBottom()
    }
  }, { immediate: true })

  const scrollToBottom = async () => {
    await nextTick()
    await new Promise(resolve => requestAnimationFrame(resolve))
    if (messagesRef.value) {
      messagesRef.value.scrollTo({
        top: messagesRef.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  }

  const handleSend = async () => {
    const text = inputText.value.trim()
    if (!text || loading.value) return
    if (!requireLogin()) return

    loading.value = true

    let sid = activeSessionId.value
    if (!sid) {
      try {
        const res = await createSession()
        sid = res.sessionId
        activeSessionId.value = sid
      } catch {
        loading.value = false
        showError(CHAT_VIEW_TEXT.ERROR_TEXT)
        return
      }
    }

    messages.value.push({ role: 'user', text })
    inputText.value = ''
    await scrollToBottom()

    try {
      const res = await sendMessage(text, sid)
      const reply = {
        role: 'ai',
        text: res.message || '',
        products: res.data?.type === 'product' ? res.data.products : null
      }
      messages.value.push(reply)
      loadSessions()
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

  const onOrderPlaced = (order) => {
    showOrderModal.value = false
    placedOrderId.value = order.orderId
    placedOrder.value = order
    showPaymentModal.value = true
  }

  const onPaymentSuccess = () => {
    showPaymentModal.value = false
    placedOrderId.value = ''
    placedOrder.value = null
    selectedProduct.value = null
  }

  const onPayLater = () => {
    showPaymentModal.value = false
    placedOrderId.value = ''
    placedOrder.value = null
    selectedProduct.value = null
  }

  onMounted(async () => {
    if (inputRef.value?.inputEl) inputRef.value.inputEl.focus()
    await loadSessions()
    if (sessionList.value.length > 0 && !activeSessionId.value) {
      switchSession(sessionList.value[0].id)
    }
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
    placedOrder,
    onOrderPlaced,
    onPaymentSuccess,
    onPayLater
  }
}
