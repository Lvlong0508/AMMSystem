// 订单状态常量定义
// 与后端 Order.java 保持一致
// 状态流转: 待支付 -> 待发货 -> 已取消/已发货 -> 已送达/已退货
// 已送达 -> 已退货

export const ORDER_STATUS = {
  PENDING: 'PENDING',      // 待支付
  PAID: 'PAID',           // 待发货
  CANCELLED: 'CANCELLED', // 已取消
  SHIPPED: 'SHIPPED',     // 已发货
  DELIVERED: 'DELIVERED', // 已送达
  RETURN_PENDING: 'RETURN_PENDING', // 待退货
  RETURNING: 'RETURNING',           // 退货中
  RETURNED: 'RETURNED',             // 已退货
}

// 状态中文映射
export const STATUS_TEXT = {
  [ORDER_STATUS.PENDING]: '待支付',
  [ORDER_STATUS.PAID]: '待发货',
  [ORDER_STATUS.CANCELLED]: '已取消',
  [ORDER_STATUS.SHIPPED]: '已发货',
  [ORDER_STATUS.DELIVERED]: '已送达',
  [ORDER_STATUS.RETURN_PENDING]: '待退货',
  [ORDER_STATUS.RETURNING]: '退货中',
  [ORDER_STATUS.RETURNED]: '已退货',
}

// 状态CSS类名映射
export const STATUS_CLASS = {
  [ORDER_STATUS.PENDING]: 'status-pending',
  [ORDER_STATUS.PAID]: 'status-paid',
  [ORDER_STATUS.CANCELLED]: 'status-cancelled',
  [ORDER_STATUS.SHIPPED]: 'status-shipped',
  [ORDER_STATUS.DELIVERED]: 'status-delivered',
  [ORDER_STATUS.RETURN_PENDING]: 'status-return-pending',
  [ORDER_STATUS.RETURNING]: 'status-returning',
  [ORDER_STATUS.RETURNED]: 'status-returned',
}

// 状态流转验证（前端校验用）
export const STATUS_TRANSITIONS = {
  [ORDER_STATUS.PENDING]: [ORDER_STATUS.PAID],
  [ORDER_STATUS.PAID]: [ORDER_STATUS.SHIPPED, ORDER_STATUS.CANCELLED],
  [ORDER_STATUS.SHIPPED]: [ORDER_STATUS.DELIVERED, ORDER_STATUS.RETURNED],
  [ORDER_STATUS.DELIVERED]: [ORDER_STATUS.RETURNED]
}

// 检查状态是否可以转换
export function canTransition(fromStatus, toStatus) {
  if (!fromStatus) return true
  const allowedTransitions = STATUS_TRANSITIONS[fromStatus]
  return allowedTransitions ? allowedTransitions.includes(toStatus) : false
}
