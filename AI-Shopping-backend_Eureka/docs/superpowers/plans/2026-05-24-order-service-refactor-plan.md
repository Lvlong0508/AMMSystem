# Order Service 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 order-service，将 userId/shopId 直接写入 t_order，消除 t_user_order 和 order_shops 关联表，实现双端分类 DTO + 统一异常处理

**Architecture:** 采用 product-service 的 Abstract/Detail 双查询模式 + auth-service 的 Converter/Exception 模式，下单时直接在 t_order 写入 userId+shopId，所有查询必须带身份过滤

**Tech Stack:** Spring Boot, MyBatis, MySQL, Feign, Redis, Lombok

**Base path:** `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka`

---

### Task 1: 数据库 SQL — 修改 02-order-init.sql

**Files:**
- Modify: `sql/init/02-order-init.sql`

- [ ] **Step 1: 重写 02-order-init.sql**

```sql
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
```

---

### Task 2: Model 层 — Order.java + DeletedOrder.java, 删除 UserOrder.java

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/model/Order.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/model/DeletedOrder.java`
- Delete: `order-service/src/main/java/com/gzasc/aishopping/order/model/UserOrder.java`

- [ ] **Step 1: Order.java — 新增 userId, shopId，更新 buildInitOrder**

```java
package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Order {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String CANCELLED = "CANCELLED";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String RETURNED = "RETURNED";

    private String orderId;
    private Long userId;
    private String shopId;
    private String productId;
    private int quantity;
    private double totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;

    private boolean canTransition(String fromStatus, String toStatus) {
        if (fromStatus == null) return true;
        switch (fromStatus) {
            case PENDING: return toStatus.equals(PAID);
            case PAID: return toStatus.equals(SHIPPED) || toStatus.equals(CANCELLED);
            case SHIPPED: return toStatus.equals(DELIVERED) || toStatus.equals(RETURNED);
            case DELIVERED: return toStatus.equals(RETURNED);
            default: return false;
        }
    }

    public Order buildInitOrder(String orderId, Long userId, String shopId,
                                String productId, int quantity, double totalPrice) {
        Order order = new Order();
        order.orderId = orderId;
        order.userId = userId;
        order.shopId = shopId;
        order.productId = productId;
        order.quantity = quantity;
        order.totalPrice = totalPrice;
        order.orderStatus = PENDING;
        order.orderDate = new Timestamp(System.currentTimeMillis());
        System.out.println("订单创建成功时间: " + order.orderDate);
        return order;
    }

    public Order payOrder(Order order) {
        if (!canTransition(order.orderStatus, PAID)) {
            throw new IllegalStateException("订单状态不允许支付操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = PAID;
        return order;
    }

    public Order shipOrder(Order order) {
        if (!canTransition(order.orderStatus, SHIPPED)) {
            throw new IllegalStateException("订单状态不允许发货操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = SHIPPED;
        return order;
    }

    public Order deliverOrder(Order order) {
        if (!canTransition(order.orderStatus, DELIVERED)) {
            throw new IllegalStateException("订单状态不允许送达操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = DELIVERED;
        return order;
    }

    public Order cancelOrder(Order order) {
        if (!canTransition(order.orderStatus, CANCELLED)) {
            throw new IllegalStateException("订单状态不允许取消操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = CANCELLED;
        return order;
    }

    public Order returnOrder(Order order) {
        if (!canTransition(order.orderStatus, RETURNED)) {
            throw new IllegalStateException("订单状态不允许退货操作，当前状态: " + order.orderStatus);
        }
        order.orderStatus = RETURNED;
        return order;
    }
}
```

- [ ] **Step 2: DeletedOrder.java — 新增 userId, shopId，更新 fromOrder**

```java
package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class DeletedOrder {
    private Integer id;
    private String orderId;
    private Long userId;
    private String shopId;
    private String productId;
    private int quantity;
    private double totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;
    private Timestamp deletedAt;

    public static DeletedOrder fromOrder(Order order) {
        DeletedOrder deletedOrder = new DeletedOrder();
        deletedOrder.setOrderId(order.getOrderId());
        deletedOrder.setUserId(order.getUserId());
        deletedOrder.setShopId(order.getShopId());
        deletedOrder.setProductId(order.getProductId());
        deletedOrder.setQuantity(order.getQuantity());
        deletedOrder.setTotalPrice(order.getTotalPrice());
        deletedOrder.setOrderStatus(order.getOrderStatus());
        deletedOrder.setOrderDate(order.getOrderDate());
        deletedOrder.setContactId(order.getContactId());
        deletedOrder.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return deletedOrder;
    }
}
```

- [ ] **Step 3: 删除 UserOrder.java**

删除 `order-service/src/main/java/com/gzasc/aishopping/order/model/UserOrder.java`

---

### Task 3: DTO 层 — 新建 3 个 DTO 类

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/dto/OrderAbstractUserDTO.java`
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/dto/OrderAbstractSellerDTO.java`
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/dto/OrderDetailDTO.java`

- [ ] **Step 1: 创建 OrderAbstractUserDTO.java**

```java
package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderAbstractUserDTO {
    private String orderId;
    private String productId;
    private String shopId;
    private BigDecimal totalPrice;
    private int quantity;
    private String orderStatus;
}
```

- [ ] **Step 2: 创建 OrderAbstractSellerDTO.java**

```java
package com.gzasc.aishopping.order.dto;

import lombok.Data;

@Data
public class OrderAbstractSellerDTO {
    private String orderId;
    private String productId;
    private Integer contactId;
    private int quantity;
    private String orderStatus;
}
```

- [ ] **Step 3: 更新 PlaceOrderRequest.java（加验证注解）**

```java
package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlaceOrderRequest {
    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private int quantity;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;
}
```

- [ ] **Step 4: 更新 ShipOrderRequest.java（加验证注解）**

```java
package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipOrderRequest {
    @NotBlank(message = "物流单号不能为空")
    private String trackingNumber;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;

    private String shippingDate;
}
```

- [ ] **Step 5: 创建 OrderDetailDTO.java**

```java
package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class OrderDetailDTO {
    private String orderId;
    private Long userId;
    private String shopId;
    private String productId;
    private int quantity;
    private BigDecimal totalPrice;
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
    private String trackingNumber;
}
```

---

### Task 4: Converter 层 — OrderConverter

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/converter/OrderConverter.java`

- [ ] **Step 1: 创建 OrderConverter.java**

```java
package com.gzasc.aishopping.order.converter;

import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderConverter {

    public OrderAbstractUserDTO toUserAbstractDTO(Order order) {
        OrderAbstractUserDTO dto = new OrderAbstractUserDTO();
        dto.setOrderId(order.getOrderId());
        dto.setProductId(order.getProductId());
        dto.setShopId(order.getShopId());
        dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        return dto;
    }

    public List<OrderAbstractUserDTO> toUserAbstractDTOList(List<Order> orders) {
        return orders.stream().map(this::toUserAbstractDTO).collect(Collectors.toList());
    }

    public OrderAbstractSellerDTO toSellerAbstractDTO(Order order) {
        OrderAbstractSellerDTO dto = new OrderAbstractSellerDTO();
        dto.setOrderId(order.getOrderId());
        dto.setProductId(order.getProductId());
        dto.setContactId(order.getContactId());
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        return dto;
    }

    public List<OrderAbstractSellerDTO> toSellerAbstractDTOList(List<Order> orders) {
        return orders.stream().map(this::toSellerAbstractDTO).collect(Collectors.toList());
    }

    public OrderDetailDTO toDetailDTO(Order order) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setOrderId(order.getOrderId());
        dto.setUserId(order.getUserId());
        dto.setShopId(order.getShopId());
        dto.setProductId(order.getProductId());
        dto.setQuantity(order.getQuantity());
        dto.setTotalPrice(BigDecimal.valueOf(order.getTotalPrice()));
        dto.setOrderStatus(order.getOrderStatus());
        dto.setOrderDate(order.getOrderDate());
        dto.setContactId(order.getContactId());
        return dto;
    }

    public OrderDetailDTO enrichDetailDTO(OrderDetailDTO dto, Map<String, Object> contactInfo,
                                          Map<String, Object> logisticsInfo) {
        if (contactInfo != null) {
            dto.setContactName((String) contactInfo.get("name"));
            dto.setContactPhone((String) contactInfo.get("phone"));
            dto.setContactAddress((String) contactInfo.get("address"));
        }
        if (logisticsInfo != null && logisticsInfo.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) logisticsInfo.get("data");
            dto.setTrackingNumber((String) data.get("trackingNumber"));
        }
        return dto;
    }
}
```

---

### Task 5: Mapper 层 — OrderMapper 重写, 删除 UserOrderMapper

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/mapper/OrderMapper.java`
- Delete: `order-service/src/main/java/com/gzasc/aishopping/order/mapper/UserOrderMapper.java`

- [ ] **Step 1: 重写 OrderMapper.java**

```java
package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO t_order (order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date, contact_id) " +
            "VALUES (#{orderId}, #{userId}, #{shopId}, #{productId}, #{quantity}, #{totalPrice}, #{orderStatus}, #{orderDate}, #{contactId})")
    int insertOrder(Order order);

    @Delete("DELETE FROM t_order WHERE order_id = #{orderId}")
    int deleteOrderById(@Param("orderId") String orderId);

    @Update("UPDATE t_order SET order_status = #{status} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Select("SELECT * FROM t_order WHERE order_id = #{orderId}")
    Order selectOrderById(@Param("orderId") String orderId);

    @Select("SELECT * FROM t_order WHERE order_id IN (${orderIds})")
    List<Order> selectOrdersByIds(@Param("orderIds") List<String> orderIds);

    // 用户端抽象查询（列表）
    @Select("SELECT order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date FROM t_order WHERE user_id = #{userId}")
    List<Order> selectAbstractOrdersByUserId(@Param("userId") Long userId);

    @Select("SELECT order_id, user_id, shop_id, product_id, quantity, total_price, order_status, order_date FROM t_order WHERE user_id = #{userId} AND order_status = #{status}")
    List<Order> selectAbstractOrdersByUserAndStatus(@Param("userId") Long userId, @Param("status") String status);

    // 用户端详情查询
    @Select("SELECT * FROM t_order WHERE user_id = #{userId} AND order_id = #{orderId}")
    Order selectOrderDetailByUser(@Param("userId") Long userId, @Param("orderId") String orderId);

    // 商家端抽象查询（列表）
    @Select("SELECT order_id, shop_id, product_id, contact_id, quantity, order_status, order_date FROM t_order WHERE shop_id = #{shopId}")
    List<Order> selectAbstractOrdersByShopId(@Param("shopId") String shopId);

    @Select("SELECT order_id, shop_id, product_id, contact_id, quantity, order_status, order_date FROM t_order WHERE shop_id = #{shopId} AND order_status = #{status}")
    List<Order> selectAbstractOrdersByShopAndStatus(@Param("shopId") String shopId, @Param("status") String status);

    // 商家端详情查询
    @Select("SELECT * FROM t_order WHERE shop_id = #{shopId} AND order_id = #{orderId}")
    Order selectOrderDetailByShop(@Param("shopId") String shopId, @Param("orderId") String orderId);
}
```

- [ ] **Step 2: 删除 UserOrderMapper.java**

删除 `order-service/src/main/java/com/gzasc/aishopping/order/mapper/UserOrderMapper.java`

---

### Task 6: 异常处理 — OrderException + GlobalExceptionHandler

**Files:**
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/exception/OrderException.java`
- Create: `order-service/src/main/java/com/gzasc/aishopping/order/controller/GlobalExceptionHandler.java`
- Note: 需要确保 `common-api` 的 `ApiResponse` 在 classpath

- [ ] **Step 1: 创建 OrderException.java**

```java
package com.gzasc.aishopping.order.exception;

public class OrderException extends RuntimeException {
    private int code = 400;

    public OrderException(String message) {
        super(message);
    }

    public OrderException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

- [ ] **Step 2: 创建 GlobalExceptionHandler.java**

```java
package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.exception.OrderException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleOrderException(OrderException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
    }
}
```

---

### Task 7: Service 接口 — OrderService 精简

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java`

- [ ] **Step 1: 重写 OrderService.java**

```java
package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.model.Order;

import java.util.List;

public interface OrderService {
    String createOrder(PlaceOrderRequest request, Long userId);
    int deleteOrder(Long userId, String orderId);
    int updateOrderStatus(String orderId, String status);
    String generateOrderId();
    void shipOrder(String orderId, ShipOrderRequest request);

    List<OrderAbstractUserDTO> getOrdersByUserId(Long userId);
    List<OrderAbstractUserDTO> getOrdersByUserIdAndStatus(Long userId, String status);
    OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);

    List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId);
    List<OrderAbstractSellerDTO> getOrdersByShopIdAndStatus(String shopId, String status);
    OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);

    List<Order> getOrdersByIds(List<String> orderIds);
}
```

---

### Task 8: Service 实现 — OrderServiceImpl 重写

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: 重写 OrderServiceImpl.java**

```java
package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.common.feign.contact.ContactFeignClient;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.feign.shop.ShopFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.converter.OrderConverter;
import com.gzasc.aishopping.order.dto.*;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.mapper.DeletedOrderMapper;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.model.DeletedOrder;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final DeletedOrderMapper deletedOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final ProductFeignClient productFeignClient;
    private final ShopFeignClient shopFeignClient;
    private final LogisticsFeignClient logisticsFeignClient;
    private final ContactFeignClient contactFeignClient;
    private final OrderConverter orderConverter;

    @Override
    @Transactional
    public String createOrder(PlaceOrderRequest request, Long userId) {
        Map<String, Object> productMap = productFeignClient.getProductById(request.getProductId());
        if (productMap == null) {
            throw new OrderException("商品不存在（错误代码：O-003）");
        }

        double price = productMap.get("price") != null
                ? ((Number) productMap.get("price")).doubleValue() : 0.0;
        int stock = productMap.get("stock") != null
                ? ((Number) productMap.get("stock")).intValue() : 0;

        if (stock < request.getQuantity()) {
            throw new OrderException("商品库存不足，当前库存：" + stock + "（错误代码：O-005）");
        }

        Map<String, Object> shopResult = shopFeignClient.getShopIdByProductId(request.getProductId());
        if (shopResult == null || !Boolean.TRUE.equals(shopResult.get("success"))) {
            throw new OrderException("获取店铺信息失败");
        }
        String shopId = String.valueOf(shopResult.get("shopId"));

        String orderId = generateOrderId();
        Order order = new Order();
        order = order.buildInitOrder(orderId, userId, shopId, request.getProductId(),
                request.getQuantity(), price * request.getQuantity());
        order.setContactId(request.getContactId());

        int result = orderMapper.insertOrder(order);
        if (result <= 0) {
            throw new OrderException("创建订单失败");
        }

        return orderId;
    }

    @Override
    @Transactional
    public int deleteOrder(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限删除");
        }

        if (Order.PENDING.equals(order.getOrderStatus()) || Order.PAID.equals(order.getOrderStatus())) {
            com.gzasc.aishopping.common.dto.product.StockDeductRequest stockReq =
                    new com.gzasc.aishopping.common.dto.product.StockDeductRequest(
                            order.getProductId(), order.getQuantity());
            productFeignClient.restoreStock(stockReq);
        }

        DeletedOrder deletedOrder = DeletedOrder.fromOrder(order);
        int backupResult = deletedOrderMapper.insertDeletedOrder(deletedOrder);
        if (backupResult <= 0) {
            throw new OrderException("备份订单失败");
        }

        return orderMapper.deleteOrderById(orderId);
    }

    @Override
    public int updateOrderStatus(String orderId, String status) {
        return orderMapper.updateOrderStatus(orderId, status);
    }

    @Override
    public String generateOrderId() {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "order:seq:" + currentDate;
        Long sequence = redisTemplate.opsForValue().increment(key);
        if (sequence != null && sequence == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        String seqStr = String.format("%05d", sequence);
        String randomChars = generateRandomLetters();
        return currentDate + seqStr + randomChars;
    }

    @Override
    @Transactional
    public void shipOrder(String orderId, ShipOrderRequest request) {
        Order order = orderMapper.selectOrderById(orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        if (!Order.PAID.equals(order.getOrderStatus())) {
            throw new OrderException("只有已支付订单才能发货，当前状态：" + order.getOrderStatus());
        }

        com.gzasc.aishopping.common.dto.logistics.LogisticsRequest logisticsRequest =
                new com.gzasc.aishopping.common.dto.logistics.LogisticsRequest();
        logisticsRequest.setOrderId(orderId);
        logisticsRequest.setType("DELIVERY");
        logisticsRequest.setContactId(request.getContactId());
        logisticsRequest.setTrackingNumber(request.getTrackingNumber());

        ApiResponse<Map<String, Object>> logisticsResponse =
                logisticsFeignClient.createLogistics(logisticsRequest);
        if (logisticsResponse == null || logisticsResponse.getData() == null) {
            throw new OrderException("创建物流记录失败");
        }

        int result = orderMapper.updateOrderStatus(orderId, Order.SHIPPED);
        if (result <= 0) {
            throw new OrderException("更新订单状态失败");
        }
    }

    @Override
    public List<OrderAbstractUserDTO> getOrdersByUserId(Long userId) {
        List<Order> orders = orderMapper.selectAbstractOrdersByUserId(userId);
        return orderConverter.toUserAbstractDTOList(orders);
    }

    @Override
    public List<OrderAbstractUserDTO> getOrdersByUserIdAndStatus(Long userId, String status) {
        List<Order> orders = orderMapper.selectAbstractOrdersByUserAndStatus(userId, status);
        return orderConverter.toUserAbstractDTOList(orders);
    }

    @Override
    public OrderDetailDTO getOrderDetailByUser(Long userId, String orderId) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限查看");
        }
        return buildDetailDTO(order);
    }

    @Override
    public List<OrderAbstractSellerDTO> getOrdersByShopId(String shopId) {
        List<Order> orders = orderMapper.selectAbstractOrdersByShopId(shopId);
        return orderConverter.toSellerAbstractDTOList(orders);
    }

    @Override
    public List<OrderAbstractSellerDTO> getOrdersByShopIdAndStatus(String shopId, String status) {
        List<Order> orders = orderMapper.selectAbstractOrdersByShopAndStatus(shopId, status);
        return orderConverter.toSellerAbstractDTOList(orders);
    }

    @Override
    public OrderDetailDTO getOrderDetailByShop(String shopId, String orderId) {
        Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限查看");
        }
        return buildDetailDTO(order);
    }

    @Override
    public List<Order> getOrdersByIds(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return orderMapper.selectOrdersByIds(orderIds);
    }

    private OrderDetailDTO buildDetailDTO(Order order) {
        OrderDetailDTO dto = orderConverter.toDetailDTO(order);

        Map<String, Object> contactInfo = null;
        try {
            if (order.getContactId() != null) {
                contactInfo = contactFeignClient.getContactById(order.getContactId());
            }
        } catch (Exception e) {
            System.err.println("获取联系人信息失败: " + e.getMessage());
        }

        Map<String, Object> logisticsInfo = null;
        try {
            ApiResponse<Map<String, Object>> response =
                    logisticsFeignClient.getLatestLogistics(order.getOrderId(), "DELIVERY");
            if (response != null) {
                logisticsInfo = Map.of("data", response.getData());
            }
        } catch (Exception e) {
            System.err.println("获取物流信息失败: " + e.getMessage());
        }

        orderConverter.enrichDetailDTO(dto, contactInfo, logisticsInfo);
        return dto;
    }

    private String generateRandomLetters() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            char c = (char) ('A' + random.nextInt(26));
            sb.append(c);
        }
        return sb.toString();
    }
}
```

---

### Task 9: Controller — OrderUserController 重写

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java`

- [ ] **Step 1: 重写 OrderUserController.java**

```java
package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderAbstractUserDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.PlaceOrderRequest;
import com.gzasc.aishopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderService orderService;

    @GetMapping("/list")
    public ApiResponse<List<OrderAbstractUserDTO>> listOrders(
            @RequestHeader("X-User-Id") Long userId) {
        List<OrderAbstractUserDTO> orders = orderService.getOrdersByUserId(userId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/list/status")
    public ApiResponse<List<OrderAbstractUserDTO>> listOrdersByStatus(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("status") String status) {
        List<OrderAbstractUserDTO> orders = orderService.getOrdersByUserIdAndStatus(userId, status);
        return ApiResponse.success(orders);
    }

    @GetMapping("/{orderId}/detail")
    public ApiResponse<OrderDetailDTO> getOrderDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByUser(userId, orderId);
        return ApiResponse.success(detail);
    }

    @PostMapping("/place")
    public ApiResponse<String> placeOrder(
            @RequestBody @Valid PlaceOrderRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        if (request.getProductId() == null) {
            return ApiResponse.error(400, "商品信息为空（错误代码：O-001）");
        }
        if (request.getContactId() == null) {
            return ApiResponse.error(400, "收货人信息为空（错误代码：O-002）");
        }
        if (request.getQuantity() <= 0) {
            return ApiResponse.error(400, "购买数量必须大于0（错误代码：O-004）");
        }
        String orderId = orderService.createOrder(request, userId);
        return ApiResponse.success("创建订单成功", orderId);
    }

    @DeleteMapping("/{orderId}")
    public ApiResponse<Void> cancelOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId) {
        orderService.deleteOrder(userId, orderId);
        return ApiResponse.success("取消订单成功", null);
    }
}
```

---

### Task 10: Controller — OrderSellerController 重写

**Files:**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java`

- [ ] **Step 1: 重写 OrderSellerController.java**

```java
package com.gzasc.aishopping.order.controller;

import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.ShipOrderRequest;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;
    private final ProductFeignClient productFeignClient;

    @GetMapping("/shop/{shopId}/list")
    public ApiResponse<List<OrderAbstractSellerDTO>> listShopOrders(
            @PathVariable("shopId") String shopId) {
        List<OrderAbstractSellerDTO> orders = orderService.getOrdersByShopId(shopId);
        return ApiResponse.success(orders);
    }

    @GetMapping("/shop/{shopId}/list/status")
    public ApiResponse<List<OrderAbstractSellerDTO>> listShopOrdersByStatus(
            @PathVariable("shopId") String shopId,
            @RequestParam("status") String status) {
        List<OrderAbstractSellerDTO> orders = orderService.getOrdersByShopIdAndStatus(shopId, status);
        return ApiResponse.success(orders);
    }

    @GetMapping("/shop/{shopId}/{orderId}")
    public ApiResponse<OrderDetailDTO> getShopOrderDetail(
            @PathVariable("shopId") String shopId,
            @PathVariable("orderId") String orderId) {
        OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);
        return ApiResponse.success(detail);
    }

    @PutMapping("/{orderId}/status")
    public ApiResponse<Void> updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestParam("status") String status,
            @RequestParam("shopId") String shopId) {
        OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);

        if (Order.PENDING.equals(detail.getOrderStatus()) && Order.PAID.equals(status)) {
            Map<String, Object> result = productFeignClient.deductStock(
                    new StockDeductRequest(detail.getProductId(), detail.getQuantity()));
            Boolean success = (Boolean) result.get("success");
            if (!Boolean.TRUE.equals(success)) {
                return ApiResponse.error(400, "更新订单状态失败：商品库存不足");
            }
        }

        if (Order.CANCELLED.equals(status) &&
                (Order.PENDING.equals(detail.getOrderStatus()) || Order.PAID.equals(detail.getOrderStatus()))) {
            productFeignClient.restoreStock(
                    new StockDeductRequest(detail.getProductId(), detail.getQuantity()));
        }

        orderService.updateOrderStatus(orderId, status);
        return ApiResponse.success("更新订单状态成功", null);
    }

    @PutMapping("/{orderId}/ship")
    public ApiResponse<Void> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid ShipOrderRequest request,
            @RequestParam("shopId") String shopId) {
        // 验证订单属于该店铺
        orderService.getOrderDetailByShop(shopId, orderId);

        orderService.shipOrder(orderId, request);
        return ApiResponse.success("发货成功", null);
    }
}
```

---

### Task 11: Controller — InternalOrderController + Feign 客户端更新

> 注意：`OrderFeignClient` 在 common-api 中（共享模块），无法引用 order-service 的 DTO。
> 解决方案：将商家端 DTO 加入 common-api 的 `dto/order/` 包（已有 `OrderDTO` 做先例），
> 内部端点直接返回 `List<OrderAbstractSellerDTO>`（不走 ApiResponse 包装）。

**Files:**
- Create: `common-api/src/main/java/com/gzasc/aishopping/common/dto/order/OrderAbstractSellerDTO.java`
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/controller/internal/InternalOrderController.java`
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/feign/order/OrderFeignClient.java`

- [ ] **Step 1: 在 common-api 新建 OrderAbstractSellerDTO.java**

```java
package com.gzasc.aishopping.common.dto.order;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderAbstractSellerDTO implements Serializable {
    private String orderId;
    private String productId;
    private Integer contactId;
    private int quantity;
    private String orderStatus;
}
```

- [ ] **Step 2: 重写 InternalOrderController.java**

```java
package com.gzasc.aishopping.order.controller.internal;

import com.gzasc.aishopping.common.dto.order.OrderAbstractSellerDTO;
import com.gzasc.aishopping.order.converter.OrderConverter;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/internal/order")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;
    private final OrderConverter orderConverter;

    @GetMapping("/batch")
    public List<Order> getOrdersByIds(@RequestParam("orderIds") String orderIds) {
        List<String> idList = Arrays.asList(orderIds.split(","));
        return orderService.getOrdersByIds(idList);
    }

    @GetMapping("/shop/{shopId}")
    public List<OrderAbstractSellerDTO> getOrdersByShopId(@PathVariable("shopId") String shopId) {
        List<com.gzasc.aishopping.order.dto.OrderAbstractSellerDTO> dtos =
                orderService.getOrdersByShopId(shopId);
        // 将 order-service 的 DTO 转为 common-api 的 DTO
        return dtos.stream().map(dto -> {
            OrderAbstractSellerDTO result = new OrderAbstractSellerDTO();
            result.setOrderId(dto.getOrderId());
            result.setProductId(dto.getProductId());
            result.setContactId(dto.getContactId());
            result.setQuantity(dto.getQuantity());
            result.setOrderStatus(dto.getOrderStatus());
            return result;
        }).toList();
    }
}
```

- [ ] **Step 3: 更新 OrderFeignClient.java （清理无效方法 + 新增内部端点）**

```java
package com.gzasc.aishopping.common.feign.order;

import com.gzasc.aishopping.common.dto.order.OrderAbstractSellerDTO;
import com.gzasc.aishopping.common.dto.order.OrderDTO;
import com.gzasc.aishopping.common.dto.order.ShipOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/internal/order/shop/{shopId}")
    List<OrderAbstractSellerDTO> getOrdersByShopId(@PathVariable("shopId") String shopId);

    @GetMapping("/internal/order/batch")
    List<OrderDTO> getOrdersByIds(@RequestParam("orderIds") List<String> orderIds);

    @PutMapping("/api/seller/order/{orderId}/ship")
    Map<String, Object> shipOrder(@PathVariable("orderId") String orderId,
                                  @RequestBody ShipOrderRequest request);
}
```

---

### Task 12: 清理验证

**Files:**
- All modified files in order-service

- [ ] **Step 1: 检查所有文件的无用 import**

对照各文件逐项检查：
- `OrderUserController.java`：移除了 `ShopFeignClient`、`LogisticsFeignClient`、`ProductFeignClient`、`ArrayList`、`HashMap`、`Map`、`PlaceOrderRequest`（保留）、`ApiResponse`
- `OrderSellerController.java`：移除了 `LogisticsFeignClient`
- `OrderServiceImpl.java`：移除了 `UserOrderMapper`、`UserOrder`

- [ ] **Step 2: 确认编译通过**

```bash
cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka
mvn compile -pl order-service -am
```

Expected: BUILD SUCCESS
