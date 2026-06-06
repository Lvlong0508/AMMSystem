import { request } from './request'

/**
 * 商家端店铺相关接口
 *
 * 端口: 8087
 * Header 需携带: `X-User-Id: <userId>`（由 request.js 拦截器自动注入）
 *
 * 注意：所有 ID（shopId、merchantId 等）均为雪花算法生成的 64 位 Long，
 * 超出 JS 安全整数范围，后端序列化为字符串，请始终以字符串类型处理。
 */

const SHOP_BASE = '/api/seller/shop'

/**
 * 重新开店
 *
 * @param {string} shopId - 店铺ID（字符串）
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * openShop("2062474586787811328")
 */
export const getShopByMerchant = (merchantId) =>
  request.get(`${SHOP_BASE}/merchant/${merchantId}`)

/**
 * 查询店铺详情（含权限校验）
 *
 * @param {string} shopId - 店铺ID（字符串）
 * @returns {Promise<{shop: {id: string, merchantId: string, shopInfoId: string, status: number, createdAt: string, updatedAt: string}}>}
 *
 * @example
 * // 请求
 * getShopDetail("2062474586787811328")
 * // 响应
 * // { shop: { id: "2062474586787811328", merchantId: "2062474586787811328", shopInfoId: "10", status: 1, createdAt: "...", updatedAt: "..." } }
 */
export const getShopDetail = (shopId) =>
  request.get(`${SHOP_BASE}/${String(shopId)}`)

/**
 * 查询店铺员工列表
 *
 * @param {string} shopId - 店铺ID（字符串）
 * @returns {Promise<{employees: Array<{merchantId: string, shopId: string, role: number, assignedBy: string}>, total: number}>}
 *
 * @example
 * // 请求
 * getShopEmployees("2062474586787811328")
 * // 响应
 * // { employees: [{ merchantId: "2062474586787811328", shopId: "2062474586787811328", role: 1, assignedBy: "2062474586787811328" }], total: 1 }
 */
export const getShopEmployees = (shopId) =>
  request.get(`${SHOP_BASE}/${String(shopId)}/employees`)

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
 * registerShop({ name: '店铺名称', description: '店铺描述', logoId: 'http://example.com/logo.jpg' })
 * // 响应
 * // { id: "2062474586787811328", message: "创建店铺成功" }
 */
export const registerShop = (data) =>
  request.post(`${SHOP_BASE}/register`, data)

/**
 * 更新店铺信息（全部可选）
 *
 * @param {string} shopId - 店铺ID（字符串）
 * @param {Object} data - 更新信息
 * @param {string} [data.name] - 新名称
 * @param {string} [data.description] - 新描述
 * @param {string} [data.logoId] - 新Logo URL
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * updateShop("2062474586787811328", { name: '新名称' })
 */
export const updateShop = (shopId, data) =>
  request.put(`${SHOP_BASE}/${String(shopId)}`, data)

export const closeShop = (shopId) =>
  request.delete(`${SHOP_BASE}/${String(shopId)}`)

export const openShop = (shopId) =>
  request.put(`${SHOP_BASE}/${String(shopId)}/open`)

/**
 * 添加店员
 *
 * @param {string} shopId - 店铺ID（字符串）
 * @param {Object} data - 店员信息
 * @param {string} data.username - 用户名（必填，3-20位字母数字下划线）
 * @param {string} [data.password] - 密码（可选）
 * @param {string} [data.phone] - 手机号（可选）
 * @param {string} [data.name] - 店员姓名（可选）
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * registerEmployee("2062474586787811328", { username: 'employee001', password: 'pass123', phone: '13800138000', name: '店员姓名' })
 */
export const registerEmployee = (shopId, data) =>
  request.post(`${SHOP_BASE}/${String(shopId)}/employees/register`, data)

/**
 * 移除店员
 *
 * @param {string} shopId - 店铺ID（字符串）
 * @param {string} merchantId - 商家/店员ID（字符串）
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * removeEmployee("2062474586787811328", "2062474586787811328")
 */
export const removeEmployee = (shopId, merchantId) =>
  request.delete(`${SHOP_BASE}/${String(shopId)}/employees/${merchantId}`)
