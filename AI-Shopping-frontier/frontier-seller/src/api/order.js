import { request } from './request'

/**
 * 商家订单相关接口
 *
 * 端口: 8082
 * Header 需携带: `X-Shop-Id: <shopId>`（由 request.js 拦截器自动注入）
 */

const ORDER_BASE = '/api/seller/order'

/**
 * 查询指定店铺的订单列表
 *
 * @param {number} shopId - 店铺ID
 * @returns {Promise<Array<{orderId: string, productId: number, contactId: number, quantity: number, orderStatus: string}>>}
 *
 * @example
 * // 请求
 * getOrderListByShop(1)
 * // 响应
 * // [{ orderId: "2026052200001ABCDE", productId: 1, contactId: 1, quantity: 1, orderStatus: "PENDING" }]
 */
export const getOrderListByShop = (shopId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/list`)

/**
 * 查询指定店铺的订单详情
 *
 * @param {number} shopId - 店铺ID
 * @param {string} orderId - 订单ID
 * @returns {Promise<{orderId: string, userId: number, shopId: number, productId: number, quantity: number, totalPrice: number, orderStatus: string, orderDate: string, contactId: number, contactName: string, contactPhone: string, contactAddress: string, trackingNumber: string|null}>}
 *
 * @example
 * // 请求
 * getOrderDetail(1, '2026052200001ABCDE')
 * // 响应
 * // { orderId: "2026052200001ABCDE", userId: 1, shopId: 1, productId: 1, quantity: 1, totalPrice: 99.99, orderStatus: "PENDING", ... }
 */
export const getOrderDetail = (shopId, orderId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/${orderId}`)

/**
 * 商家发货
 *
 * @param {string} orderId - 订单ID
 * @param {Object} data - 发货信息
 * @param {string} data.trackingNumber - 快递单号（必填）
 * @param {number} data.contactId - 联系人ID（必填）
 * @param {string} [data.shippingDate] - 发货日期（可选）
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * shipOrder('2026052200001ABCDE', { trackingNumber: 'SF1234567890', contactId: 1 })
 */
export const shipOrder = (orderId, data) =>
  request.put(`${ORDER_BASE}/${orderId}/ship`, data)

/**
 * 商家审核通过退货申请
 *
 * @param {string} orderId - 订单ID
 * @param {number} shopId - 店铺ID（请求参数）
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * approveReturn('2026052200001ABCDE', 1)
 */
export const approveReturn = (orderId, shopId) =>
  request.put(`${ORDER_BASE}/${orderId}/approve-return`, null, { params: { shopId } })

/**
 * 商家确认退货完成
 *
 * @param {string} orderId - 订单ID
 * @param {number} shopId - 店铺ID（请求参数）
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * confirmReturn('2026052200001ABCDE', 1)
 */
export const confirmReturn = (orderId, shopId) =>
  request.put(`${ORDER_BASE}/${orderId}/confirm-return`, null, { params: { shopId } })
