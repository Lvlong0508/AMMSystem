# Shop 服务 API 接口集成测试报告

## 1. 测试概述

| 项目 | 内容 |
|------|------|
| 测试目标 | 验证 Shop 服务全部 REST API 端点的功能正确性和容错能力 |
| 测试类型 | API 接口集成测试（端到端，通过 Gateway 访问 + 直连 Shop 服务） |
| 测试日期 | 2026-06-02（首次） / 2026-06-02 14:20（修复后验证） |
| 测试工具 | PowerShell `Invoke-WebRequest` / `Invoke-RestMethod` |
| 测试总用例数 | **44** |

## 2. 测试环境

| 组件 | 地址 | 状态 |
|------|------|:----:|
| MySQL | localhost:3306 | ✅ 运行中 |
| Redis | localhost:6379 | ✅ 运行中 |
| Eureka Server | http://localhost:8761 | ✅ 运行中 |
| Gateway Service | http://localhost:8080 | ✅ 运行中 |
| Auth Service | http://localhost:8086 | ✅ 运行中 |
| Shop Service | http://localhost:8087 | ✅ 运行中 |

### 路由链路

```
Client
  → GET/POST http://localhost:8080/api/{user|seller}/shop/*
    → Gateway (SaTokenAuthGlobalFilter 鉴权 + 头注入)
      → Shop Service (Controller → Service → Mapper → MySQL)
```

### 测试数据

| 类型 | 用户名 | 密码 | 商家/用户 ID（雪花算法） |
|------|--------|------|--------------------------|
| 商家 | merchant001 | 123456 | 2061615989669367808 |
| 用户 | user001 | user001 | 2061615993330995200 |

### 测试店铺

| 店铺 ID | 说明 |
|---------|------|
| 2061638090027569152 | 测试用已创建店铺 |

## 3. 测试用例及结果

### 3.1 用户端 API — 通过 Gateway（共 3 用例）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 1 | 查询活跃店铺列表（用户 token） | GET | `/api/user/shop/list?page=1&size=10` | 200 分页列表 | 200 | ✅ |
| 2 | 无 token 访问 | GET | `/api/user/shop/list` | 401 未授权 | 401 | ✅ |
| 3 | 商家 token 访问用户 API | GET | `/api/user/shop/list` | 403 无权限 | 403 | ✅ |

### 3.2 用户端 API — 直连 Shop Service（共 9 用例）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 4 | 查询活跃店铺列表（X-User-Id） | GET | `/api/user/shop/list?page=1&size=10` | 200 分页列表 | 200 | ✅ |
| 5 | 无 X-User-Id 头 | GET | `/api/user/shop/list` | 400 需登录 | 400 "请先登录" | ✅ |
| 6 | userId 旧名头 | GET | `/api/user/shop/list` | 400 不识别旧头名 | 400 "请先登录" | ✅ |
| 7 | 查询店铺详情 | GET | `/api/user/shop/2061638090027569152` | 200 店铺详情 | 200 | ✅ |
| 8 | 不存在的店铺 | GET | `/api/user/shop/99999999` | 400 友好提示 | 400 "店铺不存在或已关闭" | ✅ |
| 9 | page=-1 分页参数 | GET | `/api/user/shop/list?page=-1` | 400 参数错误 | 400 | ✅ |
| 10 | page=0 分页参数 | GET | `/api/user/shop/list?page=0` | 400 参数错误 | 400 | ✅ |
| 11 | page=abc 分页参数 | GET | `/api/user/shop/list?page=abc` | 400 参数错误 | 400 | ✅ |
| 12 | size=0 分页参数 | GET | `/api/user/shop/list?size=0` | 400 参数错误 | 400 "分页参数错误: size 必须 >= 1" | ✅ |

### 3.3 商家端 API — 通过 Gateway（共 7 用例）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 13 | 查询商家店铺 ID 列表 | GET | `/api/seller/shop/merchant/{merchantId}` | 200 shopIds 列表 | 200 | ✅ |
| 14 | 创建店铺 | POST | `/api/seller/shop/register` | 200 创建成功 | 200 | ✅ |
| 15 | 查询店铺详情 | GET | `/api/seller/shop/{shopId}` | 200 店铺详情 | 200 | ✅ |
| 16 | 查询员工列表 | GET | `/api/seller/shop/{shopId}/employees` | 200 员工列表 | 200 | ✅ |
| 17 | 更新店铺 | PUT | `/api/seller/shop/{shopId}` | 200 更新成功 | 200 | ✅ |
| 18 | 无 token 访问 | GET | `/api/seller/shop/merchant/{merchantId}` | 401 未授权 | 401 | ✅ |
| 19 | 用户 token 访问商家 API | GET | `/api/seller/shop/merchant/{merchantId}` | 403 无权限 | 403 | ✅ |

### 3.4 商家端 API — 直连 Shop Service（共 9 用例）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 20 | 创建店铺 | POST | `/api/seller/shop/register` | 200 创建成功 | 200 | ✅ |
| 21 | 查询商家店铺 ID 列表 | GET | `/api/seller/shop/merchant/{merchantId}` | 200 shopIds 列表 | 200 | ✅ |
| 22 | 查询员工列表 | GET | `/api/seller/shop/{shopId}/employees` | 200 员工列表 | 200 | ✅ |
| 23 | 更新店铺 | PUT | `/api/seller/shop/{shopId}` | 200 更新成功 | 200 | ✅ |
| 24 | 缺少 X-User-Id | POST | `/api/seller/shop/register` | 400 参数错误 | 400 "缺少必要请求头" | ✅ |
| 25 | 关闭店铺 | DELETE | `/api/seller/shop/{shopId}` | 200 关闭成功 | 200 | ✅ |
| 26 | 重新开店 | PUT | `/api/seller/shop/{shopId}/open` | 200 开店成功 | 200 | ✅ |
| 27 | 重复关闭 | DELETE | `/api/seller/shop/{shopId}` | 400 已关闭 | 400 "店铺已关闭或不存在" | ✅ |
| 28 | 关闭后开店 | PUT | `/api/seller/shop/{shopId}/open` | 200 开店成功 | 200 | ✅ |

### 3.5 内部 API（共 6 用例）

| # | 用例 | 方法 | 端点 | 预期结果 | 实际结果 | 状态 |
|---|------|------|------|----------|----------|:----:|
| 29 | 查询商家角色列表 | GET | `/internal/shop/employees/roles/{merchantId}` | 200 | 200 | ✅ |
| 30 | 查询店铺基本信息 | GET | `/internal/shop/info/{shopId}` | 200 ShopInfoDTO | 200 | ✅ |
| 31 | 批量查询（空列表） | POST | `/internal/shop/info/batch` | 200 空 Map | 200 | ✅ |
| 32 | 批量查询（null body） | POST | `/internal/shop/info/batch` | 400 参数错误 | 400 "请求体格式错误" | ✅ |
| 33 | 批量查询（空 body） | POST | `/internal/shop/info/batch` | 400 参数错误 | 400 "请求体格式错误" | ✅ |
| 34 | 非数字 shopId | GET | `/internal/shop/info/abc` | 400 参数错误 | 400 "参数格式错误" | ✅ |

### 3.6 边界/容错测试（共 10 用例）

| # | 用例 | 方法 | 端点/场景 | 预期结果 | 实际结果 | 状态 |
|---|------|------|-----------|----------|----------|:----:|
| 35 | 空名称创建店铺 | POST | `register` name="" | 400 校验错误 | 400 "店铺名称不能为空" | ✅ |
| 36 | 名称超长(>100字符) | POST | `register` name=101字 | 400 校验错误 | 400 "店铺名称最长100个字符" | ✅ |
| 37 | 描述超长(>500字符) | POST | `register` description=501字 | 400 校验错误 | 400 "店铺描述最长500个字符" | ✅ |
| 38 | 不存在店铺（商家端） | GET | `/api/seller/shop/99999999` | 400 友好提示 | 400 "无权限访问该店铺" | ✅ |
| 39 | 更新空名称 | PUT | `/api/seller/shop/{shopId}` name="" | 400 校验错误 | 400 "店铺名称不能为空" | ✅ |
| 40 | 员工账号太短(<3位) | POST | `employees/register` username="ab" | 400 校验错误 | 400 "账号长度需为3-20位" | ✅ |
| 41 | 员工手机号无效 | POST | `employees/register` phone="123" | 400 校验错误 | 400 "手机号格式不正确" | ✅ |
| 42 | 重复关闭（直连） | DELETE | `/api/seller/shop/{shopId}` | 400 已关闭 | 400 "店铺已关闭或不存在" | ✅ |
| 43 | 关闭后开店（直连） | PUT | `/api/seller/shop/{shopId}/open` | 200 开店成功 | 200 | ✅ |
| 44 | 重新开店（已开启） | PUT | `/api/seller/shop/{shopId}/open` | 400 已开启 | 400 "店铺已开启或不存在" | ✅ |

## 4. 测试结果统计

| 维度 | 用户端(GW) | 用户端(直连) | 商家端(GW) | 商家端(直连) | 内部API | 边界/容错 | **合计** |
|------|:----------:|:-----------:|:----------:|:-----------:|:-------:|:---------:|:--------:|
| 总用例数 | 3 | 9 | 7 | 9 | 6 | 10 | **44** |
| 通过 | 3 | 9 | 7 | 9 | 6 | 10 | **44** |
| 失败 | 0 | 0 | 0 | 0 | 0 | 0 | **0** |
| 通过率 | 100% | 100% | 100% | 100% | 100% | 100% | **100%** |

> **全部 44 个用例通过，通过率 100%。所有 Bug 已修复并验证通过。**

## 5. 正常业务流程（直连方式）验证状态

| 流程 | 状态 | 说明 |
|------|:----:|------|
| 商家创建店铺 | ✅ | `createShop` 事务写入三表 |
| 商家查看店铺列表 | ✅ | `getShopIdsByMerchantId` 返回 |
| 商家查看店铺详情 | ✅ | `checkShopAccess` 权限校验后返回 |
| 商家更新店铺信息 | ✅ | 仅店长可操作 |
| 商家关闭/重开店铺 | ✅ | 直连方式正常 |
| 商家查询员工列表 | ✅ | 返回员工列表 |
| 用户端查看店铺列表 | ✅ | 分页查询可用 |
| 用户端查看店铺详情 | ✅ | 仅返回活跃店铺 |
| 服务间调用（内部 API） | ✅ | 3 个内部端点均正常 |

## 6. 已有单测覆盖

| 模块 | 测试文件 | 说明 |
|------|----------|------|
| 商家端 Controller | `ShopMerchantControllerTest.java` | MockMvc |
| 用户端 Controller | `ShopUserControllerTest.java` | MockMvc |
| 内部 Controller | `InternalShopControllerTest.java` | MockMvc |
| Service 层 | `ShopServiceImplTest.java` | Mockito |
| Mapper 集成（Shop） | `ShopMapperTest.java` | @SpringBootTest |
| Mapper 集成（ShopInfo） | `ShopInfoMapperTest.java` | @SpringBootTest |
| Mapper 集成（MerchantRole） | `MerchantRoleMapperTest.java` | @SpringBootTest |
| 全局异常处理 | `GlobalExceptionHandlerTest.java` | MockMvc |

## 7. 结论

Shop 服务共测试 **44 个用例**，**全部通过（100%）**，所有 API 端点功能正确，验证与容错机制正常工作。
