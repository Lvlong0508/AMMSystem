# 订单展示重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**目标:** 重构订单服务的展示 DTO，让订单列表/详情/发货页直接返回面向页面的展示字段。

**Architecture:** 订单服务作为聚合层，通过 Feign 获取商品、店铺、联系人、物流信息，输出新展示 DTO。不修改订单表结构。

**Tech Stack:** Spring Boot, MyBatis, OpenFeign, JUnit 5

---

### Task 1: 新建 3 个展示 DTO

**Files:**
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/UserOrderCardDTO.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/SellerOrderCardDTO.java`
- Create: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/ShipmentOrderCardDTO.java`

- [ ] **Step 1: Create `UserOrderCardDTO`**

```java
package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserOrderCardDTO {
    private String orderId;
    private String shopLogoUrl;
    private String shopName;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String productType;
    private String orderStatus;
    private BigDecimal totalPrice;
}
```

- [ ] **Step 2: Create `SellerOrderCardDTO`**

```java
package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SellerOrderCardDTO {
    private String orderId;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String orderStatus;
    private BigDecimal totalPrice;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
}
```

- [ ] **Step 3: Create `ShipmentOrderCardDTO`**

```java
package com.gzasc.aishopping.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ShipmentOrderCardDTO {
    private String orderId;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String productType;
    private String orderStatus;
    private Timestamp orderDate;
    private String contactName;
    private String contactPhone;
    private String contactAddress;
}
```

---

### Task 2: 扩展 `OrderDetailDTO`

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/dto/OrderDetailDTO.java`

- [ ] **Step 1: 添加商品和店铺展示字段**

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
    private String shopLogoUrl;
    private String shopName;
    private String productId;
    private String productImageUrl;
    private String productName;
    private int quantity;
    private String productType;
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

### Task 3: 扩展 `OrderConverter`

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/converter/OrderConverter.java`

- [ ] **Step 1: 添加新 DTO 转换方法**

```java
package com.gzasc.aishopping.order.converter;

import com.gzasc.aishopping.common.dto.contact.ContactDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
import com.gzasc.aishopping.order.dto.*;
import com.gzasc.aishopping.order.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderConverter {

    // ==================== 用户端订单卡片 ====================

    public UserOrderCardDTO toUserCardDTO(Order order, ProductDTO product, ShopInfoDTO shop) {
        UserOrderCardDTO dto = new UserOrderCardDTO();
        dto.setOrderId(order.getOrderId());
        if (shop != null) {
            dto.setShopLogoUrl(shop.getLogoUrl());
            dto.setShopName(shop.getName());
        }
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
            dto.setProductType(product.getTags());
        }
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalPrice(order.getTotalPrice());
        return dto;
    }

    public List<UserOrderCardDTO> toUserCardDTOList(List<Order> orders, Map<String, ProductDTO> productMap, Map<Long, ShopInfoDTO> shopMap) {
        return orders.stream()
            .map(o -> {
                ProductDTO product = o.getProductId() != null ? productMap.get(o.getProductId()) : null;
                ShopInfoDTO shop = null;
                if (o.getShopId() != null) {
                    try {
                        shop = shopMap.get(Long.valueOf(o.getShopId()));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                return toUserCardDTO(o, product, shop);
            })
            .collect(Collectors.toList());
    }

    // ==================== 商家端订单卡片 ====================

    public SellerOrderCardDTO toSellerCardDTO(Order order, ProductDTO product, ContactDTO contact) {
        SellerOrderCardDTO dto = new SellerOrderCardDTO();
        dto.setOrderId(order.getOrderId());
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
        }
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setTotalPrice(order.getTotalPrice());
        if (contact != null) {
            dto.setContactName(contact.getName());
            dto.setContactPhone(contact.getPhone());
            dto.setContactAddress(contact.getAddress());
        }
        return dto;
    }

    public List<SellerOrderCardDTO> toSellerCardDTOList(List<Order> orders, Map<String, ProductDTO> productMap, Map<Integer, ContactDTO> contactMap) {
        return orders.stream()
            .map(o -> {
                ProductDTO product = o.getProductId() != null ? productMap.get(o.getProductId()) : null;
                ContactDTO contact = o.getContactId() != null ? contactMap.get(o.getContactId()) : null;
                return toSellerCardDTO(o, product, contact);
            })
            .collect(Collectors.toList());
    }

    // ==================== 发货页订单卡片 ====================

    public ShipmentOrderCardDTO toShipmentCardDTO(Order order, ProductDTO product, ContactDTO contact) {
        ShipmentOrderCardDTO dto = new ShipmentOrderCardDTO();
        dto.setOrderId(order.getOrderId());
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
            dto.setProductType(product.getTags());
        }
        dto.setQuantity(order.getQuantity());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setOrderDate(order.getOrderDate());
        if (contact != null) {
            dto.setContactName(contact.getName());
            dto.setContactPhone(contact.getPhone());
            dto.setContactAddress(contact.getAddress());
        }
        return dto;
    }

    public List<ShipmentOrderCardDTO> toShipmentCardDTOList(List<Order> orders, Map<String, ProductDTO> productMap, Map<Integer, ContactDTO> contactMap) {
        return orders.stream()
            .map(o -> {
                ProductDTO product = o.getProductId() != null ? productMap.get(o.getProductId()) : null;
                ContactDTO contact = o.getContactId() != null ? contactMap.get(o.getContactId()) : null;
                return toShipmentCardDTO(o, product, contact);
            })
            .collect(Collectors.toList());
    }

    // ==================== 订单详情扩展 ====================

    public OrderDetailDTO enrichDetailDTO(OrderDetailDTO dto, ProductDTO product, ShopInfoDTO shop, ContactDTO contactInfo, Map<String, Object> logisticsInfo) {
        if (product != null) {
            dto.setProductImageUrl(product.getImageUrl());
            dto.setProductName(product.getName());
            dto.setProductType(product.getTags());
        }
        if (shop != null) {
            dto.setShopLogoUrl(shop.getLogoUrl());
            dto.setShopName(shop.getName());
        }
        if (contactInfo != null) {
            dto.setContactName(contactInfo.getName());
            dto.setContactPhone(contactInfo.getPhone());
            dto.setContactAddress(contactInfo.getAddress());
        }
        if (logisticsInfo != null && logisticsInfo.get("data") instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) logisticsInfo.get("data");
            dto.setTrackingNumber((String) data.get("trackingNumber"));
        }
        return dto;
    }

    // ==================== 以下为保留的旧方法 ====================

    public OrderAbstractUserDTO toUserAbstractDTO(Order order) { ... }
    public List<OrderAbstractUserDTO> toUserAbstractDTOList(List<Order> orders) { ... }
    public OrderAbstractSellerDTO toSellerAbstractDTO(Order order) { ... }
    public List<OrderAbstractSellerDTO> toSellerAbstractDTOList(List<Order> orders) { ... }
    public OrderDetailDTO toDetailDTO(Order order) { ... }
    public OrderDetailDTO enrichDetailDTO(OrderDetailDTO dto, ContactDTO contactInfo, Map<String, Object> logisticsInfo) { ... }
}
```

> 旧方法保持不动，新方法以 `toUserCardDTO` / `toSellerCardDTO` / `toShipmentCardDTO` / 扩展 `enrichDetailDTO` 重载形式添加。

---

### Task 4: 扩展 `OrderMapper` 添加发货页查询

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/mapper/OrderMapper.java`

- [ ] **Step 1: 添加按店铺查询 PAID 订单的 SQL**

```java
@Select("SELECT * FROM t_order WHERE shop_id = #{shopId} AND order_status = 'PAID'")
List<Order> selectPaidOrdersByShopId(@Param("shopId") String shopId);
```

---

### Task 5: 更新 `OrderService` 接口

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/OrderService.java`

- [ ] **Step 1: 替换方法签名并新增发货列表**

```java
UserOrderCardDTO → List<UserOrderCardDTO> getOrdersByUserId(Long userId);
OrderDetailDTO → OrderDetailDTO getOrderDetailByUser(Long userId, String orderId);
SellerOrderCardDTO → List<SellerOrderCardDTO> getOrdersByShopId(String shopId);
OrderDetailDTO → OrderDetailDTO getOrderDetailByShop(String shopId, String orderId);
新增 → List<ShipmentOrderCardDTO> getShipmentOrdersByShopId(String shopId);
```

- [ ] **Step 2: 清理旧 DTO import**

接口不再引用 `OrderAbstractUserDTO` 和 `OrderAbstractSellerDTO`。

---

### Task 6: 更新 `OrderServiceImpl` 实现

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/service/impl/OrderServiceImpl.java`

- [ ] **Step 1: 实现聚合查询逻辑**

核心改动：

1. `getOrdersByUserId` — 查出订单 → 获取 ProductDTO Map(by productId) + ShopInfoDTO Map(by shopId) → toUserCardDTOList
2. `getOrdersByShopId` — 查出订单 → 获取 ProductDTO Map + ContactDTO Map(by contactId) → toSellerCardDTOList
3. `getShipmentOrdersByShopId` — 查 PAID 订单 → 同 2 → toShipmentCardDTOList
4. `getOrderDetailByUser/buildDetailDTO` — 扩展 enrichDetailDTO 重载，传入 product/shop/contact/logistics
5. `getOrderDetailByShop/buildDetailDTO` — 同上

关键：`ProductFeignClient.getProductById(Long)` 已有返回 `ProductDTO`，需按 productId 逐个调用或批量。

新增辅助方法：

```java
private Map<String, ProductDTO> buildProductMap(List<Order> orders) {
    return orders.stream()
        .map(Order::getProductId)
        .filter(id -> id != null)
        .distinct()
        .map(id -> {
            try {
                ApiResponse<ProductDTO> resp = productFeignClient.getProductById(Long.valueOf(id));
                return resp != null ? resp.getData() : null;
            } catch (Exception e) {
                log.warn("获取商品信息失败, productId={}", id, e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(p -> String.valueOf(p.getId()), p -> p, (a, b) -> a));
}

private Map<Integer, ContactDTO> buildContactMap(List<Order> orders) {
    return orders.stream()
        .map(Order::getContactId)
        .filter(Objects::nonNull)
        .distinct()
        .map(id -> {
            try {
                ApiResponse<ContactDTO> resp = contactFeignClient.getContactById(id);
                return resp != null ? Map.entry(id, resp.getData()) : null;
            } catch (Exception e) {
                log.warn("获取联系人信息失败, contactId={}", id, e);
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
```

---

### Task 7: 更新控制器

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderUserController.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/main/java/com/gzasc/aishopping/order/controller/OrderSellerController.java`

- [ ] **Step 1: `OrderUserController` — 更新 `/list` 和 `/{orderId}` 返回类型**

`/list` → `ApiResponse<List<UserOrderCardDTO>>`

- [ ] **Step 2: `OrderSellerController` — 更新 `/shop/{shopId}/list` 返回类型为 `List<SellerOrderCardDTO>`，新增 `/shop/{shopId}/shipment-list` 返回 `List<ShipmentOrderCardDTO>`**

```java
@GetMapping("/shop/{shopId}/list")
public ApiResponse<List<SellerOrderCardDTO>> listShopOrders(
        @PathVariable("shopId") String shopId) {
    List<SellerOrderCardDTO> orders = orderService.getOrdersByShopId(shopId);
    return ApiResponse.success(orders);
}

@GetMapping("/shop/{shopId}/shipment-list")
public ApiResponse<List<ShipmentOrderCardDTO>> listShipmentOrders(
        @PathVariable("shopId") String shopId) {
    List<ShipmentOrderCardDTO> orders = orderService.getShipmentOrdersByShopId(shopId);
    return ApiResponse.success(orders);
}
```

---

### Task 8: 更新测试

**Files:**
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/converter/OrderConverterTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/service/OrderServiceImplTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderUserControllerTest.java`
- Modify: `AI-Shopping-backend_Eureka/order-service/src/test/java/com/gzasc/aishopping/order/controller/OrderSellerControllerTest.java`

- [ ] **Step 1: `OrderConverterTest` 新增新 DTO 转换测试**

测试内容：
- `toUserCardDTO` 全部字段映射
- `toUserCardDTO` 当 product/shop 为 null 时不抛异常
- `toUserCardDTOList` 多元素
- `toSellerCardDTO` 全部字段映射
- `toSellerCardDTO` 当 contact 为 null 时不抛异常
- `toShipmentCardDTO` 全部字段映射
- `toShipmentCardDTO` 包含 orderDate
- `enrichDetailDTO(OrderDetailDTO, ProductDTO, ShopInfoDTO, ContactDTO, Map)` 全部字段匹配
- `enrichDetailDTO` 各种参数 null 时降级

- [ ] **Step 2: `OrderServiceImplTest` — 更新 mocking 和断言以适配新返回类型**

测试内容：
- 用户列表 mock ProductFeignClient/ShopFeignClient 返回
- 商家列表 mock ProductFeignClient/ContactFeignClient 返回
- 发货列表只包含 PAID
- Feign 异常时降级

- [ ] **Step 3: `OrderUserControllerTest` — 更新 JSON 路径断言以匹配新字段**

- [ ] **Step 4: `OrderSellerControllerTest` — 更新断言 + 新增 shipment-list 测试**

---

### Task 9: 验证编译

**Files:**
- 无改动

- [ ] **Step 1: 编译 order-service**

```bash
cd AI-Shopping-backend_Eureka
mvn compile -pl order-service -am -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

```bash
mvn test -pl order-service -am
```

Expected: All tests pass
