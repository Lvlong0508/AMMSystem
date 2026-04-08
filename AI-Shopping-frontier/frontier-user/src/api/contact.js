import { request } from './request'

// 创建联系人
export const createContact = (contact) =>
    request.post('/contact/create', contact)

// 删除联系人
export const deleteContact = (id) =>
    request.delete(`/contact/delete/${id}`)

// 更新联系人
export const updateContact = (contact) =>
    request.put('/contact/update', contact)

// 根据ID查询联系人
export const getContactById = (id) =>
    request.get(`/contact/get/${id}`)

// 查询所有联系人
export const getAllContacts = () =>
    request.get('/contact/list')

// 根据姓名查询联系人
export const searchContactsByName = (name) =>
    request.get('/contact/search/name', { params: { name } })

// 根据电话查询联系人
export const getContactByPhone = (phone) =>
    request.get('/contact/search/phone', { params: { phone } })
