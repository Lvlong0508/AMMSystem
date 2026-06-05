import { computed } from 'vue'

export function useChatBubble(props) {
  const isAI = computed(() => props.role === 'ai')
  const isUser = computed(() => props.role === 'user')

  return {
    isAI,
    isUser,
  }
}
