import { request } from './request'

const PRODUCT_BASE = '/api/seller/product'

export const getProductById = (productId) =>
  request.get(`${PRODUCT_BASE}/${productId}`)

export const batchGetProducts = (ids) =>
  request.get(`${PRODUCT_BASE}/batch`, { params: { ids: ids.join(',') } })

export const createProduct = (formData) =>
  request.post(`${PRODUCT_BASE}/create`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

export const updateProduct = (productId, product) =>
  request.put(`${PRODUCT_BASE}/${productId}`, product)

export const deleteProduct = (productId) =>
  request.delete(`${PRODUCT_BASE}/${productId}`)

export const listProduct = (productId) =>
  request.post(`${PRODUCT_BASE}/${productId}/list`)

export const unlistProduct = (productId) =>
  request.post(`${PRODUCT_BASE}/${productId}/unlist`)
