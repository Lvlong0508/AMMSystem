import { request } from './request'

const ORDER_BASE = '/api/seller/order'

export const getOrderListByShop = (shopId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/list`)

export const getOrderDetail = (shopId, orderId) =>
  request.get(`${ORDER_BASE}/shop/${shopId}/${orderId}`)

export const shipOrder = (orderId, data) =>
  request.put(`${ORDER_BASE}/${orderId}/ship`, data)

export const approveReturn = (orderId, shopId) =>
  request.put(`${ORDER_BASE}/${orderId}/approve-return`, null, { params: { shopId } })

export const confirmReturn = (orderId, shopId) =>
  request.put(`${ORDER_BASE}/${orderId}/confirm-return`, null, { params: { shopId } })
