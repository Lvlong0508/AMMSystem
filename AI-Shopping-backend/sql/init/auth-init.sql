-- ============================================
-- AI-Shopping 认证服务数据库初始化脚本
-- 数据库：eureka_auth
-- 表：用户表(t_user) 、商家表(t_merchant)、用户信息表(user_info)、商家信息表(merchant_info)
-- 密码使用 BCrypt 加盐加密存储
-- ============================================

-- ============================================
-- 1. 认证服务数据库 (eureka_auth)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_auth;

-- 用户基础信息表
CREATE TABLE IF NOT EXISTS user_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '信息ID',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';

-- 商家基础信息表
CREATE TABLE IF NOT EXISTS merchant_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '信息ID',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家基础信息表';

-- 消费者用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID（Snowflake）',
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

-- 商家用户表
CREATE TABLE IF NOT EXISTS t_merchant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商家ID（Snowflake）',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '商家用户名',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码（含Salt），格式: $2a$12$...',
    phone VARCHAR(20) UNIQUE COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    info_id INT COMMENT '商家信息ID（关联merchant_info表）',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_info_id (info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家用户表 - 密码使用BCrypt加盐加密';

-- 外键约束
ALTER TABLE t_user ADD CONSTRAINT fk_user_info_id FOREIGN KEY (info_id) REFERENCES user_info(id) ON DELETE SET NULL;
ALTER TABLE t_merchant ADD CONSTRAINT fk_merchant_info_id FOREIGN KEY (info_id) REFERENCES merchant_info(id) ON DELETE SET NULL;

-- ============================================
-- 完成提示
-- ============================================
SELECT '认证服务数据库初始化完成！' AS message;