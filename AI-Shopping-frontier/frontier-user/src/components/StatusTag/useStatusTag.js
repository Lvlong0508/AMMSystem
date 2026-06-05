import { computed } from 'vue'
import { STATUS_TEXT, STATUS_CLASS } from '@/config/orderStatus'

export function useStatusTag(props) {
  const statusClass = computed(() => STATUS_CLASS[props.status] || '')
  const displayText = computed(() => STATUS_TEXT[props.status] || props.status)
  return { statusClass, displayText }
}
