import { request } from './request'

/**
 * 物流查询相关接口（用户端只读）
 *
 * 端口: 8084
 * 物流记录由商家在发货/退货时创建，用户端仅可查询
 */

/**
 * 查询某订单的所有物流记录
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<Array<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string}>>}
 *
 * @example
 * // 请求
 * getLogisticsByOrderId('2026052200001ABCDE')
 * // 响应
 * // [
 * //   { id: 1, orderId: "2026052200001ABCDE", type: "DELIVERY", contactId: 1,
 * //     trackingNumber: "SF1234567890", createdAt: "2026-05-22T12:00:00.000+00:00" }
 * // ]
 */
export const getLogisticsByOrderId = (orderId) =>
  request.get(`/logistics/order/${orderId}`)

/**
 * 查询某订单最新一条指定类型的物流记录
 *
 * @param {string} orderId - 订单ID
 * @param {string} [type='DELIVERY'] - 物流类型: DELIVERY（发货）/ RETURN（退货）
 * @returns {Promise<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string}>}
 *
 * @example
 * // 请求
 * getLatestLogistics('2026052200001ABCDE', 'DELIVERY')
 * // 响应
 * // { id: 1, orderId: "2026052200001ABCDE", type: "DELIVERY",
 * //   contactId: 1, trackingNumber: "SF1234567890",
 * //   createdAt: "2026-05-22T12:00:00.000+00:00" }
 */
export const getLatestLogistics = (orderId, type = 'DELIVERY') =>
  request.get(`/logistics/order/${orderId}/latest`, { params: { type } })

/**
 * 按快递单号搜索物流记录
 *
 * @param {string} trackingNumber - 快递单号
 * @returns {Promise<Array<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string}>>}
 *
 * @example
 * // 请求
 * getLogisticsByTrackingNumber('SF1234567890')
 */
export const getLogisticsByTrackingNumber = (trackingNumber) =>
  request.get('/logistics/search/tracking', { params: { trackingNumber } })
