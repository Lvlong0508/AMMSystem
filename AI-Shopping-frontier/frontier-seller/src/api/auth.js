import { request } from './request'

/**
 * 商家认证相关接口
 *
 * 端口: 8086
 * 认证方式: Sa-Token，登录/注册成功后返回 token，后续请求在 Header 中携带 `satoken: <token>`
 * 密码使用 BCrypt 加盐加密存储
 */

/**
 * 商家注册
 *
 * @param {Object} data - 注册信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @param {string} data.phone - 手机号
 * @param {number} data.merchantId - 商家ID
 * @returns {Promise<{message: string, token: string}>}
 *
 * @example
 * // 请求
 * merchantRegister({ username: 'employee001', password: 'pass123', phone: '13900139000', merchantId: 1 })
 * // 响应
 * // { message: "注册成功", token: "xxx" }
 */
export const merchantRegister = (data) =>
  request.post('/api/seller/auth/register', data)

/**
 * 商家登录
 *
 * @param {Object} data - 登录信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise<{message: string, token: string}>}
 *
 * @example
 * // 请求
 * merchantLogin({ username: 'employee001', password: 'pass123' })
 * // 响应
 * // { message: "登录成功", token: "xxx" }
 */
export const merchantLogin = (data) =>
  request.post('/api/seller/auth/login', data)

/**
 * 商家登出
 *
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * merchantLogout()
 * // 响应
 * // { message: "登出成功" }
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
 * // 请求
 * checkMerchantUsername('employee001')
 * // 响应（可用）
 * // { available: true, message: "用户名可用" }
 * // 响应（不可用）
 * // { available: false, message: "用户名已存在" }
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
 * // 请求
 * checkMerchantPhone('13900139000')
 * // 响应（可用）
 * // { available: true, message: "手机号可用" }
 * // 响应（不可用）
 * // { available: false, message: "手机号已注册" }
 */
export const checkMerchantPhone = (phone) =>
  request.get('/api/seller/auth/check-phone', { params: { phone } })
