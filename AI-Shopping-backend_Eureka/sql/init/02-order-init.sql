-- 订单服务数据库
CREATE DATABASE IF NOT EXISTS eureka_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eureka_order;

CREATE TABLE IF NOT EXISTS t_order (
    order_id     VARCHAR(20) PRIMARY KEY COMMENT '订单ID',
    user_id      BIGINT       NOT NULL COMMENT '用户ID(Snowflake)',
    shop_id      VARCHAR(32)  NOT NULL COMMENT '店铺ID',
    product_id   VARCHAR(64)  NOT NULL COMMENT '商品ID',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price  DECIMAL(10,2)NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '订单状态：PENDING待支付/PAID待发货/SHIPPED已发货/DELIVERED已送达/CANCELLED已取消/RETURNED已退货',
    order_date   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    contact_id   INT          COMMENT '联系人ID',
    INDEX idx_user_id (user_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS deleted_orders (
    id           INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    order_id     VARCHAR(20)  NOT NULL COMMENT '订单ID',
    user_id      BIGINT       COMMENT '用户ID',
    shop_id      VARCHAR(32)  COMMENT '店铺ID',
    product_id   VARCHAR(64)  NOT NULL COMMENT '商品ID',
    quantity     INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_price  DECIMAL(10,2)NOT NULL COMMENT '订单总价',
    order_status VARCHAR(20)  NOT NULL COMMENT '删除时的订单状态',
    order_date   TIMESTAMP    NOT NULL COMMENT '原下单时间',
    contact_id   INT          COMMENT '联系人ID',
    deleted_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已删除订单备份表';

SELECT '订单服务数据库初始化完成' AS message;
