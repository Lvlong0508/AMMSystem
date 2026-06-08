-- AI-Shopping 店铺服务数据库初始化脚本
-- 创建 shop-service 所需的数据库和表结构
-- 与 auth-service 对齐：ID 统一使用 BIGINT（雪花算法），role 使用 TINYINT

-- ============================================
-- 1. 店铺服务数据库 (eureka_shop)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_shop;

-- 店铺表（运营属性）
CREATE TABLE IF NOT EXISTS shops (
    id BIGINT PRIMARY KEY COMMENT '店铺ID（雪花算法生成）',
    merchant_id BIGINT NOT NULL COMMENT '商户ID（安全算法）',
    shop_info_id BIGINT COMMENT '关联 ShopInfo ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '店铺状态：1-正常 0-已关闭',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (`status`),
    UNIQUE KEY uk_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- 店铺展示信息表
CREATE TABLE IF NOT EXISTS shop_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ShopInfo ID（数据库自增）',
    name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    description VARCHAR(500) COMMENT '店铺描述',
    logourl VARCHAR(256) COMMENT '店铺Logo URL',
    address VARCHAR(200) COMMENT '店铺地址',
    phone VARCHAR(20) COMMENT '联系电话'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺展示信息表';

-- ============================================
-- 完成提示
-- ============================================
SELECT '店铺服务数据库初始化完成！' AS message;
