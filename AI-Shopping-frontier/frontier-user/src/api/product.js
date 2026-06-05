import { request } from './request'

/**
 * 商品查询相关接口
 *
 * 端口: 8081
 */

/**
 * 分页查询可售商品列表
 *
 * @param {number} [page=0] - 页码（从0开始）
 * @returns {Promise<{products: Array<{id: number, name: string, price: number, tags: string, imageId: number, imageUrl: string}>, page: number, size: number}>}
 *
 * @example
 * // 请求
 * getAllProducts(0)
 * // 响应
 * // {
 * //   products: [
 * //     { id: 1, name: "商品名称", price: 99.99, tags: "标签1,标签2", imageId: 1, imageUrl: "http://example.com/image.jpg" }
 * //   ],
 * //   page: 0,
 * //   size: 20
 * // }
 */
export const getAllProducts = (page = 0) =>
  request.get('/api/user/product/all', { params: { page } })

/**
 * 根据ID查询商品详情
 *
 * @param {number} productId - 商品ID
 * @returns {Promise<{id: number, name: string, price: number, tags: string, description: string, stock: number, isSale: boolean, imageId: number, imageUrl: string, createdAt: string, updatedAt: string}>}
 *
 * @example
 * // 请求
 * getProductById(1)
 * // 响应
 * // {
 * //   id: 1, name: "商品名称", price: 99.99, tags: "标签1,标签2",
 * //   description: "商品描述", stock: 100, isSale: true,
 * //   imageId: 1, imageUrl: "http://example.com/image.jpg",
 * //   createdAt: "2025-01-01T00:00:00", updatedAt: "2025-01-01T00:00:00"
 * // }
 */
export const getProductById = (productId) =>
  request.get(`/api/user/product/${productId}`)

/**
 * 按名称模糊搜索商品
 *
 * @param {string} name - 搜索关键词
 * @returns {Promise<{products: Array<{id: number, name: string, price: number, tags: string, imageId: number, imageUrl: string}>, size: number}>}
 *
 * @example
 * // 请求
 * searchProducts('手机')
 * // 响应
 * // { products: [...], size: 20 }
 */
export const searchProducts = (name) =>
  request.get('/api/user/product/search', { params: { name } })

/**
 * 按价格区间查询商品
 *
 * @param {number} [minPrice=0] - 最低价格
 * @param {number} [maxPrice=100] - 最高价格
 * @param {number} [page=0] - 页码（从0开始）
 * @returns {Promise<{products: Array, page: number, size: number}>}
 *
 * @example
 * // 请求
 * getProductsByPriceRange(10, 500, 0)
 * // 响应
 * // { products: [...], page: 0, size: 20 }
 */
export const getProductsByPriceRange = (minPrice = 0, maxPrice = 100, page = 0) =>
  request.get('/api/user/product/price-range', { params: { minPrice, maxPrice, page } })
