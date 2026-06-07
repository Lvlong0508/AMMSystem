# 商家账号 ↔ 店铺 一对一重构设计

> 日期：2026-06-07
> 范围：后端（auth-service / shop-service / common-api / SQL）+ 商家端前端（frontier-seller）
> 状态：待用户审核 → 待 writing-plans

## 1. 目标 & 背景

将「一个商家账号可对应多个店铺（通过中间表 + 角色）」重构为「一个商家账号仅能对应一个店铺，且无员工概念」。

### 现状

- `auth_db.t_merchant`：商家账号（雪花 ID）
- `shop_db.shops`：店铺，`merchant_id` 字段标识创建者
- `shop_db.merchant_roles`：多对多中间表
  - `role=1` 店长
  - `role=2` 店员
  - 一名 `merchant_id` 可关联多家 shop
- `shop-service` 提供员工增删查 API（`/api/seller/shop/{id}/employees[/{merchantId}]`）
- 商家注册与店铺创建是**两步**：先调 `auth-service` 注册账号，再调 `shop-service` 创建店铺

### 问题

- 「店长 vs 店员」对当前业务是过度设计
- 多对多引入 `merchant_roles` 维护成本、并发问题
- 商家注册 → 店铺创建分两步，体验割裂

### 重构后

- 一名 `merchant_id` 最多拥有一个 `shops` 记录（DB UNIQUE 约束）
- 无员工概念
- 商家注册与店铺创建合并为**一次 HTTP 请求**（前端两个表单 → auth-service → Feign 调 shop-service 内部接口）

---

## 2. 数据模型

```
auth_db.eureka_auth
├─ t_user           (不变)
├─ user_info        (不变)
├─ t_merchant       (不变；唯一性约束已存在)
└─ merchant_info    (不变)

shop_db.eureka_shop
├─ shops            ✏️  增加 UNIQUE KEY uk_merchant_id (merchant_id)
├─ shop_info        (不变)
└─ merchant_roles   ❌  删除整张表
```

`shop-init.sql` 关键变更：

```sql
DROP TABLE IF EXISTS merchant_roles;

CREATE TABLE IF NOT EXISTS shops (
    id BIGINT PRIMARY KEY COMMENT '店铺ID（雪花算法生成）',
    merchant_id BIGINT NOT NULL COMMENT '商户ID（雪花算法）',
    shop_info_id BIGINT COMMENT '关联 ShopInfo ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-正常 0-已关闭',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (`status`),
    UNIQUE KEY uk_merchant_id (merchant_id)            -- 新增
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';
```

> 数据库尚未真正创建，无需数据迁移脚本。

---

## 3. API 变化

### 3.1 删除的 API（shop-service 商家端）

| 方法 | 路径 | 原因 |
|---|---|---|
| GET | `/api/seller/shop/{shopId}/employees` | 不再有员工 |
| POST | `/api/seller/shop/{shopId}/employees` | 不再有员工 |
| DELETE | `/api/seller/shop/{shopId}/employees/{merchantId}` | 不再有员工 |

### 3.2 保留并简化的 API（shop-service 商家端）

| 方法 | 路径 | 改动 |
|---|---|---|
| GET | `/api/seller/shop/my-shops` → 改 `/api/seller/shop/my-shop` | 返回**单个**店铺对象（语义变单数） |
| GET | `/api/seller/shop/{shopId}` | 内部用 `merchantId` 定位；可保留路径参数做兼容 |
| PUT | `/api/seller/shop/{shopId}` | 同上 |
| PATCH | `/api/seller/shop/{shopId}/close` | 同上 |
| PATCH | `/api/seller/shop/{shopId}/open` | 同上 |

### 3.3 删除的 API（shop-service 商家端）

`POST /api/seller/shop/register` —— 合并到 `auth-service` 的注册流程。

### 3.4 新增的 API（shop-service 内部）

```
POST /api/shop/internal/create-for-merchant
  Headers: X-Internal-Source: auth-service  （可选审计）
  Body: {
    "merchantId": 1234567890,
    "name": "我的小店",
    "description": "...",        // 可选
    "logoUrl": "http://..."      // 可选
  }
  Response 200: { "code": 200, "data": { "id": "16718..." } }
  Response 409: { "code": 409, "message": "该商家已绑定店铺" }
  Response 500: 系统错误
```

注册到 `InternalShopController`（沿用 `controller/internal/` 目录），仅供服务间 Feign 调用，不走 Gateway。

### 3.5 修改的 API（auth-service）

**POST `/api/seller/auth/register`**

旧请求体：
```json
{ "username", "password", "phone", "email", "merchantId" }
```

新请求体（一次提交账号 + 店铺）：
```json
{
  "username": "shop_owner_01",
  "password": "pass123",
  "phone": "13800138000",
  "email": "owner@example.com",
  "shop": {
    "name": "我的小店",
    "description": "...",
    "logoUrl": "http://..."
  }
}
```

**响应**（成功）：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "xxx",
    "accountType": "MERCHANT",
    "merchantInfo": {
      "id": "2062474586787811328",
      "username": "shop_owner_01",
      "phone": "13800138000",
      "email": "owner@example.com",
      "nickname": null
    },
    "shopId": "167184879099904"
  }
}
```

**响应**（shop 失败）：
```json
{ "code": 500, "message": "店铺创建失败，请稍后重试" }
```
此时 auth 侧账号已回滚，用户可重新注册。

---

## 4. 跨服务调用与失败处理

### 4.1 调用链路

```
Seller Frontend
    │  POST /api/seller/auth/register
    ▼
Gateway (8080)  路由不变：/api/seller/auth/**
    │
    ▼
auth-service (8086)
    │  ① 校验 username/phone 唯一性
    │  ② @Transactional → INSERT t_merchant + merchant_info
    │  ③ 生成 Sa-Token
    │  ④ Feign POST /api/shop/internal/create-for-merchant
    │     ├─ 成功：组装响应返回
    │     └─ 失败：catch → 清理 auth 记录 + 抛 500
    ▼
shop-service (8087)
       @Transactional → INSERT shops + shop_info
       UNIQUE(merchant_id) 防止重复
```

### 4.2 失败处理表

| 失败点 | 处理 |
|---|---|
| ① username/phone 重复 | 400，不调 shop |
| ② BCrypt/INSERT t_merchant 失败 | @Transactional 自动回滚 |
| ④ Feign 超时/异常 | catch → DELETE t_merchant + merchant_info → 抛 500 |
| ④ shop 返回 409 (已开店) | catch → 同上清理 + 抛 500（前端提示"账号已存在"） |
| ④ shop 返回 4xx/5xx | 同上 |

### 4.3 事务边界

- **auth-service 步骤 ①②**：`@Transactional`（t_merchant + merchant_info 一起）
- **auth-service 步骤 ④**：单独方法，**不加事务**（Feign 调用不应被事务持有）
- **shop-service 步骤**：@Transactional（shops + shop_info 一起）
- 跨服务采用「补偿式回滚」代替分布式事务

### 4.4 并发安全

| 场景 | 保护 |
|---|---|
| 同 username 并发注册 | `t_merchant.username UNIQUE` |
| 同 merchantId 并发创建 shop | `shops.uk_merchant_id UNIQUE`，返回 409 |

### 4.5 Feign Client 设计（common-api）

```java
@FeignClient(name = "shop-service", path = "/api/shop/internal")
public interface ShopInternalFeignClient {
    @PostMapping("/create-for-merchant")
    ApiResponse<Map<String, Object>> createShopForMerchant(
        @RequestHeader("X-Internal-Source") String source,
        @RequestBody CreateShopForMerchantRequest request
    );
}
```

DTO：
```java
@Data
public class CreateShopForMerchantRequest {
    private Long merchantId;
    @NotBlank private String name;
    private String description;
    private String logoUrl;
}
```

---

## 5. 前端影响（frontier-seller）

| 文件 | 改动 |
|---|---|
| `src/views/Register/Register.vue` | 增加店铺信息子表单（name/description/logoUrl） |
| `src/views/Register/Register.js` | 新增 `shop` 字段组装与提交 |
| `src/views/ShopList/ShopList.vue` | 重命名为「我的店铺」单页，去掉店铺选择器 |
| `src/views/ShopList/ShopList.js` | 适配 `my-shop` 单数接口 |
| `src/views/ShopEmployees/ShopEmployees.vue` | **删除文件** |
| `src/views/ShopEmployees/ShopEmployees.js` | **删除文件** |
| `src/views/ShopEmployees/Text.js` | **删除文件** |
| `src/api/shop.js` | `my-shops` → `my-shop` |
| `src/store/shop.js` | 去掉多店铺 store 结构，改用单店铺对象 |
| `src/router/index.js` | 去掉 `/shop-list` 多店路由，改为 `/shop`（单数） |

> 用户端（frontier-user）不受影响。

---

## 6. Java 代码变更清单

### shop-service
- ❌ 删除：
  - `model/MerchantRole.java`
  - `mapper/MerchantRoleMapper.java`（含其 xml/注解 SQL）
  - `service/MerchantRoleService.java`
  - `service/impl/MerchantRoleServiceImpl.java`
  - 测试 `service/impl/MerchantRoleServiceImplTest.java`
  - 测试 `mapper/MerchantRoleMapperTest.java`
- ✏️ 改 `ShopServiceImpl`：去掉 `merchantRoleService` 依赖；移除 `addEmployee/removeEmployee`；`getSimpleShop` 改名为 `getMyShop`；原 `createShop` 移到 `InternalShopController.createForMerchant` 由 auth 调用
- ✏️ 改 `ShopMerchantController`：删除 3 个 employee 端点
- ✏️ 改 `ShopMapper`：删除 `selectShopsByUserId`（依赖 `merchant_roles`），新增 `selectShopByMerchantId`（用 UNIQUE 约束保证唯一）
- ➕ 新增：`controller/internal/InternalShopController.createForMerchant`
- ➕ 新增：`dto/CreateShopForMerchantRequest` DTO

### auth-service
- ✏️ 改 `MerchantAuthController.register`：接受新请求体
- ➕ 新增：`dto/MerchantRegisterRequest` DTO（含 `shop` 嵌套对象 + Bean Validation）
- ✏️ 改 `MerchantAuthServiceImpl.register`：合并流程 + Feign 调用 + 失败回滚
- ➕ 注入：`ShopInternalFeignClient`

### common-api
- ❌ 删除：`dto/shop/MerchantRoleDTO.java`（仅被旧 service 引用）
- ➕ 新增：`feign/shop/ShopInternalFeignClient` 接口
- ➕ 新增：`dto/shop/CreateShopForMerchantRequest` DTO
- ➕ 新增：`dto/auth/MerchantRegisterRequest` DTO（含 `shop` 嵌套）

### 测试
- 保留并扩充：
  - `MerchantAuthServiceImplTest` —— 合并注册成功、shop 失败回滚
  - `InternalShopControllerTest` —— 成功创建、重复 merchantId 返回 409
  - `ShopServiceImplTest` —— 单店铺查询 / 更新 / 关闭 / 开启
- 删除：
  - `MerchantRoleServiceImplTest`
  - `MerchantRoleMapperTest`
  - `ShopMerchantControllerTest` 中 employee 相关用例

---

## 7. 测试策略

### 单元 / 集成测试
1. **合并注册成功**：mock `ShopInternalFeignClient` 正常返回 → 验证 t_merchant + shops 各 1 条
2. **shop 失败回滚**：mock Feign 抛异常 → 验证 t_merchant 不存在
3. **重复 merchantId**：直连 InternalShopController，第二次相同 merchantId → 409
4. **单店铺查询**：GET `/api/seller/shop/my-shop` → 返回 1 个 shop 对象
5. **DB UNIQUE 约束**：并发两次 `POST /api/shop/internal/create-for-merchant` → 1 成功 1 失败

### 手动 e2e
1. `start-end.bat` 启动后端 → 启动 seller 前端
2. 注册新商家（含店铺信息） → 验证 DB 中两条记录
3. 登录 → 进「我的店铺」 → 显示 1 家
4. 用 MySQL Workbench 再次 INSERT 同 merchant_id → 验证 UNIQUE 报错

---

## 8. 风险与回滚

| 风险 | 缓解 |
|---|---|
| Feign 调用慢导致用户感知差 | Feign timeout 3s + 友好错误提示 |
| shop 失败留下孤儿 t_merchant | catch 中清理 + 后续可加对账脚本 |
| 删除 merchant_roles 导致其他服务报错 | 搜索 `merchant_roles`/`MerchantRole` 引用，全量清理（已识别只有 shop-service） |
| 前端 Register.vue 没改完 | 编译 + 浏览器 smoke test |

**回滚方案**：
- 保留旧 `shop-init.sql` 备份
- git revert 即可

---

## 9. 后续（本次不做）

- 商家转让店铺（需新流程，暂不做）
- t_merchant 加 `shop_bound` 字段做最终一致（暂不做）
- 定期对账脚本（孤儿账号清理，暂不做）
