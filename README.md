# AI-Shopping

AI-Shopping 是一个面向电商场景的 AI 智能购物平台，采用前后端分离与微服务架构，覆盖用户购物、商家经营、商品管理、订单履约、售后退货、地址联系、物流跟踪和智能对话等核心业务流程。

## 项目亮点

- **微服务架构**：后端按网关、认证、商品、店铺、订单、物流、联系地址、聊天等领域拆分，职责边界清晰，便于扩展和维护。
- **双端前台体系**：提供用户端与商家端两个 Vue 应用，分别支撑消费者购物链路和商家后台管理链路。
- **AI 能力集成**：后端引入 LangChain4j 与 DashScope 相关依赖，为智能客服、购物咨询、对话交互等 AI 场景提供基础能力。
- **完整电商闭环**：覆盖注册登录、店铺管理、商品管理、购物下单、订单处理、物流发货、退货管理等典型电商流程。
- **工程化分层**：后端使用 Maven 多模块管理，前端使用 Vite 构建，适合团队协作、模块化开发和持续迭代。

## 核心功能

### 用户端

- 用户注册、登录与身份认证
- 商品浏览、搜索与详情查看
- 订单创建、订单查看与订单状态跟踪
- 收货地址与联系方式管理
- 物流信息查看
- AI 对话与购物咨询能力支撑

### 商家端

- 商家注册、登录与资料管理
- 店铺信息与店铺地址管理
- 商品发布、编辑、查询与维护
- 订单列表、订单详情与订单处理
- 物流发货与物流信息维护
- 退货申请处理、拒绝申请、确认退货等售后管理能力

## 系统架构

项目采用前后端分离架构：

```text
AI-Shopping
├── 前端应用
│   ├── frontier-user    用户端 Vue 应用
│   └── frontier-seller  商家端 Vue 应用
│
└── 后端微服务
    ├── gateway-service    API 网关
    ├── auth-service       认证与账号服务
    ├── shop-service       店铺服务
    ├── product-service    商品服务
    ├── order-service      订单服务
    ├── contact-service    联系人与地址服务
    ├── logistics-service  物流服务
    ├── chat-service       AI 对话服务
    └── common-api         公共 API 与通用模型
```

后端各服务通过 Spring Cloud 体系组织，网关负责统一入口，各业务服务围绕领域职责拆分；前端通过 Axios 调用后端接口，用户端与商家端分别维护独立页面、路由和业务状态。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2.4
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba 2023.0.1.2
- MyBatis / MyBatis Spring Boot Starter
- Maven 多模块工程
- Lombok
- Hutool
- LangChain4j
- DashScope SDK

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- Tailwind CSS
- SweetAlert2

### 基础设施

- Nacos：服务注册与配置管理
- Redis：缓存与临时数据能力
- Sentinel：流量治理与服务保护
- SQL 初始化脚本：位于 `AI-Shopping-backend/sql/init`

## 后端模块说明

| 模块 | 说明 |
| --- | --- |
| `gateway-service` | 后端统一网关入口，负责请求转发与服务聚合入口 |
| `auth-service` | 用户、商家认证以及账号资料相关能力 |
| `shop-service` | 店铺资料、店铺 Logo、店铺信息维护 |
| `product-service` | 商品发布、商品查询、商品信息维护 |
| `order-service` | 订单创建、订单查询、订单状态流转与售后相关业务 |
| `contact-service` | 用户联系信息、收货地址、商家地址管理 |
| `logistics-service` | 物流信息、发货记录和配送状态维护 |
| `chat-service` | AI 对话、智能咨询等聊天相关能力 |
| `common-api` | 公共响应、DTO、通用接口和跨服务复用内容 |

## 前端应用说明

| 应用 | 路径 | 说明 |
| --- | --- | --- |
| 用户端 | `AI-Shopping-frontier/frontier-user` | 面向消费者，提供商品浏览、下单、个人资料、地址、订单等功能 |
| 商家端 | `AI-Shopping-frontier/frontier-seller` | 面向商家，提供店铺、商品、订单、物流、退货等后台管理功能 |

## 目录结构

```text
.
├── AI-Shopping-backend
│   ├── auth-service
│   ├── chat-service
│   ├── common-api
│   ├── contact-service
│   ├── gateway-service
│   ├── logistics-service
│   ├── order-service
│   ├── product-service
│   ├── shop-service
│   ├── sql/init
│   └── pom.xml
├── AI-Shopping-frontier
│   ├── frontier-user
│   └── frontier-seller
├── 脚本
│   ├── start-end.bat
│   ├── stop-end.bat
│   ├── start-frontier.bat
│   ├── stop-frontier.bat
│   ├── start-nacos.bat
│   └── start-sentinel.bat
├── 前端代码规格说明.md
├── 后端代码规格说明.md
└── pom.xml
```

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js `^20.19.0 || >=22.12.0`
- npm
- Nacos
- Redis
- Sentinel
- MySQL 或兼容数据库

## 快速启动

### 1. 初始化数据库

按需执行后端 SQL 初始化脚本：

```bash
AI-Shopping-backend/sql/init
```

建议根据服务拆分顺序初始化认证、商品、订单、联系地址、物流、聊天、店铺等业务表。

### 2. 启动基础设施

项目提供了部分 Windows 启动脚本：

```bash
脚本/start-nacos.bat
脚本/start-sentinel.bat
```

Redis 需要在本地单独启动，并确保后端配置中的连接信息与本地环境一致。

### 3. 启动后端服务

进入后端根目录构建：

```bash
cd AI-Shopping-backend
mvn clean install
```

可以按模块分别启动各 Spring Boot 服务，也可以使用项目脚本启动后端：

```bash
脚本/start-end.bat
```

### 4. 启动用户端

```bash
cd AI-Shopping-frontier/frontier-user
npm install
npm run dev
```

### 5. 启动商家端

```bash
cd AI-Shopping-frontier/frontier-seller
npm install
npm run dev
```

## 常用命令

### 后端

```bash
cd AI-Shopping-backend
mvn test
mvn clean package
```

### 用户端

```bash
cd AI-Shopping-frontier/frontier-user
npm run dev
npm run build
npm run preview
```

### 商家端

```bash
cd AI-Shopping-frontier/frontier-seller
npm run dev
npm run build
npm run preview
```

## 开发说明

- 后端新增业务优先按领域拆分到对应服务模块，公共 DTO、响应结构和跨服务复用内容放入 `common-api`。
- 前端用户端与商家端保持独立，页面、路由、状态和接口封装按应用边界维护。
- 接口、数据库和业务规则变更后，应同步更新对应说明文档或初始化脚本。
- 提交前建议分别执行后端测试与前端构建，确保服务和页面可以正常运行。

## 适用场景

该项目适合作为 Java 微服务、电商业务系统、Vue 前后端分离项目、AI 应用集成实践的综合展示项目，也适合用于课程设计、毕业设计、项目答辩和个人作品集展示。
