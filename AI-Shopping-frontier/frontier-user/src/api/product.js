import { request } from './request'

// 获取所有商品列表（支持分页）
export const getAllProducts = (page = 0, size = 50) =>
    request.get('/product/all', { params: { page, size } })
