package com.gzasc.aishopping.product.service;

/**
 * 库存预占服务接口
 */
public interface ProductReservationService {

    /**
     * 预占库存
     * <p>下单后调用，锁定商品库存防止超卖。预占有效期为 30 分钟。</p>
     * @param orderId 订单ID
     * @param productId 商品ID
     * @param quantity 预占数量
     * @throws com.gzasc.aishopping.product.exception.ProductException 库存不足时抛出
     */
    void reserve(String orderId, String productId, int quantity);

    /**
     * 确认预占并扣减库存
     * <p>支付成功后调用，将预占转为正式扣减。</p>
     * @param orderId 订单ID
     * @throws com.gzasc.aishopping.product.exception.ProductException 预占不存在、状态异常或库存不足时抛出
     */
    void confirm(String orderId);

    /**
     * 释放预占
     * <p>取消订单或超时取消时调用，释放预占的库存。</p>
     * @param orderId 订单ID
     */
    void release(String orderId);

    /**
     * 释放所有已过期的预占记录
     * <p>由定时任务调用，清理因网络超时等原因产生的孤儿预占记录。</p>
     */
    void releaseExpiredReservations();
}
