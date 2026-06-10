# Eureka → Nacos 注册中心迁移设计 Spec

> **日期**：2026-06-10
> **项目**：AI-Shopping-backend_Eureka
> **目标**：将后端微服务集群的注册中心从 Netflix Eureka 迁移到 Nacos

---

## 一、背景与目标

### 现状
- 后端基于 Spring Boot 3.2.3 + Spring Cloud 2023.0.0 + Java 17
- 共 10 个模块，其中 9 个服务模块使用 Eureka 做服务发现
- Eureka Server 内嵌在项目中，依赖 spring-cloud-starter-netflix-eureka-server
- Nacos Server 已部署就绪，地址：http://10.200.97.197:8848（控制台 8080）

### 目标
- 移除 Eureka 全部依赖和配置
- 所有服务迁移到 Nacos 注册中心
- 保持 Feign 跨服务调用、Gateway 路由正常工作
- **不修改任何业务代码**

---

## 二、架构变更

### 迁移前后对比

`
迁移前：
  每个微服务 → Eureka Client → Eureka Server（内嵌项目模块）

迁移后：
  每个微服务 → Nacos Discovery → Nacos Server（独立部署中间件，地址 10.200.97.197:8848）
`

### 不需要改动的部分
- Feign 接口（5 个）：@FeignClient(name = "xxx") 无需修改
- Gateway 路由（14 条 lb://xxx）：无需修改
- 所有 Controller、Service、Mapper 层：无需修改
- Sa-Token、Redis、MySQL 等其他配置：无需修改

---

## 三、变更清单

### 3.1 删除模块

| 操作 | 路径 |
|------|------|
| 删除整个目录 | AI-Shopping-backend_Eureka/eureka-server/ |
| 移除 module 声明 | AI-Shopping-backend_Eureka/pom.xml 中去掉 <module>eureka-server</module> |

### 3.2 POM 依赖变更

**父 POM** — 新增 Spring Cloud Alibaba 依赖管理：

`xml
<properties>
    <!-- 新增 -->
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- 原 spring-cloud-dependencies 保持不变，新增 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version></version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
`

**8 个服务 POM** — 替换依赖（涉及文件见附录）：

`xml
<!-- 删除 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- 替换为 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
`

涉及服务：gateway-service、auth-service、product-service、order-service、contact-service、logistics-service、chat-service、shop-service

### 3.3 YML 配置变更

**8 个服务的 application.yml** — 统一替换：

`yaml
# 删除以下 Eureka 配置块
eureka:
  client:
    service-url:
      defaultZone: http://admin:admin@localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: :

# 替换为 Nacos 配置块
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 10.200.97.197:8848
        username: nacos
        password: nacos
`

对应的 src/test/resources/application.yml 也需要同步修改（auth-service、contact-service 有 test 配置）。

### 3.4 不需要改的配置

- Feign 超时、连接池等配置 → **保持不动**
- MySQL 数据源、Redis → **保持不动**
- Sa-Token 认证配置 → **保持不动**
- MyBatis 配置 → **保持不动**
- Gateway 路由规则 → **保持不动**

---

## 四、验证标准

1. Nacos 控制台可见所有 8 个服务已注册（健康状态为绿色）
2. Gateway 能通过 lb:// 正确路由到各个服务
3. Feign 跨服务调用正常（如 order-service 通过 Feign 调用 product-service 获取商品信息）
4. 登录、商品查询、下单等核心业务流程走通
5. eureka-server 模块可安全删除

---

## 五、回滚方案

如迁移后出现问题，回滚步骤：

1. 恢复 pom.xml 中 Eureka 依赖和 Alibaba 依赖管理的修改
2. 恢复 pplication.yml 中 Nacos 配置 → Eureka 配置
3. 恢复 eureka-server 模块
4. 启动 Eureka Server，逐个重启服务

> 由于仅涉及配置层变更，不触及业务代码，回滚风险极低。

---

## 附录：完整变更文件清单

| 序号 | 文件路径 | 操作 |
|:----:|----------|:----:|
| 1 | AI-Shopping-backend_Eureka/pom.xml | 修改 |
| 2 | AI-Shopping-backend_Eureka/eureka-server/（整个目录） | 删除 |
| 3 | AI-Shopping-backend_Eureka/gateway-service/pom.xml | 修改 |
| 4 | AI-Shopping-backend_Eureka/auth-service/pom.xml | 修改 |
| 5 | AI-Shopping-backend_Eureka/product-service/pom.xml | 修改 |
| 6 | AI-Shopping-backend_Eureka/order-service/pom.xml | 修改 |
| 7 | AI-Shopping-backend_Eureka/contact-service/pom.xml | 修改 |
| 8 | AI-Shopping-backend_Eureka/logistics-service/pom.xml | 修改 |
| 9 | AI-Shopping-backend_Eureka/chat-service/pom.xml | 修改 |
| 10 | AI-Shopping-backend_Eureka/shop-service/pom.xml | 修改 |
| 11 | AI-Shopping-backend_Eureka/gateway-service/src/main/resources/application.yml | 修改 |
| 12 | AI-Shopping-backend_Eureka/auth-service/src/main/resources/application.yml | 修改 |
| 13 | AI-Shopping-backend_Eureka/product-service/src/main/resources/application.yml | 修改 |
| 14 | AI-Shopping-backend_Eureka/order-service/src/main/resources/application.yml | 修改 |
| 15 | AI-Shopping-backend_Eureka/contact-service/src/main/resources/application.yml | 修改 |
| 16 | AI-Shopping-backend_Eureka/logistics-service/src/main/resources/application.yml | 修改 |
| 17 | AI-Shopping-backend_Eureka/chat-service/src/main/resources/application.yml | 修改 |
| 18 | AI-Shopping-backend_Eureka/shop-service/src/main/resources/application.yml | 修改 |
| 19 | AI-Shopping-backend_Eureka/auth-service/src/test/resources/application.yml | 修改 |
| 20 | AI-Shopping-backend_Eureka/contact-service/src/test/resources/application.yml | 修改 |
