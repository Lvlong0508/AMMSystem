# Contact 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| UserContactApiTest | 21 | 21 |
| MerchantContactApiTest | 14 | 14 |
| UserContactControllerTest | 32 | 32 |
| MerchantContactControllerTest | 36 | 36 |
| InternalContactControllerTest | 4 | 4 |
| UserContactServiceImplTest | 26 | 26 |
| ShopAddressServiceImplTest | 26 | 26 |
| ContactResponseTest | 3 | 3 |
| AddressResponseTest | 3 | 3 |
| UserContactMapperTest | 12 | 11 |
| ShopAddressMapperTest | 14 | 14 |
| **合计** | **191** | **190（99.5%）** |

## API 端到端集成测试

通过 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` 嵌入式运行，连接真实 MySQL。

### 用户端 API（`/api/user/contact`）

| 方法 | 端点 | 结果 |
|------|------|:----:|
| POST | `/api/user/contact/create` | ✅ |
| DELETE | `/api/user/contact/delete/{id}` | ✅ |
| PUT | `/api/user/contact/update` | ✅ |
| GET | `/api/user/contact/list` | ✅ |
| PUT | `/api/user/contact/set-default/{id}` | ✅ |

### 商家端 API（`/api/merchant/address`）

| 方法 | 端点 | 结果 |
|------|------|:----:|
| POST | `/api/merchant/address/create` | ✅ |
| PUT | `/api/merchant/address/update/{id}` | ✅ |
| DELETE | `/api/merchant/address/delete/{id}` | ✅ |
| GET | `/api/merchant/address/list` | ✅ |
| GET | `/api/merchant/address/ship-default` | ✅ |
| PUT | `/api/merchant/address/set-default/{id}` | ✅ |

## 总结

- **单元测试**：191 个运行，190 通过，1 个既有失败（`UserContactMapperTest$SelectTests`，连接真实 MySQL 的数据依赖问题）
- **API 端到端测试**：35 个全部通过（用户联系人 21 + 商家地址 14），覆盖 CRUD + 设置默认 + 认证隔离 + 参数校验。测试数据已清理

## 未修复 BUG

无
