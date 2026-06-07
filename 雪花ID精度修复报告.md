# 雪花 ID 精度修复报告

## 背景

所有服务使用雪花算法（Snowflake）生成 64 位 Long 类型 ID（如 `2062474586787811328`），超出 JavaScript `Number.MAX_SAFE_INTEGER`（9,007,199,254,740,991）。Jackson 默认将 Long 序列化为 JSON 数字，前端解析时发生精度丢失。

```
后端 Long(2062474586787811328) → Jackson → JSON 数字(2062474586787811000) → JS 解析
```

精度丢失的 ID 若被前端传回后端做匹配（路径参数、请求参数），将导致查询失败或权限校验错误。

---

## 修复策略

### 后端：Controller 层返回 String 类型 ID

所有 Controller 在返回给前端时，将 `Long` ID 转为 `String`，确保 JSON 中 ID 以字符串形式传输，前端无损接收。

| 服务 | 文件 | 改动 |
|------|------|------|
| **auth-service** | `AuthConverter.java` | `String.valueOf(merchant.getId())` 等 2 处 |
| **shop-service** | `ShopMerchantController.java` | 店铺列表、详情、创建 3 端点，shopIds 转 String |
| | `ShopUserController.java` | 列表和详情端点，Shop → ShopVO / ShopInfoVO |
| | `ShopServiceImpl.java` | 员工查询，merchantId/shopId/assignedBy 转 String |
| | `ShopConverter.java` | 新增，统一 Shop → VO 转换 |
| **product-service** | `ProductUserController.java` | 4 个端点 DTO → `Map<String, Object>`，ID 字段转 String |
| | `ProductSellerController.java` | 详情端点同上 |
| **order-service** | `OrderUserController.java` | 详情端点 userId 转 String |
| | `OrderSellerController.java` | 详情端点 userId 转 String |

**新增 VO 类：**
- `shop-service`: `ShopVO.java`, `ShopInfoVO.java`, `ShopConverter.java`
- `product-service`: `ProductAbstractVO.java`, `ProductDetailVO.java`, `ShopInfoVO.java`
- `order-service`: `OrderDetailVO.java`

**新增功能（后续迭代）：**
- `ProductUserController` / `ProductSellerController`: 新增 `GET /shop/{shopId}` 按店铺查询商品接口
- `ProductMapper`: 新增 `selectByShopId` 查询

---

### 前端：全链路 String 化

所有从后端接收的 ID 在前端强制转为 String，所有传给后端的 ID 也保持 String 形式。

| 文件 | 改动 |
|------|------|
| `store/shop.js` | `String()` 包裹所有 shop ID，防止 currentShopId 被覆盖 |
| `api/shop.js` | `String()` 包裹所有 shopId URL 参数 |
| `api/product.js` | `updateProduct` 改为接收 FormData，设置 multipart header |
| `api/request.js` | 拦截器从 `merchantId` 取 `X-User-Id`（而非 `merchantInfo.id`） |
| `AppTopBar.vue` | `selectedShopId` setter 中 `String()` 强转 |
| `ShopProducts.js` | 移除 `Number(shopId.value)`，所有 ID 使用 `String()` 包裹 |

---

## 涉及提交

| Commit | 说明 |
|--------|------|
| `17d91d1` | fix(contact-service): user_id INT→BIGINT 适配 auth 雪花 ID |
| `4f6db88` | refactor(shop): Model 层 ID 改为 Long，role 改为 Integer |
| `e279758` | refactor(shop): Service 层 ID 改为 Long，UUID 改用 Snowflake |
| `ce35846` | refactor(common): Feign 接口适配 shop 的 Long 类型 ID |
| `0d09942` | refactor: 登录流程、无店铺 UX、注册页面、前端全链路精度修复 |
| `7a76fea` | refactor: 后端 Long→String VO 层迁移（核心修复） |
| `89d82d2` | feat: 新增按 shopId 查商品 API + 文档/前端更新 |
| `192f263` | fix: 创建商品时 shopId 精度丢失 - 移除 Number() 包装 |

---

## 修复清单总览

### 后端（7 个服务，约 30 个文件）

- [x] **auth-service**: MerchantInfo/UserInfo ID 转 String
- [x] **shop-service**: Model/Service/Controller 全链路 Long 化 + Snowflake + VO 层
- [x] **product-service**: Controller 返回 Map 替代 DTO，ID 字段转 String
- [x] **order-service**: 详情端点 userId 转 String
- [x] **common-api**: Feign 接口适配 Long ID
- [x] **contact-service**: SQL user_id INT→BIGINT

### 前端（frontier-seller，约 15 个文件）

- [x] store: shop ID 全量 String()
- [x] API: 所有 shopId 参数 String()
- [x] view: Number() 替换为 String() / 直接传字符串
- [x] component: AppTopBar selectedShopId String() 强转
- [x] request 拦截器: X-User-Id 来源修正

---

## 验证

- 后端 32 个 Controller 测试全部通过
- 覆盖：auth/shop/product/order 各 Controller 的 snowflake ID 返回格式
