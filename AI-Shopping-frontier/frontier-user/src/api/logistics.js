import { request } from './request'

// 创建物流信息
export const createLogistics = (logistics) =>
    request.post('/logistics/create', logistics)

// 根据ID查询物流信息
export const getLogisticsById = (id) =>
    request.get(`/logistics/get/${id}`)

// 查询所有物流信息
export const getAllLogistics = () =>
    request.get('/logistics/list')

// 根据快递单号查询物流信息
export const getLogisticsByTrackingNumber = (trackingNumber) =>
    request.get('/logistics/search/tracking', { params: { trackingNumber } })

// 更新物流信息
export const updateLogistics = (logistics) =>
    request.put('/logistics/update', logistics)

// 删除物流信息
export const deleteLogistics = (id) =>
    request.delete(`/logistics/delete/${id}`)
