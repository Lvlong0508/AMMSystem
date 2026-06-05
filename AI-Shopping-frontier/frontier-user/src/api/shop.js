import { request } from './request'

/**
 * 店铺查询相关接口（用户端）
 *
 * 端口: 8087
 * Header 需携带: `X-User-Id: <userId>`（由 request.js 拦截器自动注入）
 */

/**
 * 分页获取活跃店铺列表
 *
 * @param {number} [page=1] - 页码（从1开始）
 * @param {number} [size=10] - 每页条数
 * @returns {Promise<{shops: Array<{id: number, merchantId: number, shopInfoId: number, status: number, createdAt: string, updatedAt: string}>, total: number, page: number, size: number}>}
 *
 * @example
 * // 请求
 * getShopList(1, 10)
 * // 响应
 * // {
 * //   shops: [{ id: 1, merchantId: 1, shopInfoId: 1, status: 1, createdAt: "2026-01-01T00:00:00", updatedAt: "2026-01-01T00:00:00" }],
 * //   total: 10, page: 1, size: 10
 * // }
 */
export const getShopList = (page = 1, size = 10) =>
  request.get('/api/user/shop/list', { params: { page, size } })

/**
 * 获取店铺详情
 *
 * @param {number} shopId - 店铺ID
 * @returns {Promise<{shop: {id: number, merchantId: number, shopInfoId: number, status: number, createdAt: string, updatedAt: string}, shopInfo: {id: number, name: string, description: string, logourl: string}}>}
 *
 * @example
 * // 请求
 * getShopDetail(1)
 * // 响应
 * // {
 * //   shop: { id: 1, merchantId: 1, shopInfoId: 1, status: 1, createdAt: "...", updatedAt: "..." },
 * //   shopInfo: { id: 1, name: "店铺名称", description: "店铺描述", logourl: "http://example.com/logo.jpg" }
 * // }
 */
export const getShopDetail = (shopId) =>
  request.get(`/api/user/shop/${shopId}`)
