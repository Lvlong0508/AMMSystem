-- 物流服务数据库
CREATE DATABASE IF NOT EXISTS eureka_logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_logistics;

CREATE TABLE IF NOT EXISTS logistics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '物流ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单号',
    type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY' COMMENT '类型: DELIVERY-发货, RETURN-退货',
    contact_id INT NOT NULL COMMENT '联系人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tracking_number VARCHAR(50) NOT NULL COMMENT '快递单号',
    INDEX idx_order_type (order_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

SELECT '物流服务数据库初始化完成' AS message;