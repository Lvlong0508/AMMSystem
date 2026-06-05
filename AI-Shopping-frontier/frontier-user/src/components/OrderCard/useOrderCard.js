import { computed } from 'vue'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'

const PROGRESS_LABEL = '已完成'

export function useOrderCard(props) {
  const formatDate = (date) => {
    if (!date) return '-'
    const d = new Date(date)
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${mm}-${dd}`
  }

  const steps = computed(() => {
    const allSteps = [
      { key: 'PENDING', label: STATUS_TEXT[ORDER_STATUS.PENDING] },
      { key: 'PAID', label: STATUS_TEXT[ORDER_STATUS.PAID] },
      { key: 'SHIPPED', label: STATUS_TEXT[ORDER_STATUS.SHIPPED] },
      { key: 'DELIVERED', label: STATUS_TEXT[ORDER_STATUS.DELIVERED] }
    ]
    const statusOrder = ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED']
    const currentIdx = statusOrder.indexOf(props.order.orderStatus)
    return allSteps.map((s, i) => ({
      ...s,
      done: i < currentIdx,
      active: i === currentIdx
    }))
  })

  const timelineProgress = computed(() => {
    const statusOrder = ['PENDING', 'PAID', 'SHIPPED', 'DELIVERED']
    const idx = statusOrder.indexOf(props.order.orderStatus)
    if (idx === -1) return ''
    return `${idx + 1}/${statusOrder.length} ${PROGRESS_LABEL}`
  })

  return { formatDate, timelineProgress, steps }
}
