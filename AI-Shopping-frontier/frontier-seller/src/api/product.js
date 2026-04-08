import { request } from './request'

// 根据商品ID获取商品详情
export const getProductById = (productId) =>
    request.get(`/product/${productId}`)

