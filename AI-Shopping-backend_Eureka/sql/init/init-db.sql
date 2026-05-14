-- AI-Shopping Eureka 微服务版本数据库初始化脚本
-- 创建各服务所需的数据库和表结构

-- ============================================
-- 1. 商品服务数据库 (eureka_product)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_product;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY COMMENT '商品ID(雪花算法生成)',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    tags VARCHAR(500) COMMENT '商品标签，逗号分隔',
    description TEXT COMMENT '商品描述',
    stock INT NOT NULL DEFAULT 0 COMMENT '商品库存',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ============================================
-- 2. 订单服务数据库 (eureka_order)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_order;

CREATE TABLE IF NOT EXISTS t_order (
    order_id VARCHAR(20) PRIMARY KEY COMMENT '订单ID',
    product_id VARCHAR(16) NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price DECIMAL(10, 2) NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING待支付/PAID待发货/SHIPPED已发货/DELIVERED已送达/CANCELLED已取消/RETURNED已退货',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    contact_id INT COMMENT '联系人ID',
    logistics_id INT COMMENT '物流信息ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 已删除订单表（用于备份删除的订单）
CREATE TABLE IF NOT EXISTS deleted_orders (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单ID',
    product_id VARCHAR(16) NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price DECIMAL(10, 2) NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20) NOT NULL COMMENT '删除时的订单状态',
    order_date TIMESTAMP NOT NULL COMMENT '原下单时间',
    contact_id INT COMMENT '联系人ID',
    logistics_id INT COMMENT '物流信息ID',
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已删除订单备份表';

-- ============================================
-- 3. 联系人服务数据库 (eureka_contact)
-- ============================================
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

-- ============================================
-- 4. 物流服务数据库 (eureka_logistics)
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_logistics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_logistics;

CREATE TABLE IF NOT EXISTS logistics (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '物流ID',
    contact_id INT NOT NULL COMMENT '联系人ID',
    shipping_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发货时间',
    tracking_number VARCHAR(50) NOT NULL COMMENT '快递单号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流表';

-- ============================================
-- 5. AI聊天服务数据库 (eureka_chat) - 可选
-- ============================================
CREATE DATABASE IF NOT EXISTS eureka_chat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_chat;

CREATE TABLE IF NOT EXISTS chat_session (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id VARCHAR(50) COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '聊天会话表';

CREATE TABLE IF NOT EXISTS chat_history (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    session_id INT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
    content TEXT NOT NULL COMMENT '聊天内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '聊天历史表';

-- ============================================
-- 完成提示
-- ============================================
SELECT '数据库初始化完成！' AS message;
SELECT '请确保MySQL中已创建以上数据库，并配置正确的用户名密码' AS hint;
