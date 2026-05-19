-- AI-Shopping 店铺服务数据库初始化脚本
-- 创建 shop-service 所需的数据库和表结构

-- ============================================
-- 1. 店铺服务数据库 (eureka_shop)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_shop;

-- 店铺表
CREATE TABLE IF NOT EXISTS shops (
    id VARCHAR(32) PRIMARY KEY COMMENT '店铺ID',
    merchant_id VARCHAR(16) NOT NULL COMMENT '商户ID(创建者)',
    name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    description VARCHAR(500) COMMENT '店铺描述',
    logo_id VARCHAR(32) COMMENT '店铺Logo图片ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '店铺状态：1-正常 0-已关闭',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

-- 商家角色表（商家员工/权限管理）
CREATE TABLE IF NOT EXISTS merchant_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    merchant_id VARCHAR(16) NOT NULL COMMENT '商家/员工ID',
    shop_id VARCHAR(33) NOT NULL COMMENT '店铺ID',
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色：OWNER-店主 MANAGER-经理 EMPLOYEE-员工',
    assigned_by VARCHAR(16) COMMENT '分配者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_shop_id (shop_id),
    UNIQUE KEY uk_merchant_shop (merchant_id, shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家角色表';

-- 商品-店铺关联表
CREATE TABLE IF NOT EXISTS product_shops (
    id VARCHAR(33) PRIMARY KEY COMMENT '关联ID',
    product_id VARCHAR(20) NOT NULL COMMENT '商品ID',
    shop_id VARCHAR(32) NOT NULL COMMENT '店铺ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_shop_id (shop_id),
    INDEX idx_product_id (product_id),
    UNIQUE KEY uk_product_shop (product_id, shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品店铺关联表';

-- 订单-店铺关联表
CREATE TABLE IF NOT EXISTS order_shops (
    id VARCHAR(33) PRIMARY KEY COMMENT '关联ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单ID',
    shop_id VARCHAR(32) NOT NULL COMMENT '店铺ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_shop_id (shop_id),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单店铺关联表';

-- ============================================
-- 完成提示
-- ============================================
SELECT '店铺服务数据库初始化完成！' AS message;