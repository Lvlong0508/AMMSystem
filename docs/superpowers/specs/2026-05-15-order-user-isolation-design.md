# 订单用户隔离设计

## 背景

订单服务当前没有用户隔离，任何用户可以查看所有订单。需要添加用户隔离机制，确保用户只能查看自己的订单。

## 方案选择

- **方案A（已选）**：新建 `t_user_order` 关联表，不修改 Order 模型，创建/查询都通过关联表来隔离用户数据

## 数据库设计

新建 `t_user_order` 表（订单数据库 eureka_order）：

```sql
CREATE TABLE IF NOT EXISTS t_user_order (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_order (user_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单关联表';
```

## 核心流程

### 1. 创建订单
1. 创建 Order 记录
2. 同时写入 t_user_order（userId + orderId）

### 2. 查询订单列表
1. 根据 userId 从 t_user_order 查询该用户的所有 orderId
2. 批量查询订单详情返回

### 3. 查询单个订单详情
1. 根据 userId + orderId 查询 t_user_order
2. 存在则返回订单详情，不存在则返回"无权限查看"

### 4. 删除订单
1. 删除 t_order 中的订单
2. 删除 t_user_order 中的关联记录

## 需要修改的文件

- SQL: 新建表脚本 `sql/init/02-order-init.sql`
- Model: 新增 UserOrder 实体类
- Mapper: 新增 UserOrderMapper
- Controller: OrderUserController 读取 X-User-Id 头
- Service: OrderServiceImpl 创建/查询/删除时操作关联表

## 前端保持不变

前端已经有获取用户订单列表的接口 `getMyOrders()`，后端修改后直接返回用户自己的订单列表，无需前端改动。