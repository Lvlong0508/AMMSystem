-- ============================================
-- AI-Shopping 认证服务数据库初始化脚本
-- 包含：用户表(t_user) 和 商家表(t_merchant)
-- 密码使用 BCrypt 加盐加密存储
-- ============================================

-- ============================================
-- 1. 消费者用户数据库 (eureka_user)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_user;

-- 用户基础信息表（用户和商家共用）
CREATE TABLE IF NOT EXISTS user_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '信息ID',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表 - 用户和商家共用';

-- 消费者用户表
CREATE TABLE IF NOT EXISTS t_user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码（含Salt），格式: $2a$12$...',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    info_id INT COMMENT '用户信息ID（关联user_info表）',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_info_id (info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费者用户表 - 密码使用BCrypt加盐加密';

-- 外键约束：t_user.info_id -> user_info.id
ALTER TABLE t_user ADD CONSTRAINT fk_user_info_id FOREIGN KEY (info_id) REFERENCES user_info(id) ON DELETE SET NULL;

-- ============================================
-- 2. 商家用户数据库 (新建 eureka_merchant)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_merchant CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_merchant;

-- 商家用户表
CREATE TABLE IF NOT EXISTS t_merchant (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '商家ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '商家用户名',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码（含Salt），格式: $2a$12$...',
    phone VARCHAR(20) UNIQUE COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    info_id INT COMMENT '用户信息ID（关联eureka_user库的user_info表）',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_info_id (info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家用户表 - 密码使用BCrypt加盐加密';

-- 外键约束：t_merchant.info_id -> eureka_user.user_info.id（跨库引用）
ALTER TABLE t_merchant ADD CONSTRAINT fk_merchant_info_id FOREIGN KEY (info_id) REFERENCES eureka_user.user_info(id) ON DELETE SET NULL;

-- ============================================
-- 完成提示
-- ============================================
SELECT '认证服务数据库初始化完成！' AS message;
