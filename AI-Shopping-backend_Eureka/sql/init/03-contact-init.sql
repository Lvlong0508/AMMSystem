-- 联系人服务数据库
CREATE DATABASE IF NOT EXISTS eureka_contact CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_contact;

CREATE TABLE IF NOT EXISTS t_contact (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '联系人ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NOT NULL COMMENT '电话',
    address VARCHAR(500) NOT NULL COMMENT '地址',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';

-- 用户-联系人关联表
CREATE TABLE IF NOT EXISTS user_contact (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id INT NOT NULL COMMENT '用户ID',
    contact_id INT NOT NULL COMMENT '联系人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_contact_id (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-联系人关联表';

SELECT '联系人服务数据库初始化完成' AS message;