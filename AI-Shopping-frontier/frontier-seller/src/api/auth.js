import { request } from './request'

/**
 * 商家认证相关接口
 * 使用 Sa-Token 进行认证
 */

// ==================== 登录/注册 ====================

/**
 * 商家登录
 * @param {Object} data - {username, password}
 * @returns {Promise} - {message, token, merchantInfo}
 */
export const merchantLogin = (data) =>
  request.post('/api/seller/auth/login', data)

/**
 * 商家注册
 * @param {Object} data - {username, password, nickname, phone}
 * @returns {Promise} - {message, token, merchantInfo}
 */
export const merchantRegister = (data) =>
  request.post('/api/seller/auth/register', data)

/**
 * 商家登出
 * @returns {Promise} - {message}
 */
export const merchantLogout = () =>
  request.post('/api/seller/auth/logout')

// ==================== 商家信息 ====================

/**
 * 获取当前登录商家信息
 * @returns {Promise} - {message, merchantInfo}
 */
export const getMerchantInfo = () =>
  request.get('/api/seller/auth/info')

// ==================== 校验接口 ====================

/**
 * 检查商家用户名是否可用
 * @param {string} username
 * @returns {Promise} - {available, message}
 */
export const checkMerchantUsername = (username) =>
  request.get('/api/seller/auth/check-username', { params: { username } })

/**
 * 检查商家手机号是否可用
 * @param {string} phone
 * @returns {Promise} - {available, message}
 */
export const checkMerchantPhone = (phone) =>
  request.get('/api/seller/auth/check-phone', { params: { phone } })
