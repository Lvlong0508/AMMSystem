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
