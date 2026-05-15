import { request as axios } from './request'

export const shopApi = {
  register: (data) => axios.post('/api/seller/shop/register', data),
  list: () => axios.get('/api/seller/shop/list'),
  detail: (shopId) => axios.get(`/api/seller/shop/${shopId}`),
  update: (shopId, data) => axios.put(`/api/seller/shop/${shopId}`, data),
  delete: (shopId) => axios.delete(`/api/seller/shop/${shopId}`),
  products: (shopId) => axios.get(`/api/seller/shop/${shopId}/products`),
  createProduct: (shopId, data) => axios.post(`/api/seller/shop/${shopId}/products`, data),
  updateProduct: (shopId, productId, data) => axios.put(`/api/seller/shop/${shopId}/products/${productId}`, data),
  deleteProduct: (shopId, productId) => axios.delete(`/api/seller/shop/${shopId}/products/${productId}`),
  orders: (shopId) => axios.get(`/api/seller/shop/${shopId}/orders/all`),
  orderDetail: (shopId, orderId) => axios.get(`/api/seller/shop/${shopId}/orders/${orderId}`),
  employees: (shopId) => axios.get(`/api/seller/shop/${shopId}/employees`),
  registerEmployee: (shopId, data) => axios.post(`/api/seller/shop/${shopId}/employees/register`, data),
  removeEmployee: (shopId, merchantId) => axios.delete(`/api/seller/shop/${shopId}/employees/${merchantId}`),
}