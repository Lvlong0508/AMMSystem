# AI-Shopping 商店服务设计文档

**日期**：2026-05-08

**功能**：新增商店服务（shop-service），实现多店铺平台，支持商家创建店铺、店员管理、商品/订单关联

---

## 第一部分：架构与数据模型

### 1.1 整体架构

```
shop-service (端口 8087)
  ├── 数据库: eureka_shop
  ├── 用户端接口: /api/user/shop/** (浏览店铺及商品)
  ├── 商家端接口: /api/seller/shop/** (店铺管理、员工管理、商品/订单关联)
  └── 内部接口: /internal/shop/** (供其他服务查询店铺关联关系)
```

### 1.2 数据库表设计

**表1：t_shop（店铺表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 店铺ID (PK, AUTO_INCREMENT) |
| name | VARCHAR(100) | 店铺名称 |
| description | TEXT | 店铺描述/公告 |
| logo_url | VARCHAR(255) | 店铺Logo |
| status | TINYINT | 0=关闭, 1=正常 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

**表2：t_merchant_role（商家店铺角色表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 记录ID (PK, AUTO_INCREMENT) |
| merchant_id | INT | 商家ID |
| shop_id | INT | 店铺ID |
| role | TINYINT | 1=店长, 2=店员 |
| assigned_by | INT | 创建人ID（店员由谁添加） |
| created_at | TIMESTAMP | 加入时间 |

**表3：t_product_shop（商品店铺关联表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 记录ID (PK, AUTO_INCREMENT) |
| product_id | INT | 商品ID |
| shop_id | INT | 店铺ID |
| created_at | TIMESTAMP | 关联时间 |

**表4：t_order_shop（订单店铺关联表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 记录ID (PK, AUTO_INCREMENT) |
| order_id | VARCHAR(50) | 订单号 |
| shop_id | INT | 店铺ID |
| created_at | TIMESTAMP | 关联时间 |

### 1.3 权限模型

| 角色 | 登录ID前缀 | 权限范围 |
|------|-----------|---------|
| 店长 | MERCHANT:{id} | 可操作自己拥有的店铺（role=1） |
| 店员 | MERCHANT:{id} | 只能操作被分配的店铺（role=2） |

**权限矩阵**：
| 操作 | 店长 (role=1) | 店员 (role=2) |
|------|---------------|--------------|
| GET 店铺/商品/订单 | ✅ | ✅ |
| PUT 店铺/商品 | ✅ | ❌ |
| DELETE 商品 | ✅ | ❌ |
| GET 店员列表 | ✅ | ❌ |
| POST/DELETE 店员 | ✅ | ❌ |

---

## 第二部分：API 设计

### 2.1 商家端 API（/api/seller/shop/**）

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/seller/shop/register` | POST | 创建店铺 | 已登录商家 |
| `/api/seller/shop/list` | GET | 获取当前用户有权限的所有店铺列表 | 已登录商家 |
| `/api/seller/shop/{shopId}` | GET | 获取店铺详情 | 店铺相关商家 |
| `/api/seller/shop/{shopId}` | PUT | 更新店铺信息 | 店长 |
| `/api/seller/shop/{shopId}` | DELETE | 关闭店铺 | 店长 |
| `/api/seller/shop/{shopId}/products` | GET | 查询店铺商品列表 | 店铺相关商家 |
| `/api/seller/shop/{shopId}/products` | POST | 创建商品 | 店长 |
| `/api/seller/shop/{shopId}/products/{productId}` | GET | 查询商品详情 | 店铺相关商家 |
| `/api/seller/shop/{shopId}/products/{productId}` | PUT | 更新商品 | 店长 |
| `/api/seller/shop/{shopId}/products/{productId}` | DELETE | 删除商品 | 店长 |
| `/api/seller/shop/{shopId}/orders/all` | GET | 查询店铺订单列表 | 店铺相关商家 |
| `/api/seller/shop/{shopId}/orders/{orderId}` | GET | 查询订单详情 | 店铺相关商家 |
| `/api/seller/shop/{shopId}/employees` | GET | 查询店员列表 | 店长 |
| `/api/seller/shop/{shopId}/employees/register` | POST | 添加店员 | 店长 |
| `/api/seller/shop/{shopId}/employees/{merchantId}` | DELETE | 移除店员 | 店长 |

### 2.2 用户端 API（/api/user/shop/**）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/user/shop/list` | GET | 浏览店铺列表（分页） |
| `/api/user/shop/{shopId}` | GET | 查看店铺详情 |
| `/api/user/shop/{shopId}/products` | GET | 查看店铺商品（分页） |
| `/api/user/shop/{shopId}/products/{productId}` | GET | 查看商品详情 |

### 2.3 内部接口（/internal/shop/**）

| 端点 | 方法 | 说明 | 调用方 |
|------|------|------|--------|
| `/internal/shop/shop-id-by-product/{productId}` | GET | 根据商品ID查店铺ID | order-service |
| `/internal/shop/check-owner/{shopId}/{merchantId}` | GET | 是否为店长 | 各服务 |
| `/internal/shop/check-access/{shopId}/{merchantId}` | GET | 是否有店铺访问权 | 各服务 |
| `/internal/shop/associate-order` | POST | 关联订单到店铺 | order-service |

### 3.1 业务流程：注册商家 + 创建店铺

```
Step 1: 前端 POST /api/seller/auth/register (创建商家账号，现有接口)
Step 2: 前端 POST /api/seller/shop/register (创建店铺)
        → shop-service: 写入 t_shop + t_merchant_role (role=1 店长)
```

### 3.2 业务流程：添加店员

```
POST /api/seller/shop/{shopId}/employees/register
Body: { username, password?, phone? }

→ shop-service 调用 auth-service /internal/auth/register-employee
→ 写入 t_merchant_role (role=2, shopId, assignedBy=当前商家ID)
→ 返回 "店员添加成功"
```

### 3.3 业务流程：创建商品

```
POST /api/seller/shop/{shopId}/products
Body: { name, price, description?, stock?, tags? }

→ shop-service 调用 product-service /internal/product/create
→ 获取返回的 productId
→ 写入 t_product_shop (productId, shopId)
→ 返回商品完整信息
```

### 3.4 业务流程：查询店铺订单列表

```
GET /api/seller/shop/{shopId}/orders/all

→ shop-service 查询 t_order_shop WHERE shopId = ? 获取 orderId 列表
→ 调用 order-service /internal/order/batch(orderIds)
→ 返回订单列表
```

### 3.5 业务流程：下单时关联订单到店铺

```
POST /api/user/order/place

→ order-service 创建订单
→ order-service 调用 shop-service /internal/shop/shop-id-by-product/{productId}
→ order-service 调用 shop-service /internal/shop/associate-order(orderId, shopId)
→ shop-service 写入 t_order_shop (orderId, shopId)
→ 创建订单完成
```

### 3.6 补偿逻辑（关键路径）

**创建商品补偿逻辑**：
```
POST /api/seller/shop/{shopId}/products
Body: { name, price, ... }

1. shop-service 调用 product-service /internal/product/create
2. 获取 productId
3. 尝试写入 t_product_shop
   - 如果失败：调用 product-service /internal/product/{id} 删除商品（回滚）
4. 返回结果
```

**添加店员补偿逻辑**：
```
POST /api/seller/shop/{shopId}/employees/register
Body: { username, password?, phone? }

1. shop-service 调用 auth-service /internal/auth/register-employee
2. 如果 auth 成功但后续失败：
   - 调用 auth-service 的密码重置接口清理账号
   - 或标记账号为"待激活"状态，由定时任务清理
```

### 3.6 网关路由配置新增

```yaml
# gateway-service/application.yml
- id: seller-shop
  uri: lb://shop-service
  predicates: Path=/api/seller/shop/**
- id: user-shop
  uri: lb://shop-service
  predicates: Path=/api/user/shop/**
- id: internal-shop
  uri: lb://shop-service
  predicates: Path=/internal/shop/**
  filters: StripPrefix=1
```

### 3.7 认证与白名单配置

**说明**：
- `/api/seller/shop/register` 加入白名单的含义是：**跳过店铺权限检查**（商家首次创建店铺时无 t_merchant_role 记录），但**必须已登录**（Token 验证仍然执行）
- 网关先校验 Token，有效后再放行到 shop-service

```java
// SaTokenAuthGlobalFilter 白名单说明
// 白名单路径：跳过店铺权限校验，但仍需已登录
 whitelist.add("/api/seller/shop/register");
```

---

## 第四部分：公共模块新增（common-api）

### 4.1 新增 DTO

```java
// ShopDTO
public class ShopDTO {
    private Integer id;
    private String name;
    private String description;
    private String logoUrl;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// MerchantRoleDTO
public class MerchantRoleDTO {
    private Integer id;
    private Integer merchantId;
    private Integer shopId;
    private Integer role;
    private Integer assignedBy;
    private LocalDateTime createdAt;
}

// ProductShopDTO
public class ProductShopDTO {
    private Integer id;
    private Integer productId;
    private Integer shopId;
    private LocalDateTime createdAt;
}
```

### 4.2 新增 Feign Client

```java
// ShopFeignClient (common-api)
@FeignClient(name = "shop-service")
public interface ShopFeignClient {
    @GetMapping("/internal/shop/shop-id-by-product/{productId}")
    Integer getShopIdByProductId(@PathVariable Integer productId);
    
    @GetMapping("/internal/shop/check-owner/{shopId}/{merchantId}")
    Boolean checkOwner(@PathVariable Integer shopId, @PathVariable Integer merchantId);
    
    @GetMapping("/internal/shop/check-access/{shopId}/{merchantId}")
    Boolean checkAccess(@PathVariable Integer shopId, @PathVariable Integer merchantId);
}

// AuthFeignClient (新增内部接口)
@FeignClient(name = "auth-service")
public interface AuthFeignClient {
    @PostMapping("/internal/auth/register-employee")
    Result registerEmployee(@RequestBody MerchantRegisterRequest request);
}

// ProductInternalFeignClient (新增内部接口)
@FeignClient(name = "product-service")
public interface ProductInternalFeignClient {
    @PostMapping("/internal/product/create")
    ProductDTO createProduct(@RequestBody ProductDTO request);
    
    @GetMapping("/internal/product/{id}")
    ProductDTO getProduct(@PathVariable Integer id);
    
    @PutMapping("/internal/product/{id}")
    ProductDTO updateProduct(@PathVariable Integer id, @RequestBody ProductDTO request);
    
    @DeleteMapping("/internal/product/{id}")
    void deleteProduct(@PathVariable Integer id);
}

// OrderInternalFeignClient (新增内部接口)
@FeignClient(name = "order-service")
public interface OrderInternalFeignClient {
    @GetMapping("/internal/order/shop-id-by-product/{productId}")
    Integer getShopIdByProductId(@PathVariable Integer productId);
    
    @GetMapping("/internal/order/batch")
    List<OrderDTO> getOrdersByIds(@RequestParam List<String> orderIds);
}
```

---

## 第五部分：前端改造

### 5.1 商家端前端（frontier-seller）

**新增页面**：
| 路由 | 组件 | 说明 |
|------|------|------|
| `/shop/register` | ShopRegister.vue | 创建店铺 |
| `/shop/list` | ShopList.vue | 店铺列表 |
| `/shop/my-shop` | MyShop.vue | 店员查看所属店铺 |
| `/shop/:shopId` | ShopDetail.vue | 店铺详情/编辑 |
| `/shop/:shopId/products` | ShopProducts.vue | 店铺商品管理 |
| `/shop/:shopId/orders` | ShopOrders.vue | 店铺订单管理 |
| `/shop/:shopId/employees` | ShopEmployees.vue | 店员管理 |

**API 调用**（shop.js 新增）：
```javascript
// 新建 src/api/shop.js
import axios from './request'

export const shopApi = {
  register: (data) => axios.post('/api/seller/shop/register', data),
  list: () => axios.get('/api/seller/shop/list'),
  myShopId: () => axios.get('/api/seller/shop/my-shop-id'),
  detail: (shopId) => axios.get(`/api/seller/shop/${shopId}`),
  update: (shopId, data) => axios.put(`/api/seller/shop/${shopId}`, data),
  delete: (shopId) => axios.delete(`/api/seller/shop/${shopId}`),
  products: (shopId) => axios.get(`/api/seller/shop/${shopId}/products`),
  createProduct: (shopId, data) => axios.post(`/api/seller/shop/${shopId}/products`, data),
  updateProduct: (shopId, productId, data) => axios.put(`/api/seller/shop/${shopId}/products/${productId}`, data),
  deleteProduct: (shopId, productId) => axios.delete(`/api/seller/shop/${shopId}/products/${productId}`),
  orders: (shopId) => axios.get(`/api/seller/shop/${shopId}/orders/all`),
  orderDetail: (shopId, orderId) => axios.get(`/api/seller/shop/${shopId}/orders/select/${orderId}`),
  employees: (shopId) => axios.get(`/api/seller/shop/${shopId}/employees`),
  registerEmployee: (shopId, data) => axios.post(`/api/seller/shop/${shopId}/employees/register`, data),
  removeEmployee: (shopId, merchantId) => axios.delete(`/api/seller/shop/${shopId}/employees/${merchantId}`),
}
```

---

## 第六部分：实施顺序

### Phase 1: 基础服务搭建
1. 创建 shop-service 模块（Maven）
2. 配置数据库连接（eureka_shop）
3. 实现 Controller、Service、Mapper 基础 CRUD

### Phase 2: 公共模块
4. 在 common-api 新增 DTO 和 Feign Client

### Phase 3: 内部接口
5. auth-service 新增 /internal/auth/register-employee
6. product-service 新增 /internal/product/create|update|delete|batch

### Phase 4: 网关与现有服务改造
7. gateway-service 新增 shop-service 路由规则
8. order-service 改造：下单时调用 shop-service 内部接口关联订单

### Phase 5: 前端实现
9. 商家端前端页面开发

---

**设计完成，等待用户确认后进入实现阶段。**