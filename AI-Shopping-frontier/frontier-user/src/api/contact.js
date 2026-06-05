import { request } from './request'

/**
 * 用户地址相关接口
 *
 * 端口: 8083
 * Header 需携带: `X-User-Id: <userId>`（由 request.js 拦截器自动注入）
 */

/**
 * 创建地址
 *
 * @param {Object} data - 地址信息
 * @param {string} data.name - 联系人姓名
 * @param {string} data.phone - 联系电话
 * @param {string} data.address - 详细地址
 * @returns {Promise<{id: number, message: string}>}
 *
 * @example
 * // 请求
 * createContact({ name: "张三", phone: "13800138000", address: "广东省深圳市南山区xxx" })
 * // 响应
 * // { id: 1, message: "创建地址成功" }
 */
export const createContact = (data) =>
  request.post('/api/user/contact/create', data)

/**
 * 更新地址
 *
 * @param {Object} data - 更新信息
 * @param {number} data.id - 地址ID
 * @param {string} data.name - 联系人姓名
 * @param {string} data.phone - 联系电话
 * @param {string} data.address - 详细地址
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * updateContact({ id: 1, name: "张三", phone: "13800138000", address: "广东省深圳市南山区xxx" })
 */
export const updateContact = (data) =>
  request.put('/api/user/contact/update', data)

/**
 * 删除地址
 *
 * @param {number} id - 地址ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * deleteContact(1)
 */
export const deleteContact = (id) =>
  request.delete(`/api/user/contact/delete/${id}`)

/**
 * 获取地址列表
 *
 * @returns {Promise<{contacts: Array<{id: number, name: string, phone: string, address: string, isDefault: number}>, total: number}>}
 *
 * @example
 * // 请求
 * getContactList()
 * // 响应
 * // {
 * //   contacts: [
 * //     { id: 1, name: "张三", phone: "13800138000", address: "xxx", isDefault: 1 }
 * //   ],
 * //   total: 1
 * // }
 */
export const getContactList = () =>
  request.get('/api/user/contact/list')

/**
 * 设置默认地址
 *
 * @param {number} id - 要设为默认的地址ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * setDefaultContact(1)
 */
export const setDefaultContact = (id) =>
  request.put(`/api/user/contact/set-default/${id}`)

// ==================== 向后兼容别名（以下函数映射到相同后端接口） ====================

/** @deprecated 请使用 getContactList() */
export const getAllContacts = getContactList
