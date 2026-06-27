# Docker 容器化部署方案

## 概述

将 AI-Shopping 项目（9个 Spring Cloud 微服务 + 2个 Vue 前端）整体容器化，通过 `docker compose up` 一键启动完整系统。

## 设计原则

- **零代码修改**：不修改任何源代码，仅通过 Docker 环境变量覆盖配置
- **目录隔离**：所有 Docker 相关文件放在 `deploy/` 目录，与项目代码完全分离
- **一键启动**：用户只需安装 Docker，执行 `docker compose up` 即可

## 架构

```
用户浏览器 --> nginx:80
                  │
                  ├── /user/*       ──→ 前端静态文件 (frontier-user dist)
                  ├── /seller/*     ──→ 前端静态文件 (frontier-seller dist)
                  └── /api/*        ──→ proxy_pass → gateway-service:8088
                                              │
                                              ├── auth-service:8086
                                              ├── product-service:8081
                                              ├── order-service:8082
                                              ├── contact-service:8083
                                              ├── logistics-service:8084
                                              ├── chat-service:8085
                                              └── shop-service:8087
                                                  │
                            ┌─────────────────────┼─────────────────────┬──────────────┐
                            ▼                     ▼                     ▼              ▼
                         MySQL:3306            Redis:6379        Nacos:8848     MongoDB:27017
                                                                 (服务发现)     (chat 记忆)
```

## 组件清单

| 组件 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | `mysql:8.0` | 3306 | 数据持久化，初始化建表（7个数据库） |
| redis | `redis:7-alpine` | 6379 | 会话/缓存 |
| mongodb | `mongo:7` | 27017 | chat-service 会话记忆存储 |
| nacos | `nacos/nacos-server:v2.3` | 8848 | 服务注册与发现 |
| gateway-service | 自构建 | 8088 | 网关 |
| auth-service | 自构建 | 8086 | 认证 |
| product-service | 自构建 | 8081 | 商品 |
| order-service | 自构建 | 8082 | 订单 |
| contact-service | 自构建 | 8083 | 联系人 |
| logistics-service | 自构建 | 8084 | 物流 |
| chat-service | 自构建 | 8085 | 聊天（依赖 MongoDB） |
| shop-service | 自构建 | 8087 | 店铺 |
| nginx | `nginx:alpine` | 80 | 前端托管 + API 反向代理 |

## 目录结构

```
deploy/
├── docker-compose.yml       # 主编排文件
├── .env                     # 统一环境变量
├── dockerfiles/
│   └── Dockerfile.java      # 通用 Java 17 微服务镜像
├── mysql/
│   └── init/
│       └── 01-init.sql      # 合并后的建库建表脚本
├── nacos/
│   └── init-config.sh       # Nacos 配置初始化脚本
└── nginx/
    └── nginx.conf           # 前端静态资源 + API 反向代理
```

## 环境变量覆盖（Env Overrides）

Spring Boot 自动将环境变量 `UPPER_CASE` 映射为 `lower.case.property`。各服务容器设置以下环境变量：

| 环境变量 | 覆盖目标 | 值 |
|---------|---------|-----|
| `SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR` | `spring.cloud.nacos.discovery.server-addr` | `nacos:8848` |
| `SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR` | `spring.cloud.nacos.config.server-addr` | `nacos:8848` |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://mysql:3306/eureka_xxx?...` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `root` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `root123` |
| `SPRING_DATA_REDIS_HOST` | `spring.data.redis.host` | `redis` |
| `SPRING_DATA_REDIS_PORT` | `spring.data.redis.port` | `6379` |
| `SPRING_DATA_MONGODB_URI` | `spring.data.mongodb.uri` | `mongodb://mongodb:27017/chat_memory_db` |
| `PROJECT_ROOT` | `project.root` | `.` |
| `NACOS_SERVER_ADDR` | 用于 Nacos init 脚本 | `nacos:8848` |

## Nacos Config 配置项清单

以下是从 Nacos Config 获取的 `app.*` 配置，部署后通过 init-config.sh 自动写入 Nacos，也可在运行时修改：

| Key | 服务 | 默认值 | 说明 |
|-----|------|--------|------|
| `app.image.base-url` | product-service | `http://product-service:8081` | 商品图片访问地址 |
| `app.image.base-url` | shop-service | `http://shop-service:8087` | 店铺图片访问地址 |
| `app.image.storage-path` | product-service | `/app/static/image/goods/main` | 商品图片存储路径 |
| `app.image.resource-location` | product-service | `file:/app/static/image/` | 商品图片静态资源映射 |
| `app.image.storage-path` | shop-service | `/app/static/image/shop/logo` | 店铺图片存储路径 |
| `app.image.resource-location` | shop-service | `file:/app/static/image/` | 店铺图片静态资源映射 |
| `app.image.max-file-size` | product-service | `5MB` | 上传大小限制 |
| `app.scheduler.reservation.enabled` | product-service | `true` | 库存清理任务开关 |
| `app.scheduler.reservation.fixed-rate` | product-service | `120000` | 清理任务间隔(ms) |
| `app.scheduler.reservation.config-path` | product-service | `/app/config/application.yml` | 配置文件路径 |

## 启动顺序

1. mysql → mongodb → redis → nacos (可并行，只需各自健康检查通过)
3. nacos 健康检查通过后 → nacos-init (执行 init-config.sh)
4. nacos-init 完成后 → 所有后端微服务 (并行)
5. 微服务健康检查通过后 → nginx

## 构建流程

### 后端镜像构建

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY ${SERVICE}.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
```

使用通用 Dockerfile，通过 build arg 传入服务名和端口。

### 前端 nginx 镜像

将两个前端 dist 目录复制到 nginx 镜像中，通过 `nginx.conf` 路由：
- `http://localhost/user/*` → frontier-user 静态文件
- `http://localhost/seller/*` → frontier-seller 静态文件
- `http://localhost/api/*` → proxy_pass 到 `http://gateway-service:8088`

## 数据持久化

使用 Docker named volumes：
- `mysql-data` → MySQL 数据文件
- `mongodb-data` → MongoDB 数据文件
- `image-data` → 商品/店铺上传图片（挂载到各服务的 `/app/static/image/`）

## README 要包含的信息

1. 前置条件：安装 Docker Desktop
2. 快速启动：`docker compose up`
3. 访问地址：`http://localhost`（用户端）、`http://localhost/seller/`（商家端）
4. 各组件端口映射表
5. Nacos 控制台地址：`http://localhost:8848/nacos`
6. Nacos app.* 配置项清单及修改方式（上面表格）
7. 图片上传配置说明（storage-path 指向 Docker volume）
8. 常见问题排查
