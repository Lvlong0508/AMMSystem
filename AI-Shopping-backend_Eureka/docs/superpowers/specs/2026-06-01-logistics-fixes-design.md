# Logistics 服务两处 Bug 修复设计

## 概述

修复 logistics-service 在 API 集成测试中发现的两个源码问题：
1. 创建物流时响应中 `createdAt` 始终为 `null`
2. `orderId` 字段缺少长度校验，超长时返回 500

## Fix 1: createdAt 响应缺失

### 问题

`LogisticsMapper.insertLogistics()` 使用 `@Options(useGeneratedKeys = true)` 仅回写自增主键 `id`，
未回读 DB 默认生成的 `created_at` 值，导致 Create 响应中 `createdAt = null`。

### 方案

采用方案二：MyBatis `@SelectKey`，insert 后自动查询 `created_at` 并回填到 `Logistics` 对象。

### 变更文件

`logistics-service/src/main/java/.../mapper/LogisticsMapper.java`

在 `insertLogistics` 方法上追加：

```java
@SelectKey(statement = "SELECT created_at FROM logistics WHERE id = #{id}",
           keyProperty = "createdAt",
           before = false,
           resultType = Timestamp.class)
```

`before = false` 表示 insert **之后**执行查询，将结果注入 `Logistics.createdAt` 字段。

## Fix 2: order_id 字段长度校验

### 问题

数据库 `logistics.order_id` 为 `VARCHAR(20)`，但 API 入口无长度校验。
传超过 20 字符的 orderId 时，MySQL 抛出 DataTruncation，全局异常处理器将其转为 500。

### 分析

orderId 由 `RedisOrderIdGenerator` 生成，格式为 `yyyyMMdd`（8位）+ `5位INCR序列` + `5位随机大写字母` = **18 位**。
`VARCHAR(20)` 完全足够。问题仅在于 API 直接调用时缺少防御性校验。

### 方案

在 `CreateLogisticsRequest.orderId` 上增加 `@Size(max = 20)` 注解。

### 变更文件

`logistics-service/src/main/java/.../dto/CreateLogisticsRequest.java`

```java
@NotBlank(message = "订单号不能为空")
@Size(max = 20, message = "订单号长度不能超过20个字符")
private String orderId;
```

## 验证

1. 创建物流后响应中 `createdAt` 不再为 `null`，返回正确的时间戳
2. 传入超过 20 字符的 orderId 时返回 400 + "订单号长度不能超过20个字符"，而非 500
