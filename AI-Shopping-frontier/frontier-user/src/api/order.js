import { request } from './request'

/**
 * ????????
 * ??: 8082
 * Header: X-User-Id: <userId>
 */

/** ??/?? */
export const placeOrder = (data) =>
  request.post('/api/user/order/place', data)

/** ?????? */
export const getOrderList = () =>
  request.get('/api/user/order/list')

/** @deprecated */
export const getMyOrders = getOrderList

/** ?????? */
export const getOrderById = (orderId) =>
  request.get('/api/user/order/' + orderId)

/** ???? */
export const cancelOrder = (orderId) =>
  request.put('/api/user/order/' + orderId + '/cancel')

/** ???? */
export const deleteOrder = (orderId) =>
  request.delete('/api/user/order/' + orderId)

/** ???? */
export const payOrder = (orderId) =>
  request.put('/api/user/order/' + orderId + '/pay')

/** ???? */
export const confirmDelivery = (orderId) =>
  request.put('/api/user/order/' + orderId + '/deliver')

/** ????????? { returnReason }? */
export const submitReturnRequest = (orderId, data) =>
  request.post('/api/user/order/' + orderId + '/return-request', data)

/** ????????? { trackingNumber, contactId }? */
export const submitReturnLogistics = (orderId, data) =>
  request.post('/api/user/order/' + orderId + '/return-logistics', data)
/** 获取订单物流信息 */
export const getLogisticsInfo = (orderId) =>
  request.get('/api/user/logistics/order/' + orderId)