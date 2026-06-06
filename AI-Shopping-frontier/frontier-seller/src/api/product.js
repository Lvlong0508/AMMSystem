import { request } from './request'

/**
 * 商家商品管理相关接口
 *
 * 端口: 8081
 * Header 需携带: `satoken: <token>`（由 request.js 拦截器自动注入）
 */

const PRODUCT_BASE = '/api/seller/product'

/**
 * 创建商品
 *
 * 请求方式: multipart/form-data
 * `product` 字段为 JSON 字符串，`image` 字段为图片文件（仅支持 JPG/PNG，最大 10MB）
 *
 * @param {FormData} formData - 包含 product(JSON) 和 image(File) 的 FormData
 * @returns {Promise<{id: number, message: string}>}
 *
 * @example
 * // 请求
 * const fd = new FormData()
 * fd.append('product', JSON.stringify({ name: '商品名称', description: '商品描述', price: 99.99, stock: 100, shopId: 1 }))
 * fd.append('image', fileInput.files[0])
 * createProduct(fd)
 * // 响应
 * // { id: 1, message: "创建商品成功" }
 */
export const createProduct = (formData) =>
  request.post(`${PRODUCT_BASE}/create`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

/**
 * 更新商品（全部可选，以 multipart/form-data 提交）
 *
 * @param {number} productId - 商品ID
 * @param {Object} product - 更新信息
 * @param {string} [product.name] - 新名称
 * @param {string} [product.description] - 新描述
 * @param {number} [product.price] - 新价格
 * @param {number} [product.stock] - 新库存
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * updateProduct(1, { name: '新名称', price: 199.99 })
 */
export const updateProduct = (productId, product) =>
  request.put(`${PRODUCT_BASE}/${productId}`, product)

/**
 * 删除商品
 *
 * @param {number} productId - 商品ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * deleteProduct(1)
 */
export const deleteProduct = (productId) =>
  request.delete(`${PRODUCT_BASE}/${productId}`)

/**
 * 查询商品详情
 *
 * @param {number} productId - 商品ID
 * @returns {Promise<{id: number, name: string, price: number, tags: string, description: string, stock: number, isSale: boolean, imageId: number, imageUrl: string, createdAt: string, updatedAt: string}>}
 *
 * @example
 * // 请求
 * getProductById(1)
 * // 响应
 * // { id: 1, name: "商品名称", price: 99.99, tags: "标签1,标签2", description: "商品描述", stock: 100, isSale: true, ... }
 */
export const getProductById = (productId) =>
  request.get(`${PRODUCT_BASE}/${productId}`)

/**
 * 批量查询商品抽象信息
 *
 * @param {number[]} ids - 商品ID数组
 * @returns {Promise<Array<{id: number, name: string, price: number, imageUrl: string}>>}
 *
 * @example
 * // 请求
 * batchGetProducts([1, 2, 3])
 * // 响应
 * // [{ id: 1, name: "商品名称", price: 99.99, imageUrl: "..." }]
 */
export const batchGetProducts = (ids) =>
  request.get(`${PRODUCT_BASE}/batch`, { params: { ids: ids.join(',') } })

/**
 * 上架商品
 *
 * @param {number} productId - 商品ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * listProduct(1)
 */
export const listProduct = (productId) =>
  request.post(`${PRODUCT_BASE}/${productId}/list`)

/**
 * 下架商品
 *
 * @param {number} productId - 商品ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * unlistProduct(1)
 */
export const unlistProduct = (productId) =>
  request.post(`${PRODUCT_BASE}/${productId}/unlist`)

/**
 * 按店铺ID查询商品列表（全部商品，含未上架）
 *
 * @param {number} shopId - 店铺ID
 * @returns {Promise<Array<{id: number, name: string, price: number, tags: string, isSale: boolean, imageId: number, imageUrl: string, shop: {id: string, name: string, description: string, logoUrl: string}}>>}
 *
 * @example
 * // 请求
 * getProductsByShop(1)
 * // 响应
 * // [{ id: 1, name: "商品名称", price: 99.99, tags: "标签1,标签2", isSale: true, imageUrl: "...", shop: {...} }]
 */
export const getProductsByShop = (shopId) =>
  request.get(`${PRODUCT_BASE}/shop/${shopId}`)
