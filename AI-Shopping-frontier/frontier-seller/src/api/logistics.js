import { request } from './request'

/**
 * 物流管理相关接口（商家端可读写）
 *
 * 端口: 8084
 * 物流记录由商家在发货/退货时创建，也可查询和管理
 */

const LOGISTICS_BASE = '/api/seller/logistics'

/**
 * 创建物流记录
 *
 * @param {Object} data - 物流信息
 * @param {string} data.orderId - 订单ID（必填）
 * @param {string} [data.type='DELIVERY'] - 物流类型: DELIVERY（发货）/ RETURN（退货）
 * @param {number} data.contactId - 联系人ID（必填）
 * @param {string} data.trackingNumber - 快递单号（必填）
 * @returns {Promise<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string, message: string}>}
 *
 * @example
 * // 请求
 * createLogistics({ orderId: '2026052200001ABCDE', type: 'DELIVERY', contactId: 1, trackingNumber: 'SF1234567890' })
 * // 响应
 * // {
 * //   id: 1, orderId: "2026052200001ABCDE", type: "DELIVERY", contactId: 1,
 * //   trackingNumber: "SF1234567890", createdAt: "2026-05-22T12:00:00.000+00:00",
 * //   message: "创建物流信息成功"
 * // }
 */
export const createLogistics = (data) =>
  request.post(`${LOGISTICS_BASE}/create`, data)

/**
 * 查询所有物流记录
 *
 * @returns {Promise<Array<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string}>>}
 *
 * @example
 * // 请求
 * getAllLogistics()
 * // 响应
 * // [{ id: 1, orderId: "2026052200001ABCDE", type: "DELIVERY", contactId: 1, trackingNumber: "SF1234567890", createdAt: "..." }]
 */
export const getAllLogistics = () =>
  request.get(`${LOGISTICS_BASE}/list`)

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
  request.get(`${LOGISTICS_BASE}/search/tracking`, { params: { trackingNumber } })

/**
 * 删除物流记录
 *
 * @param {number} id - 物流记录ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * deleteLogistics(1)
 */
export const deleteLogistics = (id) =>
  request.delete(`${LOGISTICS_BASE}/delete/${id}`)

/**
 * 查询某订单的所有物流记录
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<Array<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string}>>}
 *
 * @example
 * // 请求
 * getLogisticsByOrder('2026052200001ABCDE')
 * // 响应
 * // [{ id: 1, orderId: "2026052200001ABCDE", type: "DELIVERY", contactId: 1, trackingNumber: "SF1234567890", createdAt: "..." }]
 */
export const getLogisticsByOrder = (orderId) =>
  request.get(`${LOGISTICS_BASE}/order/${orderId}`)

/**
 * 查询某订单最新一条指定类型的物流记录
 *
 * @param {string} orderId - 订单ID
 * @param {string} [type='DELIVERY'] - 物流类型: DELIVERY（发货）/ RETURN（退货）
 * @returns {Promise<{id: number, orderId: string, type: string, contactId: number, trackingNumber: string, createdAt: string}>}
 *
 * @example
 * // 请求
 * getLatestLogisticsByOrder('2026052200001ABCDE', 'DELIVERY')
 * // 响应
 * // { id: 1, orderId: "2026052200001ABCDE", type: "DELIVERY", contactId: 1, trackingNumber: "SF1234567890", createdAt: "..." }
 */
export const getLatestLogisticsByOrder = (orderId, type = 'DELIVERY') =>
  request.get(`${LOGISTICS_BASE}/order/${orderId}/latest`, { params: { type } })
