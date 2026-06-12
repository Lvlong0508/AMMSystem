import { ref } from 'vue'
import { createSession, getSessions, deleteSession } from '@/api/chat'

export const sessionList = ref([])
export const activeSessionId = ref('')
export const loadingSessions = ref(false)

export async function loadSessions() {
  loadingSessions.value = true
  try {
    const list = await getSessions()
    sessionList.value = list.filter(s => s.title !== '新对话')
  } finally {
    loadingSessions.value = false
  }
}

export function clearActiveSession() {
  activeSessionId.value = ''
}

export function switchSession(sessionId) {
  activeSessionId.value = sessionId
}

export async function removeSession(sessionId) {
  await deleteSession(sessionId)
  sessionList.value = sessionList.value.filter(s => s.id !== sessionId)
  if (activeSessionId.value === sessionId) {
    activeSessionId.value = ''
  }
}
