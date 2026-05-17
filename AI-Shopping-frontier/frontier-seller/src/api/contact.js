import { request } from './request'

// 创建联系人
// 创建联系人（商家不需要此功能）
// export const createContact = (contact) =>
//     request.post('/api/user/contact/create', contact)

// 删除联系人
// 删除联系人（商家不需要此功能）
// export const deleteContact = (id) =>
//     request.delete(`/api/user/contact/${id}`)

// 更新联系人
// 更新联系人（商家不需要此功能）
// export const updateContact = (contact) =>
//     request.put('/api/user/contact/${contact.id}', contact)

// 根据ID查询联系人
export const getContactById = (id) =>
    request.get(`/api/seller/contact/get/${id}`)

// 查询所有联系人
export const getAllContacts = () =>
    request.get('/api/seller/contact/list')

// 根据姓名查询联系人
export const searchContactsByName = (name) =>
    request.get('/api/seller/contact/search/name', { params: { name } })

// 根据电话查询联系人
export const getContactByPhone = (phone) =>
    request.get('/api/seller/contact/search/phone', { params: { phone } })

// ========== 商家地址管理 API ==========
// 获取商家地址列表
export const getAddressList = (shopId) =>
    request.get('/api/seller/address/list', { headers: { 'X-Shop-Id': shopId } })

// 获取发货地址列表（仅发货地址，不含退货地址）
export const getShipAddressList = (shopId) =>
    request.get('/api/seller/address/ship-list', { headers: { 'X-Shop-Id': shopId } })

// 新增地址
export const addAddress = (shopId, address) =>
    request.post('/api/seller/address/add', address, { headers: { 'X-Shop-Id': shopId } })

// 修改地址
export const updateAddress = (shopId, id, address) =>
    request.put(`/api/seller/address/update/${id}`, address, { headers: { 'X-Shop-Id': shopId } })

// 删除地址
export const deleteAddress = (shopId, id) =>
    request.delete(`/api/seller/address/delete/${id}`, { headers: { 'X-Shop-Id': shopId } })

// 设置默认地址
export const setDefaultAddress = (shopId, id) =>
    request.put(`/api/seller/address/set-default/${id}`, {}, { headers: { 'X-Shop-Id': shopId } })
