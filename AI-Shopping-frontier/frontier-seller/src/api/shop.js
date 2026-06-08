import { request } from './request'

/**
 * 商家端店铺相关接口
 *
 * 端口: 8087
 * Header 需携带: `X-User-Id: <userId>`（由 request.js 拦截器自动注入）
 */

const SHOP_BASE = '/api/seller/shop'

/**
 * 查询我的店铺（一对一语义，返回单个店铺对象）
 *
 * @returns {Promise<{shop: {id: number, name: string|null, status: number}|null}>}
 */
export const getMyShop = () => request.get(`${SHOP_BASE}/my-shop`)

/**
 * 创建店铺（已注册商家单独创建店铺）
 *
 * 支持 JSON 对象或 FormData；注册页使用 FormData，其中 shop 为 JSON Blob，logo 为可选 JPG/PNG 文件。
 *
 * @param {Object|FormData} data
 * @returns {Promise<{id: number}>}
 */
export const createShop = (data) => request.post(`${SHOP_BASE}/register`, data)

/**
 * 查询店铺详情（含权限校验）
 */
export const getShopDetail = (shopId) => request.get(`${SHOP_BASE}/${shopId}`)

/**
 * 更新店铺信息（全部可选）
 */
export const updateShop = (shopId, data) => request.put(`${SHOP_BASE}/${shopId}`, data)

/**
 * 关闭店铺
 */
export const closeShop = (shopId) => request.patch(`${SHOP_BASE}/${shopId}/close`)

/**
 * 重新开店
 */
export const openShop = (shopId) => request.patch(`${SHOP_BASE}/${shopId}/open`)
