import { request } from './request'

/**
 * 用户订单相关接口
 *
 * 端口: 8082
 * Header 需携带: `X-User-Id: <userId>`（由 request.js 拦截器自动注入）
 */

/**
 * 创建/下单
 *
 * @param {Object} data - 订单信息
 * @param {number} data.productId - 商品ID（必填）
 * @param {number} data.quantity - 数量（必填，最小值为1）
 * @param {number} data.contactId - 地址ID（必填）
 * @returns {Promise<{orderId: string, message: string}>}
 *
 * @example
 * // 请求
 * placeOrder({ productId: 1, quantity: 2, contactId: 1 })
 * // 响应
 * // { orderId: "2026052200001ABCDE", message: "下单成功" }
 */
export const placeOrder = (data) =>
  request.post('/api/user/order/place', data)

/**
 * 查询当前用户的订单列表
 *
 * @returns {Promise<Array<{orderId: string, productId: number, shopId: number, totalPrice: number, quantity: number, orderStatus: string}>>}
 *
 * @example
 * // 请求
 * getOrderList()
 * // 响应
 * // [
 * //   { orderId: "2026052200001ABCDE", productId: 1, shopId: 1, totalPrice: 99.99, quantity: 1, orderStatus: "PENDING" }
 * // ]
 */
export const getOrderList = () =>
  request.get('/api/user/order/list')

/** @deprecated 请使用 getOrderList() */
export const getMyOrders = getOrderList

/**
 * 查询当前用户的订单详情
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<{orderId: string, userId: number, shopId: number, productId: number, quantity: number, totalPrice: number, orderStatus: string, orderDate: string, contactId: number, contactName: string, contactPhone: string, contactAddress: string, trackingNumber: string|null}>}
 *
 * @example
 * // 请求
 * getOrderById('2026052200001ABCDE')
 * // 响应
 * // {
 * //   orderId: "2026052200001ABCDE", userId: 1, shopId: 1, productId: 1,
 * //   quantity: 1, totalPrice: 99.99, orderStatus: "PENDING",
 * //   orderDate: "2026-05-22T12:00:00", contactId: 1,
 * //   contactName: "张三", contactPhone: "13800138000",
 * //   contactAddress: "广东省深圳市南山区xxx", trackingNumber: "SF1234567890"
 * // }
 */
export const getOrderById = (orderId) =>
  request.get(`/api/user/order/${orderId}`)

/**
 * 取消订单
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * cancelOrder('2026052200001ABCDE')
 */
export const cancelOrder = (orderId) =>
  request.put(`/api/user/order/${orderId}/cancel`)

/**
 * 删除订单（逻辑删除）
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * deleteOrder('2026052200001ABCDE')
 */
export const deleteOrder = (orderId) =>
  request.delete(`/api/user/order/${orderId}`)

/**
 * 支付订单
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * payOrder('2026052200001ABCDE')
 */
export const payOrder = (orderId) =>
  request.put(`/api/user/order/${orderId}/pay`)

/**
 * 用户确认收货
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * confirmDelivery('2026052200001ABCDE')
 */
export const confirmDelivery = (orderId) =>
  request.put(`/api/user/order/${orderId}/deliver`)

/**
 * 用户提交退货申请
 *
 * @param {string} orderId - 订单ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * submitReturnRequest('2026052200001ABCDE')
 */
export const submitReturnRequest = (orderId) =>
  request.post(`/api/user/order/${orderId}/return-request`)
