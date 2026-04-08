import { request } from './request'

// 创建订单
export const placeOrder = (productId, quantity = 1, contact) =>
    request.post('/order/place', { productId, quantity, contact })

// 删除订单
export const deleteOrder = (orderId) =>
    request.delete(`/order/${orderId}`)

// 更新订单信息
export const updateOrder = (orderId, order) =>
    request.put(`/order/${orderId}`, order)

// 根据订单ID查询订单
export const getOrderById = (orderId) =>
    request.get(`/order/${orderId}`)

// 查询所有订单
export const getAllOrders = () =>
    request.get('/order/list')

// 根据客户名称查询订单
export const getOrdersByCustomerName = (customerName) =>
    request.get(`/order/customer/${customerName}`)

// 根据订单状态查询订单
export const getOrdersByStatus = (status) =>
    request.get(`/order/status/${status}`)

// 更新订单状态
export const updateOrderStatus = (orderId, status) =>
    request.put(`/order/${orderId}/status`, null, { params: { status } })

// 发货（创建物流信息）
export const shipOrder = (orderId, trackingNumber, contactId, shippingDate) =>
    request.put(`/order/${orderId}/ship`, null, {
        params: { trackingNumber, contactId, shippingDate }
    })
