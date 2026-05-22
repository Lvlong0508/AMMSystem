# 物流重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 将订单与物流的关系从 `订单.logisticsId → 物流.id` 改为 `物流.orderId → 订单.orderId`，物流新增 `type`（DELIVERY/RETURN）和 `orderId` 字段，删除订单侧的 `logistics_id`。

**架构：** 物流记录通过 `order_id` + `type` 反向关联订单，一个订单可有多条物流（发货+退货）。删除订单侧的 `logistics_id` 字段，所有物流查询通过 `orderId` 进行。

**改动顺序：** common-api（契约）→ logistics-service（提供方）→ order-service（消费方）

---

### Task 1: 数据库 SQL 文件（已完成）

**文件：**
- Modify: `sql/init/04-logistics-init.sql`
- Modify: `sql/init/02-order-init.sql`

- [x] **Step 1: 修改 logistics 表** — 新增 `order_id`、`type`、`created_at` 替换 `shipping_date`，加联合索引
- [x] **Step 2: 修改 order 表** — 删掉 `t_order` 和 `deleted_orders` 的 `logistics_id` 列

---

### Task 2: common-api — LogisticsRequest（修改）

**文件：**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/dto/logistics/LogisticsRequest.java`

- [ ] **Step 1: 给 LogisticsRequest 加 orderId 和 type，去掉 shippingDate**

```java
package com.gzasc.aishopping.common.dto.logistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogisticsRequest implements Serializable {
    private String orderId;
    private String type;
    private Integer contactId;
    private String trackingNumber;
}
```

---

### Task 3: common-api — LogisticsFeignClient（修改）

**文件：**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/feign/logistics/LogisticsFeignClient.java`

- [ ] **Step 1: 替换 Feign 接口，增加按 orderId 查询，去掉按 id 查询和关闭**

```java
package com.gzasc.aishopping.common.feign.logistics;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "logistics-service")
public interface LogisticsFeignClient {

    @PostMapping("/internal/logistics/create")
    ApiResponse<Map<String, Object>> createLogistics(@RequestBody LogisticsRequest request);

    @GetMapping("/internal/logistics/order/{orderId}")
    ApiResponse<List<Map<String, Object>>> getLogisticsByOrder(@PathVariable("orderId") String orderId);

    @GetMapping("/internal/logistics/order/{orderId}/latest")
    ApiResponse<Map<String, Object>> getLatestLogistics(
            @PathVariable("orderId") String orderId,
            @RequestParam("type") String type);
}
```

---

### Task 4: common-api — OrderDTO（修改）

**文件：**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/dto/order/OrderDTO.java`

- [ ] **Step 1: 删除 logisticsId 字段**

```java
    private Timestamp orderDate;
    private Integer contactId;
    private String contactName;
    // logisticsId 已删除
```

---

### Task 5: logistics — Logistics 实体（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/model/Logistics.java`

- [ ] **Step 1: 加 orderId、type，shippingDate 改为 createdAt**

```java
package com.gzasc.aishopping.logistics.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Logistics {
    private Integer id;
    private String orderId;
    private String type;
    private Integer contactId;
    private Timestamp createdAt;
    private String trackingNumber;
}
```

---

### Task 6: logistics — LogisticsMapper（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/mapper/LogisticsMapper.java`

- [ ] **Step 1: 更新所有 SQL，加 order_id/type，删 update 相关 SQL，新增按 orderId 查询方法**

```java
package com.gzasc.aishopping.logistics.mapper;

import com.gzasc.aishopping.logistics.model.Logistics;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LogisticsMapper {

    @Insert("INSERT INTO logistics (order_id, type, contact_id, tracking_number) " +
            "VALUES (#{orderId}, #{type}, #{contactId}, #{trackingNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertLogistics(Logistics logistics);

    @Delete("DELETE FROM logistics WHERE id = #{id}")
    int deleteLogisticsById(Integer id);

    @Select("SELECT * FROM logistics WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsById(Integer id);

    @Select("SELECT * FROM logistics")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    List<Logistics> selectAllLogistics();

    @Select("SELECT * FROM logistics WHERE tracking_number = #{trackingNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLogisticsByTrackingNumber(String trackingNumber);

    @Select("SELECT * FROM logistics WHERE order_id = #{orderId} ORDER BY created_at DESC")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    List<Logistics> selectLogisticsByOrderId(String orderId);

    @Select("SELECT * FROM logistics WHERE order_id = #{orderId} AND type = #{type} ORDER BY created_at DESC LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "contactId", column = "contact_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "trackingNumber", column = "tracking_number")
    })
    Logistics selectLatestLogisticsByOrderIdAndType(@Param("orderId") String orderId, @Param("type") String type);
}
```

---

### Task 7: logistics — CreateLogisticsRequest（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/dto/CreateLogisticsRequest.java`

- [ ] **Step 1: 加 orderId、type，shippingDate 改 createdAt，去掉 shippingDate 解析**

```java
package com.gzasc.aishopping.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLogisticsRequest {
    @NotBlank(message = "订单号不能为空")
    private String orderId;

    private String type;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;

    @NotBlank(message = "运单号不能为空")
    private String trackingNumber;
}
```

---

### Task 8: logistics — 删除 UpdateLogisticsRequest

**文件：**
- Delete: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/dto/UpdateLogisticsRequest.java`

- [ ] **Step 1: 删除此文件**（不再需要更新物流接口）

---

### Task 9: logistics — LogisticsResponse（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/dto/LogisticsResponse.java`

- [ ] **Step 1: 加 orderId、type，shippingDate 改 createdAt**

```java
package com.gzasc.aishopping.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsResponse {
    private Integer id;
    private String orderId;
    private String type;
    private Integer contactId;
    private String trackingNumber;
    private Timestamp createdAt;
}
```

---

### Task 10: logistics — LogisticsConverter（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/converter/LogisticsConverter.java`

- [ ] **Step 1: 更新三个 toModel 方法和 toResponse 方法**

```java
package com.gzasc.aishopping.logistics.converter;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;
import org.springframework.stereotype.Component;

@Component
public class LogisticsConverter {

    public Logistics toModel(CreateLogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setOrderId(request.getOrderId());
        logistics.setType(request.getType() != null ? request.getType() : "DELIVERY");
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        return logistics;
    }

    public Logistics toModel(LogisticsRequest request) {
        Logistics logistics = new Logistics();
        logistics.setOrderId(request.getOrderId());
        logistics.setType(request.getType() != null ? request.getType() : "DELIVERY");
        logistics.setContactId(request.getContactId());
        logistics.setTrackingNumber(request.getTrackingNumber());
        return logistics;
    }

    public LogisticsResponse toResponse(Logistics logistics) {
        if (logistics == null) return null;
        return LogisticsResponse.builder()
                .id(logistics.getId())
                .orderId(logistics.getOrderId())
                .type(logistics.getType())
                .contactId(logistics.getContactId())
                .trackingNumber(logistics.getTrackingNumber())
                .createdAt(logistics.getCreatedAt())
                .build();
    }
}
```

---

### Task 11: logistics — LogisticsService 接口（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/service/LogisticsService.java`

- [ ] **Step 1: 加新方法，删旧方法**

```java
package com.gzasc.aishopping.logistics.service;

import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;

import java.util.List;

public interface LogisticsService {

    LogisticsResponse createLogistics(CreateLogisticsRequest request);

    LogisticsResponse createLogistics(Logistics logistics);

    void deleteLogisticsById(Integer id);

    LogisticsResponse getLogisticsById(Integer id);

    List<LogisticsResponse> getAllLogistics();

    LogisticsResponse getLogisticsByTrackingNumber(String trackingNumber);

    List<LogisticsResponse> getLogisticsByOrderId(String orderId);

    LogisticsResponse getLatestLogistics(String orderId, String type);
}
```

---

### Task 12: logistics — LogisticsServiceImpl（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/service/impl/LogisticsServiceImpl.java`

- [ ] **Step 1: 删 updateLogistics 方法，新增 getLogisticsByOrderId 和 getLatestLogistics**

```java
package com.gzasc.aishopping.logistics.service.impl;

import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.exception.LogisticsException;
import com.gzasc.aishopping.logistics.mapper.LogisticsMapper;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final LogisticsMapper logisticsMapper;
    private final LogisticsConverter logisticsConverter;

    @Override
    @Transactional
    public LogisticsResponse createLogistics(CreateLogisticsRequest request) {
        Logistics logistics = logisticsConverter.toModel(request);
        return createLogistics(logistics);
    }

    @Override
    @Transactional
    public LogisticsResponse createLogistics(Logistics logistics) {
        int result = logisticsMapper.insertLogistics(logistics);
        if (result <= 0) {
            throw new LogisticsException("创建物流信息失败");
        }
        return logisticsConverter.toResponse(logistics);
    }

    @Override
    @Transactional
    public void deleteLogisticsById(Integer id) {
        Logistics exists = logisticsMapper.selectLogisticsById(id);
        if (exists == null) {
            throw new LogisticsException("物流信息不存在");
        }
        logisticsMapper.deleteLogisticsById(id);
    }

    @Override
    public LogisticsResponse getLogisticsById(Integer id) {
        Logistics logistics = logisticsMapper.selectLogisticsById(id);
        if (logistics == null) {
            throw new LogisticsException("物流信息不存在");
        }
        return logisticsConverter.toResponse(logistics);
    }

    @Override
    public List<LogisticsResponse> getAllLogistics() {
        return logisticsMapper.selectAllLogistics().stream()
                .map(logisticsConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LogisticsResponse getLogisticsByTrackingNumber(String trackingNumber) {
        Logistics logistics = logisticsMapper.selectLogisticsByTrackingNumber(trackingNumber);
        if (logistics == null) {
            throw new LogisticsException("物流信息不存在");
        }
        return logisticsConverter.toResponse(logistics);
    }

    @Override
    public List<LogisticsResponse> getLogisticsByOrderId(String orderId) {
        return logisticsMapper.selectLogisticsByOrderId(orderId).stream()
                .map(logisticsConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public LogisticsResponse getLatestLogistics(String orderId, String type) {
        Logistics logistics = logisticsMapper.selectLatestLogisticsByOrderIdAndType(orderId, type);
        if (logistics == null) {
            throw new LogisticsException("物流信息不存在");
        }
        return logisticsConverter.toResponse(logistics);
    }
}
```

---

### Task 13: logistics — LogisticsController（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/controller/LogisticsController.java`

- [ ] **Step 1: 新增 `/logistics/order/{orderId}` 和 `/logistics/order/{orderId}/latest`，删除 `/logistics/get/{id}` 和 `/logistics/update`**

```java
package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.logistics.dto.CreateLogisticsRequest;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    @PostMapping("/create")
    public ApiResponse<LogisticsResponse> createLogistics(@RequestBody @Valid CreateLogisticsRequest request) {
        LogisticsResponse result = logisticsService.createLogistics(request);
        return ApiResponse.success("创建物流信息成功", result);
    }

    @GetMapping("/list")
    public ApiResponse<List<LogisticsResponse>> getAllLogistics() {
        List<LogisticsResponse> logistics = logisticsService.getAllLogistics();
        return ApiResponse.success("查询成功", logistics);
    }

    @GetMapping("/search/tracking")
    public ApiResponse<LogisticsResponse> getLogisticsByTrackingNumber(@RequestParam("trackingNumber") String trackingNumber) {
        LogisticsResponse logistics = logisticsService.getLogisticsByTrackingNumber(trackingNumber);
        return ApiResponse.success("查询成功", logistics);
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteLogistics(@PathVariable("id") Integer id) {
        logisticsService.deleteLogisticsById(id);
        return ApiResponse.success("删除物流信息成功", null);
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<LogisticsResponse>> getLogisticsByOrder(@PathVariable("orderId") String orderId) {
        List<LogisticsResponse> logistics = logisticsService.getLogisticsByOrderId(orderId);
        return ApiResponse.success("查询成功", logistics);
    }

    @GetMapping("/order/{orderId}/latest")
    public ApiResponse<LogisticsResponse> getLatestLogistics(
            @PathVariable("orderId") String orderId,
            @RequestParam("type") String type) {
        LogisticsResponse logistics = logisticsService.getLatestLogistics(orderId, type);
        return ApiResponse.success("查询成功", logistics);
    }
}
```

---

### Task 14: logistics — InternalLogisticsController（修改）

**文件：**
- Modify: `logistics-service/src/main/java/com/gzasc/aishopping/logistics/controller/InternalLogisticsController.java`

- [ ] **Step 1: 删按 id 查询/关闭，新增按 orderId 查询**

```java
package com.gzasc.aishopping.logistics.controller;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.logistics.converter.LogisticsConverter;
import com.gzasc.aishopping.logistics.dto.LogisticsResponse;
import com.gzasc.aishopping.logistics.model.Logistics;
import com.gzasc.aishopping.logistics.service.LogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/logistics")
@RequiredArgsConstructor
public class InternalLogisticsController {

    private final LogisticsService logisticsService;
    private final LogisticsConverter logisticsConverter;

    @PostMapping("/create")
    public ApiResponse<LogisticsResponse> createLogistics(@RequestBody LogisticsRequest request) {
        Logistics logistics = logisticsConverter.toModel(request);
        LogisticsResponse result = logisticsService.createLogistics(logistics);
        return ApiResponse.success("创建物流信息成功", result);
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<LogisticsResponse>> getLogisticsByOrder(@PathVariable("orderId") String orderId) {
        List<LogisticsResponse> logistics = logisticsService.getLogisticsByOrderId(orderId);
        return ApiResponse.success("查询成功", logistics);
    }

    @GetMapping("/order/{orderId}/latest")
    public ApiResponse<LogisticsResponse> getLatestLogistics(
            @PathVariable("orderId") String orderId,
            @RequestParam("type") String type) {
        LogisticsResponse logistics = logisticsService.getLatestLogistics(orderId, type);
        return ApiResponse.success("查询成功", logistics);
    }
}
```

---

### Task 15: order — Order 实体（修改）

**文件：**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/model/Order.java`

- [ ] **Step 1: 删 logisticsId 字段，shipOrder 方法不再需要 logisticsId 参数**

```java
    private String orderStatus;
    private Timestamp orderDate;
    private Integer contactId;

    // logisticsId 已删除

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
```

---

### Task 16: order — DeletedOrder 实体（修改）

**文件：**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/model/DeletedOrder.java`

- [ ] **Step 1: 删 logisticsId 字段和 fromOrder 中的引用**

```java
    private Integer contactId;
    // logisticsId 已删除
    private Timestamp deletedAt;

    public static DeletedOrder fromOrder(Order order) {
        DeletedOrder deletedOrder = new DeletedOrder();
        deletedOrder.setOrderId(order.getOrderId());
        deletedOrder.setProductId(order.getProductId());
        deletedOrder.setQuantity(order.getQuantity());
        deletedOrder.setTotalPrice(order.getTotalPrice());
        deletedOrder.setOrderStatus(order.getOrderStatus());
        deletedOrder.setOrderDate(order.getOrderDate());
        deletedOrder.setContactId(order.getContactId());
        deletedOrder.setDeletedAt(new Timestamp(System.currentTimeMillis()));
        return deletedOrder;
    }
```

---

### Task 17: order — OrderMapper（修改）

**文件：**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/mapper/OrderMapper.java`

- [ ] **Step 1: 删所有 `logistics_id` 相关的 SQL/映射，删 `updateOrder` 和 `updateOrderLogisticsId` 方法**

```java
package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO t_order (order_id, product_id, quantity, total_price, " +
            "order_status, order_date, contact_id) " +
            "VALUES (#{orderId}, #{productId}, #{quantity}, #{totalPrice}, " +
            "#{orderStatus}, #{orderDate}, #{contactId})")
    int insertOrder(Order order);

    @Delete("DELETE FROM t_order WHERE order_id = #{orderId}")
    int deleteOrderById(String orderId);

    @Select("SELECT * FROM t_order WHERE order_id = #{orderId}")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id")
    })
    Order selectOrderById(String orderId);

    @Select("SELECT * FROM t_order")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id")
    })
    List<Order> selectAllOrders();

    @Select("SELECT * FROM t_order WHERE order_status = #{status}")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id")
    })
    List<Order> selectOrdersByStatus(String status);

    @Update("UPDATE t_order SET order_status = #{status} WHERE order_id = #{orderId}")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Select("<script>" +
            "SELECT * FROM t_order WHERE order_id IN " +
            "<foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>" +
            "#{orderId}" +
            "</foreach>" +
            "</script>")
    @Results({
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "orderStatus", column = "order_status"),
            @Result(property = "orderDate", column = "order_date"),
            @Result(property = "contactId", column = "contact_id")
    })
    List<Order> selectOrdersByIds(@Param("orderIds") List<String> orderIds);
}
```

---

### Task 18: order — DeletedOrderMapper（修改）

**文件：**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/mapper/DeletedOrderMapper.java`

- [ ] **Step 1: 删所有 SQL 中的 `logistics_id` 引用**

所有 INSERT 和 @Results 中删掉 `logistics_id`。
INSERT 改为：`(order_id, product_id, quantity, total_price, order_status, order_date, contact_id, deleted_at)`

---

### Task 19: order — OrderSellerController（重写发货/查单逻辑）

**文件：**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java`

- [ ] **Step 1: getOrderById — 删 logisticsId 输出，改为通过 orderId 查物流**

```java
    @GetMapping("/{orderId}")
    public Map<String, Object> getOrderById(@PathVariable("orderId") String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return Map.of("success", false, "message", "订单不存在");
            }
            Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("orderId", order.getOrderId());
            orderMap.put("productId", order.getProductId());
            orderMap.put("quantity", order.getQuantity());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("orderStatus", order.getOrderStatus());
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("contactId", order.getContactId());
            // 通过 orderId 查最新发货物流
            try {
                Map<String, Object> logisticsResult = logisticsFeignClient.getLatestLogistics(orderId, "DELIVERY");
                if (logisticsResult != null && logisticsResult.containsKey("data")) {
                    Object data = logisticsResult.get("data");
                    if (data instanceof Map) {
                        Map<String, Object> logistics = (Map<String, Object>) data;
                        orderMap.put("trackingNumber", logistics.get("trackingNumber"));
                        orderMap.put("logistics", logistics);
                    }
                }
            } catch (Exception e) {
                System.err.println("获取物流信息失败: " + e.getMessage());
            }
            return Map.of("success", true, "order", orderMap);
        } catch (Exception e) {
            return Map.of("success", false, "message", "查询订单错误：" + e.getMessage());
        }
    }
```

- [ ] **Step 2: shipOrder — 创建物流时传 orderId，不再回写 logisticsId**

```java
    @PutMapping("/{orderId}/ship")
    public Map<String, String> shipOrder(
            @PathVariable("orderId") String orderId,
            @RequestBody ShipOrderRequest request) {

        if (orderId == null || orderId.trim().isEmpty()) {
            return Map.of("message", "发货失败：订单ID不能为空");
        }
        if (request == null) {
            return Map.of("message", "发货失败：请求体不能为空");
        }
        if (request.getTrackingNumber() == null || request.getTrackingNumber().trim().isEmpty()) {
            return Map.of("message", "发货失败：物流单号不能为空");
        }
        if (request.getContactId() == null) {
            return Map.of("message", "发货失败：联系人ID不能为空");
        }

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return Map.of("message", "发货失败：订单不存在");
        }

        if (Order.SHIPPED.equals(order.getOrderStatus())) {
            return Map.of("message", "发货成功（已发货）");
        }

        if (!Order.PAID.equals(order.getOrderStatus())) {
            return Map.of("message", "发货失败：订单状态为【" + order.getOrderStatus() + "】，只有已支付订单才能发货");
        }

        try {
            LogisticsRequest logisticsRequest = new LogisticsRequest();
            logisticsRequest.setOrderId(orderId);
            logisticsRequest.setType("DELIVERY");
            logisticsRequest.setContactId(request.getContactId());
            logisticsRequest.setTrackingNumber(request.getTrackingNumber());

            Map<String, Object> logisticsResult = logisticsFeignClient.createLogistics(logisticsRequest);
            Object data = logisticsResult.get("data");
            if (data == null) {
                String logisticsMessage = (String) logisticsResult.get("message");
                return Map.of("message", "发货失败：" + (logisticsMessage != null ? logisticsMessage : "创建物流返回数据为空"));
            }

            int result = orderService.updateOrderStatus(orderId, Order.SHIPPED);

            if (result > 0) {
                return Map.of("message", "发货成功");
            } else {
                return Map.of("message", "发货失败：更新订单状态失败");
            }
        } catch (Exception e) {
            return Map.of("message", "发货错误：" + e.getMessage());
        }
    }
```

- [ ] **Step 3: 删 `compensateCloseLogistics` 方法和已发货时返回 logisticsId 的逻辑**

删掉 `compensateCloseLogistics()` 方法，删掉 import 中的 `UpdateLogisticsRequest` 引用。

---

### Task 20: order — OrderUserController 查单逻辑修改

**文件：**
- Modify: `order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java`

- [ ] **Step 1: convertToMapWithLogistics — 删 logisticsId，改为通过 orderId 查物流**

```java
    private Map<String, Object> convertToMapWithLogistics(Order order) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", order.getOrderId());
        orderMap.put("productId", order.getProductId());
        orderMap.put("quantity", order.getQuantity());
        orderMap.put("totalPrice", order.getTotalPrice());
        orderMap.put("orderStatus", order.getOrderStatus());
        orderMap.put("orderDate", order.getOrderDate());
        orderMap.put("contactId", order.getContactId());

        try {
            Map<String, Object> logisticsResult = logisticsFeignClient.getLatestLogistics(order.getOrderId(), "DELIVERY");
            if (logisticsResult != null && logisticsResult.containsKey("data")) {
                Object data = logisticsResult.get("data");
                if (data instanceof Map) {
                    Map<String, Object> logistics = (Map<String, Object>) data;
                    orderMap.put("trackingNumber", logistics.get("trackingNumber"));
                    orderMap.put("logistics", logistics);
                }
            }
        } catch (Exception e) {
            System.err.println("获取物流信息失败: " + e.getMessage());
        }

        return orderMap;
    }
```

---

### Task 21: 编译验证

**文件：**
- Workdir: `AI-Shopping-backend_Eureka/`

- [ ] **Step 1: Maven 编译**

Run: `mvn clean compile -pl common-api,logistics-service,order-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如有编译错误，按错误信息修复**
