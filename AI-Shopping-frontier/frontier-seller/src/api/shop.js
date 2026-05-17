import { request as axios } from './request'

export const shopApi = {
  // 店铺管理（管理类）
  register: (data) => axios.post('/api/seller/shop/manage/shop/register', data),
  list: () => axios.get('/api/seller/shop/list'),
  detail: (shopId) => axios.get(`/api/seller/shop/query/shop/${shopId}`),
  update: (shopId, data) => axios.put(`/api/seller/shop/manage/shop/${shopId}`, data),
  delete: (shopId) => axios.delete(`/api/seller/shop/manage/shop/${shopId}`),

  // 商品管理（管理类）
  products: (shopId) => axios.get(`/api/seller/shop/query/${shopId}/products`),
  createProduct: (shopId, data) => axios.post(`/api/seller/shop/manage/${shopId}/products`, data),
  updateProduct: (shopId, productId, data) => axios.put(`/api/seller/shop/manage/${shopId}/products/${productId}`, data),
  deleteProduct: (shopId, productId) => axios.delete(`/api/seller/shop/manage/${shopId}/products/${productId}`),

  // 订单查询（查询类）
  orders: (shopId) => axios.get(`/api/seller/shop/query/${shopId}/orders`),
  orderDetail: (shopId, orderId) => axios.get(`/api/seller/shop/query/${shopId}/orders/${orderId}`),

  // 员工管理（管理类）
  employees: (shopId) => axios.get(`/api/seller/shop/query/${shopId}/employees`),
  registerEmployee: (shopId, data) => axios.post(`/api/seller/shop/manage/${shopId}/employees/register`, data),
  removeEmployee: (shopId, merchantId) => axios.delete(`/api/seller/shop/manage/${shopId}/employees/${merchantId}`),

  // 内部接口
  getMerchantRoles: (merchantId) => axios.get(`/internal/shop/employees/roles/${merchantId}`),
}