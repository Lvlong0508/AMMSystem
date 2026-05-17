import { request } from './request'

// 根据订单ID查询订单（需要shopId验证）
export const getOrderById = (orderId) =>
    request.get(`/api/seller/order/${orderId}`)

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
