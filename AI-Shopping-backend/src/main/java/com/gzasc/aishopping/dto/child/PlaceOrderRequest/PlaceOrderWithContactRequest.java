package com.gzasc.aishopping.dto;

import com.gzasc.aishopping.model.Contact;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 下单请求 DTO（包含收货人信息）
 * 继承自 PlaceOrderRequest，添加 contact 字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlaceOrderWithContactRequest extends PlaceOrderRequest {
    private Contact contact;
}
