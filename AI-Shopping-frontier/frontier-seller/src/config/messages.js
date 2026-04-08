// 智能购物系统 - 提示消息配置
// 统一在此修改提示文字，无需改动业务代码

export const APP_TITLE = '智能购物系统'

export const CONTACT_MESSAGES = {
  // 成功提示
  CREATE_SUCCESS: '地址创建成功',
  UPDATE_SUCCESS: '地址更新成功',
  DELETE_SUCCESS: '地址删除成功',

  // 失败提示
  CREATE_FAILED: '创建失败，请稍后重试',
  UPDATE_FAILED: '更新失败，请稍后重试',
  DELETE_FAILED: '删除失败，请稍后重试',
  LOAD_FAILED: '获取地址列表失败',
  SEARCH_FAILED: '搜索失败',
  OPERATION_FAILED: '操作失败',

  // 确认提示
  DELETE_CONFIRM_TITLE: '确认删除',
  DELETE_CONFIRM_TEXT: '确定要删除该地址信息吗？此操作不可恢复。',

  // 按钮文字
  CONFIRM_BUTTON: '确定',
  CANCEL_BUTTON: '取消',
  CLOSE_BUTTON: '关闭'
}

export const ORDER_MESSAGES = {
  // 成功提示
  UPDATE_SUCCESS: '订单状态更新成功',
  SHIP_SUCCESS: '发货成功',
  DELETE_SUCCESS: '订单删除成功',

  // 失败提示
  LOAD_FAILED: '加载订单失败',
  UPDATE_FAILED: '更新状态失败',
  SHIP_FAILED: '发货失败',
  DELETE_FAILED: '删除订单失败',
  OPERATION_FAILED: '操作失败',

  // 确认提示
  DELETE_CONFIRM_TITLE: '确认删除',
  DELETE_CONFIRM: '确定要删除该订单吗？此操作不可恢复。',

  // 按钮文字
  CONFIRM_BUTTON: '确定',
  CANCEL_BUTTON: '取消'
}

export const COMMON_MESSAGES = {
  OPERATION_FAILED: '操作失败',
  NETWORK_ERROR: '网络错误，请检查连接',
  UNKNOWN_ERROR: '未知错误，请稍后重试'
}
