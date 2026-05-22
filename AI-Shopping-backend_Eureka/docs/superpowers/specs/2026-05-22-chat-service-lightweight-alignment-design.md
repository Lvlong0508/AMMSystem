# Chat Service 轻量对齐设计

## 背景

Chat Service 与 Auth Service 在架构规范上存在显著差距。本方案以最小改动对齐 Auth Service 的响应格式、异常处理、路由和安全性规范。

## 变更范围

### 1. 响应格式对齐

- Controller 返回 `ApiResponse<String>` 替代 `Map<String, String>`
- 所有响应走统一格式 `{code, message, data}`

### 2. 异常处理

- 新增 `ChatException`（继承 `RuntimeException`，含 code/message）
- 新增 `GlobalExceptionHandler`（`@RestControllerAdvice`，处理 `ChatException`、`MethodArgumentNotValidException`、通用 `Exception`）

### 3. 路由对齐

- Controller 基路径从 `/chat` 改为 `/api/user/chat`
- 与网关路由规则 `gateway-service → /api/user/chat → chat-service:8085` 一致

### 4. 参数校验

- 新增 `ChatRequest` DTO（含 `@NotBlank String message`）
- Controller 参数加上 `@RequestBody @Valid`

### 5. API Key 安全

- `application.yml` 中 `api-key` 改为 `${DASHSCOPE_API_KEY}`，从环境变量读取

### 6. 目录结构变更

```
chat-service/src/main/java/com/gzasc/aishopping/chat/
├── ChatServiceApplication.java    （不变）
├── controller/
│   ├── ChatController.java        （修改：ApiResponse + 路由 + 校验）
│   └── GlobalExceptionHandler.java（新增）
├── dto/
│   └── ChatRequest.java           （新增）
├── exception/
│   └── ChatException.java         （新增）
├── AiService/                     （不变）
├── tools/                         （不变）
└── utils/                         （不变）
```

### 7. 测试：SDK 配置读取验证

新增测试类 `ChatServiceApplicationTests`，验证 LangChain4j DashScope SDK 配置是否正确加载：
- `dashscope.chat-model.api-key` 是否可读取
- `dashscope.chat-model.model-name` 是否为 `glm-5.1`
- `dashscope.chat-model.temperature` 是否加载

使用 `@SpringBootTest` + `@TestPropertySource` + 环境变量 mock 覆盖 api-key。

## 不变部分

- Tools 层（ProductTools、OrderTools）不动
- AiService（Assistant）不动
- AiToolFormatter 不动
- 无 Service 层引入（保持扁平）

## 影响范围

| 影响项 | 说明 |
|--------|------|
| 前端 `frontier-user` | 需确认是否依赖原始 `Map` 格式响应 |
| 网关路由 | 无需改动，新增 `/api/user/chat` 路由已在网关配置中 |
| 其他服务 | 无影响 |
