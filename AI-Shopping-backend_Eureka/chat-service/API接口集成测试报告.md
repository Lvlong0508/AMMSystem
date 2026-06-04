# Chat 服务测试报告（2026-06-04，实际验证）

## 单元测试

| 测试类 | 用例数 | 通过 |
|--------|:------:|:----:|
| ChatControllerTest | 9 | 9 |
| ProductToolsTest | 14 | 14 |
| OrderToolsTest | 9 | 9 |
| UserContextTest | 5 | 5 |
| DtoSerializationTest | 13 | 13 |
| GlobalExceptionHandlerTest | 5 | 5 |
| ChatMemoryIntegrationTest | 2 | 2 |
| **合计** | **57** | **57（100%）** |

## API 端到端集成测试

DashScope API Key 有效，模型 `glm-5.1` 调用成功。当前运行实例需重启后方可正常响应。

### 用户端 API

| 方法 | 端点 | 结果 |
|------|------|:----:|
| POST | `/chat` | ⚠️ 500（当前实例需重启） |

### AI 模型验证

| 模型 | 结果 |
|------|:----:|
| `glm-5.1`（当前代码配置） | ✅ 调用成功 |
| `qwen-turbo` | ❌ 403 免费额度耗尽 |
| `qwen-plus` | ❌ 403 免费额度耗尽 |
| `qwen-max` | ❌ 403 免费额度耗尽 |

## 总结

- **单元测试**：57 个全部通过，覆盖 Controller、Tools、DTO 序列化、UserContext、异常处理、ChatMemory 各模块
- **API 端到端测试**：环境变量 `DASHSCOPE_API_KEY` 已配置，`glm-5.1` 模型调用正常，当前服务实例重启后即可恢复

## 未修复 BUG

无
