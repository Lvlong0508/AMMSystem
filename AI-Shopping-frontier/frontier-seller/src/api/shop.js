import { request } from './request'

const SHOP_BASE = '/api/seller/shop'

export const getShopByMerchant = (merchantId) =>
  request.get(`${SHOP_BASE}/merchant/${merchantId}`)

export const getShopDetail = (shopId) =>
  request.get(`${SHOP_BASE}/${shopId}`)

export const getShopEmployees = (shopId) =>
  request.get(`${SHOP_BASE}/${shopId}/employees`)

export const registerShop = (data) =>
  request.post(`${SHOP_BASE}/register`, data)

export const updateShop = (shopId, data) =>
  request.put(`${SHOP_BASE}/${shopId}`, data)

export const closeShop = (shopId) =>
  request.delete(`${SHOP_BASE}/${shopId}`)

export const openShop = (shopId) =>
  request.put(`${SHOP_BASE}/${shopId}/open`)

export const registerEmployee = (shopId, data) =>
  request.post(`${SHOP_BASE}/${shopId}/employees/register`, data)

export const removeEmployee = (shopId, merchantId) =>
  request.delete(`${SHOP_BASE}/${shopId}/employees/${merchantId}`)
