# chat-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28

## 1. 概述

本文档覆盖 chat-service（端口 8085，数据库 eureka_chat）的全部核心逻辑，包括 AI 聊天接口的请求/响应、LangChain4j 工具调用（商品查询、订单查询）、用户上下文解析、DTO 多态序列化以及全局异常处理。

**架构特点：**
- 控制器（ChatController）+ @AiService（Assistant）+ @Tool（ProductTools / OrderTools）三层
- AI 通过 `langchain4j-dashscope` 调用大模型（glm-5.1），`enable-search: false`，`temperature: 0.7`
- 工具层通过 Feign 调用 product-service 和 order-service
- 用户身份从请求头 `X-User-Id` 解析，未携带时抛出异常
- 响应类型 `ApiResponse<AiResponse>`，其中 `AiResponse.data` 为 sealed 接口 `Data`，可序列化为 ProductData 或 OrderData

**测试分级：**
- **P0** - 核心流程（正常聊天、参数校验、工具调用路径、错误处理），必须每轮回归通过
- **P1** - 重要特性（多态序列化、分页/状态过滤、边界参数）
- **P2** - 边缘场景与异常（Feign 异常、非法输入、并发竞态）

---

## 2. 测试环境

| 项目 | 说明 |
|------|------|
| 服务端口 | 8085 |
| 数据库 | eureka_chat |
| 依赖服务 | product-service (Feign)、order-service (Feign) |
| 注册中心 | Eureka Server |
| AI 模型 | Dashscope glm-5.1 (api-key: ${DASHSCOPE_API_KEY}) |
| Mock 策略 | 单元测试 Mock Feign 客户端 + Mock HttpServletRequest；集成测试可嵌入 Dashscope Mock Server |

---

## 3. 测试用例表

### 3.1 AI 聊天接口

**端点：** `POST /chat/chat`（控制器 @RequestMapping("/chat") + @PostMapping("/chat")）
**请求体：** `ChatRequest`（`{ "message": "..." }`，@NotBlank）
**响应：** `ApiResponse<AiResponse>`

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CH-001 | 正常聊天 - 纯文本回复（无工具调用） | 用户发送普通文本问题（如"你好"），无需调用商品/订单工具 | 1. Mock Assistant.chat 返回 AiResponse(message="你好！我是小物", reason="greeting", data=null)<br>2. POST /chat/chat (body: {"message": "你好"})<br>3. 验证返回 | 1. HTTP 200<br>2. ApiResponse.code=200<br>3. ApiResponse.data.message="你好！我是小物"<br>4. ApiResponse.data.reason="greeting"<br>5. ApiResponse.data.data=null | P0 |
| CH-002 | 正常聊天 - 商品查询（工具调用后返回 ProductData） | 用户询问商品列表，AI 调用 getAllProducts 后回复 | 1. Mock Assistant 返回 AiResponse(message="为您找到以下商品", reason="called getAllProducts", data=ProductData(products=[...]))<br>2. POST /chat/chat (body: {"message": "有哪些商品"})<br>3. 验证返回 | 1. HTTP 200<br>2. ApiResponse.data.data.type="product"<br>3. products 数组非空且包含 id/name/price/shopName | P0 |
| CH-003 | 正常聊天 - 订单查询（工具调用后返回 OrderData） | 用户查询订单，AI 调用 OrderTools 后回复 | 1. Mock Assistant 返回 AiResponse(message="您的订单", reason="called getOrderById", data=OrderData(orders=[...]))<br>2. POST /chat/chat (body: {"message": "查一下我的订单"})<br>3. 验证返回 | 1. HTTP 200<br>2. ApiResponse.data.data.type="order"<br>3. orders 数组包含 orderId/orderStatus/totalPrice | P0 |
| CH-004 | 空消息校验 | 请求 message 为空字符串 | 1. POST /chat/chat (body: {"message": ""}) | 1. HTTP 400<br>2. ApiResponse.code=400<br>3. 错误信息提示"消息内容不能为空" | P0 |
| CH-005 | 空白消息校验 | 请求 message 为纯空格字符串 | 1. POST /chat/chat (body: {"message": "   "}) | 1. HTTP 400<br>2. @NotBlank 触发 | P0 |
| CH-006 | 消息体缺少 message 字段 | JSON 中不含 message | 1. POST /chat/chat (body: {}) | 1. HTTP 400<br>2. 校验错误提示 message 不能为 null | P0 |
| CH-007 | null 请求体 | 发送空 body | 1. POST /chat/chat (无 body) | 1. 真正空 body → HTTP 500（HttpMessageNotReadableException，无 body 导致反序列化失败）<br>2. 发送 {} → HTTP 400（@NotBlank 校验触发） | P1 |
| CH-008 | 超长消息 | message 长度超过 10000 字符 | 1. POST /chat/chat (body: {"message": "a..."重复10001次}) | 1. HTTP 200（无长度限制则不报错）<br>2. AI 正常处理或截断（取决于模型限制） | P1 |
| CH-009 | 特化字符消息 | message 含 HTML/JS/JSON 注入内容 | 1. POST /chat/chat (body: {"message": "<script>alert(1)</script>"}) | 1. HTTP 200<br>2. AI 正常处理，消息在 message 字段中原义返回<br>3. 无 XSS 注入风险（纯 JSON 响应） | P2 |
| CH-010 | 多语言消息 | message 包含中/英/日/特殊 Unicode | 1. 分别发送中英文混合、纯日文、Emoji 消息 | 1. 全部返回 200<br>2. 编码无异常，返回 UTF-8 正确显示 | P2 |
| CH-011 | 连续对话（无状态验证） | 当前为 stateless 设计，每次请求独立 | 1. 发送"推荐手机"<br>2. 再发送"刚才推荐的是什么" | 1. 第二次请求无上下文记忆<br>2. AI 仅基于当前消息回复，或提示无历史记录 | P1 |

---

### 3.2 ProductTools（商品查询工具）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CH-012 | getAllProducts - 正常分页 | Mock Feign 返回含 data 数组的 Map（message="查询成功"） | 1. Mock productFeignClient.getAllProducts(0) 返回含两条商品的 Map<br>2. 调用 ProductTools.getAllProducts(0) | 1. 返回 List 长度为 2<br>2. 每条记录含 shopName（从 shop.name 提取）<br>3. shop 字段已被移除 | P0 |
| CH-013 | getAllProducts - 空数据页 | Feign 返回 data 为空的数组 | 1. Mock 返回 data=[] | 1. 返回空 List | P1 |
| CH-014 | getAllProducts - Feign 返回 null | Feign 返回 null | 1. Mock productFeignClient.getAllProducts(0) 返回 null | 1. 返回空 List<br>2. 不抛异常 | P1 |
| CH-015 | getAllProducts - Feign 返回 error | Feign 返回 message≠"查询成功" | 1. Mock 返回 message="系统错误", data=null | 1. 返回空 List | P1 |
| CH-016 | getAllProducts - data 非 List | Feign 返回 data 为 String | 1. Mock 返回 data="error_string" | 1. 返回空 List<br>2. instanceOf List 检查通过 | P2 |
| CH-017 | getAllProducts - 商品无 shop 字段 | 商品数据不含 shop 属性 | 1. Mock 返回 items 不含 shop 字段 | 1. shopName 为 null<br>2. 不抛 NullPointerException<br>3. shop 字段不存在，移除无影响 | P2 |
| CH-018 | getAllProducts - shop 字段非 Map | shop 字段为 String | 1. Mock 返回 shop="invalid" | 1. shopName 为 null<br>2. shop 字段被移除 | P2 |
| CH-019 | getProductDetails - 正常查询 | Mock Feign 返回含 data map 的响应 | 1. Mock getProductByIdExternal(1L) 返回含商品 data 的 Map<br>2. 调用 getProductDetails("1") | 1. 返回完整商品 Map<br>2. 包含 shopName（从 shop.name 提取）<br>3. shop 已被移除 | P0 |
| CH-020 | getProductDetails - 商品 ID 格式错误 | 参数为字母串 | 1. 调用 getProductDetails("abc") | 1. 抛出 AiToolException("商品ID格式不正确") | P0 |
| CH-021 | getProductDetails - 商品 ID 为负数字符串 | 参数为 "-1" | 1. Mock 正常返回（负数 ID 在产品服务侧可能存在） | 1. 按正常流程处理<br>2. 由产品服务决定是否存在 | P2 |
| CH-022 | getProductDetails - Feign 返回 null | Feign 返回 null | 1. Mock getProductByIdExternal(999L) 返回 null | 1. 抛出 AiToolException("id不存在或商品已下架") | P1 |
| CH-023 | getProductDetails - Feign 返回 message≠"查询成功" | Feign 返回错误消息 | 1. Mock 返回 message="商品不存在" | 1. 抛出 AiToolException("id不存在或商品已下架") | P1 |
| CH-024 | getProductDetails - data 非 Map | data 为字符串或数组 | 1. Mock 返回 data="error" | 1. 抛出 AiToolException("id不存在或商品已下架") | P2 |
| CH-025 | getProductDetails - 商品无 shop | data 中不含 shop 字段 | 1. Mock 返回不含 shop 的 data | 1. shopName 为 null<br>2. 不抛异常 | P2 |

---

### 3.3 OrderTools（订单查询工具）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CH-026 | getOrderById - 正常查询 | Mock UserContext.getUserId()=100L；Mock Feign 返回订单 Map | 1. Mock orderFeignClient.getOrderById("ORD001", 100L) 返回订单 Map<br>2. 调用 getOrderById("ORD001") | 1. 返回订单 Map<br>2. 包含 orderId/orderStatus 等字段 | P0 |
| CH-027 | getOrderById - Feign 返回 null | Feign 返回 null | 1. Mock 返回 null | 1. 抛出 AiToolException("订单不存在") | P1 |
| CH-028 | getOrderById - Feign 返回非 Map | Feign 返回 List | 1. Mock 返回 List 对象 | 1. 返回空 HashMap<br>2. 不抛异常 | P2 |
| CH-029 | getOrderById - UserContext 无 request | 无请求上下文 | 1. Mock RequestContextHolder.getRequestAttributes() 返回 null | 1. 抛出 RuntimeException("No request context available") | P0 |
| CH-030 | getOrderById - X-User-Id 缺失 | 有 request 但无 X-User-Id 头 | 1. Mock getHeader("X-User-Id") 返回 null | 1. 抛出 RuntimeException("X-User-Id header is missing") | P0 |
| CH-031 | getAllOrders - 正常查询 | Mock Feign 返回 List | 1. Mock orderFeignClient.getAllOrders(100L) 返回 List<br>2. 调用 getAllOrders() | 1. 返回完整订单列表<br>2. 列表顺序与 Feign 返回一致 | P0 |
| CH-032 | getAllOrders - Feign 返回非 List | Feign 返回 null 或 Map | 1. Mock 返回 null | 1. 返回空 List<br>2. 不抛异常 | P1 |
| CH-033 | getOrdersByStatus - 正常过滤 | getAllOrders 返回多状态订单 | 1. getAllOrders 返回 3 条订单（PAID, PAID, SHIPPED）<br>2. 调用 getOrdersByStatus("PAID") | 1. 返回 2 条，orderStatus=PAID | P0 |
| CH-034 | getOrdersByStatus - 无匹配状态 | 无指定状态订单 | 1. getAllOrders 返回 0 条或全部不匹配 | 1. 返回空 List | P1 |
| CH-035 | getOrdersByStatus - 无效状态值 | 传"INVALID_STATUS" | 1. getAllOrders 返回正常数据<br>2. 调用 getOrdersByStatus("INVALID_STATUS") | 1. 返回空 List（无匹配） | P2 |
| CH-036 | getOrdersByStatus - status 为 null/size=0 字符串 | 传 null 或空串 | 1. 调用 getOrdersByStatus(null) | 1. 返回空列表（Objects.equals 安全处理 null，不会抛 NPE） | P2 |
| CH-037 | OrderTools - 并发调用 | 多线程同时调用 getAllOrders | 1. 10 线程同时调用 getOrdersByStatus("PAID") | 1. 无竞态，返回正确结果<br>2. 每个线程都拿到完整的 PAID 订单列表 | P2 |

---

### 3.4 UserContext

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CH-038 | getUserId - 正常解析 | RequestContextHolder 有 attributes，Header X-User-Id=100 | 1. Mock ServletRequestAttributes.getRequest() 返回含 X-User-Id=100 的 request<br>2. 调用 UserContext.getUserId() | 1. 返回 Long 100 | P0 |
| CH-039 | getUserId - X-User-Id 为数字字符串 | Header 值="42" | 1. Mock 返回 "42" | 1. 返回 Long 42 | P1 |
| CH-040 | getUserId - X-User-Id 为大数字 | Header 值="9999999999" | 1. Mock 返回 "9999999999" | 1. 返回 Long 9999999999 | P1 |
| CH-041 | getUserId - 无请求上下文 | RequestContextHolder.getRequestAttributes() 返回 null | 1. Mock 返回 null<br>2. 调用 getUserId() | 1. 抛出 RuntimeException("No request context available") | P0 |
| CH-042 | getUserId - X-User-Id Header 缺失 | Header 名非 X-User-Id 或不存在 | 1. Mock getHeader("X-User-Id") 返回 null | 1. 抛出 RuntimeException("X-User-Id header is missing") | P0 |
| CH-043 | getUserId - X-User-Id Header 为空串 | Header 值="" | 1. Mock getHeader 返回 "" | 1. 抛出 RuntimeException("X-User-Id header is missing") | P0 |
| CH-044 | getUserId - X-User-Id 为非数字 | Header 值="abc" | 1. Mock 返回 "abc"<br>2. 调用 getUserId() | 1. 抛出 NumberFormatException（由 Long.parseLong 抛出）<br>2. 当前代码未捕获，被全局 Exception 处理器捕获返回 500<br>3. 应关注是否有更优雅的处理方式 | P2 |

---

### 3.5 DTO 序列化/多态

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CH-045 | AiResponse 完整序列化与反序列化 | 标准 Java 对象 | 1. 构造 AiResponse(message="m", reason="r", data=null)<br>2. 序列化为 JSON<br>3. 反序列化回 AiResponse | 1. JSON: {"message":"m","reason":"r","data":null}<br>2. 反序列化后字段一致 | P0 |
| CH-046 | ProductData 序列化 - type=product | ProductData(products=[...]) | 1. 构造 ProductData(items)<br>2. 序列化 | 1. JSON 含 "type":"product"<br>2. 含 "products" 数组 | P0 |
| CH-047 | ProductData 反序列化 - 识别 type=product | JSON: {"type":"product","products":[...]} | 1. 将 JSON 反序列化为 Data 接口 | 1. 正确识别为 ProductData 实例<br>2. instanceof ProductData 为 true | P0 |
| CH-048 | OrderData 序列化 - type=order | OrderData(orders=[...]) | 1. 构造 OrderData(items)<br>2. 序列化 | 1. JSON 含 "type":"order"<br>2. 含 "orders" 数组 | P0 |
| CH-049 | OrderData 反序列化 - 识别 type=order | JSON: {"type":"order","orders":[...]} | 1. JSON 反序列化为 Data | 1. 正确识别为 OrderData 实例<br>2. instanceof OrderData 为 true | P0 |
| CH-050 | ProductItem 完整字段 | 全字段构造 | 1. new ProductItem(1L, "手机", 2999.0, "电子产品", "desc", 100, "url", "shopA")<br>2. 序列化 | 1. 所有字段正确序列化<br>2. stock 为数字，price 为数字 | P1 |
| CH-051 | ProductItem 空字段 | 部分字段为 null | 1. ProductItem(null, null, null, null, null, null, null, null)<br>2. 序列化 | 1. JSON 中 null 字段正确输出为 null<br>2. 不抛序列化异常 | P2 |
| CH-052 | OrderItem 完整字段 | 全字段构造 | 1. new OrderItem("O001", "P001", 2, 5998.0, "PAID", "2026-05-28", "张三", "138xxx", "地址")<br>2. 序列化 | 1. 所有字段正确<br>2. 字段名符合驼峰命名 | P1 |
| CH-053 | AiResponse 中 data 为 null | 纯文本回复场景 | 1. AiResponse(message="你好", reason="greeting", data=null)<br>2. 序列化 | 1. JSON 中 data 为 null | P1 |
| CH-054 | 未知 type 反序列化 | JSON: {"type":"unknown"} | 1. 反序列化 Data | 1. 抛 JsonMappingException（无匹配 subtype）<br>2. 全局异常处理器返回 500 | P2 |
| CH-055 | Data 接口 sealed 限制 | 尝试定义新的实现类 | 1. 编写 class OtherData implements Data | 1. 编译错误（sealed 仅允许 ProductData 和 OrderData）<br>2. 类型安全由编译器保证 | P1 |

---

### 3.6 异常处理

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| CH-056 | ChatException 处理 - 无参构造（默认 code=500） | 服务层抛出 ChatException("自定义错误") | 1. 模拟抛出 ChatException("商品推荐失败")<br>2. 全局异常处理器捕获 | 1. HTTP 500<br>2. ApiResponse.code=500<br>3. message="商品推荐失败" | P0 |
| CH-057 | ChatException 处理 - 带 code 构造 | 抛出 ChatException(4001, "业务错误") | 1. 模拟抛出 ChatException(4001, "业务错误") | 1. HTTP 500（@ResponseStatus 硬编码为 500）<br>2. ApiResponse.code=4001（反映自定义业务码）<br>3. message="业务错误" | P1 |
| CH-058 | MethodArgumentNotValidException 处理 | @Valid 校验失败 | 1. POST /chat/chat (body: {"message": ""}) | 1. HTTP 400<br>2. ApiResponse.code=400<br>3. message="消息内容不能为空"（来自 @NotBlank） | P0 |
| CH-059 | 未知 Exception 处理 | 服务层抛出 RuntimeException | 1. 模拟抛出 ArithmeticException（或任何未检查异常） | 1. HTTP 500<br>2. ApiResponse.code=500<br>3. message="系统错误，请稍后重试" | P0 |
| CH-060 | AiToolException 传递 | ProductTools.getProductDetails("abc") 触发 | 1. POST /chat/chat (body: {"message": "查一下商品abc"})<br>2. AI 调用 ProductTools 时工具方法抛出 AiToolException | 1. AiToolException 继承 RuntimeException<br>2. 被全局 Exception 处理器捕获<br>3. HTTP 500，message="系统错误，请稍后重试"<br>4. 日志记录原始异常信息"商品ID格式不正确" | P1 |
| CH-061 | Feign 调用超时 | product-service 响应超时 | 1. Mock Feign 抛出 ReadTimeoutException<br>2. 调用 ProductTools.getAllProducts(0) | 1. Feign 异常被传递到工具方法<br>2. 未被 ProductTools 捕获，抛出到 AI 层<br>3. 全局异常处理器返回 500<br>4. 日志记录超时堆栈 | P2 |
| CH-062 | Feign 调用返回非 JSON | product-service 返回 HTML 错误页 | 1. Mock Feign 返回 HTML 字符串<br>2. 调用 getAllProducts | 1. Feign 解码异常<br>2. 全局异常处理器返回 500 | P2 |

---

## 4. 测试要点总结

| 维度 | 要点 |
|------|------|
| **核心流程 (P0)** | 聊天接口正常请求/响应；参数校验（空消息、空白消息、缺字段）；数据必须无幻觉（System Prompt 约束） |
| **工具调用** | ProductTools 分页/详情查询需覆盖 Feign 正常返回、null 返回、错误返回、非预期类型返回；OrderTools 需覆盖 UserContext 正常/异常 |
| **UserContext** | 请求上下文为空、X-User-Id 缺失/空串/非数字全部需抛异常；非数字 userId 会抛 NumberFormatException，当前未做特殊处理 |
| **DTO 多态** | sealed 接口 + Jackson @JsonTypeInfo/@JsonSubTypes 保证 type 字段路由；product/order 两种类型必须正确序列化与反序列化；未知 type 报错 |
| **异常传递** | ChatException、MethodArgumentNotValidException、Exception 三层处理；AiToolException 继承 RuntimeException，走默认 Exception 路径返回 500 |
| **幂等/无状态** | 聊天接口为 stateless 设计，每次请求独立，无会话记忆；连续对话需前端维护上下文 |
| **系统提示词** | 必须调用工具后再回答，数据真实无幻觉，输出含 message+reason+data 三段结构——需通过集成测试验证 AI 实际输出符合格式约束 |
