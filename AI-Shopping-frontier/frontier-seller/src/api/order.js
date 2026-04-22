import { request } from './request'

// 根据订单ID查询订单
export const getOrderById = (orderId) =>
    request.get(`/api/seller/order/${orderId}`)

// 查询所有订单
export const getAllOrders = () =>
    request.get('/api/seller/order/list')

// 根据订单状态查询订单
export const getOrdersByStatus = (status) =>
    request.get(`/api/seller/order/status/${status}`)

// 更新订单状态
export const updateOrderStatus = (orderId, status) =>
    request.put(`/api/seller/order/${orderId}/status`, null, { params: { status } })

// 发货（创建物流信息）
export const shipOrder = (orderId, trackingNumber, contactId, shippingDate) =>
    request.put(`/api/seller/order/${orderId}/ship`, {
        trackingNumber,
        contactId,
        shippingDate
    })
