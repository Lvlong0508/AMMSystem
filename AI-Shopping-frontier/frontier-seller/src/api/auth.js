import { request } from './request'

// ==================== 登录/注册 ====================

export const merchantLogin = (data) =>
  request.post('/api/seller/auth/login', data)

export const merchantRegister = (data) =>
  request.post('/api/seller/auth/register', data)

export const merchantLogout = () =>
  request.post('/api/seller/auth/logout')

// ==================== 校验接口 ====================

export const checkMerchantUsername = (username) =>
  request.get('/api/seller/auth/check-username', { params: { username } })

export const checkMerchantPhone = (phone) =>
  request.get('/api/seller/auth/check-phone', { params: { phone } })
