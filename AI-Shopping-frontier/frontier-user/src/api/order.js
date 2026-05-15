import { request } from './request'

// 创建订单
export const placeOrder = (productId, quantity = 1, contactId) =>
    request.post('/api/user/order/place', { productId, quantity, contactId })

// 取消订单
export const cancelOrder = (orderId) =>
    request.delete(`/api/user/order/${orderId}`)

// 根据订单ID查询订单
export const getOrderById = (orderId) =>
    request.get(`/api/user/order/${orderId}`)

// 查询我的订单列表
export const getMyOrders = () =>
    request.get('/api/user/order/list')

// 更新订单状态
export const updateOrderStatus = (orderId, status) =>
    request.put(`/api/user/order/${orderId}/status`, null, { params: { status } })
