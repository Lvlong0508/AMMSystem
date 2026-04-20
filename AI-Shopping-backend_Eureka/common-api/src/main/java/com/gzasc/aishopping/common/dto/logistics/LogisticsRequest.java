package com.gzasc.aishopping.common.dto.logistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 物流请求 DTO
 * 用于创建物流记录
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogisticsRequest implements Serializable {
    private Integer contactId;
    private String trackingNumber;
    private String shippingDate;
}
