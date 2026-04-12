# AI-Shopping-backend Eureka 微服务架构

基于 Spring Cloud Netflix Eureka 的微服务版本，与原单体应用 `AI-Shopping-backend` 并存。

## 架构概览

| 服务 | 端口 | 职责 |
|------|------|------|
| eureka-server | 8761 | 服务注册与发现中心 |
| product-service | 8081 | 商品管理（CRUD、库存） |
| order-service | 8082 | 订单管理（下单、状态流转） |
| contact-service | 8083 | 联系人/收货地址管理 |
| logistics-service | 8084 | 物流跟踪、发货管理 |
| chat-service | 8085 | AI智能对话（通义千问） |

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

### 3. 启动服务（按顺序）

```bash
# 1. 注册中心
cd eureka-server
mvn spring-boot:run

# 2. 商品服务
cd ../product-service
mvn spring-boot:run

# 3. 订单服务
cd ../order-service
mvn spring-boot:run

# 4. 联系人服务
cd ../contact-service
mvn spring-boot:run

# 5. 物流服务
cd ../logistics-service
mvn spring-boot:run

# 6. AI聊天服务
cd ../chat-service
mvn spring-boot:run
```

### 4. 验证服务

- Eureka 控制台：http://localhost:8761
- 商品服务：http://localhost:8081/api/product/all
- 订单服务：http://localhost:8082/api/order/list
- 联系人服务：http://localhost:8083/api/contact/list
- 物流服务：http://localhost:8084/api/logistics/list
- AI聊天：POST http://localhost:8085/api/chat

## API 接口

### 商品服务 (8081)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/product/all | 查询所有商品 |
| GET | /api/product/{id} | 根据ID查询商品 |
| GET | /api/product/search?name=xxx | 根据名称搜索 |
| POST | /api/product/create | 创建商品 |
| PUT | /api/product/{id} | 更新商品 |
| DELETE | /api/product/{id} | 删除商品 |

### 订单服务 (8082)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/order/list | 查询所有订单 |
| GET | /api/order/{id} | 根据ID查询订单 |
| GET | /api/order/status/{status} | 根据状态查询 |
| POST | /api/order/place | 创建订单 |
| PUT | /api/order/{id} | 更新订单 |
| PUT | /api/order/{id}/status | 更新订单状态 |
| DELETE | /api/order/{id} | 删除订单 |

### 联系人服务 (8083)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/contact/list | 查询所有联系人 |
| GET | /api/contact/get/{id} | 根据ID查询 |
| GET | /api/contact/search/name?name=xxx | 根据姓名搜索 |
| POST | /api/contact/create | 创建联系人 |
| PUT | /api/contact/update | 更新联系人 |
| DELETE | /api/contact/delete/{id} | 删除联系人 |

### 物流服务 (8084)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/logistics/list | 查询所有物流信息 |
| GET | /api/logistics/get/{id} | 根据ID查询物流 |
| GET | /api/logistics/search/tracking?trackingNumber=xxx | 根据快递单号查询 |
| POST | /api/logistics/create | 创建物流信息 |
| PUT | /api/logistics/update | 更新物流信息 |
| DELETE | /api/logistics/delete/{id} | 删除物流信息 |

### AI聊天服务 (8085)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat | 发送消息，返回AI回复 |

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
│   ├── order-service/
│   ├── contact-service/
│   ├── logistics-service/
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

## 目录结构

```
AI-Shopping-backend_Eureka/
├── pom.xml                      # 父工程POM
├── README.md                    # 本文件
├── init-db.sql                  # 数据库初始化脚本
├── eureka-server/               # 注册中心
├── product-service/             # 商品服务
├── order-service/               # 订单服务
├── contact-service/            # 联系人服务
├── logistics-service/          # 物流服务
└── chat-service/               # AI聊天服务
```

## 常见问题

1. **服务注册不上**：检查 Eureka 是否已启动，网络是否连通
2. **Feign 调用失败**：确认被调用服务已注册到 Eureka
3. **数据库连接失败**：检查 MySQL 服务状态和连接配置
4. **订单号生成失败**：确认 Redis 服务已启动

## 扩展建议

- 引入 API Gateway 统一入口
- 添加配置中心（Spring Cloud Config）
- 添加服务熔断降级（Resilience4j）
- 添加分布式链路追踪（Micrometer + Zipkin）
- 容器化部署（Docker Compose / Kubernetes）

---

**版本**：v1.0  
**创建日期**：2026-04-12
