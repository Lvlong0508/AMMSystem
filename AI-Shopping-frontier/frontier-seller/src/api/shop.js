import { request } from './request'

/**
 * 商家端店铺相关接口
 *
 * 端口: 8087
 * Header 需携带: `X-User-Id: <userId>`（由 request.js 拦截器自动注入）
 */

const SHOP_BASE = '/api/seller/shop'

/**
 * 根据商家ID查询关联店铺ID列表
 *
 * @param {string} merchantId - 商家ID
 * @returns {Promise<{shopIds: string[]}>}
 *
 * @example
 * // 请求
 * getShopByMerchant("merchant-uuid")
 * // 响应
 * // { shopIds: ["shop-uuid-1", "shop-uuid-2"] }
 */
export const getShopByMerchant = (merchantId) =>
  request.get(`${SHOP_BASE}/merchant/${merchantId}`)

/**
 * 查询店铺详情（含权限校验）
 *
 * @param {string} shopId - 店铺ID
 * @returns {Promise<{shop: Object}>}
 *
 * @example
 * // 请求
 * getShopDetail("shop-uuid")
 */
export const getShopDetail = (shopId) =>
  request.get(`${SHOP_BASE}/${shopId}`)

/**
 * 查询店铺员工列表
 *
 * @param {string} shopId - 店铺ID
 * @returns {Promise<{employees: Array, total: number}>}
 *
 * @example
 * // 请求
 * getShopEmployees("shop-uuid")
 */
export const getShopEmployees = (shopId) =>
  request.get(`${SHOP_BASE}/${shopId}/employees`)

/**
 * 创建店铺
 *
 * @param {Object} data - 店铺信息
 * @param {string} data.name - 店铺名称（必填，最长100字符）
 * @param {string} [data.description] - 店铺描述（可选，最长500字符）
 * @param {string} [data.logoId] - 店铺Logo URL
 * @returns {Promise<{id: string, message: string}>}
 *
 * @example
 * // 请求
 * registerShop({ name: '店铺名称', description: '店铺描述', logoId: 'logo-abc' })
 * // 响应
 * // { id: "shop-uuid", message: "创建店铺成功" }
 */
export const registerShop = (data) =>
  request.post(`${SHOP_BASE}/register`, data)

/**
 * 更新店铺信息（全部可选）
 *
 * @param {string} shopId - 店铺ID
 * @param {Object} data - 更新信息
 * @param {string} [data.name] - 新名称
 * @param {string} [data.description] - 新描述
 * @param {string} [data.logoId] - 新Logo URL
 * @returns {Promise<{message: string}>}
 */
export const updateShop = (shopId, data) =>
  request.put(`${SHOP_BASE}/${shopId}`, data)

export const closeShop = (shopId) =>
  request.delete(`${SHOP_BASE}/${shopId}`)

export const openShop = (shopId) =>
  request.put(`${SHOP_BASE}/${shopId}/open`)

/**
 * 添加店员
 *
 * @param {string} shopId - 店铺ID
 * @param {Object} data - 店员信息
 * @param {string} data.username - 用户名（必填）
 * @param {string} [data.password] - 密码（可选）
 * @param {string} [data.phone] - 手机号（可选）
 * @param {string} [data.name] - 店员姓名（可选）
 * @returns {Promise<{message: string}>}
 */
export const registerEmployee = (shopId, data) =>
  request.post(`${SHOP_BASE}/${shopId}/employees/register`, data)

/**
 * 移除店员
 *
 * @param {string} shopId - 店铺ID
 * @param {string} merchantId - 商家/店员ID
 * @returns {Promise<{message: string}>}
 */
export const removeEmployee = (shopId, merchantId) =>
  request.delete(`${SHOP_BASE}/${shopId}/employees/${merchantId}`)
