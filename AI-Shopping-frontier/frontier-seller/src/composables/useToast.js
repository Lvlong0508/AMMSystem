import { ElMessage } from 'element-plus'

export function useToast() {
  function show(message, type = 'success', duration = 3000) {
    ElMessage({ message, type, duration })
  }

  function hide() {
    ElMessage.closeAll()
  }

  return { show, hide }
}
