import { request } from './request'

const ADDRESS_BASE = '/api/merchant/address'

export const createAddress = (address) =>
  request.post(`${ADDRESS_BASE}/create`, address)

export const updateAddress = (id, address) =>
  request.put(`${ADDRESS_BASE}/update/${id}`, address)

export const deleteAddress = (id) =>
  request.delete(`${ADDRESS_BASE}/delete/${id}`)

export const getAddressList = () =>
  request.get(`${ADDRESS_BASE}/list`)

export const getShipDefaultAddress = () =>
  request.get(`${ADDRESS_BASE}/ship-default`)

export const setDefaultAddress = (id) =>
  request.put(`${ADDRESS_BASE}/set-default/${id}`)
