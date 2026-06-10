# Gateway 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| GatewayFullIntegrationTest | 20 | 19 |
| SaTokenAuthGlobalFilterTest | 8 | 8 |
| IpRateLimitFilterTest | 8 | 8 |
| AuthServiceImplTest | 23 | 23 |
| GlobalErrorWebExceptionHandlerTest | 6 | 6 |
| GatewayServiceApplicationTests | 1 | 1 |
| **合计** | **66** | **65（98.5%）** |

## API 端到端集成测试

通过 Gateway 端口 8080 测试路由转发、认证鉴权、角色隔离和错误处理。

| # | 用例 | 方法 | 端点 | 结果 |
|---|------|------|------|:----:|
| 1 | 白名单：check-username | GET | `/api/user/auth/check-username` | ✅ 200 |
| 2 | 白名单：用户注册 | POST | `/api/user/auth/register` | ✅ 200 + token |
| 3 | 白名单：用户登录 | POST | `/api/user/auth/login` | ✅ 200 + token |
| 4 | 有效 Token 访问 | GET | `/api/user/product/all` | ✅ 200 |
| 5 | USER 访问商家 API | GET | `/api/seller/product/list` | ✅ 403 |
| 6 | 无 Token 访问 | GET | `/api/user/product/all` | ✅ 401 |
| 7 | 无效 Token | GET | `/api/user/product/all` | ✅ 401 |
| 8 | 不存在路由 | GET | `/api/nonexistent/xxx` | ✅ 404 |

## 总结

- **单元测试**：66 个运行，65 通过，1 个失败（`GatewayFullIntegrationTest.sellerShopRegisterWhitelist` — 白名单路径 `/api/seller/shop/register` 已在生产配置中移除但测试未同步）
- **API 端到端测试**：8 个关键场景全部通过，认证/鉴权/路由/角色隔离均正常

## 未修复 BUG

| BUG | 描述 |
|-----|------|
| sellerShopRegisterWhitelist 测试失败 | 测试引用的白名单路径 `/api/seller/shop/register` 仅存在于测试配置中，生产代码只有 8 个白名单路径 |
| AuthServiceImplTest 测试数不匹配 | 报告声称 31 个，实际 23 个（`validateToken`/`extractRole` 测试已重构移除） |
| SaTokenAuthGlobalFilterTest 测试数 | 报告声称 7 个，实际 8 个（新增 header 注入测试未同步更新报告） |
| GatewayFullIntegrationTest 测试数 | 报告声称 19 个，实际 20 个（同上） |
