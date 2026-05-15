# 订单用户隔离实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在订单服务中添加用户隔离机制，确保用户只能查看自己的订单

**Architecture:** 通过新建 `t_user_order` 关联表实现用户隔离，创建/查询/删除订单时都需要操作关联表

**Tech Stack:** Java Spring Boot, MyBatis, MySQL

---

## 文件结构

```
order-service/src/main/java/com/gzasc/aishopping/order/
├── model/
│   ├── Order.java (已有)
│   ├── DeletedOrder.java (已有)
│   └── UserOrder.java (新增) - 用户订单关联实体
├── mapper/
│   ├── OrderMapper.java (已有)
│   ├── DeletedOrderMapper.java (已有)
│   └── UserOrderMapper.java (新增) - 关联表 Mapper
├── service/
│   ├── OrderService.java (已有接口)
│   └── impl/OrderServiceImpl.java (修改)
└── controller/
    └── OrderUserController.java (修改)
```

---

## Task 1: 新建关联表 SQL 脚本

**Files:**
- Modify: `AI-Shopping-backend_Eureka/sql/init/02-order-init.sql`

- [ ] **Step 1: 在 02-order-init.sql 中添加新建表语句**

在文件末尾 `SELECT '订单服务数据库初始化完成'` 之前添加：

```sql
-- 用户订单关联表
CREATE TABLE IF NOT EXISTS t_user_order (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    order_id VARCHAR(20) NOT NULL COMMENT '订单ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_order (user_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订单关联表';
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-backend_Eureka/sql/init/02-order-init.sql
git commit -m "feat(order): 添加用户订单关联表 t_user_order"
```

---

## Task 2: 创建 UserOrder 实体类

**Files:**
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/model/UserOrder.java`

- [ ] **Step 1: 创建 UserOrder.java**

```java
package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class UserOrder {
    private Integer id;
    private Integer userId;
    private String orderId;
    private Timestamp createdAt;
}
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/model/UserOrder.java
git commit -m "feat(order): 添加 UserOrder 实体类"
```

---

## Task 3: 创建 UserOrderMapper

**Files:**
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/mapper/UserOrderMapper.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/resources/mapper/UserOrderMapper.xml`

- [ ] **Step 1: 创建 UserOrderMapper.java 接口**

```java
package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.UserOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserOrderMapper {
    int insert(UserOrder userOrder);

    int deleteByOrderId(@Param("orderId") String orderId);

    List<String> selectOrderIdsByUserId(@Param("userId") Integer userId);

    UserOrder selectByUserIdAndOrderId(@Param("userId") Integer userId, @Param("orderId") String orderId);

    List<UserOrder> selectByUserId(@Param("userId") Integer userId);
}
```

- [ ] **Step 2: 创建 UserOrderMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.gzasc.aishopping.order.mapper.UserOrderMapper">

    <insert id="insert" parameterType="com.gzasc.aishopping.order.model.UserOrder" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO t_user_order (user_id, order_id)
        VALUES (#{userId}, #{orderId})
    </insert>

    <delete id="deleteByOrderId">
        DELETE FROM t_user_order WHERE order_id = #{orderId}
    </delete>

    <select id="selectOrderIdsByUserId" resultType="java.lang.String">
        SELECT order_id FROM t_user_order WHERE user_id = #{userId}
    </select>

    <select id="selectByUserIdAndOrderId" resultType="com.gzasc.aishopping.order.model.UserOrder">
        SELECT * FROM t_user_order WHERE user_id = #{userId} AND order_id = #{orderId}
    </select>

    <select id="selectByUserId" resultType="com.gzasc.aishopping.order.model.UserOrder">
        SELECT * FROM t_user_order WHERE user_id = #{userId}
    </select>

</mapper>
```

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/mapper/UserOrderMapper.java
git add AI-Shopping-backend_Eureka/order-service/src/main/resources/mapper/UserOrderMapper.xml
git commit -m "feat(order): 添加 UserOrderMapper"
```

---

## Task 4: 修改 OrderService 接口添加新方法

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java`

- [ ] **Step 1: 添加新方法声明**

在 OrderService 接口中添加：

```java
int createUserOrder(Integer userId, String orderId);
List<Order> getOrdersByUserId(Integer userId);
Order getOrderByUserId(Integer userId, String orderId);
int deleteUserOrder(String orderId);
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java
git commit -m "feat(order): OrderService 添加用户隔离方法"
```

---

## Task 5: 修改 OrderServiceImpl 实现用户隔离

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: 添加 UserOrderMapper 依赖注入**

在类中添加：
```java
private final UserOrderMapper userOrderMapper;
```

- [ ] **Step 2: 实现 createUserOrder 方法**

```java
@Override
public int createUserOrder(Integer userId, String orderId) {
    System.out.println(new Date() + ": run createUserOrder, userId=" + userId + ", orderId=" + orderId);
    try {
        UserOrder userOrder = new UserOrder();
        userOrder.setUserId(userId);
        userOrder.setOrderId(orderId);
        return userOrderMapper.insert(userOrder);
    } catch (Exception e) {
        e.printStackTrace();
        return -1;
    }
}
```

- [ ] **Step 3: 实现 getOrdersByUserId 方法**

```java
@Override
public List<Order> getOrdersByUserId(Integer userId) {
    System.out.println(new Date() + ": run getOrdersByUserId, userId=" + userId);
    List<String> orderIds = userOrderMapper.selectOrderIdsByUserId(userId);
    if (orderIds == null || orderIds.isEmpty()) {
        return List.of();
    }
    return orderMapper.selectOrdersByIds(orderIds);
}
```

- [ ] **Step 4: 实现 getOrderByUserId 方法**

```java
@Override
public Order getOrderByUserId(Integer userId, String orderId) {
    System.out.println(new Date() + ": run getOrderByUserId, userId=" + userId + ", orderId=" + orderId);
    UserOrder userOrder = userOrderMapper.selectByUserIdAndOrderId(userId, orderId);
    if (userOrder == null) {
        return null;
    }
    return orderMapper.selectOrderById(orderId);
}
```

- [ ] **Step 5: 实现 deleteUserOrder 方法**

```java
@Override
public int deleteUserOrder(String orderId) {
    System.out.println(new Date() + ": run deleteUserOrder, orderId=" + orderId);
    return userOrderMapper.deleteByOrderId(orderId);
}
```

- [ ] **Step 6: 修改 deleteOrder 方法，添加删除关联记录**

在 deleteOrder 方法中，删除订单后添加：
```java
// 删除关联表记录
deletedOrderMapper.deleteByOrderId(orderId);
```

- [ ] **Step 7: 提交**

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java
git commit -m "feat(order): OrderServiceImpl 实现用户隔离逻辑"
```

---

## Task 6: 修改 OrderUserController 添加用户隔离

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java`

- [ ] **Step 1: 添加获取 userId 的方法**

添加私有方法：
```java
private Integer parseUserId(String userIdStr) {
    if (userIdStr == null || userIdStr.trim().isEmpty()) {
        return null;
    }
    try {
        return Integer.parseInt(userIdStr);
    } catch (NumberFormatException e) {
        return null;
    }
}
```

- [ ] **Step 2: 修改 getOrderById 方法，添加用户验证**

将：
```java
@GetMapping("/{orderId}")
public Map<String, Object> getOrderById(@PathVariable("orderId") String orderId) {
    try {
        Order order = orderService.getOrderById(orderId);
        if (order != null) {
            return Map.of("message", "查询成功", "order", order);
        } else {
            return Map.of("message", "查询失败：订单不存在");
        }
    } catch (Exception e) {
        return Map.of("message", "查询订单错误：" + e.getMessage());
    }
}
```

改为：
```java
@GetMapping("/{orderId}")
public Map<String, Object> getOrderById(
        @PathVariable("orderId") String orderId,
        @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
    Integer userId = parseUserId(userIdStr);
    if (userId == null) {
        return Map.of("message", "查询订单错误：未登录（错误代码：O-006）");
    }
    try {
        Order order = orderService.getOrderByUserId(userId, orderId);
        if (order != null) {
            return Map.of("message", "查询成功", "order", order);
        } else {
            return Map.of("message", "查询失败：订单不存在或无权限查看");
        }
    } catch (Exception e) {
        return Map.of("message", "查询订单错误：" + e.getMessage());
    }
}
```

- [ ] **Step 3: 修改 getUserOrders 方法，获取用户自己的订单**

将：
```java
@GetMapping("/list")
public Map<String, Object> getUserOrders() {
    try {
        List<Order> orders = orderService.getAllOrders();
        return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
    } catch (Exception e) {
        return Map.of("message", "查询订单错误：" + e.getMessage());
    }
}
```

改为：
```java
@GetMapping("/list")
public Map<String, Object> getUserOrders(
        @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
    Integer userId = parseUserId(userIdStr);
    if (userId == null) {
        return Map.of("message", "查询订单错误：未登录（错误代码：O-007）");
    }
    try {
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return Map.of("message", "查询成功", "orders", orders, "total", orders.size());
    } catch (Exception e) {
        return Map.of("message", "查询订单错误：" + e.getMessage());
    }
}
```

- [ ] **Step 4: 修改 placeOrder 方法，创建订单时同时创建关联**

在 orderService.createOrder(order) 后添加：
```java
// 创建用户订单关联
orderService.createUserOrder(userId, orderId);
```

- [ ] **Step 5: 修改 cancelOrder 方法，删除订单时同时删除关联**

将 `orderService.deleteOrder(orderId)` 调用后添加：
```java
// 删除用户订单关联
orderService.deleteUserOrder(orderId);
```

- [ ] **Step 6: 提交**

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java
git commit -m "feat(order): OrderUserController 添加用户隔离"
```

---

## Task 7: 添加 DeletedOrderMapper 删除关联方法（可选）

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/mapper/DeletedOrderMapper.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/resources/mapper/DeletedOrderMapper.xml`

如果需要保留删除订单的备份，需要在 DeletedOrderMapper 中添加删除关联记录的方法。

---

## 验证步骤

1. 执行 SQL 脚本创建 `t_user_order` 表
2. 启动 order-service
3. 使用不同用户登录，测试：
   - 获取自己的订单列表
   - 通过订单ID查询订单（验证只能查看自己的订单）
   - 创建新订单
   - 删除订单
4. 验证用户隔离是否正常工作