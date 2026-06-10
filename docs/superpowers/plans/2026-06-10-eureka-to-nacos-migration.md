# Eureka → Nacos 注册中心迁移 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** 将 AI-Shopping-backend_Eureka 的注册中心从 Eureka 全量迁移至 Nacos，零业务代码改动

**Architecture:** 删除 eureka-server 内嵌模块，在 8 个服务模块中将 spring-cloud-starter-netflix-eureka-client 替换为 spring-cloud-starter-alibaba-nacos-discovery，YML 中的 eureka 配置块替换为 nacos discovery 配置，Feign 和 Gateway 路由基于 Spring Cloud 抽象层自动适配，无需变动。

**Tech Stack:** Spring Boot 3.2.3, Spring Cloud 2023.0.0, Spring Cloud Alibaba 2023.0.1.0, Nacos Server 2.x

---

### Task 1: 父 POM 新增 Alibaba 依赖管理

**Files:**
- Modify: AI-Shopping-backend_Eureka/pom.xml

- [ ] **Step 1: 在 properties 中新增版本号**

在 <properties> 块中新增：
`xml
        <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
`

- [ ] **Step 2: 在 dependencyManagement 中新增 Alibaba BOM**

在 <dependencyManagement><dependencies> 块中（spring-cloud-dependencies 之后）新增：
`xml
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version></version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
`

- [ ] **Step 3: 从 modules 中移除 eureka-server**

找到 <modules> 列表，删除 <module>eureka-server</module> 这一行。

---

### Task 2: 删除 eureka-server 模块

**Files:**
- Delete: AI-Shopping-backend_Eureka/eureka-server/（整个目录）

- [ ] **Step 1: 删除 eureka-server 模块目录**

`powershell
Remove-Item -Recurse -Force "F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\eureka-server"
`

---

### Task 3: 替换 gateway-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/gateway-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/gateway-service/src/main/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**

找到：
`xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
`
替换为：
`xml
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
`

- [ ] **Step 2: 替换 application.yml 中的 Eureka 配置**

找到文件末尾的 Eureka 配置块：
`yaml
eureka:
  client:
    service-url:
      defaultZone: http://admin:admin@localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: :
`
替换为：
`yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 10.200.97.197:8848
`

---

### Task 4: 替换 auth-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/auth-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/auth-service/src/main/resources/application.yml
- Modify: AI-Shopping-backend_Eureka/auth-service/src/test/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**

找到：
`xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
`
替换为：
`xml
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
`

- [ ] **Step 2: 替换 main 的 application.yml 中的 Eureka 配置**

删除：
`yaml
eureka:
  client:
    service-url:
      defaultZone: http://admin:admin@localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    instance-id: :
`
插入：
`yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 10.200.97.197:8848
`

- [ ] **Step 3: 替换 test 的 application.yml 中的 Eureka 配置**

对 uth-service/src/test/resources/application.yml 执行同样替换。

---

### Task 5: 替换 product-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/product-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/product-service/src/main/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**

与 Task 4 Step 1 相同的替换模式。

- [ ] **Step 2: 替换 application.yml 中的 Eureka 配置**

替换文件末尾的 eureka.client.service-url.defaultZone 和 eureka.instance.* 配置块为 Nacos 配置。

---

### Task 6: 替换 order-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/order-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/order-service/src/main/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**

相同替换模式。

- [ ] **Step 2: 替换 application.yml 中的 Eureka 配置**

替换文件末尾的 Eureka 配置块为 Nacos 配置。

---

### Task 7: 替换 contact-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/contact-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/contact-service/src/main/resources/application.yml
- Modify: AI-Shopping-backend_Eureka/contact-service/src/test/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**
- [ ] **Step 2: 替换 main 的 application.yml 中的 Eureka 配置**
- [ ] **Step 3: 替换 test 的 application.yml 中的 Eureka 配置**

---

### Task 8: 替换 logistics-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/logistics-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/logistics-service/src/main/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**
- [ ] **Step 2: 替换 application.yml 中的 Eureka 配置**

---

### Task 9: 替换 chat-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/chat-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/chat-service/src/main/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**
- [ ] **Step 2: 替换 application.yml 中的 Eureka 配置**

---

### Task 10: 替换 shop-service 的依赖和配置

**Files:**
- Modify: AI-Shopping-backend_Eureka/shop-service/pom.xml
- Modify: AI-Shopping-backend_Eureka/shop-service/src/main/resources/application.yml

- [ ] **Step 1: 替换 pom.xml 中的依赖**
- [ ] **Step 2: 替换 application.yml 中的 Eureka 配置**

---

### Task 11: 验证构建

**Files:** 无文件变更

- [ ] **Step 1: 在父项目执行 Maven 编译**

`powershell
cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka
mvn clean compile -q
`
Expected: BUILD SUCCESS（编译无错，所有 Nacos 依赖正确拉取）

- [ ] **Step 2: 验证 Nacos 控制台服务列表**

打开 http://10.200.97.197:8080 → 服务管理 → 服务列表
Expected: 暂无服务注册（尚未启动微服务）

---

### Task 12: 本地启动验证

**Files:** 无文件变更

- [ ] **Step 1: 依次启动服务验证注册**

按依赖顺序启动服务（Nacos 已运行）：
1. auth-service (8086)
2. product-service (8081)
3. contact-service (8083)
4. order-service (8082)
5. logistics-service (8084)
6. chat-service (8085)
7. shop-service (8087)
8. gateway-service (8080)

启动方式示例（为每个服务开新终端）：
`powershell
cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\auth-service
mvn spring-boot:run
`

- [ ] **Step 2: 检查 Nacos 控制台**

打开 Nacos 控制台 → 服务管理 → 服务列表，确认 8 个服务全部注册，健康实例数为绿色。

- [ ] **Step 3: 测试核心接口**

`powershell
# 测试登录
curl -X POST http://localhost:8080/api/user/auth/login -H "Content-Type: application/json" -d '{"username":"test","password":"test"}'

# 测试商品列表
curl http://localhost:8080/api/user/product/list

# 测试 Gateway 路由
curl http://localhost:8080/api/seller/shop/info
`
Expected: 接口正常返回，Gateway 正确路由到后端服务。
