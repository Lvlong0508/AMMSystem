-- 商品服务数据库
CREATE DATABASE IF NOT EXISTS eureka_product CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_product;

CREATE TABLE IF NOT EXISTS product_images (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '图片ID',
    url VARCHAR(500) NOT NULL COMMENT '图片URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY COMMENT '商品ID(雪花算法生成)',
    name VARCHAR(255) NOT NULL COMMENT '商品名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    tags VARCHAR(500) COMMENT '商品标签，逗号分隔',
    description TEXT COMMENT '商品描述',
    stock INT NOT NULL DEFAULT 0 COMMENT '商品库存',
    is_sale TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否在售：1在售，0下架',
    image_id INT COMMENT '图片ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (image_id) REFERENCES product_images(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品预留表';


-- 索引：加速价格区间查询
CREATE INDEX idx_price ON products(price);

-- 索引：加速按在售状态+价格查询（用户端/商家端常见场景）
CREATE INDEX idx_is_sale_price ON products(is_sale, price);

CREATE TABLE IF NOT EXISTS salable_products (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='可售商品表';

SELECT '商品服务数据库初始化完成' AS message;