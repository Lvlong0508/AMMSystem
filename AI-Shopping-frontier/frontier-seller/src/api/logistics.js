import { request } from './request'

const LOGISTICS_BASE = '/logistics'

export const createLogistics = (data) =>
  request.post(`${LOGISTICS_BASE}/create`, data)

export const getAllLogistics = () =>
  request.get(`${LOGISTICS_BASE}/list`)

export const getLogisticsByTrackingNumber = (trackingNumber) =>
  request.get(`${LOGISTICS_BASE}/search/tracking`, { params: { trackingNumber } })

export const deleteLogistics = (id) =>
  request.delete(`${LOGISTICS_BASE}/delete/${id}`)

export const getLogisticsByOrder = (orderId) =>
  request.get(`${LOGISTICS_BASE}/order/${orderId}`)

export const getLatestLogisticsByOrder = (orderId, type = 'DELIVERY') =>
  request.get(`${LOGISTICS_BASE}/order/${orderId}/latest`, { params: { type } })
