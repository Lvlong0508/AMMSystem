import { ref } from 'vue'

export const newChatCounter = ref(0)

export function triggerNewChat() {
  newChatCounter.value++
}
