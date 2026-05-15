-- 商品服务数据库
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

SELECT '商品服务数据库初始化完成' AS message;