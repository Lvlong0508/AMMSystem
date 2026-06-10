// Auth
export { merchantRegister, merchantLogin, merchantLogout, checkMerchantUsername, checkMerchantPhone } from './auth'

// Contact
export { createAddress, updateAddress, deleteAddress, getAddressList, getShipDefaultAddress, setDefaultAddress } from './contact'

// Logistics
export { createLogistics, getAllLogistics, getLogisticsByTrackingNumber, deleteLogistics, getLogisticsByOrder, getLatestLogisticsByOrder } from './logistics'

// Order
export {
  getOrderListByShop,
  getShipmentList,
  getOrderDetail,
  shipOrder,
  approveReturn,
  confirmReturn,
  getReturnRequestsPending,
  getReturnRequestsProcessed,
  reviewReturnRequest
} from './order'

// Product
export { createProduct, updateProduct, deleteProduct, getProductById, batchGetProducts, listProduct, unlistProduct, getProductsByShop } from './product'

// Shop
export { getMyShop, createShop, getShopDetail, updateShop, closeShop, openShop } from './shop'
