-- 联系人表
CREATE TABLE `t_contact` (
    `id`           INT AUTO_INCREMENT PRIMARY KEY COMMENT '联系人ID',
    `name`         VARCHAR(100) NOT NULL COMMENT '姓名',
    `phone`        VARCHAR(20)  NOT NULL COMMENT '电话',
    `address`      VARCHAR(500) NOT NULL COMMENT '地址',
    `created_at`   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人信息表';
