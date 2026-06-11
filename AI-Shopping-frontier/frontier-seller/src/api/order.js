import { request } from './request'

/**
 * 商家订单相关接口
 *
 * 端口: 8082
 */

const ORDER_BASE = '/api/seller/order'

/**
 * 查询指定店铺的订单列表
 * @param {string} shopId - 店铺ID
 */
export const getOrderListByShop = (shopId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/list`)

/**
 * 查询待发货订单列表
 * @param {string} shopId - 店铺ID
 */
export const getShipmentList = (shopId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/shipment-list`)

/**
 * 查询指定店铺的订单详情
 * @param {string} shopId - 店铺ID
 * @param {string} orderId - 订单ID
 */
export const getOrderDetail = (shopId, orderId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/${orderId}`)

/**
 * 商家发货
 * @param {string} orderId - 订单ID
 * @param {Object} data - { trackingNumber, contactId, shippingDate? }
 */
export const shipOrder = (orderId, data) =>
  request.put(`${ORDER_BASE}/${orderId}/ship`, data, { params: { shopId: data.shopId } })

/**
 * 商家确认退货完成
 * @param {string} orderId - 订单ID
 * @param {string} shopId - 店铺ID
 */
export const confirmReturn = (orderId, shopId) =>
  request.put(`${ORDER_BASE}/${orderId}/confirm-return`, null, { params: { shopId } })

/**
 * 查询待处理的退货申请
 * @param {string} shopId - 店铺ID
 */
export const getReturnRequestsPending = (shopId) =>
  request.get(`${ORDER_BASE}/return-requests/pending`, { params: { shopId } })

/**
 * 查询已处理的退货记录
 * @param {string} shopId - 店铺ID
 */
export const getReturnRequestsProcessed = (shopId) =>
  request.get(`${ORDER_BASE}/return-requests/processed`, { params: { shopId } })

/**
 * 审核退货申请
 * @param {string} orderId - 订单ID
 * @param {string} shopId - 店铺ID
 * @param {string} status - AGREED 或 REJECTED
 */
export const reviewReturnRequest = (orderId, shopId, status) =>
  request.put(`${ORDER_BASE}/return-requests/${orderId}/review`, { status }, { params: { shopId } })

export const approveReturn = (orderId, shopId) =>
  reviewReturnRequest(orderId, shopId, 'agreed')
