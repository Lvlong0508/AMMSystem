# AI-Shopping-backend Eureka 微服务架构

基于 Spring Cloud Netflix Eureka 的微服务版本，与原单体应用 `AI-Shopping-backend` 并存。

## 架构概览

### 核心服务

| 服务 | 端口 | 职责 |
|------|------|------|
| eureka-server | 8761 | 服务注册与发现中心 |
| gateway-service | 8080 | API网关（统一入口、路由转发） |
| auth-service | 8086 | 认证服务（用户/商家登录鉴权） |
| product-service | 8081 | 商品管理（CRUD、库存） |
| order-service | 8082 | 订单管理（下单、状态流转） |
| contact-service | 8083 | 联系人/收货地址管理 |
| logistics-service | 8084 | 物流跟踪、发货管理 |
| chat-service | 8085 | AI智能对话（通义千问） |

### API 模块

| 模块 | 说明 |
|------|------|
| product-api | 商品服务 Feign 接口定义 |
| order-api | 订单服务 Feign 接口定义 |
| contact-api | 联系人服务 Feign 接口定义 |
| logistics-api | 物流服务 Feign 接口定义 |

## 技术栈

- Java 17
- Spring Boot 3.2.3
- Spring Cloud 2023.0.x
- Spring Cloud Netflix Eureka
- OpenFeign（服务间调用）
- MyBatis 3.0.3
- MySQL 8.x
- Redis（订单号生成）
- LangChain4j 0.35.0（AI能力）

## 快速开始

### 1. 环境准备

- JDK 17+
- MySQL 8.x
- Redis 5.x+
- Maven 3.8+

### 2. 数据库初始化

```bash
mysql -u root -p < init-db.sql
```

### 3. 一键启动（Windows）

```bash
start-all.bat
```

或手动按顺序启动：

```bash
# 1. 注册中心
cd eureka-server && mvn spring-boot:run

# 2. 商品服务
cd ../product-service && mvn spring-boot:run

# 3. 订单服务
cd ../order-service && mvn spring-boot:run

# 4. 联系人服务
cd ../contact-service && mvn spring-boot:run

# 5. 物流服务
cd ../logistics-service && mvn spring-boot:run

# 6. AI聊天服务
cd ../chat-service && mvn spring-boot:run
```

### 4. 停止所有服务（Windows）

```bash
stop-all.bat
```

### 5. 验证服务

- Eureka 控制台：http://localhost:8761
- Eureka控制台：http://localhost:8761
- 网关统一入口：http://localhost:8080
- AI聊天：POST http://localhost:8085/chat

## API 接口

### 认证服务 (auth-service)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/auth/register | 用户注册 |
| POST | /api/user/auth/login | 用户登录 |
| POST | /api/user/auth/logout | 用户登出 |
| GET | /api/user/auth/info | 获取用户信息 |
| POST | /api/seller/auth/register | 商家注册 |
| POST | /api/seller/auth/login | 商家登录 |
| POST | /api/seller/auth/logout | 商家登出 |
| GET | /api/seller/auth/info | 获取商家信息 |

### 商品服务 (product-service)

**用户端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/product/all | 查询所有商品 |
| GET | /api/user/product/{id} | 根据ID查询商品 |
| GET | /api/user/product/search?name=xxx | 根据名称搜索 |

**商家端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/seller/product/create | 创建商品 |
| PUT | /api/seller/product/{id} | 更新商品 |
| DELETE | /api/seller/product/{id} | 删除商品 |

**废弃接口（向后兼容）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /product/all | 查询所有商品（已废弃）|
| GET | /product/{id} | 根据ID查询商品（已废弃）|

### 订单服务 (order-service)

**用户端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/order/list | 查询我的订单 |
| GET | /api/user/order/{id} | 根据ID查询订单 |
| POST | /api/user/order/place | 创建订单 |
| DELETE | /api/user/order/{id} | 取消订单 |

**商家端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/seller/order/list | 查询所有订单 |
| GET | /api/seller/order/status/{status} | 根据状态查询 |
| PUT | /api/seller/order/{id}/ship | 订单发货 |
| PUT | /api/seller/order/{id}/status | 更新订单状态 |

**废弃接口（向后兼容）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /order/list | 查询订单列表（已废弃）|

### 联系人服务 (contact-service)

**用户端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/contact/list | 查询所有联系人 |
| GET | /api/user/contact/{id} | 根据ID查询 |
| GET | /api/user/contact/search/name?name=xxx | 根据姓名搜索 |
| POST | /api/user/contact/create | 创建联系人 |
| PUT | /api/user/contact/{id} | 更新联系人 |
| DELETE | /api/user/contact/{id} | 删除联系人 |

**废弃接口（向后兼容）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /contact/list | 查询联系人（已废弃）|

### 物流服务 (logistics-service)

**用户端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/user/logistics/{id} | 根据ID查询物流 |
| GET | /api/user/logistics/search/tracking?trackingNumber=xxx | 根据快递单号查询 |

**商家端接口：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/seller/logistics/list | 查询所有物流信息 |
| POST | /api/seller/logistics/create | 创建物流信息 |
| PUT | /api/seller/logistics/{id} | 更新物流信息 |
| DELETE | /api/seller/logistics/{id} | 删除物流信息 |

**废弃接口（向后兼容）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /logistics/list | 查询物流列表（已废弃）|

### AI聊天服务 (chat-service)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /chat | 发送消息，返回AI回复 |

请求体：
```json
{
  "message": "有什么商品推荐？"
}
```

## 服务间调用

- order-service 通过 OpenFeign 调用 product-service（查询商品、扣减库存）
- chat-service 通过 OpenFeign 调用 product-service（获取商品列表用于推荐）

## 与原项目的关系

```
AI-Shopping/
├── AI-Shopping-backend/          # 原单体应用（端口8080）
├── AI-Shopping-backend_Eureka/   # 本微服务版本
│   ├── eureka-server/
│   ├── product-service/
│   ├── product-api/
│   ├── order-service/
│   ├── order-api/
│   ├── contact-service/
│   ├── contact-api/
│   ├── logistics-service/
│   ├── logistics-api/
│   └── chat-service/
└── AI-Shopping-frontier/        # 前端（共用）
```

两个后端版本**独立运行**，前端可通过修改 baseURL 切换。

## 配置说明

各服务的 `application.yml` 中需配置：

1. **数据库连接**：修改为自己的 MySQL 用户名密码
2. **Redis 连接**：order-service 需要 Redis 生成订单号
3. **Eureka 地址**：默认 http://localhost:8761/eureka/
4. **LangChain4j API Key**：chat-service 中配置通义千问 API Key

## 项目结构

```
AI-Shopping-backend_Eureka/
├── pom.xml                      # 父工程POM
├── README.md                    # 本文件
├── init-db.sql                  # 数据库初始化脚本
├── start-all.bat                # Windows一键启动脚本
├── stop-all.bat                 # Windows一键停止脚本
├── eureka-server/               # 注册中心
├── product-service/             # 商品服务实现
├── product-api/                 # 商品服务API接口
├── order-service/               # 订单服务实现
├── order-api/                   # 订单服务API接口
├── contact-service/             # 联系人服务实现
├── contact-api/                  # 联系人服务API接口
├── logistics-service/           # 物流服务实现
├── logistics-api/               # 物流服务API接口
└── chat-service/                # AI聊天服务
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| 服务注册不上 | 检查 Eureka 是否已启动，网络是否连通 |
| Feign 调用失败 | 确认被调用服务已注册到 Eureka |
| 数据库连接失败 | 检查 MySQL 服务状态和连接配置 |
| 订单号生成失败 | 确认 Redis 服务已启动 |

## 扩展建议

- 引入 API Gateway 统一入口
- 添加配置中心（Spring Cloud Config）
- 添加服务熔断降级（Resilience4j）
- 添加分布式链路追踪（Micrometer + Zipkin）
- 容器化部署（Docker Compose / Kubernetes）

---

**版本**：v1.1  
**更新日期**：2026-04-22（API拆分为user/seller端）
