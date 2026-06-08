# 订单退货流程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `order-service` 中实现退货申请、商家审核、退货物流回填、订单状态联动。

**Architecture:** 新增 `ReturnRequestService` 管理退货全流程。`return_requests` 单表存所有记录，`status` 区分 `applying/agreed/rejected`。用户申请不改订单状态；商家同意时事务内更新状态并 CAS 订单→`RETURN_PENDING`；用户填物流后订单→`RETURNING`。无 WebSocket，无复制场景。

**Tech Stack:** Java 17, Spring Boot 3.2.3, MyBatis 注解 Mapper, Spring Validation, OpenFeign, JUnit 5, Mockito, MockMvc。

---

## File Structure

- Modify: `AI-Shopping-backend_Eureka/sql/init/02-order-init.sql`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/model/ReturnRequest.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/CreateReturnRequest.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/ReviewReturnRequest.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/SubmitReturnLogisticsRequest.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/ReturnRequestDTO.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/mapper/ReturnRequestMapper.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/ReturnRequestService.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/ReturnRequestServiceImpl.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/service/ReturnRequestServiceImplTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderUserControllerTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderSellerControllerTest.java`

---

### Task 1: 数据库表结构

**Files:**
- Modify: `AI-Shopping-backend_Eureka/sql/init/02-order-init.sql`

- [ ] **Step 1: 追加 `return_requests` 表**

在 `deleted_orders` 表后、`SELECT` 语句前追加：

```sql
CREATE TABLE IF NOT EXISTS return_requests (
    order_id      VARCHAR(20) PRIMARY KEY COMMENT '订单ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    shop_id       VARCHAR(32)  NOT NULL COMMENT '店铺ID',
    return_reason VARCHAR(500) NOT NULL COMMENT '退货原因',
    status        VARCHAR(20)  NOT NULL DEFAULT 'applying' COMMENT '状态:applying审核中/agreed同意/rejected拒绝',
    logistics_id  INT          NULL COMMENT '退货物流ID',
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_date  TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT chk_return_status CHECK (status IN ('applying','agreed','rejected')),
    INDEX idx_shop_status (shop_id, status),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退货申请表';
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-backend_Eureka/sql/init/02-order-init.sql
git commit -m "feat: add return_requests table"
```

---

### Task 2: 模型、DTO、Mapper

**Files:**
- Create: `ReturnRequest.java`, DTOs, `ReturnRequestMapper.java`

- [ ] **Step 1: 创建 `ReturnRequest` 模型**

```java
package com.gzasc.aishopping.order.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ReturnRequest {
    public static final String APPLYING = "applying";
    public static final String AGREED = "agreed";
    public static final String REJECTED = "rejected";

    private String orderId;
    private Long userId;
    private String shopId;
    private String returnReason;
    private String status;
    private Integer logisticsId;
    private Timestamp createdDate;
    private Timestamp updatedDate;

    public boolean isApplying() {
        return APPLYING.equals(this.status);
    }

    public boolean isAgreed() {
        return AGREED.equals(this.status);
    }
}
```

- [ ] **Step 2: 创建 DTO**

```java
// CreateReturnRequest.java
package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReturnRequest {
    @NotBlank(message = "退货原因不能为空")
    @Size(max = 500, message = "退货原因不能超过500字")
    private String returnReason;
}
```

```java
// ReviewReturnRequest.java
package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewReturnRequest {
    @NotBlank(message = "审核结果不能为空")
    private String status;
}
```

```java
// SubmitReturnLogisticsRequest.java
package com.gzasc.aishopping.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReturnLogisticsRequest {
    @NotBlank(message = "快递单号不能为空")
    private String trackingNumber;

    @NotNull(message = "联系人ID不能为空")
    private Integer contactId;
}
```

```java
// ReturnRequestDTO.java
package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ReturnRequestDTO {
    private String orderId;
    private Long userId;
    private String shopId;
    private String returnReason;
    private String status;
    private Integer logisticsId;
    private Timestamp createdDate;
    private Timestamp updatedDate;
}
```

- [ ] **Step 3: 创建 `ReturnRequestMapper`**

```java
package com.gzasc.aishopping.order.mapper;

import com.gzasc.aishopping.order.model.ReturnRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReturnRequestMapper {

    @Insert("INSERT INTO return_requests (order_id, user_id, shop_id, return_reason, status, created_date) " +
            "VALUES (#{orderId}, #{userId}, #{shopId}, #{returnReason}, #{status}, #{createdDate})")
    int insert(ReturnRequest request);

    @Select("SELECT * FROM return_requests WHERE order_id = #{orderId}")
    ReturnRequest selectByOrderId(@Param("orderId") String orderId);

    @Select("SELECT * FROM return_requests WHERE order_id = #{orderId} AND user_id = #{userId}")
    ReturnRequest selectByOrderIdAndUser(@Param("orderId") String orderId, @Param("userId") Long userId);

    @Select("SELECT * FROM return_requests WHERE order_id = #{orderId} AND shop_id = #{shopId}")
    ReturnRequest selectByOrderIdAndShop(@Param("orderId") String orderId, @Param("shopId") String shopId);

    @Select("SELECT * FROM return_requests WHERE shop_id = #{shopId} AND status = #{status} ORDER BY created_date DESC")
    List<ReturnRequest> selectByShopAndStatus(@Param("shopId") String shopId, @Param("status") String status);

    @Update("UPDATE return_requests SET status = #{status}, updated_date = CURRENT_TIMESTAMP WHERE order_id = #{orderId}")
    int updateStatus(@Param("orderId") String orderId, @Param("status") String status);

    @Update("UPDATE return_requests SET logistics_id = #{logisticsId}, updated_date = CURRENT_TIMESTAMP WHERE order_id = #{orderId}")
    int updateLogisticsId(@Param("orderId") String orderId, @Param("logisticsId") Integer logisticsId);
}
```

- [ ] **Step 4: 编译并提交**

```bash
mvn -pl AI-Shopping-backend_Eureka/order-service -am -DskipTests compile
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/model/ReturnRequest.java AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/mapper/ReturnRequestMapper.java
git commit -m "feat: add return request model mapper dto"
```

Expected: `BUILD SUCCESS`.

---

### Task 3: ReturnRequestService 核心逻辑

**Files:**
- Create: `ReturnRequestService.java`
- Create: `ReturnRequestServiceImpl.java`
- Create: `ReturnRequestServiceImplTest.java`

- [ ] **Step 1: 写失败测试**

```java
package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.CreateReturnRequest;
import com.gzasc.aishopping.order.dto.ReviewReturnRequest;
import com.gzasc.aishopping.order.dto.ReturnRequestDTO;
import com.gzasc.aishopping.order.dto.SubmitReturnLogisticsRequest;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.mapper.ReturnRequestMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.model.ReturnRequest;
import com.gzasc.aishopping.order.service.impl.ReturnRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceImplTest {

    @Mock private ReturnRequestMapper returnRequestMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private LogisticsFeignClient logisticsFeignClient;

    private ReturnRequestServiceImpl returnRequestService;

    @Captor private ArgumentCaptor<ReturnRequest> requestCaptor;

    private final Long userId = 100L;
    private final String shopId = "SHOP001";
    private final String orderId = "ORDER001";
    private final Timestamp now = new Timestamp(System.currentTimeMillis());

    @BeforeEach
    void setUp() {
        returnRequestService = new ReturnRequestServiceImpl(returnRequestMapper, orderMapper, logisticsFeignClient);
    }

    private Order createOrder(String status) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setOrderStatus(status);
        return order;
    }

    private ReturnRequest createReturnRequest(String status) {
        ReturnRequest r = new ReturnRequest();
        r.setOrderId(orderId);
        r.setUserId(userId);
        r.setShopId(shopId);
        r.setReturnReason("商品有瑕疵");
        r.setStatus(status);
        r.setCreatedDate(now);
        r.setUpdatedDate(now);
        return r;
    }

    @Nested
    @DisplayName("createReturnRequest")
    class CreateReturnRequestTests {

        @Test
        @DisplayName("成功创建退货申请")
        void createReturnRequest_success() {
            CreateReturnRequest req = new CreateReturnRequest();
            req.setReturnReason("商品有瑕疵");
            Order order = createOrder(Order.SHIPPED);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(null);
            when(returnRequestMapper.insert(any())).thenReturn(1);

            assertDoesNotThrow(() -> returnRequestService.createReturnRequest(userId, orderId, req));

            verify(returnRequestMapper).insert(requestCaptor.capture());
            ReturnRequest inserted = requestCaptor.getValue();
            assertEquals(orderId, inserted.getOrderId());
            assertEquals(userId, inserted.getUserId());
            assertEquals(shopId, inserted.getShopId());
            assertEquals(ReturnRequest.APPLYING, inserted.getStatus());
        }

        @Test
        @DisplayName("订单不存在抛异常")
        void createReturnRequest_orderNotFound() {
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(null);
            CreateReturnRequest req = new CreateReturnRequest();
            req.setReturnReason("原因");
            assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));
        }

        @Test
        @DisplayName("订单状态不允许退货抛异常")
        void createReturnRequest_invalidStatus() {
            Order order = createOrder(Order.PENDING);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            CreateReturnRequest req = new CreateReturnRequest();
            req.setReturnReason("原因");
            assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));
        }

        @Test
        @DisplayName("已有退货申请抛异常")
        void createReturnRequest_duplicate() {
            Order order = createOrder(Order.SHIPPED);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            when(returnRequestMapper.selectByOrderId(orderId)).thenReturn(createReturnRequest(ReturnRequest.APPLYING));
            CreateReturnRequest req = new CreateReturnRequest();
            req.setReturnReason("原因");
            assertThrows(OrderException.class, () -> returnRequestService.createReturnRequest(userId, orderId, req));
        }
    }

    @Nested
    @DisplayName("reviewReturnRequest")
    class ReviewReturnRequestTests {

        @Test
        @DisplayName("商家同意退货")
        void reviewReturnRequest_agreed() {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus(ReturnRequest.AGREED);
            ReturnRequest pending = createReturnRequest(ReturnRequest.APPLYING);
            Order order = createOrder(Order.SHIPPED);
            when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(pending);
            when(orderMapper.selectOrderDetailByShop(shopId, orderId)).thenReturn(order);
            when(returnRequestMapper.updateStatus(orderId, ReturnRequest.AGREED)).thenReturn(1);
            when(orderMapper.updateOrderStatusCasMulti(orderId, Order.RETURN_PENDING, List.of(Order.SHIPPED, Order.DELIVERED))).thenReturn(1);

            assertDoesNotThrow(() -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
            verify(returnRequestMapper).updateStatus(orderId, ReturnRequest.AGREED);
            verify(orderMapper).updateOrderStatusCasMulti(orderId, Order.RETURN_PENDING, List.of(Order.SHIPPED, Order.DELIVERED));
        }

        @Test
        @DisplayName("商家拒绝退货")
        void reviewReturnRequest_rejected() {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus(ReturnRequest.REJECTED);
            ReturnRequest pending = createReturnRequest(ReturnRequest.APPLYING);
            when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(pending);
            when(returnRequestMapper.updateStatus(orderId, ReturnRequest.REJECTED)).thenReturn(1);

            assertDoesNotThrow(() -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
            verify(returnRequestMapper).updateStatus(orderId, ReturnRequest.REJECTED);
            verify(orderMapper, never()).updateOrderStatusCasMulti(any(), any(), any());
        }

        @Test
        @DisplayName("审核状态无效抛异常")
        void reviewReturnRequest_invalidStatus() {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus("invalid");
            assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
        }

        @Test
        @DisplayName("退货申请不存在抛异常")
        void reviewReturnRequest_notFound() {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus(ReturnRequest.AGREED);
            when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(null);
            assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
        }

        @Test
        @DisplayName("CAS失败回滚")
        void reviewReturnRequest_casFailure() {
            ReviewReturnRequest req = new ReviewReturnRequest();
            req.setStatus(ReturnRequest.AGREED);
            ReturnRequest pending = createReturnRequest(ReturnRequest.APPLYING);
            Order order = createOrder(Order.SHIPPED);
            when(returnRequestMapper.selectByOrderIdAndShop(orderId, shopId)).thenReturn(pending);
            when(orderMapper.selectOrderDetailByShop(shopId, orderId)).thenReturn(order);
            when(returnRequestMapper.updateStatus(orderId, ReturnRequest.AGREED)).thenReturn(1);
            when(orderMapper.updateOrderStatusCasMulti(orderId, Order.RETURN_PENDING, List.of(Order.SHIPPED, Order.DELIVERED))).thenReturn(0);

            assertThrows(OrderException.class, () -> returnRequestService.reviewReturnRequest(shopId, orderId, req));
        }
    }

    @Nested
    @DisplayName("submitReturnLogistics")
    class SubmitReturnLogisticsTests {

        @Test
        @DisplayName("提交退货物流成功")
        void submitReturnLogistics_success() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123456789");
            req.setContactId(1);
            ReturnRequest agreed = createReturnRequest(ReturnRequest.AGREED);
            Order order = createOrder(Order.RETURN_PENDING);

            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(agreed);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            when(logisticsFeignClient.createLogistics(any(LogisticsRequest.class)))
                    .thenReturn(ApiResponse.success(Map.of("id", 42)));
            when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(1);
            when(orderMapper.updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING)).thenReturn(1);

            assertDoesNotThrow(() -> returnRequestService.submitReturnLogistics(userId, orderId, req));

            ArgumentCaptor<LogisticsRequest> logisticsCaptor = ArgumentCaptor.forClass(LogisticsRequest.class);
            verify(logisticsFeignClient).createLogistics(logisticsCaptor.capture());
            assertEquals(orderId, logisticsCaptor.getValue().getOrderId());
            assertEquals("RETURN", logisticsCaptor.getValue().getType());
            verify(returnRequestMapper).updateLogisticsId(orderId, 42);
            verify(orderMapper).updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING);
        }

        @Test
        @DisplayName("退货申请不存在抛异常")
        void submitReturnLogistics_notFound() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123");
            req.setContactId(1);
            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(null);
            assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
        }

        @Test
        @DisplayName("退货申请未同意抛异常")
        void submitReturnLogistics_notAgreed() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123");
            req.setContactId(1);
            ReturnRequest applying = createReturnRequest(ReturnRequest.APPLYING);
            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(applying);
            assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
        }

        @Test
        @DisplayName("订单状态不是RETURN_PENDING抛异常")
        void submitReturnLogistics_wrongOrderStatus() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123");
            req.setContactId(1);
            ReturnRequest agreed = createReturnRequest(ReturnRequest.AGREED);
            Order order = createOrder(Order.SHIPPED);
            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(agreed);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
        }

        @Test
        @DisplayName("物流创建失败抛异常")
        void submitReturnLogistics_logisticsFail() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123");
            req.setContactId(1);
            ReturnRequest agreed = createReturnRequest(ReturnRequest.AGREED);
            Order order = createOrder(Order.RETURN_PENDING);
            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(agreed);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            when(logisticsFeignClient.createLogistics(any())).thenReturn(ApiResponse.error(500, "创建失败"));
            assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
        }

        @Test
        @DisplayName("已存在物流ID抛异常")
        void submitReturnLogistics_duplicateLogistics() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123");
            req.setContactId(1);
            ReturnRequest agreed = createReturnRequest(ReturnRequest.AGREED);
            agreed.setLogisticsId(99);
            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(agreed);
            assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
        }

        @Test
        @DisplayName("CAS订单状态失败抛异常")
        void submitReturnLogistics_casFailure() {
            SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
            req.setTrackingNumber("SF123");
            req.setContactId(1);
            ReturnRequest agreed = createReturnRequest(ReturnRequest.AGREED);
            Order order = createOrder(Order.RETURN_PENDING);
            when(returnRequestMapper.selectByOrderIdAndUser(orderId, userId)).thenReturn(agreed);
            when(orderMapper.selectOrderDetailByUser(userId, orderId)).thenReturn(order);
            when(logisticsFeignClient.createLogistics(any()))
                    .thenReturn(ApiResponse.success(Map.of("id", 42)));
            when(returnRequestMapper.updateLogisticsId(orderId, 42)).thenReturn(1);
            when(orderMapper.updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING)).thenReturn(0);
            assertThrows(OrderException.class, () -> returnRequestService.submitReturnLogistics(userId, orderId, req));
        }
    }

    @Nested
    @DisplayName("list 方法")
    class ListTests {
        @Test
        @DisplayName("按店铺和状态查询")
        void listByShop_success() {
            when(returnRequestMapper.selectByShopAndStatus(shopId, ReturnRequest.APPLYING))
                    .thenReturn(List.of(createReturnRequest(ReturnRequest.APPLYING)));
            List<ReturnRequestDTO> result = returnRequestService.listByShop(shopId, ReturnRequest.APPLYING);
            assertEquals(1, result.size());
            assertEquals(orderId, result.get(0).getOrderId());
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn -pl AI-Shopping-backend_Eureka/order-service -Dtest=ReturnRequestServiceImplTest test
```

Expected: FAIL (compilation error, service classes not found).

- [ ] **Step 3: 创建服务接口**

```java
package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.*;
import java.util.List;

public interface ReturnRequestService {
    void createReturnRequest(Long userId, String orderId, CreateReturnRequest request);
    void reviewReturnRequest(String shopId, String orderId, ReviewReturnRequest request);
    void submitReturnLogistics(Long userId, String orderId, SubmitReturnLogisticsRequest request);
    List<ReturnRequestDTO> listByShop(String shopId, String status);
}
```

- [ ] **Step 4: 实现服务**

```java
package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.*;
import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.mapper.OrderMapper;
import com.gzasc.aishopping.order.mapper.ReturnRequestMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.model.ReturnRequest;
import com.gzasc.aishopping.order.service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestMapper returnRequestMapper;
    private final OrderMapper orderMapper;
    private final LogisticsFeignClient logisticsFeignClient;

    @Override
    @Transactional
    public void createReturnRequest(Long userId, String orderId, CreateReturnRequest request) {
        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        if (!Order.SHIPPED.equals(order.getOrderStatus()) && !Order.DELIVERED.equals(order.getOrderStatus())) {
            throw new OrderException("当前订单状态不允许申请退货");
        }
        ReturnRequest existing = returnRequestMapper.selectByOrderId(orderId);
        if (existing != null) {
            throw new OrderException("该订单已存在退货申请");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrderId(orderId);
        returnRequest.setUserId(userId);
        returnRequest.setShopId(order.getShopId());
        returnRequest.setReturnReason(request.getReturnReason());
        returnRequest.setStatus(ReturnRequest.APPLYING);
        returnRequest.setCreatedDate(new Timestamp(System.currentTimeMillis()));

        int inserted = returnRequestMapper.insert(returnRequest);
        if (inserted <= 0) {
            throw new OrderException("创建退货申请失败");
        }
        log.info("退货申请已提交, orderId={}", orderId);
    }

    @Override
    @Transactional
    public void reviewReturnRequest(String shopId, String orderId, ReviewReturnRequest request) {
        if (!ReturnRequest.AGREED.equals(request.getStatus()) && !ReturnRequest.REJECTED.equals(request.getStatus())) {
            throw new OrderException("审核状态无效");
        }

        ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndShop(orderId, shopId);
        if (returnRequest == null || !returnRequest.isApplying()) {
            throw new OrderException("退货申请不存在或已被处理");
        }

        int updated = returnRequestMapper.updateStatus(orderId, request.getStatus());
        if (updated <= 0) {
            throw new OrderException("审核失败");
        }

        if (ReturnRequest.AGREED.equals(request.getStatus())) {
            Order order = orderMapper.selectOrderDetailByShop(shopId, orderId);
            if (order == null) {
                throw new OrderException("订单不存在");
            }
            int casUpdated = orderMapper.updateOrderStatusCasMulti(
                    orderId, Order.RETURN_PENDING, List.of(Order.SHIPPED, Order.DELIVERED));
            if (casUpdated <= 0) {
                throw new OrderException("订单状态变更失败，请重试");
            }
            log.info("退货审核通过, orderId={}", orderId);
        } else {
            log.info("退货申请已拒绝, orderId={}", orderId);
        }
    }

    @Override
    @Transactional
    public void submitReturnLogistics(Long userId, String orderId, SubmitReturnLogisticsRequest request) {
        ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndUser(orderId, userId);
        if (returnRequest == null) {
            throw new OrderException("退货申请不存在");
        }
        if (!returnRequest.isAgreed()) {
            throw new OrderException("退货申请未通过审核");
        }
        if (returnRequest.getLogisticsId() != null) {
            throw new OrderException("已提交过退货物流信息");
        }

        Order order = orderMapper.selectOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        if (!Order.RETURN_PENDING.equals(order.getOrderStatus())) {
            throw new OrderException("订单状态不允许提交退货物流");
        }

        LogisticsRequest logisticsReq = new LogisticsRequest();
        logisticsReq.setOrderId(orderId);
        logisticsReq.setType("RETURN");
        logisticsReq.setContactId(request.getContactId());
        logisticsReq.setTrackingNumber(request.getTrackingNumber());

        ApiResponse<Map<String, Object>> response = logisticsFeignClient.createLogistics(logisticsReq);
        if (response == null || response.getData() == null) {
            throw new OrderException("创建退货物流失败");
        }
        Object idObj = response.getData().get("id");
        if (idObj == null) {
            throw new OrderException("获取物流ID失败");
        }
        int logisticsId;
        if (idObj instanceof Number) {
            logisticsId = ((Number) idObj).intValue();
        } else {
            logisticsId = Integer.parseInt(idObj.toString());
        }

        int updated = returnRequestMapper.updateLogisticsId(orderId, logisticsId);
        if (updated <= 0) {
            throw new OrderException("更新退货物流信息失败");
        }

        int casUpdated = orderMapper.updateOrderStatusCas(orderId, Order.RETURNING, Order.RETURN_PENDING);
        if (casUpdated <= 0) {
            throw new OrderException("订单状态变更失败，请重试");
        }

        log.info("退货物流已提交, orderId={}, logisticsId={}", orderId, logisticsId);
    }

    @Override
    public List<ReturnRequestDTO> listByShop(String shopId, String status) {
        List<ReturnRequest> list = returnRequestMapper.selectByShopAndStatus(shopId, status);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private ReturnRequestDTO toDTO(ReturnRequest r) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setOrderId(r.getOrderId());
        dto.setUserId(r.getUserId());
        dto.setShopId(r.getShopId());
        dto.setReturnReason(r.getReturnReason());
        dto.setStatus(r.getStatus());
        dto.setLogisticsId(r.getLogisticsId());
        dto.setCreatedDate(r.getCreatedDate());
        dto.setUpdatedDate(r.getUpdatedDate());
        return dto;
    }
}
```

- [ ] **Step 5: 跑测试确认通过**

```bash
mvn -pl AI-Shopping-backend_Eureka/order-service -Dtest=ReturnRequestServiceImplTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: 提交**

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/ReturnRequestService.java AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/ReturnRequestServiceImpl.java AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/service/ReturnRequestServiceImplTest.java
git commit -m "feat: implement return request service"
```

---

### Task 4: Controller 改造

**Files:**
- Modify: `OrderUserController.java`
- Modify: `OrderSellerController.java`
- Modify: `OrderServiceImpl.java`
- Modify: `OrderUserControllerTest.java`
- Modify: `OrderSellerControllerTest.java`
- Modify: `OrderServiceImplTest.java`

- [ ] **Step 1: 更新 `OrderServiceImplTest`（新增 mock）**

`OrderServiceImpl` 新增依赖 `ReturnRequestService`，当前测试构造时需 mock：

```java
// OrderServiceImplTest.java — 追加 mock 字段
@Mock private ReturnRequestService returnRequestService;
```

将 `returnRequestService` 传入构造器（Mockito 不会自动注入构造函数，需手动追加到 `new OrderServiceImpl(..., returnRequestService)`）：

```java
// setUp() 中修改构造调用
orderService = new OrderServiceImpl(orderMapper, deletedOrderMapper, orderIdSelector,
        productFeignClient, logisticsFeignClient, contactFeignClient, orderConverter,
        fileFallbackDaemon, returnRequestService);
```

- [ ] **Step 2: 替换 `OrderServiceImpl.requestReturn`**

```java
// OrderServiceImpl.java — 替换现有 requestReturn 方法
@Override
@Transactional
public void requestReturn(Long userId, String orderId) {
    CreateReturnRequest req = new CreateReturnRequest();
    req.setReturnReason("用户申请退货");
    returnRequestService.createReturnRequest(userId, orderId, req);
}
```

需要在 `OrderServiceImpl` 中注入 `ReturnRequestService`：
```java
// 在字段区域新增
private final ReturnRequestService returnRequestService;
```

- [ ] **Step 3: 改造 `OrderUserController`**

路由不变，`POST /{orderId}/return-request` 改为接收 `@RequestBody CreateReturnRequest`，新增 `POST /{orderId}/return-logistics`：

```java
// OrderUserController.java — 追加 return-logistics，修改 return-request 接收请求体
@RestController
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderUserController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;

    // ... 现有端点不变 ...

    @PostMapping("/{orderId}/return-request")
    public ApiResponse<Void> requestReturn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid CreateReturnRequest request) {
        returnRequestService.createReturnRequest(userId, orderId, request);
        return ApiResponse.success("退货申请已提交", null);
    }

    @PostMapping("/{orderId}/return-logistics")
    public ApiResponse<Void> submitReturnLogistics(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("orderId") String orderId,
            @RequestBody @Valid SubmitReturnLogisticsRequest request) {
        returnRequestService.submitReturnLogistics(userId, orderId, request);
        return ApiResponse.success("退货物流已提交", null);
    }
}
```

注意：旧端点不再走 `OrderService.requestReturn`，改为直接调 `ReturnRequestService.createReturnRequest`。`OrderServiceImpl.requestReturn` 只保留给旧内部调用方（不对外）。

- [ ] **Step 4: 改造 `OrderSellerController`**

```java
// OrderSellerController.java — 追加审核相关端点
@RestController
@RequestMapping("/api/seller/order")
@RequiredArgsConstructor
public class OrderSellerController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;

    // ... 现有端点不变 ...

    @GetMapping("/return-requests/pending")
    public ApiResponse<List<ReturnRequestDTO>> listPendingReturns(
            @RequestParam("shopId") String shopId) {
        List<ReturnRequestDTO> list = returnRequestService.listByShop(shopId, ReturnRequest.APPLYING);
        return ApiResponse.success(list);
    }

    @GetMapping("/return-requests/processed")
    public ApiResponse<List<ReturnRequestDTO>> listProcessedReturns(
            @RequestParam("shopId") String shopId) {
        List<ReturnRequestDTO> agreed = returnRequestService.listByShop(shopId, ReturnRequest.AGREED);
        List<ReturnRequestDTO> rejected = returnRequestService.listByShop(shopId, ReturnRequest.REJECTED);
        agreed.addAll(rejected);
        return ApiResponse.success(agreed);
    }

    @PutMapping("/return-requests/{orderId}/review")
    public ApiResponse<Void> reviewReturnRequest(
            @PathVariable("orderId") String orderId,
            @RequestParam("shopId") String shopId,
            @RequestBody @Valid ReviewReturnRequest request) {
        returnRequestService.reviewReturnRequest(shopId, orderId, request);
        return ApiResponse.success("审核完成", null);
    }
}
```

- [ ] **Step 5: 更新现有 Controller 测试的 `setUp` + 追加新测试**

**`OrderUserControllerTest`：**

```java
// OrderUserControllerTest.java — 追加 mock 和 ObjectMapper
@Mock private ReturnRequestService returnRequestService;
private final ObjectMapper objectMapper = new ObjectMapper();

// setUp() 改为注入 ReturnRequestService
@BeforeEach
void setUp() {
    var controller = new OrderUserController(orderService, returnRequestService);
    var validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc = standaloneSetup(controller)
            .setValidator(validator)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
}
```

替换现有 `requestReturn` 测试（不需要 `orderService` mock，改为 returnRequestService + body）：

```java
@Test
@DisplayName("OR-028 申请退货 - 正常提交")
void requestReturn_success() throws Exception {
    CreateReturnRequest req = new CreateReturnRequest();
    req.setReturnReason("商品有瑕疵");
    doNothing().when(returnRequestService).createReturnRequest(anyLong(), anyString(), any());
    mockMvc.perform(post("/api/user/order/{orderId}/return-request", "ORDER001")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("退货申请已提交"));
}

@Test
@DisplayName("OR-029 申请退货 - 参数校验失败（原因空）")
void requestReturn_validationFail() throws Exception {
    CreateReturnRequest req = new CreateReturnRequest();
    req.setReturnReason("");
    mockMvc.perform(post("/api/user/order/{orderId}/return-request", "ORDER001")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
}
```

删除旧的 `requestReturn_shipped` 和 `requestReturn_wrongStatus` 测试（已被替换）。

需追加 import：`com.fasterxml.jackson.databind.ObjectMapper`。

**`OrderSellerControllerTest`：**

```java
// OrderSellerControllerTest.java — 追加 mock 字段
@Mock private ReturnRequestService returnRequestService;

// setUp() 改为注入 ReturnRequestService
@BeforeEach
void setUp() {
    var controller = new OrderSellerController(orderService, returnRequestService);
    var validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc = standaloneSetup(controller)
            .setValidator(validator)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
}
```

追加新测试：

```java
// OrderUserControllerTest.java — 追加
@Nested
@DisplayName("退货物流")
class ReturnLogisticsTests {
    @Test
    @DisplayName("提交退货物流成功")
    void submitReturnLogistics_success() throws Exception {
        SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
        req.setTrackingNumber("SF123456789");
        req.setContactId(1);
        doNothing().when(returnRequestService).submitReturnLogistics(anyLong(), anyString(), any());
        mockMvc.perform(post("/api/user/order/{orderId}/return-logistics", "ORDER001")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("退货物流已提交"));
    }

    @Test
    @DisplayName("提交退货物流-参数校验失败")
    void submitReturnLogistics_validationFail() throws Exception {
        SubmitReturnLogisticsRequest req = new SubmitReturnLogisticsRequest();
        req.setTrackingNumber("");
        req.setContactId(null);
        mockMvc.perform(post("/api/user/order/{orderId}/return-logistics", "ORDER001")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
```

```java
// OrderSellerControllerTest.java — 追加测试
@Nested
@DisplayName("退货审核")
class ReturnReviewTests {
    @Test
    @DisplayName("商家查看待审核列表")
    void listPendingReturns() throws Exception {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setOrderId("ORDER001");
        dto.setReturnReason("瑕疵");
        dto.setStatus(ReturnRequest.APPLYING);
        when(returnRequestService.listByShop("SHOP001", ReturnRequest.APPLYING))
                .thenReturn(List.of(dto));
        mockMvc.perform(get("/api/seller/order/return-requests/pending")
                        .param("shopId", "SHOP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value("ORDER001"));
    }

    @Test
    @DisplayName("商家审核同意")
    void reviewReturnRequest_agreed() throws Exception {
        ReviewReturnRequest req = new ReviewReturnRequest();
        req.setStatus(ReturnRequest.AGREED);
        doNothing().when(returnRequestService).reviewReturnRequest(anyString(), anyString(), any());
        mockMvc.perform(put("/api/seller/order/return-requests/{orderId}/review", "ORDER001")
                        .param("shopId", "SHOP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("审核完成"));
    }

    @Test
    @DisplayName("商家审核拒绝")
    void reviewReturnRequest_rejected() throws Exception {
        ReviewReturnRequest req = new ReviewReturnRequest();
        req.setStatus(ReturnRequest.REJECTED);
        doNothing().when(returnRequestService).reviewReturnRequest(anyString(), anyString(), any());
        mockMvc.perform(put("/api/seller/order/return-requests/{orderId}/review", "ORDER001")
                        .param("shopId", "SHOP001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("审核完成"));
    }
}
```

- [ ] **Step 6: 跑测试并提交**

```bash
mvn -pl AI-Shopping-backend_Eureka/order-service -Dtest=OrderUserControllerTest,OrderSellerControllerTest test
```

Expected: `BUILD SUCCESS`.

```bash
git add AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderUserControllerTest.java AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderSellerControllerTest.java
git commit -m "feat: expose return request endpoints"
```

---

### Task 5: 全量验证

**Files:**
- All files changed above.

- [ ] **Step 1: 运行订单服务全部测试**

```bash
mvn -pl AI-Shopping-backend_Eureka/order-service -am test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: 编译验证**

```bash
mvn -pl AI-Shopping-backend_Eureka/order-service -am -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: 检查变更范围**

```bash
git status
git diff --stat
```

Expected: 只包含本计划涉及文件，无无关变更。

---

## Self-Review

- Spec coverage: 覆盖数据表、模型、Mapper、Service、Controller、测试和验证命令。
- Placeholder scan: 无 TBD、TODO 或未定需求。
- Type consistency: DTO、Model、Mapper、Service 方法命名一致；状态值与 spec 一致。
- 去掉了 WebSocket（按简化版设计）；去掉了双表设计（单表 `return_requests`）。
- `submitReturnLogistics` 成功后订单状态改为 `RETURNING`（按用户确认）。
