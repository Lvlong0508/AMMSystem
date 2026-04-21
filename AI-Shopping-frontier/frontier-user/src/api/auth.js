import { request } from './request'

/**
 * 用户认证相关接口
 * 使用 Sa-Token 进行认证
 */

// ==================== 登录/注册 ====================

/**
 * 用户登录
 * @param {Object} data - {username, password}
 * @returns {Promise} - {message, token, userInfo}
 */
export const userLogin = (data) =>
  request.post('/auth/user/login', data)

/**
 * 用户注册
 * @param {Object} data - {username, password, nickname, phone}
 * @returns {Promise} - {message, token, userInfo}
 */
export const userRegister = (data) =>
  request.post('/auth/user/register', data)

/**
 * 用户登出
 * @returns {Promise} - {message}
 */
export const userLogout = () =>
  request.post('/auth/user/logout')

// ==================== 用户信息 ====================

/**
 * 获取当前登录用户信息
 * @returns {Promise} - {message, userInfo}
 */
export const getUserInfo = () =>
  request.get('/auth/user/info')

// ==================== 校验接口 ====================

/**
 * 检查用户名是否可用
 * @param {string} username
 * @returns {Promise} - {available, message}
 */
export const checkUsername = (username) =>
  request.get('/auth/user/check-username', { params: { username } })

/**
 * 检查手机号是否可用
 * @param {string} phone
 * @returns {Promise} - {available, message}
 */
export const checkPhone = (phone) =>
  request.get('/auth/user/check-phone', { params: { phone } })
