-- 订单表
CREATE TABLE `t_order` (
    `order_id`           VARCHAR(32)    NOT NULL COMMENT '订单ID',
    `product_id`         VARCHAR(20)    NOT NULL COMMENT '商品ID（外键）',
    `quantity`           INT            NOT NULL DEFAULT 0 COMMENT '数量',
    `total_price`        DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '总价',
    `order_status`       VARCHAR(50)    NOT NULL COMMENT '订单状态',
    `order_date`         TIMESTAMP      NOT NULL COMMENT '生成日期时间',
    `contact_id`         INT            NULL COMMENT '收货人联系人ID（外键）',
    `logistics_id`       INT            NULL COMMENT '物流信息ID（外键）',
    `created_at`         TIMESTAMP      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`order_id`),
    FOREIGN KEY (`contact_id`) REFERENCES `t_contact`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`id`) ON DELETE RESTRICT,
    FOREIGN KEY (`logistics_id`) REFERENCES `logistics`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单信息表';