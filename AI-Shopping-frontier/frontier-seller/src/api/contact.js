import { request } from './request'

/**
 * 商家地址相关接口
 *
 * 端口: 8083
 * Header 需携带: `X-Shop-Id: <shopId>`（由 request.js 拦截器自动注入）
 */

const ADDRESS_BASE = '/api/merchant/address'

/**
 * 创建店铺地址
 *
 * @param {Object} data - 地址信息
 * @param {string} data.name - 联系人/仓库名称
 * @param {string} data.phone - 联系电话
 * @param {string} data.address - 详细地址
 * @param {number} [data.addressType=1] - 地址类型（1=收货/退货地址）
 * @param {number} [data.isDefault] - 是否默认（1=是，0=否）
 * @returns {Promise<{id: number, message: string}>}
 *
 * @example
 * // 请求
 * createAddress({ name: "仓库A", phone: "13800138000", address: "广东省深圳市南山区xxx", addressType: 1, isDefault: 1 })
 * // 响应
 * // { id: 1, message: "创建地址成功" }
 */
export const createAddress = (address) =>
  request.post(`${ADDRESS_BASE}/create`, address)

/**
 * 更新店铺地址
 *
 * @param {number} id - 地址ID
 * @param {Object} data - 更新信息
 * @param {string} [data.name] - 联系人/仓库名称
 * @param {string} [data.phone] - 联系电话
 * @param {string} [data.address] - 详细地址
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * updateAddress(1, { name: "仓库B", phone: "13800138001" })
 */
export const updateAddress = (id, address) =>
  request.put(`${ADDRESS_BASE}/update/${id}`, address)

/**
 * 删除店铺地址
 *
 * @param {number} id - 地址ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * deleteAddress(1)
 */
export const deleteAddress = (id) =>
  request.delete(`${ADDRESS_BASE}/delete/${id}`)

/**
 * 获取店铺地址列表
 *
 * @returns {Promise<{contacts: Array<{id: number, name: string, phone: string, address: string, addressType: number, isDefault: number}>, total: number}>}
 *
 * @example
 * // 请求
 * getAddressList()
 * // 响应
 * // { contacts: [{ id: 1, name: "仓库A", phone: "13800138000", address: "xxx", addressType: 1, isDefault: 1 }], total: 1 }
 */
export const getAddressList = () =>
  request.get(`${ADDRESS_BASE}/list`)

/**
 * 获取默认收货地址
 *
 * @returns {Promise<{id: number, name: string, phone: string, address: string, addressType: number, isDefault: number}>}
 *
 * @example
 * // 请求
 * getShipDefaultAddress()
 * // 响应
 * // { id: 1, name: "仓库A", phone: "13800138000", address: "xxx", addressType: 1, isDefault: 1 }
 */
export const getShipDefaultAddress = () =>
  request.get(`${ADDRESS_BASE}/ship-default`)

/**
 * 设置默认地址
 *
 * @param {number} id - 要设为默认的地址ID
 * @returns {Promise<{message: string}>}
 *
 * @example
 * // 请求
 * setDefaultAddress(1)
 */
export const setDefaultAddress = (id) =>
  request.put(`${ADDRESS_BASE}/set-default/${id}`)
