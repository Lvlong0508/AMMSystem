import { ref, computed } from 'vue'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useOrderCard(props, emit) {
  const expanded = ref(false)

  const statusText = computed(() => STATUS_TEXT[props.order.orderStatus] || props.order.orderStatus)
  const isPendingShip = computed(() => props.order.orderStatus === ORDER_STATUS.PAID)

  function toggle() {
    expanded.value = !expanded.value
  }

  function handleDetail() {
    emit('detail', props.order)
  }

  function handleShip() {
    emit('ship', props.order)
  }

  function formatPrice(price) {
    return price != null ? `¥${Number(price).toFixed(2)}` : '-'
  }

  function formatDate(dateStr) {
    return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-'
  }

  return {
    T,
    expanded,
    statusText,
    isPendingShip,
    toggle,
    handleDetail,
    handleShip,
    formatPrice,
    formatDate
  }
}
