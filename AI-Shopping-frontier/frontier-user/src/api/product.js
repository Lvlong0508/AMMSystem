import { request } from './request'

// 获取所有商品列表（支持分页）
export const getAllProducts = (page = 0, size = 50) =>
    request.get('/api/user/product/all', { params: { page, size } })

// 根据商品ID获取商品详情
export const getProductById = (productId) =>
    request.get(`/api/user/product/${productId}`)
