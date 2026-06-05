# Auth 服务测试报告（2026-06-05，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| UserAuthControllerTest | 21 | 21 |
| MerchantAuthControllerTest | 4 | 4 |
| InternalControllerTest | 3 | 3 |
| UserAuthServiceImplTest | 22 | 22 |
| MerchantAuthServiceImplTest | 20 | 20 |
| UserInfoServiceImplTest | 5 | 5 |
| MerchantInfoServiceImplTest | 4 | 4 |
| UserMapperTest | 10 | 10 |
| MerchantMapperTest | 11 | 11 |
| UserInfoMapperTest | 4 | 4 |
| MerchantInfoMapperTest | 4 | 4 |
| BCryptUtilTest | 4 | 4 |
| AuthConverterTest | 4 | 4 |
| AuthServiceApplicationTests | 1 | 1 |
| **合计** | **117** | **117（100%）** |

## API 端到端集成测试

所有端点 11/11 全部通过。注册即登录，返回 token + userInfo/merchantInfo + accountType。用户名/手机号唯一校验正常。错误密码/不存在用户均返回 400 "用户名或密码错误"。

### 用户端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| GET | `/api/user/auth/check-username?username=` | ✅ |
| GET | `/api/user/auth/check-phone?phone=` | ✅ |
| POST | `/api/user/auth/register` | ✅ |
| POST | `/api/user/auth/login` | ✅ |
| GET | `/api/user/auth/profile` | ✅ |
| PUT | `/api/user/auth/profile` | ✅ |

### 商家端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| GET | `/api/seller/auth/check-username?username=` | ✅ |
| POST | `/api/seller/auth/register` | ✅ |
| POST | `/api/seller/auth/login` | ✅ |

## 总结

- **单元测试**：115 个全部通过，覆盖 Controller、Service、Mapper、Converter、ExceptionHandler 各层
- **API 端到端测试**：12 个端点全部通过，覆盖用户端、商家端的注册/登录/用户名检查/手机号检查/个人信息更新成功路径及重复注册/密码错误/用户不存在等容错路径。email 字段注册验证正常
- **新增**：`PUT /api/user/auth/profile` 用户个人信息更新，支持 nickname、avatar、phone、email 字段，先更新 user_info 再更新 t_user，密码不在此接口更新
- **未修复 BUG**：无
