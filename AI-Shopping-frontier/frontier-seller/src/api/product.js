import { request } from './request'

// 根据商品ID获取商品详情
export const getProductById = (productId) =>
    request.get(`/api/user/product/${productId}`)

// 创建商品
export const createProduct = (product) =>
    request.post('/api/seller/product/create', product)

// 更新商品
export const updateProduct = (productId, product) =>
    request.put(`/api/seller/product/${productId}`, product)

// 删除商品
export const deleteProduct = (productId) =>
    request.delete(`/api/seller/product/${productId}`)

