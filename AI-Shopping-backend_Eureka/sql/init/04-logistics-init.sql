-- 物流服务数据库
CREATE DATABASE IF NOT EXISTS eureka_logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_logistics;

CREATE TABLE IF NOT EXISTS logistics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '物流ID',
    contact_id INT NOT NULL COMMENT '联系人ID',
    shipping_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发货时间',
    tracking_number VARCHAR(50) NOT NULL COMMENT '快递单号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

SELECT '物流服务数据库初始化完成' AS message;