import { computed } from 'vue'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useOrderCard(props) {
  const statusText = computed(() => STATUS_TEXT[props.order.orderStatus] || props.order.orderStatus)

  const statusType = computed(() => {
    const m = { PENDING: 'info', PAID: 'warning', SHIPPED: 'primary', DELIVERED: 'success', CANCELLED: 'danger', RETURN_PENDING: 'warning', RETURNING: 'warning', RETURNED: 'danger' }
    return m[props.order.orderStatus] || 'info'
  })

  const actionVisible = computed(() => {
    return props.order.orderStatus === ORDER_STATUS.PAID
  })

  const returnActionVisible = computed(() => {
    return props.order.orderStatus === ORDER_STATUS.RETURNING
  })

  function formatPrice(price) {
    return price != null ? `¥${Number(price).toFixed(2)}` : '-'
  }

  function formatDate(dateStr) {
    return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-'
  }

  return { T, statusText, statusType, actionVisible, returnActionVisible, formatPrice, formatDate }
}
