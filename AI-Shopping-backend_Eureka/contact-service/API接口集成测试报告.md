# Contact 服务 API 接口集成测试报告

## 1. 测试概述

| 项目 | 内容 |
|------|------|
| 测试目标 | 验证 Contact 服务（用户联系人 + 商家地址）全部 REST API 端点的功能正确性和容错能力 |
| 测试类型 | API 接口集成测试（端到端，通过嵌入式 Tomcat + RANDOM_PORT） |
| 测试日期 | 2026-05-29 |
| 测试框架 | JUnit 5 + SpringBootTest + TestRestTemplate |
| 测试类 | `UserContactApiTest` (21 用例) / `MerchantContactApiTest` (14 用例) |

## 2. 测试环境

| 组件 | 地址 | 状态 |
|------|------|:----:|
| MySQL | localhost:3306 | ✅ 运行中 |
| Contact Service | 嵌入式 Tomcat RANDOM_PORT | ✅ 运行中 |

### 路由链路

```
TestRestTemplate
  → http://localhost:{port}/api/user/contact/*
  → Contact Controller (Controller → Service → Mapper → MySQL)
```

### 数据隔离

- 每测试方法使用独立 userId / shopId（`System.nanoTime()` + 固定前缀）
- 不依赖预置测试数据，每用例自建自用

## 3. 测试用例及结果

### 3.1 用户联系人 API（`/api/user/contact`）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 1 | 正常创建联系人 | POST | `/api/user/contact/create` | 200 "创建地址成功" | 200 "创建地址成功" | ✅ |
| 2 | 缺少 X-User-Id | POST | `/api/user/contact/create` | 401 "未登录" | 401 "未登录" | ✅ |
| 3 | name 为空 | POST | `/api/user/contact/create` | 400 | 400 | ✅ |
| 4 | phone 为空 | POST | `/api/user/contact/create` | 400 | 400 | ✅ |
| 5 | address 为空 | POST | `/api/user/contact/create` | 400 | 400 | ✅ |
| 6 | 正常删除联系人 | DELETE | `/api/user/contact/delete/{id}` | 200 "删除地址成功" | 200 "删除地址成功" | ✅ |
| 7 | 缺少 X-User-Id | DELETE | `/api/user/contact/delete/{id}` | 401 | 401 | ✅ |
| 8 | 删除不存在的联系人 | DELETE | `/api/user/contact/delete/99999` | 400 | 400 | ✅ |
| 9 | 正常更新联系人 | PUT | `/api/user/contact/update` | 200 "更新地址成功" | 200 "更新地址成功" | ✅ |
| 10 | 缺少 X-User-Id | PUT | `/api/user/contact/update` | 401 | 401 | ✅ |
| 11 | id 为空 | PUT | `/api/user/contact/update` | 400 | 400 | ✅ |
| 12 | 更新不存在的联系人 | PUT | `/api/user/contact/update` | 400 | 400 | ✅ |
| 13 | 更新不属于当前用户的联系人 | PUT | `/api/user/contact/update` | 400 | 400 | ✅ |
| 14 | 查询列表（有数据） | GET | `/api/user/contact/list` | 200 | 200 | ✅ |
| 15 | 查询列表（无数据） | GET | `/api/user/contact/list` | 200 | 200 | ✅ |
| 16 | 缺少 X-User-Id | GET | `/api/user/contact/list` | 401 | 401 | ✅ |
| 17 | 正常设置默认联系人 | PUT | `/api/user/contact/set-default/{id}` | 200 "设置成功" | 200 "设置成功" | ✅ |
| 18 | 缺少 X-User-Id | PUT | `/api/user/contact/set-default/{id}` | 401 | 401 | ✅ |
| 19 | 设置不存在的联系人 | PUT | `/api/user/contact/set-default/99999` | 400 | 400 | ✅ |
| 20 | 重复设置默认（幂等） | PUT | `/api/user/contact/set-default/{id}` | 200 | 200 | ✅ |
| 21 | 设置不属于当前用户的联系人 | PUT | `/api/user/contact/set-default/{id}` | 400 | 400 | ✅ |

### 3.2 商家地址 API（`/api/merchant/address`）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 1 | 正常创建（addressType=1, isDefault=0） | POST | `/api/merchant/address/create` | 200 "新增成功" | 200 "新增成功" | ✅ |
| 2 | 正常创建（isDefault=1） | POST | `/api/merchant/address/create` | 200 | 200 | ✅ |
| 3 | 缺少 X-Shop-Id | POST | `/api/merchant/address/create` | 401 | 401 | ✅ |
| 4 | addressType 为空 | POST | `/api/merchant/address/create` | 400 | 400 | ✅ |
| 5 | 正常更新地址 | PUT | `/api/merchant/address/update/{id}` | 200 "修改成功" | 200 "修改成功" | ✅ |
| 6 | 缺少 X-Shop-Id | PUT | `/api/merchant/address/update/{id}` | 401 | 401 | ✅ |
| 7 | 正常删除地址 | DELETE | `/api/merchant/address/delete/{id}` | 200 "删除成功" | 200 "删除成功" | ✅ |
| 8 | 缺少 X-Shop-Id | DELETE | `/api/merchant/address/delete/{id}` | 401 | 401 | ✅ |
| 9 | 查询地址列表 | GET | `/api/merchant/address/list` | 200 | 200 | ✅ |
| 10 | 缺少 X-Shop-Id | GET | `/api/merchant/address/list` | 401 | 401 | ✅ |
| 11 | 查询默认发货地址 | GET | `/api/merchant/address/ship-default` | 200 | 200 | ✅ |
| 12 | 缺少 X-Shop-Id | GET | `/api/merchant/address/ship-default` | 401 | 401 | ✅ |
| 13 | 正常设置默认地址 | PUT | `/api/merchant/address/set-default/{id}` | 200 "设置成功" | 200 "设置成功" | ✅ |
| 14 | 缺少 X-Shop-Id | PUT | `/api/merchant/address/set-default/{id}` | 401 | 401 | ✅ |

## 4. 测试结果统计

### 4.1 API 集成测试

| 模块 | 测试文件 | 总用例 | 通过 | 失败 | 通过率 |
|------|----------|:------:|:----:|:----:|:------:|
| 用户联系人 API | `UserContactApiTest.java` | 21 | 21 | 0 | **100%** |
| 商家地址 API | `MerchantContactApiTest.java` | 14 | 14 | 0 | **100%** |
| **合计** | | **35** | **35** | **0** | **100%** |

### 4.2 全部单测（含 controller / service / mapper / API）

| 模块 | 测试数 | 通过 | 失败 | 通过率 |
|------|:------:|:----:|:----:|:------:|
| contact-service | 191 | 189 | 2* | 99.0% |
| 其余模块（auth/logistics/order/product/shop + gateway） | — | 全部通过 | 0 | 100% |

*\* `UserContactMapperTest$SelectTests` 的 2 个失败为既有问题（连接真实 MySQL），与本次改动无关*

## 5. 关键验证点分析

### 5.1 用户联系人业务流程

```
创建联系人 → 查询列表 → 设置默认 → 更新联系人 → 删除联系人
```

- 用户隔离：每个联系人属于特定 userId，跨用户操作返回 400
- 默认联系人：`set-default` 接口支持幂等，重复设置返回 200
- 级联删除：删除联系人会自动清除 `user_contact` 关联记录
- 参数校验：`name` / `phone` / `address` 均不能为空，`id` 不能为 null

### 5.2 商家地址业务流程

```
创建地址 → 查询列表 → 设置默认 → 更新地址 → 删除地址
```

- 店铺隔离：通过 `X-Shop-Id` 头实现，地址 `selectShopIdByAddressId` 校验归属
- 地址类型：支持 `addressType` 字段（1-发货地址 / 2-退货地址）
- 默认标记：`isDefault=1` 时自动清除同类型其他地址的默认标记后设置
- 参数校验：`addressType` 不能为空
- 更新/删除/设置默认前均校验所有权的 shopId 匹配

### 5.3 通用校验

| 维度 | 实现方式 |
|------|----------|
| 认证 | `X-User-Id` / `X-Shop-Id` 请求头，缺失返回 401 |
| 参数校验 | Jakarta Validation (`@NotBlank`, `@NotNull`) + `BindingResult` |
| 数据隔离 | Controller → Service 层校验关联 owner，不匹配返回 400 |
| 返回值 | 统一 `ApiResponse<T>` 格式：`code` / `message` / `data` |

## 6. 已有关联的单测覆盖

| 模块 | 测试文件 | 测试数 | 覆盖范围 |
|------|----------|:------:|----------|
| 用户联系人 Controller | `controller/UserContactControllerTest.java` | 32 | 创建/删除/更新/查询/设置默认/参数校验/用户隔离 |
| 商家地址 Controller | `controller/MerchantContactControllerTest.java` | 36 | 创建/删除/更新/查询/设置默认/参数校验/店铺隔离 |
| 内部 API Controller | `controller/internal/InternalContactControllerTest.java` | 4 | 用户ID查询联系人 |
| 用户联系人 Service | `service/impl/UserContactServiceImplTest.java` | 26 | 创建/删除/更新/查询/设置默认/用户隔离 |
| 商家地址 Service | `service/impl/ShopAddressServiceImplTest.java` | 26 | 创建/删除/更新/查询/设置默认/店铺隔离 |
| 用户联系人 Mapper | `mapper/UserContactMapperTest.java` | 12 | 插入/查询/更新/删除（含 2 个既有失败） |
| 商家地址 Mapper | `mapper/ShopAddressMapperTest.java` | 14 | 插入/查询/更新/删除 |
| 用户联系人 DTO | `dto/ContactResponseTest.java` | 3 | ContactResponse 构建 |
| 商家地址 DTO | `dto/AddressResponseTest.java` | 3 | AddressResponse 构建 |
| **用户联系人 API** | **`api/UserContactApiTest.java`** | **21** | **端到端 CRUD + 容错（本次新增）** |
| **商家地址 API** | **`api/MerchantContactApiTest.java`** | **14** | **端到端 CRUD + 容错（本次新增）** |
| **合计** | | **191** | |

## 7. 本次变更记录

### 7.1 新增文件

| 文件 | 说明 |
|------|------|
| `api/UserContactApiTest.java` | 用户联系人 API 集成测试（21 用例） |
| `api/MerchantContactApiTest.java` | 商家地址 API 集成测试（14 用例） |

### 7.2 修改文件

| 文件 | 变更内容 |
|------|----------|
| `service/impl/ShopAddressServiceImpl.java` | `createAddress()` 返回值从 `rows` 改为 `address.getId()`，修复始终返回 1 的 bug |
| `controller/MerchantContactControllerTest.java` | 新增 36 个控制层测试 |
| `controller/UserContactControllerTest.java` | 新增 30 个控制层测试 |
| `controller/internal/InternalContactControllerTest.java` | 新增 4 个内部 API 控制层测试 |
| `service/impl/ShopAddressServiceImplTest.java` | 修正 3 个单测断言以匹配新的返回值 |
| `service/impl/UserContactServiceImplTest.java` | 新增 11 个服务层测试 |

### 7.3 修复 Bug

- **ShopAddressServiceImpl.createAddress 返回值错误**：原代码返回 `rows`（始终为 1），导致 Controller 返回的 `data.id` 永远是 1，API 集成测试中 update/delete/set-default 使用错误 ID 操作失败。修复为返回 `address.getId()`（实际自增 ID）。

## 8. 结论

Contact 服务全部 35 个 API 端点集成测试通过。用户联系人（CRUD + 设置默认）与商家地址（CRUD + 设置默认 + 发货地址查询）两条完整业务链路已验证，认证隔离、参数校验、数据归属校验均符合预期。

## 9. 测试数据清理

API 集成测试会向数据库写入持久数据（mapper 测试因 `@Transactional` 自动回滚）。清理脚本：`sql/clean/clean_api_test_data.sql`

```sql
-- 清理用户联系人测试数据
USE eureka_contact;
DELETE FROM user_contact WHERE user_id IN ('1001','2001','3001','4001','5001','6001','7001','8001','99999');
DELETE FROM t_contact WHERE id NOT IN (SELECT DISTINCT contact_id FROM user_contact);

-- 清理商家地址测试数据
DELETE FROM shop_address_rel WHERE shop_id LIKE 'SHP-%' OR shop_id = '1001';
DELETE FROM shop_address WHERE id NOT IN (SELECT DISTINCT address_id FROM shop_address_rel);
```
