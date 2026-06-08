import { request } from './request'

/**
 * 商家认证相关接口
 *
 * 端口: 8086
 * 认证方式: Sa-Token，登录/注册成功后返回 token，后续请求在 Header 中携带 `satoken: <token>`
 * 密码使用 BCrypt 加盐加密存储
 */

/**
 * 商家注册（支持一步完成账号+店铺创建）
 *
 * 重构后：注册时可选择同时创建店铺（shop 字段可选）。
 * 填 shop 字段时，auth-service 会同步调用 shop-service 创建店铺。
 * 不再需要注册成功后跳转到单独的创建店铺页面。
 *
 * @param {Object} data - 注册信息
 * @param {string} data.username - 用户名（必填）
 * @param {string} data.password - 密码（必填）
 * @param {string} data.phone - 手机号（必填）
 * @param {string} [data.email] - 邮箱（可选）
 * @param {string} [data.nickname] - 昵称（可选）
 * @param {Object} [data.shop] - 店铺信息（可选，填了则同时创建店铺）
 * @param {string} data.shop.name - 店铺名称（如果传了 shop 则必填）
 * @param {string} [data.shop.description] - 店铺描述
 * @param {string} [data.shop.logoUrl] - 店铺 Logo URL
 * @returns {Promise<{message: string, token: string, accountType: string, merchantInfo: Object}>}
 *
 * @example
 * // 仅注册账号
 * merchantRegister({ username: 'seller01', password: 'pass123', phone: '13900139000' })
 *
 * @example
 * // 注册账号 + 创建店铺
 * merchantRegister({
 *   username: 'seller02',
 *   password: 'pass123',
 *   phone: '13900139001',
 *   shop: { name: '我的小店', description: '新店开张', logoUrl: 'http://logo.jpg' }
 * })
 *
 * // 响应
 * // {
 * //   code: 200,
 * //   message: "注册成功",
 * //   data: {
 * //     token: "xxx",
 * //     accountType: "MERCHANT",
 * //     merchantInfo: { id: "2062474586787811328", username: "seller02", ... }
 * //   }
 * // }
 */
export const merchantRegister = (data) =>
  request.post('/api/seller/auth/register', data)

/**
 * 商家登录
 *
 * @param {Object} data - 登录信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise<{message: string, token: string, accountType: string, merchantInfo: Object}>}
 *
 * @example
 * merchantLogin({ username: 'seller01', password: 'pass123' })
 * // 响应
 * // { code: 200, data: { token: "xxx", accountType: "MERCHANT", merchantInfo: {...} } }
 */
export const merchantLogin = (data) =>
  request.post('/api/seller/auth/login', data)

/**
 * 商家登出
 *
 * @returns {Promise<{message: string}>}
 */
export const merchantLogout = () =>
  request.post('/api/seller/auth/logout')

/**
 * 检查用户名是否可用
 *
 * @param {string} username - 要检查的用户名
 * @returns {Promise<{available: boolean, message: string}>}
 *
 * @example
 * checkMerchantUsername('seller01')
 * // 响应（可用）: { available: true, message: "用户名可用" }
 * // 响应（不可用）: { available: false, message: "用户名已存在" }
 */
export const checkMerchantUsername = (username) =>
  request.get('/api/seller/auth/check-username', { params: { username } })

/**
 * 检查手机号是否可用
 *
 * @param {string} phone - 要检查的手机号
 * @returns {Promise<{available: boolean, message: string}>}
 *
 * @example
 * checkMerchantPhone('13900139000')
 * // 响应（可用）: { available: true, message: "手机号可用" }
 * // 响应（不可用）: { available: false, message: "手机号已注册" }
 */
export const checkMerchantPhone = (phone) =>
  request.get('/api/seller/auth/check-phone', { params: { phone } })