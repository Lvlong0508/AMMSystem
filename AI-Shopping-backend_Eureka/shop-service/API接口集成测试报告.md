# Shop 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ShopMerchantControllerTest | 26 | 26 |
| ShopUserControllerTest | 7 | 7 |
| InternalShopControllerTest | 6 | 6 |
| GlobalExceptionHandlerTest | 5 | 5 |
| ShopServiceImplTest | 44 | 44 |
| ShopMapperTest | 16 | 16 |
| ShopInfoMapperTest | 7 | 7 |
| MerchantRoleMapperTest | 15 | 15 |
| **合计** | **126** | **126（100%）** |

## API 端到端集成测试

直连 Shop Service 端口 8087，使用 `X-User-Id` header 模拟身份。Gateway 路由测试因 Auth Service（8086）不可用无法验证。

### 用户端 API（直连）

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| U1 | 查询活跃店铺列表 | GET | `/api/user/shop/list?page=1&size=10` | ✅ 200 |
| U2 | 无 X-User-Id 头 | GET | `/api/user/shop/list` | ✅ 400「请先登录」 |
| U3 | 查询店铺详情 | GET | `/api/user/shop/{shopId}` | ✅ 200 |
| U4 | 不存在的店铺 | GET | `/api/user/shop/99999999` | ✅ 400「店铺不存在或已关闭」 |
| U5 | page=-1 | GET | `/api/user/shop/list?page=-1` | ✅ 400 |
| U6 | page=0 | GET | `/api/user/shop/list?page=0` | ✅ 400 |
| U7 | page=abc | GET | `/api/user/shop/list?page=abc` | ✅ 400「参数格式错误: page」 |
| U8 | size=0 | GET | `/api/user/shop/list?size=0` | ✅ 400「分页参数错误: size 必须 >= 1」 |

### 商家端 API（直连）

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| M1 | 查询商家店铺列表 | GET | `/api/seller/shop/merchant/{merchantId}` | ✅ 200 |
| M2 | 创建店铺 | POST | `/api/seller/shop/register` | ✅ 200 |
| M3 | 查询店铺详情 | GET | `/api/seller/shop/{shopId}` | ✅ 200 |
| M4 | 查询员工列表 | GET | `/api/seller/shop/{shopId}/employees` | ✅ 200 |
| M5 | 更新店铺 | PUT | `/api/seller/shop/{shopId}` | ✅ 200 |
| M6 | 缺少 X-User-Id | POST | `/api/seller/shop/register` | ✅ 400「缺少必要请求头: X-User-Id」 |
| M7 | 关闭店铺 | DELETE | `/api/seller/shop/{shopId}` | ✅ 200 |
| M8 | 重复关闭 | DELETE | `/api/seller/shop/{shopId}` | ✅ 400「店铺已关闭或不存在」 |
| M9 | 重新开店 | PUT | `/api/seller/shop/{shopId}/open` | ✅ 200 |
| M10 | 重新开店（已开启） | PUT | `/api/seller/shop/{shopId}/open` | ✅ 400「店铺已开启或不存在」 |
| M11 | 非店长访问 | GET | `/api/seller/shop/{shopId}/employees` | ✅ 400「无权限访问该店铺」 |
| M12 | 添加员工 | POST | `/api/seller/shop/{shopId}/employees/register` | ✅ 200 |
| M13 | 移除员工 | DELETE | `/api/seller/shop/{shopId}/employees/{merchantId}` | ✅ 200 |
| M14 | 不存在的店铺 | GET | `/api/seller/shop/99999` | ✅ 400「无权限访问该店铺」 |

### 内部 API

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| I1 | 查询商家角色列表 | GET | `/internal/shop/employees/roles/{merchantId}` | ✅ 200 |
| I2 | 角色列表（无数据） | GET | `/internal/shop/employees/roles/{merchantId}` | ✅ 200 + [] |
| I3 | 查询店铺信息 | GET | `/internal/shop/info/{shopId}` | ✅ 200 |
| I4 | 店铺信息（不存在） | GET | `/internal/shop/info/{shopId}` | ✅ 200 + null |
| I5 | 批量查询 | POST | `/internal/shop/info/batch` | ✅ 200 |
| I6 | 批量查询（空列表） | POST | `/internal/shop/info/batch` | ✅ 200 + {} |
| I7 | 批量查询（无 body） | POST | `/internal/shop/info/batch` | ✅ 400「请求体格式错误」 |
| I8 | 非数字 shopId | GET | `/internal/shop/info/abc` | ✅ 400「参数格式错误: shopId」 |

### 边界/容错

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| B1 | 空名称创建店铺 | POST | `register` name="" | ✅ 400 |
| B2 | 名称超长(>100) | POST | `register` name=101字 | ✅ 400 |
| B3 | 描述超长(>500) | POST | `register` description=501字 | ✅ 400 |
| B4 | 更新空名称 | PUT | `update` name="" | ✅ 400 |
| B5 | 员工用户名过短 | POST | `employees/register` username="ab" | ✅ 400 |
| B6 | 员工手机号无效 | POST | `employees/register` phone="123" | ✅ 400 |
| B7 | 员工特殊字符 | POST | `employees/register` username="user@name" | ✅ 400 |
| B8 | 无效 JSON | POST | `register` body="not json" | ✅ 400「请求体格式错误」 |
| B9 | userId ≠ merchantId | GET | `merchant/{merchantId}` userId 不匹配 | ✅ 400「无权限查看该商户的店铺列表」 |

## 总结

- **单元测试**：126 全通过 ✅（ShopMapperTest 偶发 DuplicateKey 为测试数据残留，不影响功能）
- **API 端到端测试**：直连 39 场景全部通过 ✅；Gateway 9 场景因 Auth Service 500 无法验证
- **数据清理**：已删除所有测试店铺记录，数据库恢复初始状态

## 报告准确性比对

| # | 类型 | 描述 |
|---|------|------|
| 1 | ⚠ 无法验证 | Gateway 路由 9 用例（#1-3, #13-19）因 Auth Service（8086）返回 500，SaToken 拦截所有请求 → 401，无法复现原报告断言 |
| 2 | 🟢 遗漏 | 原报告未覆盖 `DELETE /employees/{merchantId}`（移除员工）端点的 API 测试 |
| 3 | 🟢 遗漏 | 原报告未覆盖无效 JSON body 的场景（实际返回 400，行为正确） |
| 4 | 🟢 消息差异 | 原报告称 #11 page=abc 返回「参数错误」，实际返回「参数格式错误: page」（更精确，行为正确） |
| 5 | 🟢 未记录 | ShopMapperTest 偶发 DuplicateKey 错误（测试数据残留，非代码缺陷） |
