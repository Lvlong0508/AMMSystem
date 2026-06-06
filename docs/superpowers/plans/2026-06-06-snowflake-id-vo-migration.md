# Snowflake ID 精度修复 — VO 迁移计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 14+1 个 A 类 API 端点的 Long 类型 ID 改为 VO（View Object）方式以字符串序列化返回前端，避免 JavaScript 精度丢失。

**架构：** 对每个返回实体/DTO 给前端的 API，创建对应 VO（String 类型 ID 字段），在 Controller 中通过 Converter 将实体/DTO 转为 VO 输出。简单 ID 返回（如 `Map.of("id", id)`）直接用 `String.valueOf`。VO 是独立类，不修改实体/DTO 字段类型，内部 Feign 调用不受影响。

**审查结论（codegraph 交叉验证）：**
- Shop 实体/DTO 的 ID 字段类型全部为 `Long` ✓
- `OrderSellerController.getShopOrderDetail` 被遗漏，需加入 ✓
- Feign 调用链路安全：所有改动仅在 Controller 层，VO 独立创建不影响实体/DTO ✓
- `ShopServiceImplTest` 断言不改（VO 转换在 Controller 层，Service 返回实体不变）✓

**Tech Stack:** Java 17, Spring Boot, Jackson, Lombok

---

### 前置：补回已被回滚的修复（codegraph 确认原始状态）

> **codegraph 验证结论：** `AuthConverter.toMerchantInfoMap:32` 当前为 `merchant.getId()`（Long），`ShopMerchantController.getShopsByMerchant:32` 当前为 `List<Long>` 直接返回。需补回。

**文件：**
- 修改：`auth-service/.../converter/AuthConverter.java:32`
- 修改：`auth-service/.../controller/ShopMerchantController.java:31-33`

- [ ] **补回 AuthConverter.toMerchantInfoMap String.valueOf**
  ```java
  // AuthConverter.java:32
  info.put("id", String.valueOf(merchant.getId()));
  ```

- [ ] **补回 ShopMerchantController.getShopsByMerchant List\<String>**
  ```java
  // ShopMerchantController.java:31-33
  List<Long> shopIds = shopService.getShopIdsByMerchantId(merchantId);
  List<String> shopIdStrs = shopIds.stream().map(String::valueOf).toList();
  return ApiResponse.success(Map.of("shopIds", shopIdStrs));
  ```

- [ ] **补回相关测试断言**
  - `AuthConverterTest.java:77,96`: `assertEquals("200", result.get("id"))`
  - `ShopMerchantControllerTest.java:335-337`: `value("1")`, `value("2")`, `value("3")`

---

### Task 1: Auth 服务 — toUserInfoMap String.valueOf

**文件：**
- 修改：`auth-service/.../converter/AuthConverter.java:17`
- 修改：`auth-service/.../converter/AuthConverterTest.java:35,54`

- [ ] **toUserInfoMap 的 id 转 String**
  ```java
  // AuthConverter.java:17
  info.put("id", String.valueOf(user.getId()));
  ```

- [ ] **更新测试**
  ```java
  // AuthConverterTest.java:35, 54
  assertEquals("100", result.get("id"));
  ```

---

### Task 2: Shop 服务 — 创建 VO + Converter

**文件：**
- 创建：`shop-service/.../vo/ShopVO.java`
- 创建：`shop-service/.../vo/ShopInfoVO.java`
- 创建：`shop-service/.../service/impl/ShopConverter.java`

- [ ] **创建 ShopVO**
  ```java
  package com.gzasc.aishopping.shop.vo;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  import java.time.LocalDateTime;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ShopVO {
      private String id;
      private String merchantId;
      private String shopInfoId;
      private Integer status;
      private LocalDateTime createdAt;
      private LocalDateTime updatedAt;
  }
  ```

- [ ] **创建 ShopInfoVO**
  ```java
  package com.gzasc.aishopping.shop.vo;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ShopInfoVO {
      private String id;
      private String name;
      private String description;
      private String logourl;
  }
  ```

- [ ] **创建 ShopConverter**
  ```java
  package com.gzasc.aishopping.shop.service.impl;

  import com.gzasc.aishopping.common.dto.shop.ShopInfoDTO;
  import com.gzasc.aishopping.shop.vo.ShopInfoVO;
  import com.gzasc.aishopping.shop.vo.ShopVO;
  import com.gzasc.aishopping.shop.model.Shop;
  import org.springframework.stereotype.Component;

  @Component
  public class ShopConverter {

      public ShopVO toShopVO(Shop shop) {
          return new ShopVO(
                  String.valueOf(shop.getId()),
                  String.valueOf(shop.getMerchantId()),
                  String.valueOf(shop.getShopInfoId()),
                  shop.getStatus(),
                  shop.getCreatedAt(),
                  shop.getUpdatedAt()
          );
      }

      public ShopInfoVO toShopInfoVO(ShopInfoDTO dto) {
          if (dto == null) return null;
          return new ShopInfoVO(
                  String.valueOf(dto.getId()),
                  dto.getName(),
                  dto.getDescription(),
                  dto.getLogoUrl()
          );
      }
  }
  ```

---

### Task 3: Shop 服务 — 商家端 Controller

> codegraph 确认：`getShopWithAccessCheck` 仅在本 Controller 调用，不在 Feign 中。`Shop` 实体的 `id`/`merchantId`/`shopInfoId` 均为 Long。

**文件：** `shop-service/.../controller/ShopMerchantController.java`

- [ ] **getShop: 注入 ShopConverter，实体转 VO**
  ```java
  // 类字段注入
  private final ShopConverter shopConverter;

  // getShop 方法
  @GetMapping("/{shopId}")
  public ApiResponse<Map<String, Object>> getShop(
          @PathVariable("shopId") Long shopId,
          @RequestHeader("X-User-Id") Long userId) {
      Shop shop = shopService.getShopWithAccessCheck(shopId, userId);
      ShopVO vo = shopConverter.toShopVO(shop);
      return ApiResponse.success(Map.of("shop", vo));
  }
  ```

- [ ] **createShop: id 加 String.valueOf**
  ```java
  return ApiResponse.success("创建店铺成功", Map.of("id", String.valueOf(shop.getId())));
  ```

---

### Task 4: Shop 服务 — 用户端 Controller

> codegraph 确认：Service 返回 `Map<String, Object>` 内含实体列表/实体+DTO。Controller 需从中提取实体，转 VO 后放回。

**文件：** `shop-service/.../controller/ShopUserController.java`

- [ ] **注入 ShopConverter**
  ```java
  private final ShopConverter shopConverter;
  ```

- [ ] **getShopList: List\<Shop> → List\<ShopVO>**
  ```java
  @GetMapping("/list")
  public ApiResponse<Map<String, Object>> getShopList(
          @RequestHeader(value = "X-User-Id", required = false) Long userId,
          @RequestParam(value = "page", defaultValue = "1") int page,
          @RequestParam(value = "size", defaultValue = "10") int size) {
      if (userId == null) {
          throw new ShopException("请先登录");
      }
      Map<String, Object> result = shopService.getUserShopList(page, size);
      List<Shop> shops = (List<Shop>) result.get("shops");
      result.put("shops", shops.stream().map(shopConverter::toShopVO).toList());
      return ApiResponse.success(result);
  }
  ```

- [ ] **getShopDetail: Shop → ShopVO, ShopInfoDTO → ShopInfoVO**
  ```java
  @GetMapping("/{shopId}")
  public ApiResponse<Map<String, Object>> getShopDetail(
          @PathVariable("shopId") Long shopId,
          @RequestHeader(value = "X-User-Id", required = false) Long userId) {
      if (userId == null) {
          throw new ShopException("请先登录");
      }
      Map<String, Object> result = shopService.getActiveShopById(shopId);
      Shop shop = (Shop) result.get("shop");
      ShopInfoDTO shopInfo = (ShopInfoDTO) result.get("shopInfo");
      result.put("shop", shopConverter.toShopVO(shop));
      result.put("shopInfo", shopConverter.toShopInfoVO(shopInfo));
      return ApiResponse.success(result);
  }
  ```

---

### Task 5: Shop 服务 — 员工列表 Service 层

> codegraph 确认：`getShopEmployees` 第168-171行当前三个 ID 均为 Long 直接 put，无 String.valueOf。

**文件：** `shop-service/.../service/impl/ShopServiceImpl.java:168-171`

- [ ] **三个 Long ID 加 String.valueOf**
  ```java
  emp.put("merchantId", String.valueOf(mr.getMerchantId()));
  emp.put("shopId", String.valueOf(mr.getShopId()));
  emp.put("assignedBy", String.valueOf(mr.getAssignedBy()));
  ```

---

### Task 6: Product 服务 — 创建 VO

> codegraph 确认：`ProductWithImageAbstractDTO.id` 为 `Long`（L15），`ProductWithImageDetailDTO.id` 为 `Long`（L16），`shop.id` 为 `Long`（ShopInfoDTO L14）。两个 DTO 均标注 `@Deprecated`。命名空间无冲突。

**文件：**
- 创建：`product-service/.../vo/ShopInfoVO.java`
- 创建：`product-service/.../vo/ProductAbstractVO.java`
- 创建：`product-service/.../vo/ProductDetailVO.java`

- [ ] **创建 ShopInfoVO**
  ```java
  package com.gzasc.aishopping.product.vo;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ShopInfoVO {
      private String id;
      private String name;
      private String description;
      private String logourl;
  }
  ```

- [ ] **创建 ProductAbstractVO**
  ```java
  package com.gzasc.aishopping.product.vo;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  import java.math.BigDecimal;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ProductAbstractVO {
      private String id;
      private String name;
      private BigDecimal price;
      private String tags;
      private Integer imageId;
      private String imageUrl;
      private ShopInfoVO shop;
  }
  ```

- [ ] **创建 ProductDetailVO**
  ```java
  package com.gzasc.aishopping.product.vo;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  import java.math.BigDecimal;
  import java.util.Date;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ProductDetailVO {
      private String id;
      private String name;
      private BigDecimal price;
      private String tags;
      private String description;
      private Integer stock;
      private Boolean isSale;
      private Integer imageId;
      private String imageUrl;
      private ShopInfoVO shop;
      private Date createdAt;
      private Date updatedAt;
  }
  ```

---

### Task 7: Product 服务 — 用户端 Controller 四个端点

> codegraph 确认：四个端点分别返回 `List<ProductWithImageAbstractDTO>` 或 `ProductWithImageDetailDTO`。

**文件：** `product-service/.../controller/ProductUserController.java`

Controller 内添加 4 个私有转换方法，4 个端点调用转换后返回。

- [ ] **添加 private 转换方法**
  - `shopInfoToVO(ShopInfoDTO)` → `ShopInfoVO`
  - `toAbstractVO(ProductWithImageAbstractDTO)` → `ProductAbstractVO`
  - `toDetailVO(ProductWithImageDetailDTO)` → `ProductDetailVO`
  - list 版本 `toAbstractVOList`、`toDetailVOList`

- [ ] **/all 端点：返回 List\<ProductAbstractVO>**
  ```java
  return ApiResponse.success(Map.of("products", toAbstractVOList(products), "page", page, "size", products.size()));
  ```

- [ ] **/{productId} 端点：返回 ProductDetailVO**
  ```java
  public ApiResponse<Map<String, Object>> getProductById(...) {
      ...
      return ApiResponse.success(toDetailVO(product));
  }
  ```

- [ ] **/search 端点：返回 List\<ProductDetailVO>**
  ```java
  return ApiResponse.success(Map.of("products", toDetailVOList(products), "total", products.size()));
  ```

- [ ] **/price-range 端点：返回 List\<ProductAbstractVO>**
  ```java
  return ApiResponse.success(Map.of("products", toAbstractVOList(products), "page", page, "size", products.size()));
  ```

---

### Task 8: Product 服务 — 商家端 Controller

> codegraph 确认：`getProductDetail` 返回 `ApiResponse<ProductWithImageDetailDTO>`。

**文件：** `product-service/.../controller/ProductSellerController.java`

- [ ] **getProductDetail: DTO 转 VO 返回**
  ```java
  public ApiResponse<Map<String, Object>> getProductDetail(...) {
      ...
      return ApiResponse.success(toDetailVO(product));
  }
  ```
  添加与 `ProductUserController` 相同的 `shopInfoToVO` 和 `toDetailVO` 方法。

---

### Task 9: Order 服务 — 创建 VO + 两个 Controller

> codegraph 确认：
> - `OrderDetailDTO.userId` 是 `Long`（L10），`shopId`/`productId` 已是 `String`
> - `OrderUserController.getOrderDetail` 返回 `ApiResponse<OrderDetailDTO>`
> - **额外发现：** `OrderSellerController.getShopOrderDetail`（`GET /api/seller/order/shop/{shopId}/{orderId}`）也返回 `ApiResponse<OrderDetailDTO>`，同样需修复

**文件：**
- 创建：`order-service/.../vo/OrderDetailVO.java`
- 修改：`order-service/.../controller/OrderUserController.java`
- 修改：`order-service/.../controller/OrderSellerController.java`

- [ ] **创建 OrderDetailVO**
  ```java
  package com.gzasc.aishopping.order.vo;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  import java.math.BigDecimal;
  import java.sql.Timestamp;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class OrderDetailVO {
      private String orderId;
      private String userId;
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

- [ ] **OrderUserController.getOrderDetail: DTO → VO**
  ```java
  public ApiResponse<Map<String, Object>> getOrderDetail(...) {
      OrderDetailDTO detail = orderService.getOrderDetailByUser(userId, orderId);
      return ApiResponse.success(toOrderVO(detail));
  }
  ```

- [ ] **OrderSellerController.getShopOrderDetail: DTO → VO**
  ```java
  public ApiResponse<Map<String, Object>> getShopOrderDetail(...) {
      OrderDetailDTO detail = orderService.getOrderDetailByShop(shopId, orderId);
      return ApiResponse.success(toOrderVO(detail));
  }
  ```

- [ ] **两个 Controller 各自添加 private toOrderVO 方法**

---

### Task 10: 更新测试

> codegraph 确认：
> - `ShopServiceImplTest.getActiveShopById_active:591` 用 `assertSame(shop, ...)` 检查 Service 返回值 — **不改**（VO 转换在 Controller 层，Service 仍返回实体）
> - Controller 测试的 JSON path 断言需要更新

**文件与具体改动：**

- [ ] **ShopMerchantControllerTest.java:350-352** — 改 `value(1)` → `value("1")`, `value(1001)` → `value("1001")`
  ```java
  .andExpect(jsonPath("$.data.shop.id").value("1"))
  .andExpect(jsonPath("$.data.shop.merchantId").value("1001"))
  .andExpect(jsonPath("$.data.shop.shopInfoId").value("10"))
  .andExpect(jsonPath("$.data.shop.status").value(1))
  ```

- [ ] **ShopMerchantControllerTest.java:66** — 改 `value(10001)` → `value("10001")`
  ```java
  .andExpect(jsonPath("$.data.id").value("10001"))
  ```

- [ ] **ProductUserControllerTest.java:76,165** — 改 `value(1001)` → `value("1001")`, `value(1002)` → `value("1002")`
  ```java
  .andExpect(jsonPath("$.data.id").value("1001"))
  ```

- [ ] **ProductSellerControllerTest.java:279** — 改 `value(4001)` → `value("4001")`
  ```java
  .andExpect(jsonPath("$.data.id").value("4001"))
  ```

- [ ] **OrderUserControllerTest** — `getOrderDetail_success` 检查 `$.data.orderId` 和 `$.data.contactName`（String 字段），**不改动**

---

### Task 11: 更新 API 文档

**文件：** `后端api说明.md`

- [ ] **商家端认证 — 新增登录/注册响应示例**（merchantInfo.id 为字符串）
- [ ] **商家关联店铺 — shopIds 示例改为字符串数组**
- [ ] **商家端概述 — 顶部加 ID 类型说明**（雪花 ID 序列化为字符串）
