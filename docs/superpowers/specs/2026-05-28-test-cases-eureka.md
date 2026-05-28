# eureka-server 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28 | 项目: AI-Shopping 智能购物平台

## 1. 概述

eureka-server 是 AI-Shopping 平台的注册中心，基于 Spring Cloud Netflix Eureka Server 实现。该模块**不包含**任何业务 Controller、Service 或 Entity，职责仅限于服务注册与发现、Eureka Dashboard 可视化、健康检查端点暴露以及 Basic 认证安全防护。

**测试范围**: 应用启动与上下文加载、Eureka Dashboard UI 访问、Eureka REST API（注册/心跳/摘除/下线）、安全认证与 CSRF 防护、健康检查端点、配置生效性验证、自我保护与 eviction 行为。

**测试类型**: 单元测试（配置类）+ 集成测试（启动上下文 + MockMVC）。

## 2. 测试环境

- **JDK**: 17
- **Spring Boot**: 3.2.3
- **Spring Cloud**: Netflix Eureka Server
- **端口**: 8761
- **安全账号**: admin / admin
- **测试框架**: JUnit 5 + Spring Boot Test + MockMvc
- **依赖**: `spring-cloud-starter-netflix-eureka-server`, `spring-boot-starter-security`

## 3. 测试用例表

### 3.1 应用启动与上下文加载

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-001 | 应用上下文加载成功 | 无 | 1. 使用 `@SpringBootTest` 启动应用上下文 | 1. 应用上下文加载成功，无 Bean 创建失败异常 | P0 |
| EU-002 | 指定端口启动 | 应用已关闭 | 1. 启动应用<br>2. 检查 `server.port` 配置值 | 1. 应用在 8761 端口启动成功 | P0 |
| EU-003 | `EurekaServerApplication` 主类加载 | 无 | 1. 使用 `@SpringBootTest` 加载上下文<br>2. 通过 ApplicationContext 检查主类 Bean | 1. 主类对应的 Application 对象存在 | P0 |
| EU-004 | 禁用自身注册 | 应用已启动 | 1. 检查 Eureka 客户端配置 `register-with-eureka` | 1. 值为 `false`，eureka-server 不向自己注册 | P0 |
| EU-005 | 禁用自身拉取 | 应用已启动 | 1. 检查 Eureka 客户端配置 `fetch-registry` | 1. 值为 `false`，eureka-server 不从自己拉取注册表 | P0 |

### 3.2 Eureka Dashboard UI 访问

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-006 | 未认证访问 Dashboard 跳转登录 | 应用已启动 | 1. 浏览器访问 `http://localhost:8761/`<br>2. 不携带 Basic 认证头 | 1. 返回 HTTP 401 Unauthorized<br>2. 弹出浏览器 Basic 认证对话框 | P0 |
| EU-007 | 正确凭据访问 Dashboard | 应用已启动 | 1. 使用 `admin` / `admin` 的 Basic 认证头<br>2. GET 请求 `http://localhost:8761/` | 1. 返回 HTTP 200<br>2. 响应包含 Eureka Dashboard HTML 页面<br>3. 页面显示 "Instances currently registered with Eureka" | P0 |
| EU-008 | 错误密码访问 Dashboard | 应用已启动 | 1. 使用 `admin` / `wrong` 的 Basic 认证头<br>2. GET 请求 `http://localhost:8761/` | 1. 返回 HTTP 401 Unauthorized | P0 |
| EU-009 | Dashboard 页面 CSS 正常加载 | 应用已启动 | 1. 使用正确 Basic 认证<br>2. GET 请求 `/eureka/css/wro.css` | 1. 返回 HTTP 200<br>2. Content-Type 为 `text/css` | P1 |
| EU-010 | Dashboard 页面 JS 正常加载 | 应用已启动 | 1. 使用正确 Basic 认证<br>2. GET 请求 `/eureka/js/wro.js` | 1. 返回 HTTP 200<br>2. Content-Type 为 `application/javascript` | P1 |
| EU-011 | Dashboard 页面 lastN 标签页数据 | 应用已启动，且有服务注册过 | 1. 使用正确 Basic 认证<br>2. GET 请求 `/eureka/lastn` | 1. 返回 HTTP 200<br>2. 页面包含最近注册/心跳信息 | P2 |

### 3.3 Eureka REST API（服务注册与发现）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-012 | 服务注册端点无需认证 | 应用已启动 | 1. POST 请求 `/eureka/apps/MY-SERVICE`<br>2. 携带合法 InstanceInfo XML/JSON<br>3. **不携带** Basic 认证头 | 1. 返回 HTTP 204 No Content<br>2. 服务成功注册到 Eureka | P0 |
| EU-013 | 服务心跳续约 | 某服务已注册 | 1. PUT 请求 `/eureka/apps/MY-SERVICE/instance-id`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200 OK | P0 |
| EU-014 | 服务心跳续约（服务不存在） | 无注册 | 1. PUT 请求 `/eureka/apps/NONEXIST/unknown-id`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 404 Not Found | P1 |
| EU-015 | 获取注册表（全量） | 无其他服务注册 | 1. GET 请求 `/eureka/apps`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. XML/JSON 中 `applications` 的 `versions__delta` 为 `1` | P0 |
| EU-016 | 获取注册表（增量） | 有服务注册或下线 | 1. GET 请求 `/eureka/apps/delta`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. 响应包含最近变化的服务实例列表 | P1 |
| EU-017 | 获取指定服务注册信息 | 某服务已注册 | 1. GET 请求 `/eureka/apps/MY-SERVICE`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. 响应中包含该服务的 InstanceInfo | P0 |
| EU-018 | 获取指定服务实例信息 | 某服务某实例已注册 | 1. GET 请求 `/eureka/apps/MY-SERVICE/instance-id`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. 响应中包含指定实例的 InstanceInfo | P1 |
| EU-019 | 服务下线 | 某服务已注册 | 1. DELETE 请求 `/eureka/apps/MY-SERVICE/instance-id`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200 OK<br>2. 再次查询该实例返回 404 | P0 |
| EU-020 | 服务下线（实例不存在） | 无注册 | 1. DELETE 请求 `/eureka/apps/NONEXIST/unknown-id`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 404 Not Found | P2 |
| EU-021 | 服务下线（重复下线） | 某实例已下线 | 1. DELETE 请求 `/eureka/apps/MY-SERVICE/instance-id`<br>2. 对已下线实例再次下线 | 1. 返回 HTTP 404 Not Found | P2 |

### 3.4 Eureka REST API（状态与元数据）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-022 | 服务实例状态更新 | 某实例已注册且状态为 UP | 1. PUT 请求 `/eureka/apps/MY-SERVICE/instance-id/status?value=DOWN`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200 OK | P1 |
| EU-023 | 服务实例状态覆盖删除 | 某实例已设置状态覆盖 | 1. DELETE 请求 `/eureka/apps/MY-SERVICE/instance-id/status`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200 OK | P2 |
| EU-024 | 批量查询对等体复制状态 | 应用已启动 | 1. GET 请求 `/eureka/vips/MY-SERVICE`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. 响应包含该 VIP 地址的实例列表 | P2 |
| EU-025 | 安全虚拟 IP 查询 | 应用已启动 | 1. GET 请求 `/eureka/svips/MY-SERVICE`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. 响应包含该 SVIP 地址的实例列表 | P2 |

### 3.5 安全配置

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-026 | CSRF 已禁用 | 应用已启动 | 1. 使用正确 Basic 认证<br>2. POST 请求 `/eureka/apps/ANY-SERVICE`<br>3. **不携带** CSRF Token | 1. 返回 HTTP 204 或合理业务响应（非 403） | P0 |
| EU-027 | 静态资源路径无需认证 | 应用已启动 | 1. GET 请求 `/eureka/css/wro.css`<br>2. **不携带** Basic 认证头 | 1. 返回 HTTP 200 | P1 |
| EU-028 | Eureka REST 端点无需认证 | 应用已启动 | 1. POST 请求 `/eureka/apps/TEST-SERVICE`<br>2. **不携带** Basic 认证头 | 1. 返回 HTTP 204 No Content（注册成功）或 400（非法数据）<br>2. **绝不返回 401** | P0 |
| EU-029 | Actuator 端点无需认证 | 应用已启动 | 1. GET 请求 `/actuator/health`<br>2. **不携带** Basic 认证头 | 1. 返回 HTTP 200<br>2. JSON 中 `status` 为 `UP` | P1 |
| EU-030 | 任意非白名单路径需认证 | 应用已启动 | 1. GET 请求 `/some-random-path`<br>2. 不携带 Basic 认证头 | 1. 返回 HTTP 401 Unauthorized | P1 |
| EU-031 | Basic 认证配置生效 | 应用已启动 | 1. 使用 `admin` / `admin` 的 Basic 认证头<br>2. GET 请求 Dashboard (`/`)<br>3. 验证响应中包含 Eureka Dashboard 内容 | 1. 返回 HTTP 200<br>2. 页面标题或 `<h1>` 包含 "Eureka" | P0 |
| EU-032 | Actuator 端点列表可访问 | 应用已启动 | 1. GET 请求 `/actuator`<br>2. 不携带 Basic 认证 | 1. 返回 HTTP 200<br>2. JSON 中 `_links` 包含 `health` 等端点 | P2 |

### 3.6 配置生效性验证

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-033 | 自我保护模式开启 | 应用已启动 | 1. 通过 Actuator 或 Eureka 内部 API 检查 `enableSelfPreservation` | 1. 自我保护模式为 `true` | P1 |
| EU-034 | eviction 间隔配置 | 应用已启动 | 1. 检查 Eureka Server 内部配置 `evictionIntervalTimerInMs` | 1. 值为 5000ms | P1 |
| EU-035 | Eureka serviceUrl 包含认证信息 | 应用已启动 | 1. 检查 Eureka 客户端配置 `serviceUrl.defaultZone` | 1. URL 格式为 `http://admin:admin@localhost:8761/eureka/` | P1 |
| EU-036 | 应用名称配置 | 应用已启动 | 1. 检查 `spring.application.name` | 1. 值为 `eureka-server` | P1 |

### 3.7 服务注册生命周期（综合场景）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-037 | 注册 → 心跳 → 查询 完整链路 | 应用已启动 | 1. POST 注册服务 A，实例 ID 为 `instance-1`<br>2. 等待 2 秒<br>3. PUT 心跳续约<br>4. GET 查询 `/eureka/apps`<br>5. GET 查询 `/eureka/apps/A/instance-1` | 1. 注册返回 204<br>2. 心跳返回 200<br>3. 全量注册表包含 A<br>4. 实例具体信息正确 | P0 |
| EU-038 | 服务下线后注册表不再包含 | 某服务已注册 | 1. DELETE 下线实例<br>2. GET 查询全量注册表<br>3. GET 查询该实例 | 1. 下线返回 200<br>2. 全量注册表中不再包含该实例<br>3. 具体查询返回 404 | P0 |
| EU-039 | 同一服务多实例注册 | 应用已启动 | 1. POST 注册服务 B 实例 `b-1`<br>2. POST 注册服务 B 实例 `b-2`<br>3. GET 查询 `/eureka/apps/B` | 1. 两次注册均返回 204<br>2. 查询 B 返回 2 个实例 | P1 |
| EU-040 | 实例状态更新后查询同步 | 某实例已注册（UP 状态） | 1. PUT 设置实例状态为 `DOWN`<br>2. GET 查询该实例 | 1. 状态更新返回 200<br>2. 实例状态字段变为 `DOWN` | P1 |
| EU-041 | 状态覆盖删除后恢复 | 某实例状态被设为 DOWN | 1. DELETE 删除状态覆盖<br>2. GET 查询该实例 | 1. 删除返回 200<br>2. 实例状态恢复为注册时声明的状态 | P2 |

### 3.8 自我保护与 Eviction

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|---------|---------|---------|--------|
| EU-042 | 服务未按时心跳被 evict | 某服务已注册但停止心跳 | 1. 注册服务 C 实例 `c-1`<br>2. **不发送心跳**<br>3. 等待超过 eviction 阈值（默认 90s + 5s 间隔）<br>4. GET 查询 `/eureka/apps/C` | 1. 最终该实例被剔除（但因自我保护开启，需大流量场景才能触发剔除） | P2 |
| EU-043 | 自我保护模式下不剔除实例 | 应用以自我保护模式运行（阈值 > 85%） | 1. 模拟大量注册实例<br>2. 所有实例停止心跳<br>3. 等待 eviction 周期 | 1. 自我保护模式激活<br>2. 注册表保留所有实例不被剔除 | P2 |

## 4. 测试要点总结

### 4.1 核心覆盖矩阵

| 测试维度 | 覆盖用例 | 说明 |
|---------|---------|------|
| 应用上下文加载 | EU-001 ~ EU-005 | 验证启动配置与基础属性 |
| Eureka Dashboard | EU-006 ~ EU-011 | 验证 UI 页面在 Basic 认证下的可访问性 |
| 服务注册 API | EU-012, EU-037 | 注册接口无需认证的关键行为 |
| 服务心跳 API | EU-013 ~ EU-014 | 续约和异常场景 |
| 注册表查询 | EU-015 ~ EU-018, EU-037 | 全量、增量、按服务、按实例四种查询 |
| 服务下线 | EU-019 ~ EU-021, EU-038 | 正常下线、不存在、重复下线 |
| 实例状态管理 | EU-022 ~ EU-023, EU-040 ~ EU-041 | 状态设置、覆盖、删除与恢复 |
| 安全认证 | EU-006 ~ EU-008, EU-026 ~ EU-032 | CSRF 禁用、白名单、Basic 认证 |
| 配置有效性 | EU-033 ~ EU-036 | 自我保护、eviction 间隔、URL、app name |
| 多实例场景 | EU-039 | 同服务多实例注册 |
| 自我保护 | EU-042 ~ EU-043 | eviction 行为与自我保护模式 |
| VIP/SVIP 查询 | EU-024 ~ EU-025 | 批量查询端点 |

### 4.2 测试优先级分布

| 优先级 | 数量 | 占比 | 说明 |
|--------|------|------|------|
| P0 | 10 | 23.3% | 核心功能：启动、注册、心跳、Dashboard 认证、CSRF 禁用 |
| P1 | 16 | 37.2% | 重要功能：增量查询、状态更新、静态资源、配置验证、多实例 |
| P2 | 17 | 39.5% | 边缘功能：VIP/SVIP 查询、状态覆盖删除、eviction、自我保护 |

### 4.3 关键测试原则

1. **Eureka REST API 端点无需认证** — `/eureka/apps/**`、`/eureka/svc/**`、`/eureka/delta/**` 所有客户端调用都应免认证；只有 Dashboard UI 页面（`/`）需要 Basic 认证。
2. **CSRF 已禁用** — 所有 POST/PUT/DELETE 请求不需要 CSRF Token，Eureka 客户端用 HTTP 基本操作即可完成注册。
3. **自我保护模式开启** — 生产环境下 eviction 行为受自我保护影响，测试中 eviction 场景需注意该配置。
4. **多实例注册中的 ID 唯一性** — Eureka 按 `appName` + `instanceId` 区分实例，同一 instanceId 的重复注册应视为心跳续约而非创建新实例。
