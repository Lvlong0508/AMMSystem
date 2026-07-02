package com.gzasc.aishopping.order.stream;

/**
 * 订单事件类型枚举。
 * 每种类型对应一种业务操作，作为 Redis Stream 消息中 eventType 字段的取值。
 */
public enum OrderEventType {

    /** 支付成功后确认库存扣减，实际占用库存 */
    STOCK_CONFIRM,

    /** 取消订单后恢复已扣减的库存 */
    STOCK_RESTORE,

    /** 支付成功后为订单创建物流记录 */
    LOGISTICS_CREATE,

    /** 超时未支付或订单取消时释放预占库存 */
    RESERVATION_RELEASE,

    /** 订单完成后异步清理关联的退货申请记录 */
    RETURN_REQUEST_CLEANUP
}
