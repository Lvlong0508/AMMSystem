import { request } from './request'

// 创建物流信息
// 创建物流信息（商家功能，用户端不可用）
// export const createLogistics = (logistics) =>
//     request.post('/api/seller/logistics/create', logistics)

// 根据ID查询物流信息
export const getLogisticsById = (id) =>
    request.get(`/api/user/logistics/${id}`)

// 查询所有物流信息
// 查询所有物流信息（商家功能，用户端不可用）
// export const getAllLogistics = () =>
//     request.get('/api/seller/logistics/list')

// 根据快递单号查询物流信息
export const getLogisticsByTrackingNumber = (trackingNumber) =>
    request.get('/api/user/logistics/search/tracking', { params: { trackingNumber } })

// 更新物流信息
// 更新物流信息（商家功能，用户端不可用）
// export const updateLogistics = (logistics) =>
//     request.put('/api/seller/logistics/${logistics.id}', logistics)

// 删除物流信息
// 删除物流信息（商家功能，用户端不可用）
// export const deleteLogistics = (id) =>
//     request.delete(`/api/seller/logistics/${id}`)
