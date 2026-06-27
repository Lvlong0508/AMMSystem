-- 商品服务数据�?
CREATE DATABASE IF NOT EXISTS eureka_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_product;

CREATE TABLE IF NOT EXISTS product_images (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
    url VARCHAR(500) NOT NULL COMMENT '图片URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片�?;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY COMMENT '商品ID(雪花算法生成)',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    tags VARCHAR(500) COMMENT '商品标签，逗号分隔',
    description TEXT COMMENT '商品描述',
    stock INT NOT NULL DEFAULT 0 COMMENT '商品库存',
    is_sale TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否在售�?在售�?下架',
    image_id INT COMMENT '图片ID',
    shop_id BIGINT COMMENT '所属店铺ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (image_id) REFERENCES product_images(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品�?;

CREATE TABLE IF NOT EXISTS product_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    created_at DATETIME NOT NULL DEFAULT NOW(),
    expired_at DATETIME NOT NULL,
    INDEX idx_product_status (product_id, status),
    INDEX idx_expired (status, expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品预留�?;


-- 索引：加速价格区间查�?
CREATE INDEX idx_price ON products(price);

-- 索引：加速按在售状�?价格查询（用户端/商家端常见场景）
CREATE INDEX idx_is_sale_price ON products(is_sale, price);

SELECT '商品服务数据库初始化完成' AS message;
-- 订单服务数据�?
CREATE DATABASE IF NOT EXISTS eureka_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_order;

CREATE TABLE IF NOT EXISTS t_order (
    order_id     VARCHAR(20) PRIMARY KEY COMMENT '订单ID',
    user_id      BIGINT       NOT NULL COMMENT '用户ID(Snowflake)',
    shop_id      VARCHAR(32)  NOT NULL COMMENT '店铺ID',
    product_id   VARCHAR(64)  NOT NULL COMMENT '商品ID',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price  DECIMAL(10,2)NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING待支�?PAID待发�?SHIPPED已发�?DELIVERED已送达/CANCELLED已取�?RETURN_PENDING待退�?RETURNING退货中/RETURNED已退�?,
    order_date   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    contact_id   INT          COMMENT '联系人ID',
    INDEX idx_user_id (user_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单�?;

CREATE TABLE IF NOT EXISTS deleted_orders (
    id           INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    order_id     VARCHAR(20)  NOT NULL COMMENT '订单ID',
    user_id      BIGINT       COMMENT '用户ID',
    shop_id      VARCHAR(32)  COMMENT '店铺ID',
    product_id   VARCHAR(64)  NOT NULL COMMENT '商品ID',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price  DECIMAL(10,2)NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20)  NOT NULL COMMENT '删除时的订单状�?,
    order_date   TIMESTAMP    NOT NULL COMMENT '原下单时�?,
    contact_id   INT          COMMENT '联系人ID',
    deleted_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已删除订单备份表';

CREATE TABLE IF NOT EXISTS return_requests (
    order_id      VARCHAR(20) PRIMARY KEY COMMENT '订单ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    shop_id       VARCHAR(32)  NOT NULL COMMENT '店铺ID',
    return_reason VARCHAR(500) NOT NULL COMMENT '退货原�?,
    status        VARCHAR(20)  NOT NULL DEFAULT 'applying' COMMENT '状�?applying审核�?agreed同意/rejected拒绝',
    logistics_id  INT          NULL COMMENT '退货物流ID',
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_date  TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT chk_return_status CHECK (status IN ('applying','agreed','rejected')),
    INDEX idx_shop_status (shop_id, status),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货申请表';

SELECT '订单服务数据库初始化完成' AS message;
-- 联系人服务数据库
CREATE DATABASE IF NOT EXISTS eureka_contact CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_contact;

CREATE TABLE IF NOT EXISTS t_contact (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '联系人ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NOT NULL COMMENT '电话',
    address VARCHAR(500) NOT NULL COMMENT '地址',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认�?-�?1-�?,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联系人表';

-- 用户-联系人关联表
CREATE TABLE IF NOT EXISTS user_contact (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID（auth-service雪花ID�?,
    contact_id INT NOT NULL COMMENT '联系人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_contact_id (contact_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-联系人关联表';

SELECT '联系人服务数据库初始化完�? AS message;

-- 商家地址�?
CREATE TABLE IF NOT EXISTS shop_address (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    name VARCHAR(100) NOT NULL COMMENT '收货人姓�?,
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    address VARCHAR(500) NOT NULL COMMENT '完整地址',
    address_type TINYINT NOT NULL DEFAULT 1 COMMENT '地址类型�?-发货地址 2-退货地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认�?-�?1-�?,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_address_type (address_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家地址�?;

-- 商店地址关联�?
CREATE TABLE IF NOT EXISTS shop_address_rel (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    shop_id VARCHAR(33) NOT NULL COMMENT '店铺ID',
    address_id INT NOT NULL COMMENT '地址ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_shop_id (shop_id),
    INDEX idx_address_id (address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商店地址关联�?;

SELECT '商家地址表初始化完成' AS message;
-- 物流服务数据�?
CREATE DATABASE IF NOT EXISTS eureka_logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_logistics;

CREATE TABLE IF NOT EXISTS logistics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '物流ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单�?,
    type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY' COMMENT '类型: DELIVERY-发货, RETURN-退�?,
    contact_id INT NOT NULL COMMENT '联系人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    tracking_number VARCHAR(50) NOT NULL COMMENT '快递单�?,
    CONSTRAINT chk_logistics_type CHECK (type IN ('DELIVERY', 'RETURN')),
    INDEX idx_order_type (order_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流�?;

SELECT '物流服务数据库初始化完成' AS message;
-- AI聊天服务数据�?CREATE DATABASE IF NOT EXISTS eureka_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_chat;

CREATE TABLE IF NOT EXISTS chat_session (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id VARCHAR(50) COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '聊天会话�?;

CREATE TABLE IF NOT EXISTS chat_history (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    session_id INT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
    content TEXT NOT NULL COMMENT '聊天内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '聊天历史�?;

SELECT 'AI聊天服务数据库初始化完成' AS message;
-- ============================================
-- AI-Shopping 认证服务数据库初始化脚本
-- 数据库：eureka_auth
-- 表：用户�?t_user) 、商家表(t_merchant)、用户信息表(user_info)、商家信息表(merchant_info)
-- 密码使用 BCrypt 加盐加密存储
-- ============================================

-- ============================================
-- 1. 认证服务数据�?(eureka_auth)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_auth;

-- 用户基础信息�?CREATE TABLE IF NOT EXISTS user_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '信息ID',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息�?;

-- 商家基础信息�?CREATE TABLE IF NOT EXISTS merchant_info (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '信息ID',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家基础信息�?;

-- 消费者用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID（Snowflake�?,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户�?,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码（含Salt），格式: $2a$12$...',
    phone VARCHAR(20) UNIQUE COMMENT '手机�?,
    email VARCHAR(100) COMMENT '邮箱',
    info_id INT COMMENT '用户信息ID（关联user_info表）',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_info_id (info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费者用户表 - 密码使用BCrypt加盐加密';

-- 商家用户�?CREATE TABLE IF NOT EXISTS t_merchant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商家ID（Snowflake�?,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '商家用户�?,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码（含Salt），格式: $2a$12$...',
    phone VARCHAR(20) UNIQUE COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    info_id INT COMMENT '商家信息ID（关联merchant_info表）',
    status TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_info_id (info_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家用户�?- 密码使用BCrypt加盐加密';

-- 外键约束
ALTER TABLE t_user ADD CONSTRAINT fk_user_info_id FOREIGN KEY (info_id) REFERENCES user_info(id) ON DELETE SET NULL;
ALTER TABLE t_merchant ADD CONSTRAINT fk_merchant_info_id FOREIGN KEY (info_id) REFERENCES merchant_info(id) ON DELETE SET NULL;

-- ============================================
-- 完成提示
-- ============================================
SELECT '认证服务数据库初始化完成�? AS message;
-- AI-Shopping 店铺服务数据库初始化脚本
-- 创建 shop-service 所需的数据库和表结构
-- �?auth-service 对齐：ID 统一使用 BIGINT（雪花算法），role 使用 TINYINT

-- ============================================
-- 1. 店铺服务数据�?(eureka_shop)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_shop;

-- 店铺表（运营属性）
CREATE TABLE IF NOT EXISTS shops (
    id BIGINT PRIMARY KEY COMMENT '店铺ID（雪花算法生成）',
    merchant_id BIGINT NOT NULL COMMENT '商户ID（安全算法）',
    shop_info_id BIGINT COMMENT '关联 ShopInfo ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '店铺状态：1-正常 0-已关�?,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (`status`),
    UNIQUE KEY uk_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺�?;

-- 店铺展示信息�?CREATE TABLE IF NOT EXISTS shop_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ShopInfo ID（数据库自增�?,
    name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    description VARCHAR(500) COMMENT '店铺描述',
    logourl VARCHAR(256) COMMENT '店铺Logo URL',
    address VARCHAR(200) COMMENT '店铺地址',
    phone VARCHAR(20) COMMENT '联系电话'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺展示信息�?;

-- ============================================
-- 完成提示
-- ============================================
SELECT '店铺服务数据库初始化完成�? AS message;
