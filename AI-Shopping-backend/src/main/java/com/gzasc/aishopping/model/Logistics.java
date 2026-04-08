package com.gzasc.aishopping.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Logistics {
    private Integer id;              // 物流信息ID（主键）
    private Integer contactId;       // 联系人ID（外键）
    private Timestamp shippingDate;  // 发货日期时间
    private String trackingNumber;   // 快递单号
}
