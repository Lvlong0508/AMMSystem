import { ref, nextTick, onMounted } from 'vue'
import { sendMessage } from '@/api/chat'
import { CHAT_VIEW_TEXT } from './Text'

const ERROR_TEXT = CHAT_VIEW_TEXT.ERROR_TEXT

export function useChatView() {
  const messages = ref([])
  const loading = ref(false)
  const inputText = ref('')
  const inputRef = ref(null)
  const messagesRef = ref(null)

  const scrollToBottom = async () => {
    await nextTick()
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  }

  const handleSend = async () => {
    const text = inputText.value.trim()
    if (!text || loading.value) return

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
      messages.value.push({ role: 'ai', text: ERROR_TEXT })
    } finally {
      loading.value = false
      await scrollToBottom()
    }
  }

  const handleNewChat = () => {
    messages.value = []
  }

  onMounted(() => {
    if (inputRef.value) inputRef.value.focus()
  })

  return {
    messages,
    loading,
    inputText,
    inputRef,
    messagesRef,
    handleSend,
    handleNewChat
  }
}
