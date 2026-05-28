# contact-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28

## 1. 概述

本文档为 AI-Shopping 项目 contact-service 模块的测试用例。contact-service 负责用户通讯录和店铺地址管理，提供联系人 CRUD、默认地址设置、店铺发货/退货地址管理等能力。数据库使用 eureka_contact，端口 8083。

### 接口清单

| 控制器 | 路由 | 方法 |
|--------|------|------|
| UserContactController | /api/user/contact | create / delete / update / list / set-default |
| MerchantContactController | /api/merchant/address | create / update / delete / list / ship-default / set-default |
| InternalContactController | /internal/contact | get by id |

> **注意**: 
> 1. Gateway 将商家地址路由从 `/api/seller/address/**` 转发到 contact-service，但控制器路径为 `/api/merchant/address`，两者不匹配，需在 Gateway 层配置 RewritePath。
> 2. ContactException 类已定义但**业务代码中从未抛出**。所有原本应抛出 ContactException 的位置实际直接返回 `ApiResponse.error(code, message)`，下面用例中所有 "触发 ContactException" 的预期均已修正。

## 2. 测试环境

| 项目 | 配置 |
|------|------|
| 服务端口 | 8083 |
| 数据库 | eureka_contact |
| 基础 URL | http://localhost:8083 |
| 请求头 | Content-Type: application/json |
| 鉴权头 | X-User-Id (用户侧), X-Shop-Id (商户侧) |

## 3. 测试用例表

### 3.1 用户联系人管理

#### POST /api/user/contact/create — 创建联系人

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-001 | 正常创建联系人 | 用户已登录，存在有效的 X-User-Id | 1. 请求 POST /api/user/contact/create<br>2. Header: X-User-Id=1001<br>3. Body: {"name":"张三","phone":"13800138000","address":"北京市朝阳区"} | 1. 返回 200<br>2. ApiResponse 中 code 为 200<br>3. data 为新建联系人的 id（正整数）<br>4. 数据库 t_contact 中新增记录<br>5. user_contact 关联表正确写入 | P0 |
| CT-002 | 创建联系人时 name 为空 | X-User-Id=1001 | 1. POST /api/user/contact/create<br>2. Body: {"name":"","phone":"13800138000","address":"北京市"} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException<br>3. 提示 name 不能为空 | P1 |
| CT-003 | 创建联系人时 phone 为空 | X-User-Id=1001 | 1. POST /api/user/contact/create<br>2. Body: {"name":"张三","phone":"","address":"北京市"} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException<br>3. 提示 phone 不能为空 | P1 |
| CT-004 | 创建联系人时 address 为空 | X-User-Id=1001 | 1. POST /api/user/contact/create<br>2. Body: {"name":"张三","phone":"13800138000","address":""} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException<br>3. 提示 address 不能为空 | P1 |
| CT-005 | 创建联系人时缺少 X-User-Id | 请求头中无 X-User-Id | 1. POST /api/user/contact/create<br>2. Body: {"name":"张三","phone":"13800138000","address":"北京市"}<br>3. 不传 X-User-Id | 1. 返回 401<br>2. message='未登录'（代码直接返回 ApiResponse.error(401, "未登录")，不抛出 ContactException） | P1 |
| CT-006 | 创建联系人时 phone 为非法格式（非数字） | X-User-Id=1001 | 1. POST /api/user/contact/create<br>2. Body: {"name":"张三","phone":"abc","address":"北京市"} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException（若开启 pattern 校验）或成功创建（视校验级别而定） | P2 |
| CT-007 | 创建联系人并同时设为默认（当前不支持） | X-User-Id=1001，用户已有其他联系人 | 1. POST /api/user/contact/create<br>2. Body: {"name":"默认联系人","phone":"13900139000","address":"上海市","isDefault":true}<br>**说明**: CreateContactRequest DTO 中无 isDefault 字段，该参数会被忽略 | 1. 返回 200，返回 id<br>2. 该联系人在 t_contact 中 isDefault=0（isDefault 参数被忽略，无法在创建时设置默认）<br>3. 需通过 PUT /api/user/contact/set-default/{id} 另行设置默认 | P1 |

#### DELETE /api/user/contact/delete/{id} — 删除联系人

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-008 | 正常删除联系人 | X-User-Id=1001，存在联系人 id=1 且属于该用户 | 1. DELETE /api/user/contact/delete/1<br>2. Header: X-User-Id=1001 | 1. 返回 200<br>2. data 为操作成功信息<br>3. 数据库 t_contact 中 id=1 的记录被删除<br>4. 关联表中的记录被清理 | P0 |
| CT-009 | 删除不存在的联系人 | X-User-Id=1001 | 1. DELETE /api/user/contact/delete/99999 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, "地址不存在")（注意：代码中消息为 "地址不存在" 而非 "联系人不存在"，存在命名不一致） | P1 |
| CT-010 | 删除不属于当前用户的联系人 | X-User-Id=1001，联系人 id=2 属于 X-User-Id=1002 | 1. DELETE /api/user/contact/delete/2<br>2. Header: X-User-Id=1001 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, "无权限操作")，不抛出 ContactException | P1 |
| CT-011 | 删除联系人时缺少 X-User-Id | 联系人 id=1 存在 | 1. DELETE /api/user/contact/delete/1<br>2. 不传 X-User-Id | 1. 返回 401<br>2. message='未登录'（代码直接返回 ApiResponse.error(401, "未登录")，不抛出 ContactException） | P1 |
| CT-012 | 删除时 id 为负数 | X-User-Id=1001 | 1. DELETE /api/user/contact/delete/-1 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, ...) 或触发参数校验异常，不抛出 ContactException | P2 |

#### PUT /api/user/contact/update — 更新联系人

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-013 | 正常更新联系人 | X-User-Id=1001，存在联系人 id=1 且属于该用户 | 1. PUT /api/user/contact/update<br>2. Header: X-User-Id=1001<br>3. Body: {"id":1,"name":"李四","phone":"13700137000","address":"上海市浦东新区"} | 1. 返回 200<br>2. 数据库 t_contact id=1 的记录 name/phone/address 已更新 | P0 |
| CT-014 | 更新不存在的联系人 | X-User-Id=1001 | 1. PUT /api/user/contact/update<br>2. Body: {"id":99999,"name":"李四","phone":"13700137000","address":"上海市"} | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, "地址不存在")，不抛出 ContactException | P1 |
| CT-015 | 更新不属于当前用户的联系人 | X-User-Id=1001，联系人 id=2 属于 X-User-Id=1002 | 1. PUT /api/user/contact/update<br>2. Body: {"id":2,"name":"李四","phone":"13700137000","address":"上海市"} | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, "无权限操作")，不抛出 ContactException | P1 |
| CT-016 | 更新请求中 id 为空 | X-User-Id=1001 | 1. PUT /api/user/contact/update<br>2. Body: {"name":"李四","phone":"13700137000","address":"上海市"} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException（id 为 @NotNull） | P1 |
| CT-017 | 更新请求 body 为 null | X-User-Id=1001 | 1. PUT /api/user/contact/update<br>2. Body: null | 1. 返回 400<br>2. 参数解析异常 | P2 |

#### GET /api/user/contact/list — 查询联系人列表

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-018 | 正常查询联系人列表 | X-User-Id=1001，该用户有 3 个联系人 | 1. GET /api/user/contact/list<br>2. Header: X-User-Id=1001 | 1. 返回 200<br>2. data.contacts 长度为 3<br>3. data.total 为 3<br>4. 每个联系人的字段(id/name/phone/address/isDefault)正确 | P0 |
| CT-019 | 查询无联系人的用户 | X-User-Id=2000，该用户无联系人 | 1. GET /api/user/contact/list<br>2. Header: X-User-Id=2000 | 1. 返回 200<br>2. data.contacts 为空列表<br>3. data.total 为 0 | P1 |
| CT-020 | 查询列表时缺少 X-User-Id | 请求头中无 X-User-Id | 1. GET /api/user/contact/list<br>2. 不传 X-User-Id | 1. 返回 401<br>2. message='未登录'（代码直接返回 ApiResponse.error(401, "未登录")，不抛出 ContactException） | P1 |
| CT-021 | 查询列表时默认联系人标记正确 | X-User-Id=1001，联系人 id=5 是默认联系人 | 1. GET /api/user/contact/list<br>2. Header: X-User-Id=1001 | 1. data.contacts 中 id=5 的 isDefault=1<br>2. 其他联系方式 isDefault=0 | P1 |

#### PUT /api/user/contact/set-default/{id} — 设置默认联系人

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-022 | 正常设置默认联系人 | X-User-Id=1001，有联系人 id=1(非默认)、id=2(默认) | 1. PUT /api/user/contact/set-default/1<br>2. Header: X-User-Id=1001 | 1. 返回 200<br>2. 数据库 t_contact id=1 的 isDefault=1<br>3. 数据库 t_contact id=2 的 isDefault=0 | P0 |
| CT-023 | 设置不存在的联系人为默认 | X-User-Id=1001 | 1. PUT /api/user/contact/set-default/99999 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, "地址不存在")，不抛出 ContactException | P1 |
| CT-024 | 设置不属于当前用户的联系人为默认 | X-User-Id=1001，联系人 id=10 属于 X-User-Id=1002 | 1. PUT /api/user/contact/set-default/10 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, "无权限操作")，不抛出 ContactException | P1 |
| CT-025 | 对已经是默认的联系人重复设置默认 | X-User-Id=1001，id=1 已是默认 | 1. PUT /api/user/contact/set-default/1<br>2. Header: X-User-Id=1001 | 1. 返回 200<br>2. id=1 的 isDefault 仍为 1<br>3. 不报错，幂等 | P2 |

### 3.2 店铺地址管理

#### POST /api/merchant/address/create — 创建店铺地址

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-026 | 正常创建发货地址 | 店铺已登录，存在有效的 X-Shop-Id | 1. POST /api/merchant/address/create<br>2. Header: X-Shop-Id=5001<br>3. Body: {"name":"店铺仓库","phone":"021-12345678","address":"上海市嘉定区物流园A区","addressType":1,"isDefault":false} | 1. 返回 200<br>2. data 为新建地址 id（正整数）<br>3. 数据库 shop_address 新增记录<br>4. shop_address_rel 关联表正确写入 | P0 |
| CT-027 | 正常创建退货地址 | X-Shop-Id=5001 | 1. POST /api/merchant/address/create<br>2. Body: {"name":"售后部","phone":"021-87654321","address":"上海市浦东新区售后中心","addressType":2,"isDefault":true} | 1. 返回 200<br>2. addressType=2<br>3. isDefault=1<br>4. 同类默认地址（addressType=2）被清 | P1 |
| CT-028 | 创建地址时缺少 addressType | X-Shop-Id=5001 | 1. POST /api/merchant/address/create<br>2. Body: {"name":"店铺","phone":"02112345678","address":"上海市","isDefault":false} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException<br>3. 提示 addressType 不能为空 | P1 |
| CT-029 | 创建地址时 addressType 为非法值 | X-Shop-Id=5001 | 1. POST /api/merchant/address/create<br>2. Body: {"name":"店铺","phone":"02112345678","address":"上海市","addressType":3,"isDefault":false} | 1. 返回 400<br>2. 触发 MethodArgumentNotValidException 或直接返回 ApiResponse.error(400, ...)，不抛出 ContactException | P1 |
| CT-030 | 创建地址时缺少 X-Shop-Id | 请求头中无 X-Shop-Id | 1. POST /api/merchant/address/create<br>2. Body: {"name":"店铺","phone":"02112345678","address":"上海市","addressType":1,"isDefault":false}<br>3. 不传 X-Shop-Id | 1. 返回 401<br>2. message='未登录'（代码直接返回 ApiResponse.error(401, "未登录")，不抛出 ContactException） | P1 |
| CT-031 | 创建地址且设为默认时，同类默认只有一个 | X-Shop-Id=5001，已有发货默认地址 id=10 | 1. POST /api/merchant/address/create<br>2. Body: {"name":"新仓库","phone":"0211111111","address":"上海市","addressType":1,"isDefault":true} | 1. 返回 200<br>2. 新地址 isDefault=1<br>3. 旧地址 id=10 被清为 isDefault=0 | P1 |

#### PUT /api/merchant/address/update/{id} — 更新店铺地址

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-032 | 正常更新店铺地址 | X-Shop-Id=5001，地址 id=10 属于该店铺 | 1. PUT /api/merchant/address/update/10<br>2. Header: X-Shop-Id=5001<br>3. Body: {"name":"新仓库名","phone":"0212222222","address":"上海市松江区","addressType":1,"isDefault":true} | 1. 返回 200<br>2. 数据库 shop_address id=10 的记录已更新 | P0 |
| CT-033 | 更新不存在地址 | X-Shop-Id=5001 | 1. PUT /api/merchant/address/update/99999<br>2. Body: {"name":"测试","phone":"0210000000","address":"上海市","addressType":1,"isDefault":false} | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, ...)，不抛出 ContactException | P1 |
| CT-034 | 更新不属于当前店铺的地址 | X-Shop-Id=5001，地址 id=20 属于 X-Shop-Id=5002 | 1. PUT /api/merchant/address/update/20<br>2. Body: {"name":"测试","phone":"0210000000","address":"上海市","addressType":1,"isDefault":false} | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, ...)，不抛出 ContactException | P1 |

#### DELETE /api/merchant/address/delete/{id} — 删除店铺地址

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-035 | 正常删除店铺地址 | X-Shop-Id=5001，地址 id=10 属于该店铺 | 1. DELETE /api/merchant/address/delete/10<br>2. Header: X-Shop-Id=5001 | 1. 返回 200<br>2. 数据库 shop_address id=10 被删除<br>3. 关联表 shop_address_rel 中记录被清理 | P0 |
| CT-036 | 删除不存在的店铺地址 | X-Shop-Id=5001 | 1. DELETE /api/merchant/address/delete/99999 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, ...)，不抛出 ContactException | P1 |
| CT-037 | 删除不属于当前店铺的地址 | X-Shop-Id=5001，地址 id=20 属于 X-Shop-Id=5002 | 1. DELETE /api/merchant/address/delete/20 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, ...)，不抛出 ContactException | P1 |

#### GET /api/merchant/address/list — 查询店铺地址列表

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-038 | 正常查询店铺地址列表 | X-Shop-Id=5001，该店铺有 2 个发货地址 + 1 个退货地址 | 1. GET /api/merchant/address/list<br>2. Header: X-Shop-Id=5001 | 1. 返回 200<br>2. data.addresses 长度为 3<br>3. data.total 为 3<br>4. 每个地址返回 id/name/phone/address/addressType/isDefault | P0 |
| CT-039 | 查询无地址的店铺 | X-Shop-Id=6000，该店铺无地址 | 1. GET /api/merchant/address/list<br>2. Header: X-Shop-Id=6000 | 1. 返回 200<br>2. data.addresses 为空列表<br>3. data.total 为 0 | P1 |
| CT-040 | 查询列表时不同类型地址混合正确 | X-Shop-Id=5001 | 1. GET /api/merchant/address/list<br>2. Header: X-Shop-Id=5001 | 1. 返回列表中 addressType 正确标记（1=发货, 2=退货）<br>2. 默认地址 isDefault=1 | P2 |

#### GET /api/merchant/address/ship-default — 查询默认发货地址

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-041 | 正常查询默认发货地址 | X-Shop-Id=5001，存在 addressType=1 且 isDefault=1 的地址 | 1. GET /api/merchant/address/ship-default<br>2. Header: X-Shop-Id=5001 | 1. 返回 200<br>2. data 为默认发货地址详情<br>3. data.addressType=1<br>4. data.isDefault=true | P0 |
| CT-042 | 店铺无默认发货地址 | X-Shop-Id=5001，仅设置了退货默认地址，未设置发货默认 | 1. GET /api/merchant/address/ship-default<br>2. Header: X-Shop-Id=5001 | 1. 返回 200 或 400（视设计而定）<br>2. 若 200，data 可能为 null 或默认返回第一个发货地址 | P1 |

#### PUT /api/merchant/address/set-default/{id} — 设置店铺默认地址

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-043 | 正常设置发货默认地址 | X-Shop-Id=5001，地址 id=30(addressType=1)不是默认 | 1. PUT /api/merchant/address/set-default/30<br>2. Header: X-Shop-Id=5001 | 1. 返回 200<br>2. 同类(addressType=1)其他地址 isDefault 被清<br>3. id=30 的 isDefault=1 | P0 |
| CT-044 | 设置退货地址为默认 | X-Shop-Id=5001，地址 id=40(addressType=2)不是默认 | 1. PUT /api/merchant/address/set-default/40<br>2. Header: X-Shop-Id=5001 | 1. 返回 200<br>2. 仅清除同类(addressType=2)的默认<br>3. 发货地址默认状态不受影响 | P1 |
| CT-045 | 设置不存在的地址为默认 | X-Shop-Id=5001 | 1. PUT /api/merchant/address/set-default/99999 | 1. 返回 400<br>2. 直接返回 ApiResponse.error(400, ...)，不抛出 ContactException | P1 |

### 3.3 内部接口

#### GET /internal/contact/{id} — 根据 ID 查询联系人

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-046 | 正常查询联系人 | 联系人 id=1 存在 | 1. GET /internal/contact/1 | 1. 返回 200<br>2. 返回 Contact 对象(id/name/phone/address/isDefault)<br>3. 字段值正确 | P0 |
| CT-047 | 查询不存在的联系人 | 联系人 id=99999 不存在 | 1. GET /internal/contact/99999 | 1. 返回 200<br>2. data 为 null（视实现而定）或返回 400/404 | P1 |
| CT-048 | 查询 id 为 0 或负数 | — | 1. GET /internal/contact/0<br>2. GET /internal/contact/-1 | 1. 返回 400 参数校验异常<br>2. 或返回 data 为 null | P2 |

### 3.4 异常处理

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CT-049 | 请求路径不存在 | — | 1. GET /api/user/contact/nonexistent | 1. 返回 404 | P2 |
| CT-050 | 方法不允许（GET 请求 POST 接口） | — | 1. GET /api/user/contact/create | 1. 返回 405 | P2 |
| CT-051 | 请求体 JSON 格式错误 | X-User-Id=1001 | 1. POST /api/user/contact/create<br>2. Body: "{invalid json}" | 1. 返回 400<br>2. JSON 解析异常信息 | P2 |
| CT-052 | X-User-Id 格式非法（非数字） | — | 1. POST /api/user/contact/create<br>2. Header: X-User-Id=abc<br>3. Body: 合法请求体 | 1. 返回 401<br>2. message='未登录'（代码直接返回 ApiResponse.error(401, "未登录")，不抛出 ContactException）<br>3. 或触发类型转换异常返回 400 | P1 |
| CT-053 | 并发创建默认联系人 | X-User-Id=1001 | 1. 并发发送 2 个 POST /api/user/contact/create<br>2. 均设置 isDefault=true | 1. 两个请求均成功<br>2. 数据库最终只有 1 个联系人是默认<br>3. 无脏数据 | P2 |
| CT-054 | 服务内部异常（数据库连接失败） | 数据库不可用 | 1. 发送任意 API 请求 | 1. 返回 500<br>2. 触发 Exception 全局处理器 | P2 |

## 4. 测试要点总结

### 4.1 用户联系人模块关注点

- **所有权校验**：所有用户操作（删除/更新/设置默认）必须校验联系人归属于当前 X-User-Id，验证 selectUserIdsByContactId 的正确性
- **默认互斥**：一个用户只能有一个 isDefault=1 的联系人，设置新默认时需清除旧默认（setDefaultById + clearDefaultByUserId）
- **级联删除**：删除联系人时需同时清理 user_contact 关联表记录
- **新建关联**：create 接口插入 t_contact 后必须建立 user_contact 关联

### 4.2 店铺地址模块关注点

- **店铺归属校验**：所有操作通过 shop_address_rel 校验地址是否属于当前 X-Shop-Id（selectShopIdByAddressId）
- **地址类型隔离**：发货地址(addressType=1)和退货地址(addressType=2)的默认状态互不干扰，clearDefaultByType 按 addressType 过滤
- **默认互斥同类型内**：同一 addressType 下只能有一个默认地址
- **级联删除**：删除地址时清理 shop_address_rel 关联

### 4.3 通用关注点

- **参数校验**：@NotBlank(name/phone/address)、@NotNull(addressType/id) 等校验注解的覆盖
- **请求头缺失**：X-User-Id / X-Shop-Id 缺失时的异常处理
- **操作不存在资源**：返回 400 ContactException，信息需友好
- **跨模块安全**：防止一个用户/店铺操作另一个用户/店铺的地址
- **幂等性**：对已默认的资源重复设默认不应报错
