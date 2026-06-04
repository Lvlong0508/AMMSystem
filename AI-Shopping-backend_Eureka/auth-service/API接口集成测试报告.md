# Auth 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| UserAuthControllerTest | 17 | 17 |
| MerchantAuthControllerTest | 4 | 4 |
| InternalControllerTest | 3 | 3 |
| UserAuthServiceImplTest | 17 | 17 |
| MerchantAuthServiceImplTest | 20 | 20 |
| UserInfoServiceImplTest | 4 | 4 |
| MerchantInfoServiceImplTest | 4 | 4 |
| UserMapperTest | 10 | 10 |
| MerchantMapperTest | 11 | 11 |
| UserInfoMapperTest | 4 | 4 |
| MerchantInfoMapperTest | 4 | 4 |
| BCryptUtilTest | 4 | 4 |
| AuthConverterTest | 4 | 4 |
| AuthServiceApplicationTests | 1 | 1 |
| **合计** | **107** | **107（100%）** |

## API 端到端集成测试

所有端点 10/10 全部通过。注册即登录，返回 token + userInfo/merchantInfo + accountType。用户名/手机号唯一校验正常。错误密码/不存在用户均返回 400 "用户名或密码错误"。

### 用户端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| GET | `/api/user/auth/check-username?username=` | ✅ |
| GET | `/api/user/auth/check-phone?phone=` | ✅ |
| POST | `/api/user/auth/register` | ✅ |
| POST | `/api/user/auth/login` | ✅ |

### 商家端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| GET | `/api/seller/auth/check-username?username=` | ✅ |
| POST | `/api/seller/auth/register` | ✅ |
| POST | `/api/seller/auth/login` | ✅ |

## 总结

- **单元测试**：107 个全部通过，覆盖 Controller、Service、Mapper、Converter、ExceptionHandler 各层
- **API 端到端测试**：10 个端点全部通过，覆盖用户端、商家端的注册/登录/用户名检查/手机号检查成功路径及重复注册/密码错误/用户不存在等容错路径。email 字段注册验证正常
- **未修复 BUG**：无
