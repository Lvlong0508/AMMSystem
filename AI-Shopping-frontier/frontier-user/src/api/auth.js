import { request } from './request'

/**
 * 用户认证相关接口
 *
 * 端口: 8086
 * 认证方式: Sa-Token，登录/注册成功后返回 token，后续请求在 Header 中携带 `satoken: <token>`
 * 密码使用 BCrypt 加盐加密存储
 */

/**
 * 用户注册
 *
 * @param {Object} data - 注册信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @param {string} data.phone - 手机号
 * @param {string} [data.email] - 邮箱（可选）
 * @returns {Promise<{message: string, token: string}>}
 *
 * @example
 * // 请求
 * userRegister({ username: 'user123', password: 'password123', phone: '13800138000', email: 'user@example.com' })
 * // 响应
 * // { message: "注册成功", token: "xxx" }
 */
export const userRegister = (data) =>
  request.post('/api/user/auth/register', data)

/**
 * 用户登录
 *
 * @param {Object} data - 登录信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise<{message: string, token: string}>}
 *
 * @example
 * // 请求
 * userLogin({ username: 'user123', password: 'password123' })
 * // 响应
 * // { message: "登录成功", token: "xxx" }
 */
export const userLogin = (data) =>
  request.post('/api/user/auth/login', data)

/**
 * 用户登出
 *
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * userLogout()
 * // 响应
 * // { message: "登出成功" }
 */
export const userLogout = () =>
  request.post('/api/user/auth/logout')

/**
 * 检查用户名是否可用
 *
 * @param {string} username - 要检查的用户名
 * @returns {Promise<{available: boolean, message: string}>}
 *
 * @example
 * // 请求
 * checkUsername('user123')
 * // 响应（可用）
 * // { available: true, message: "用户名可用" }
 * // 响应（不可用）
 * // { available: false, message: "用户名已存在" }
 */
export const checkUsername = (username) =>
  request.get('/api/user/auth/check-username', { params: { username } })

/**
 * 检查手机号是否可用
 *
 * @param {string} phone - 要检查的手机号
 * @returns {Promise<{available: boolean, message: string}>}
 *
 * @example
 * // 请求
 * checkPhone('13800138000')
 * // 响应（可用）
 * // { available: true, message: "手机号可用" }
 * // 响应（不可用）
 * // { available: false, message: "手机号已注册" }
 */
export const checkPhone = (phone) =>
  request.get('/api/user/auth/check-phone', { params: { phone } })
