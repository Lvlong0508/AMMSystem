# shop-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28 | 模块: shop-service (端口 8087)

---

## 1. 概述

本文档覆盖 shop-service 的全部对外接口，包含商家端 `/api/seller/shop`、用户端 `/api/user/shop`、内部接口 `/internal/shop`。用例按业务场景分组，共 **49 个** 用例。

| 分组 | 数量 | 覆盖范围 |
|------|------|----------|
| 店铺创建 | 6 | 正常创建、缺少名称、描述超长、Logo 缺失、自动建 ShopInfo、自动建店长角色 |
| 店铺管理 (更新/关闭/开启) | 9 | 正常更新/关闭/开启、店铺不存在、非店长操作、已关闭店铺重复关闭 |
| 员工管理 (添加/移除) | 7 | 正常添加/移除、Feign 调用失败、非店长操作、员工已存在、参数校验 |
| 店铺查询 (商家端) | 7 | 按商家查店铺ID、按ID查详情、查员工列表、无权限访问、店铺不存在 |
| 店铺查询 (用户端) | 5 | 活跃店铺列表分页、店铺详情、已关闭店铺详情、未登录、空列表 |
| 内部接口 (Feign) | 6 | 查商家角色、查店铺信息、批量查、空集合、不存在、部分存在 |
| 权限校验 | 4 | 店长权限校验、普通店员访问、无任何角色、跨店铺权限 |
| 参数校验 | 3 | @Valid 触发的校验异常 |
| 异常处理 | 2 | 未知异常捕获、日志记录 |

---

## 2. 测试环境

| 项目 | 说明 |
|------|------|
| 服务端口 | 8087 |
| 数据库 | eureka_shop |
| 基础路径(商家) | `/api/seller/shop` |
| 基础路径(用户) | `/api/user/shop` |
| 基础路径(内部) | `/internal/shop` |
| 头信息 | `X-User-Id` (商家用户ID) |
| 预期异常 | ShopException(400), MethodArgumentNotValidException(400), Exception(500) |
| 角色定义 | role=1 店长, role=2 店员 |

---

## 3. 测试用例表

### 3.1 店铺创建

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-001 | 创建店铺成功 - 完整参数 | 用户登录态有效, userId=1001 | 1. POST `/api/seller/shop/register`<br>2. Header: `X-User-Id: 1001`<br>3. Body: `{"name":"测试店铺","description":"这是一个测试店铺","logoId":"logo-abc-123"}` | 1. 返回 HTTP 200<br>2. ApiResponse.message = "创建店铺成功"<br>3. ApiResponse.data.id 不为空 (Long 类型)<br>4. 数据库新增 ShopInfo 记录<br>5. 数据库新增 Shop 记录, status=1, merchantId=1001<br>6. 数据库新增 MerchantRole 记录, role=1, merchantId=1001 | P0 |
| SH-002 | 创建店铺成功 - 仅必填参数 | 用户登录态有效 | 1. POST `/api/seller/shop/register`<br>2. Header: `X-User-Id: 1001`<br>3. Body: `{"name":"最小店铺"}` | 1. 返回 HTTP 200<br>2. ApiResponse.data.id 不为空<br>3. 数据库 ShopInfo.description 为 null, logoUrl 为 null | P1 |
| SH-003 | 创建店铺 - name 为空 | — | 1. POST `/api/seller/shop/register`<br>2. Body: `{"description":"测试"}` | 1. 返回 HTTP 400<br>2. message 包含 "店铺名称不能为空" | P0 |
| SH-004 | 创建店铺 - name 超长 | — | 1. POST `/api/seller/shop/register`<br>2. Body: `{"name":"a...(101个字符)"}` | 1. 返回 HTTP 400<br>2. message 包含 "店铺名称最长100个字符" | P1 |
| SH-005 | 创建店铺 - description 超长 | — | 1. POST `/api/seller/shop/register`<br>2. Body: `{"name":"正常","description":"a...(501个字符)"}` | 1. 返回 HTTP 400<br>2. message 包含 "店铺描述最长500个字符" | P2 |
| SH-006 | 创建店铺 - 数据回滚 | 插入 Shop 成功后 MerchantRole 插入失败 | 1. 模拟 MerchantRoleMapper.insert 抛出异常<br>2. POST `/api/seller/shop/register`<br>3. Body: `{"name":"回滚测试"}` | 1. 返回 500 或 400<br>2. 数据库中无 ShopInfo/Shop/MerchantRole 残留 (事务回滚)<br>**注意**: 当前代码未检查 MerchantRoleMapper.insert 的返回值。若插入静默失败(返回 0 但不抛异常)，不会触发回滚，数据库中会残留 ShopInfo 和 Shop 记录 | P1 |

### 3.2 店铺管理 (更新/关闭/开启)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-007 | 更新店铺成功 | 存在 shopId=1, 当前用户为店长 | 1. PUT `/api/seller/shop/1`<br>2. Header: `X-User-Id: 1001`<br>3. Body: `{"name":"新店铺名","description":"新描述","logoId":"new-logo"}` | 1. 返回 HTTP 200<br>2. ApiResponse.message = "更新店铺成功"<br>3. 数据库 ShopInfo.name = "新店铺名", description = "新描述", logo_url = "new-logo" | P0 |
| SH-008 | 更新店铺 - 仅更新名称 | shopId=1, 用户为店长 | PUT `/api/seller/shop/1` Body: `{"name":"仅改名称"}` | 1. HTTP 200<br>2. 代码做全量覆盖，未提供的字段被置空：ShopInfo 的 name 更新为新值，description 和 logoUrl 将被设为 null（而非保持不变） | P1 |
| SH-009 | 更新店铺 - 店铺不存在 | 传入不存在的 shopId=999 | PUT `/api/seller/shop/999` Body: `{"name":"不存在"}` | 1. HTTP 400<br>2. message 包含 "店铺不存在" | P1 |
| SH-010 | 更新店铺 - 非店长操作 | 用户为店员 (role=2) | PUT `/api/seller/shop/1` Body: `{"name":"无权限"}` | 1. HTTP 400<br>2. message 包含 "仅店长可操作" | P0 |
| SH-011 | 关闭店铺成功 | shopId=1, 当前用户为店长, 店铺当前 status=1 | DELETE `/api/seller/shop/1` Header: `X-User-Id: 1001` | 1. HTTP 200<br>2. ApiResponse.message = "关闭店铺成功"<br>3. 数据库 Shop.status = 0 | P0 |
| SH-012 | 关闭店铺 - 非店长操作 | 用户为店员 (role=2) | DELETE `/api/seller/shop/1` Header: `X-User-Id: 1002` | 1. HTTP 400<br>2. message 包含 "仅店长可操作" | P0 |
| SH-013 | 关闭店铺 - 已关闭店铺再次关闭 | shopId=1 已关闭 (status=0), 当前用户为店长 | DELETE `/api/seller/shop/1` Header: `X-User-Id: 1001` | 1. HTTP 400<br>2. message 包含 "关闭店铺失败" (mapper.closeShop 返回 0) | P1 |
| SH-014 | 重新开店成功 | shopId=1 已关闭 (status=0), 店长操作 | PUT `/api/seller/shop/1/open` Header: `X-User-Id: 1001` | 1. HTTP 200<br>2. ApiResponse.message = "重新开店成功"<br>3. 数据库 Shop.status = 1 | P1 |
| SH-015 | 重新开店 - 非店长操作 | 用户为店员 | PUT `/api/seller/shop/1/open` Header: `X-User-Id: 1002` | 1. HTTP 400<br>2. message 包含 "仅店长可操作" | P1 |

### 3.3 员工管理 (添加/移除)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-016 | 添加店员成功 | shopId=1, 当前用户为店长, auth-service 正常 | 1. POST `/api/seller/shop/1/employees/register`<br>2. Header: `X-User-Id: 1001`<br>3. Body: `{"username":"emp01","password":"Abc123","phone":"13800138001","name":"店员小王"}` | 1. HTTP 200<br>2. ApiResponse.message = "添加店员成功"<br>3. 内部 Feign 调用 auth-service 注册成功<br>4. 数据库新增 MerchantRole, role=2, assignedBy=1001 | P0 |
| SH-017 | 添加店员 - Feign 返回失败 | auth-service 返回错误 | 1. 模拟 AuthFeignClient.registerEmployee 返回 message="用户名已存在"<br>2. POST `/api/seller/shop/1/employees/register` Body 同上 | 1. HTTP 400<br>2. message 包含 "添加店员失败: 用户名已存在" | P1 |
| SH-018 | 添加店员 - Feign 返回 null | auth-service 异常返回 null | 1. 模拟 AuthFeignClient.registerEmployee 返回 null<br>2. POST `/api/seller/shop/1/employees/register` Body 同上 | 1. HTTP 400<br>2. message 包含 "添加店员失败: 注册失败" | P1 |
| SH-019 | 添加店员 - 非店长操作 | 当前用户为店员 (role=2) | POST `/api/seller/shop/1/employees/register` Header: `X-User-Id: 1002` | 1. HTTP 400<br>2. message 包含 "仅店长可操作" | P0 |
| SH-020 | 移除店员成功 | shopId=1 存在, 被移除 merchantId=2001 为店员 | DELETE `/api/seller/shop/1/employees/2001` Header: `X-User-Id: 1001` | 1. HTTP 200<br>2. ApiResponse.message = "移除店员成功"<br>3. 数据库 MerchantRole 表中 merchantId=2001 & shopId=1 记录被删除 | P1 |
| SH-021 | 移除店员 - 非店长操作 | 当前用户为店员 | DELETE `/api/seller/shop/1/employees/2001` Header: `X-User-Id: 1002` | 1. HTTP 400<br>2. message 包含 "仅店长可操作" | P1 |
| SH-022 | 添加店员 - 请求参数校验 | Body 中 username 为空 | POST `/api/seller/shop/1/employees/register` Body: `{"password":"Abc123","phone":"13800138001","name":"测试"}` | 1. HTTP 400<br>2. message 包含 "账号不能为空" | P1 |

### 3.4 店铺查询 (商家端)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-023 | 按商家查询店铺ID列表 | merchantId=1001 关联 3 家店铺 | GET `/api/seller/shop/merchant/1001` | 1. HTTP 200<br>2. ApiResponse.data.shopIds 为 List\<Long\><br>3. shopIds 包含该商家所有关联店铺 ID | P1 |
| SH-024 | 按商家查询 - 无店铺 | merchantId=999 无任何角色记录 | GET `/api/seller/shop/merchant/999` | 1. HTTP 200<br>2. ApiResponse.data.shopIds 为空列表 `[]` | P2 |
| SH-025 | 查询店铺详情 - 有权限 | shopId=1, 当前用户在店长或店员角色中 | GET `/api/seller/shop/1` Header: `X-User-Id: 1001` | 1. HTTP 200<br>2. ApiResponse.data.shop 包含 Shop 全部字段 (id, merchantId, shopInfoId, status, createdAt, updatedAt) | P0 |
| SH-026 | 查询店铺详情 - 无权限 | 当前用户与该店铺无任何角色关联 | GET `/api/seller/shop/1` Header: `X-User-Id: 9999` | 1. HTTP 400<br>2. message 包含 "无权限访问该店铺" | P0 |
| SH-027 | 查询店铺详情 - 店铺不存在 | shopId=999 不存在 | GET `/api/seller/shop/999` Header: `X-User-Id: 1001` | 1. HTTP 400<br>2. message 包含 "店铺不存在" | P1 |
| SH-028 | 查询员工列表 | shopId=1, 当前用户有权限, 该店有 3 名角色 | GET `/api/seller/shop/1/employees` Header: `X-User-Id: 1001` | 1. HTTP 200<br>2. ApiResponse.data.employees 为 List, 包含 merchantId/shopId/role/assignedBy<br>3. ApiResponse.data.total = 3 | P1 |
| SH-029 | 查询员工列表 - 无权限 | 当前用户与该店铺无角色关联 | GET `/api/seller/shop/1/employees` Header: `X-User-Id: 9999` | 1. HTTP 400<br>2. message 包含 "无权限访问该店铺" | P1 |

### 3.5 店铺查询 (用户端)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-030 | 查询活跃店铺列表 - 有数据 | 数据库中存在 >=3 条 status=1 的店铺 | GET `/api/user/shop/list?page=1&size=10` Header: `X-User-Id: 2001` | 1. HTTP 200<br>2. ApiResponse.data.shops 为 List\<Shop\>, 均为 status=1<br>3. ApiResponse.data.total >= 3<br>4. ApiResponse.data.page = 1, size = 10 | P0 |
| SH-031 | 查询活跃店铺列表 - 分页 | 数据库中有 15 条活跃店铺 | GET `/api/user/shop/list?page=2&size=10` Header: `X-User-Id: 2001` | 1. HTTP 200<br>2. ApiResponse.data.shops 返回 5 条<br>3. 第 2 页数据与第 1 页不重复 | P1 |
| SH-032 | 查询活跃店铺列表 - 无数据 | 数据库中无 status=1 的店铺 | GET `/api/user/shop/list` Header: `X-User-Id: 2001` | 1. HTTP 200<br>2. ApiResponse.data.shops 为空列表<br>3. total = 0 | P2 |
| SH-033 | 查询店铺详情 - 活跃店铺 | shopId=1 状态为 status=1 | GET `/api/user/shop/1` Header: `X-User-Id: 2001` | 1. HTTP 200<br>2. ApiResponse.data.shop 包含 Shop 字段<br>3. ApiResponse.data.shopInfo 包含 ShopInfoDTO (name, description, logoUrl) | P0 |
| SH-034 | 查询店铺详情 - 已关闭 | shopId=2 状态为 status=0 | GET `/api/user/shop/2` Header: `X-User-Id: 2001` | 1. HTTP 400<br>2. message 包含 "店铺不存在或已关闭" | P1 |

### 3.6 内部接口 (Feign)

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-035 | 查询商家角色列表 | merchantId=1001 在 3 家店铺有角色 | GET `/internal/shop/employees/roles/1001` | 1. HTTP 200<br>2. ApiResponse.data.roles 为 List\<MerchantRole\><br>3. 每个角色包含 id, merchantId, shopId, role, assignedBy, createdAt | P1 |
| SH-036 | 查询商家角色列表 - 无角色 | merchantId=999 无角色 | GET `/internal/shop/employees/roles/999` | 1. HTTP 200<br>2. ApiResponse.data.roles 为空列表 | P2 |
| SH-037 | 查询店铺信息 | shopId=1 存在且关联 ShopInfo | GET `/internal/shop/info/1` | 1. HTTP 200<br>2. ApiResponse 返回 ShopInfoDTO (id, name, description, logoUrl) | P1 |
| SH-038 | 查询店铺信息 - 无关联 info | shopId 存在但 shopInfoId 为 null | GET `/internal/shop/info/{shopId}` | 1. HTTP 200<br>2. ApiResponse 返回 null | P1 |
| SH-039 | 批量查询店铺信息 | 传入 shopIds=[1,2,3], 1 和 2 存在, 3 不存在 | POST `/internal/shop/info/batch` Body: `[1,2,3]` | 1. HTTP 200<br>2. ApiResponse 返回 Map\<Long,ShopInfoDTO\><br>3. key=1 和 2 有值, key=3 不存在 | P1 |
| SH-040 | 批量查询 - 空集合 | 传入空集合 | POST `/internal/shop/info/batch` Body: `[]` | 1. HTTP 200<br>2. ApiResponse 返回空 Map `{}` | P2 |

### 3.7 权限校验

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-041 | 店长可执行管理操作 | userId=1001 在 shopId=1 有 role=1 | 执行 PUT/DELETE 等店长接口 | 操作正常执行, 返回 HTTP 200 | P0 |
| SH-042 | 店员无法执行管理操作 | userId=1002 在 shopId=1 有 role=2, 无 role=1 | 执行 PUT `/api/seller/shop/1` | HTTP 400, message "仅店长可操作" | P0 |
| SH-043 | 店员可以查看店铺 | userId=1002 在 shopId=1 有 role=2 | GET `/api/seller/shop/1` Header: `X-User-Id: 1002` | 1. HTTP 200<br>2. 正常返回店铺信息 (checkShopAccess 通过) | P1 |
| SH-044 | 无任何角色无法访问 | userId=9999 在 shopId=1 无角色 | GET `/api/seller/shop/1` Header: `X-User-Id: 9999` | HTTP 400, message "无权限访问该店铺" | P1 |

### 3.8 参数校验

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-045 | 创建店铺 - name 为空白字符串 | — | POST `/api/seller/shop/register` Body: `{"name":""}` | 1. HTTP 400<br>2. message 包含 "店铺名称不能为空" | P1 |
| SH-046 | 添加店员 - username 过短 | — | POST `/api/seller/shop/1/employees/register` Body: `{"username":"ab"}` | 1. HTTP 400<br>2. message 包含 "账号长度需为3-20位" | P1 |
| SH-047 | 添加店员 - username 含特殊字符 | — | POST `/api/seller/shop/1/employees/register` Body: `{"username":"user@name"}` | 1. HTTP 400<br>2. message 包含 "账号只能包含字母、数字、下划线" | P2 |

### 3.9 异常处理

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| SH-048 | 未知异常返回 500 | 服务内部抛出未捕获的 RuntimeException | 模拟 Service 层抛出 RuntimeException | 1. HTTP 500<br>2. ApiResponse.message = "系统错误，请稍后重试"<br>3. 日志记录 error 级别堆栈 | P0 |
| SH-049 | 用户端未登录 | 请求不带 X-User-Id Header | GET `/api/user/shop/list` (不传 X-User-Id) | 1. HTTP 400<br>2. message 包含 "请先登录" | P0 |

---

## 4. 测试要点总结

| 维度 | 说明 |
|------|------|
| 核心链路 | 店铺创建 (ShopInfo → Shop → MerchantRole) 事务一致性需重点覆盖 |
| 权限模型 | 2 层权限: checkShopOwner(仅店长) / checkShopAccess(有角色即可), 需区分测试 |
| 外部依赖 | 添加店员依赖 auth-service Feign 调用, 需 mock 各种返回值 (成功/失败/null/异常) |
| 事务 | 创建店铺涉及 3 张表写入, 需验证异常回滚; 更新/关闭/开启各自独立事务 |
| 分页 | 用户端列表使用 offset 分页, 需覆盖边界值 (page=1, page=2, 无数据) |
| 软删除 | 关闭店铺仅改 status=0, 用户端不可见但商家端可查看, 需验证 |
| 动态 SQL | batchGetShopInfo 使用 IN 查询, 需验证空集合/部分命中/全部命中等场景 |
