-- 物流信息表
CREATE TABLE `logistics` (
    `id`              INT            AUTO_INCREMENT PRIMARY KEY COMMENT '物流信息ID（主键）',
    `contact_id`      INT            NOT NULL COMMENT '发货人联系人ID（外键）',
    `shipping_date`   TIMESTAMP      NOT NULL COMMENT '发货日期时间',
    `tracking_number` VARCHAR(100)   NOT NULL COMMENT '快递单号',
    `created_at`      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (`contact_id`) REFERENCES `t_contact`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物流信息表';
