import { ref, nextTick, watch, onMounted, onBeforeUnmount } from 'vue'
import { newChatCounter } from '@/stores/chatStore'
import * as THREE from 'three'
import NET from 'vanta/dist/vanta.net.min'
import { sendMessage } from '@/api/chat'
import { CHAT_VIEW_TEXT } from './Text'
import { requireLogin } from '@/stores/authStore'

const ERROR_TEXT = CHAT_VIEW_TEXT.ERROR_TEXT

export function useChatView() {
  const messages = ref([])
  const loading = ref(false)
  const inputText = ref('')
  const inputRef = ref(null)
  const messagesRef = ref(null)

  watch(newChatCounter, () => {
    messages.value = []
  })

  // Vanta 动画背景的引用和实例
  const vantaRef = ref(null)
  let vantaEffect = null

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
      messages.value.push({ role: 'ai', text: ERROR_TEXT })
    } finally {
      loading.value = false
      await scrollToBottom()
    }
  }

  onMounted(() => {
    if (inputRef.value) inputRef.value.focus()

    // 初始化 Vanta 动画
    if (vantaRef.value) {
      vantaEffect = NET({
        el: vantaRef.value,
        THREE: THREE,
        color: 0xcbd5e1,          // 淡淡的蓝灰色线条，与你的边框颜色呼应
        backgroundColor: 0xffffff, // 纯白色背景
        points: 10.00,
        maxDistance: 22.00,
        spacing: 18.00,
        showDots: true
      })
    }
  })

  // 必须在组件卸载时销毁实例，防止内存泄漏
  onBeforeUnmount(() => {
    if (vantaEffect) {
      vantaEffect.destroy()
    }
  })

  return {
    messages,
    loading,
    inputText,
    inputRef,
    messagesRef,
    vantaRef,
    handleSend
  }
}