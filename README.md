# AI-Shopping 智能购物平台

基于 Spring Cloud 微服务架构的 AI 智能购物平台，集成 LangChain4j AI 购物助手，提供用户端和商家端完整购物体验。

## 项目简介

AI-Shopping 是一个融合 AI 聊天购物的现代化电商平台。用户可以通过与 AI 助手"小物"对话来浏览商品、下单购买、查询订单，商家可以通过管理平台处理订单发货。

## 技术栈

### 后端
- **Java 17** + **Spring Boot 3.2.3**
- **Spring Cloud 2023.0.0** (Eureka, Gateway, OpenFeign)
- **MyBatis 3.0.3** + **MySQL 8.x**
- **Redis** (会话管理、订单号生成、缓存)
- **Sa-Token 1.39.0** (认证授权)
- **LangChain4j 0.35.0** + **DashScope (GLM)** (AI 聊天)
- **Maven** 多模块构建

### 前端
- **Vue 3** (Composition API + `<script setup>`)
- **Vite** + **Vue Router 5**
- **Axios** + **TailwindCSS** + **SweetAlert2**

## 项目结构

```
AI-Shopping/
├── AI-Shopping-backend_Eureka/          # 后端微服务
│   ├── eureka-server/                   # 服务注册中心 (8761)
│   ├── gateway-service/                 # API 网关 (8080)
│   ├── auth-service/                    # 认证服务 (8086)
│   ├── product-service/                 # 商品服务 (8081)
│   ├── order-service/                   # 订单服务 (8082)
│   ├── contact-service/                 # 联系人/地址服务 (8083)
│   ├── logistics-service/               # 物流服务 (8084)
│   ├── chat-service/                    # AI 聊天服务 (8085)
│   └── common-api/                      # 公共模块 (DTO + Feign 接口)
├── AI-Shopping-frontier/                # 前端应用
│   ├── frontier-user/                   # 用户端 (AI 聊天购物)
│   └── frontier-seller/                 # 商家端 (订单发货管理)
├── init-db.sql                          # 数据库初始化脚本
├── auth-init.sql                        # 认证数据库初始化
├── start-all.bat                        # 一键启动脚本
└── stop-all.bat                         # 一键停止脚本
```

## 核心功能

### 用户端
- **AI 智能购物助手**: 通过自然对话浏览商品、获取推荐
- **商品浏览**: 搜索、分页查看商品详情
- **订单管理**: 下单、支付、查看订单状态、确认收货
- **地址管理**: 收货地址增删改查
- **实时物流**: 查看订单物流信息

### 商家端
- **订单管理**: 查看待发货订单、处理发货
- **商品管理**: 商品信息维护、库存管理

### 平台特性
- **统一认证**: Sa-Token + BCrypt 密码加密，支持用户/商家双角色
- **API 网关**: 统一路由、IP 限流 (30次/分钟)、Token 校验、角色权限控制
- **服务治理**: Eureka 服务注册发现，OpenFeign 服务间调用
- **AI Tool Calling**:  LangChain4j 工具调用实现商品查询、订单查询等实时数据交互

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.x
- Redis
- Node.js ^20.19.0 或 >=22.12.0

### 数据库初始化

```bash
# 执行数据库初始化脚本
mysql -u root -p < init-db.sql
mysql -u root -p < auth-init.sql
```

### 启动后端

```bash
# 方式一：使用一键启动脚本 (Windows)
start-all.bat

# 方式二：手动启动各微服务
cd AI-Shopping-backend_Eureka
# 依次启动各服务 (建议顺序: eureka -> gateway -> 其他服务)
mvn spring-boot:run -pl eureka-server
mvn spring-boot:run -pl gateway-service
# ... 其他服务
```

### 启动前端

```bash
# 用户端
cd AI-Shopping-frontier/frontier-user
npm install
npm run dev

# 商家端
cd AI-Shopping-frontier/frontier-seller
npm install
npm run dev
```

## 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Eureka Server | 8761 | 服务注册中心 |
| API Gateway | 8080 | 统一网关入口 |
| Product Service | 8081 | 商品服务 |
| Order Service | 8082 | 订单服务 |
| Contact Service | 8083 | 联系人/地址服务 |
| Logistics Service | 8084 | 物流服务 |
| Chat Service | 8085 | AI 聊天服务 |
| Auth Service | 8086 | 认证服务 |

## API 路由

### 用户端路由 (`/api/user/*`)
- `/api/user/products` - 商品浏览
- `/api/user/orders` - 订单管理
- `/api/user/contacts` - 地址管理
- `/api/user/logistics` - 物流查询
- `/api/user/chat` - AI 聊天

### 商家端路由 (`/api/seller/*`)
- `/api/seller/products` - 商品管理
- `/api/seller/orders` - 订单管理
- `/api/seller/logistics` - 发货管理

## 架构设计

```
┌─────────────────────────────────────────────────────┐
│                   前端层 (Browser)                    │
│         frontier-user     │     frontier-seller     │
└──────────────┬────────────┴────────────┬────────────┘
               │                         │
┌──────────────▼─────────────────────────▼──────────┐
│                  API Gateway (8080)                │
│   IP 限流 │ Sa-Token 认证 │ 角色权限 │ 路由分发    │
└──────────────┬────────────────────────────────────┘
               │
    ┌──────────┼──────────┬──────────┬─────────┐
    │          │          │          │         │
  auth      product    order    contact  logistics  chat
  (8086)     (8081)    (8082)   (8083)   (8084)   (8085)
    │          │          │          │         │         │
  MySQL      MySQL     MySQL     MySQL    MySQL    DashScope
  x2                   +Redis                          (GLM)
```

## 订单状态机

```
PENDING → PAID → SHIPPED → DELIVERED
   ↓        ↓       ↓          ↓
CANCELLED  CANCELLED RETURNED  (完成)
```

## 认证机制

- **密码加密**: BCrypt 加盐哈希
- **Token 管理**: Sa-Token 存储于 Redis
- **角色区分**: USER / MERCHANT 前缀隔离
- **网关鉴权**: 白名单机制 + Token 校验 + 角色权限验证

## AI 聊天助手

基于 LangChain4j 框架，使用通义千问/GLM 大模型，通过 Tool Calling 实现：
- 商品搜索与推荐
- 订单状态查询
- 物流信息跟踪
- 智能购物引导

System Prompt 严格约束 AI 行为：禁止幻觉、数据必须真实、友好交互。

## 开发说明

### 服务间调用
- 使用 OpenFeign 进行同步 RPC 调用
- `common-api` 模块统一定义 Feign Client 和 DTO
- 内部接口使用 `/internal/*` 前缀

### 数据库分布
- 每个微服务独立数据库 (eureka_user, eureka_product, eureka_order 等)
- 认证服务使用双数据源 (users, merchants)

## 许可

本项目仅供学习和研究使用。
