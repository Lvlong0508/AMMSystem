# Shop Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建独立 shop-service 微服务（端口 8087），实现多店铺平台，支持商家创建店铺、店员管理、商品/订单关联

**Architecture:** 新增独立 shop-service 微服务，使用独立数据库 eureka_shop，通过中间表（t_merchant_role、t_product_shop、t_order_shop）关联现有商家、商品、订单数据。服务间调用使用 common-api 中新增的 Feign Client。

**Tech Stack:** Spring Boot 3.2.3, Spring Cloud 2023.0.0, MyBatis 3.0.3, MySQL, Eureka

---

## Phase 1: 基础服务搭建

### Task 1.1: 创建 shop-service Maven 模块

**Files:**
- Modify: `AI-Shopping-backend_Eureka/pom.xml:14-24`
- Create: `AI-Shopping-backend_Eureka/shop-service/pom.xml`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/ShopServiceApplication.java`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/config/DataSourceConfig.java`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/resources/application.yml`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/resources/mapper/ShopMapper.xml`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/entity/Shop.java`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/entity/MerchantRole.java`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/entity/ProductShop.java`
- Create: `AI-Shopping-backend_Eureka/shop-service/src/main/java/com/gzasc/aishopping/shop/entity/OrderShop.java`

- [ ] **Step 1: Add shop-service module to parent pom.xml**

```xml
<module>shop-service</module>
```

在 `<modules>` 中添加 shop-service

- [ ] **Step 2: Create shop-service/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.gzasc</groupId>
        <artifactId>AI-Shopping-backend_Eureka</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <artifactId>shop-service</artifactId>
    <name>shop-service</name>
    <description>商店服务</description>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.gzasc</groupId>
            <artifactId>common-api</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create ShopServiceApplication.java**

```java
package com.gzasc.aishopping.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.gzasc.aishopping.shop.mapper")
public class ShopServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Create DataSourceConfig.java**

```java
package com.gzasc.aishopping.shop.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Primary
    @Bean("dataSource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .url("jdbc:mysql://localhost:3306/eureka_shop?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai")
                .username("root")
                .password("root")
                .build();
    }

    @Primary
    @Bean("sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml"));
        return sessionFactory.getObject();
    }

    @Primary
    @Bean("transactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

- [ ] **Step 5: Create application.yml**

```yaml
server:
  port: 8087

spring:
  application:
    name: shop-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.gzasc.aishopping.shop.entity
```

- [ ] **Step 6: Create entity classes**

Shop.java, MerchantRole.java, ProductShop.java, OrderShop.java - 实体类，包含 id、对应字段、getter/setter

- [ ] **Step 7: Create Mapper interfaces**

ShopMapper.java, MerchantRoleMapper.java, ProductShopMapper.java, OrderShopMapper.java

- [ ] **Step 8: Run tests**

Run: `mvn spring-boot:run -pl shop-service`
Expected: 服务启动成功，端口 8087

- [ ] **Step 9: Commit**

```bash
git add AI-Shopping-backend_Eureka/pom.xml AI-Shopping-backend_Eureka/shop-service/
git commit -m "feat: add shop-service module skeleton"
```

### Task 1.2: 实现店铺 CRUD API

**Files:**
- Create: `ShopController.java`
- Create: `ShopService.java`
- Create: `ShopServiceImpl.java`
- Create: `ShopMapper.xml`

- [ ] **Step 1: Write the failing test**

```java
@Test
void testRegisterShop() {
    // 测试创建店铺
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Write implementation**

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Commit**

---

## Phase 2: 公共模块

### Task 2.1: 新增 DTO

**Files:**
- Modify: `common-api/pom.xml`
- Create: `common-api/src/main/java/com/gzasc/aishopping/common/dto/shop/ShopDTO.java`
- Create: `common-api/src/main/java/com/gzasc/aishopping/common/dto/shop/MerchantRoleDTO.java`
- Create: `common-api/src/main/java/com/gzasc/aishopping/common/dto/shop/ProductShopDTO.java`

### Task 2.2: 新增 Feign Client

**Files:**
- Create: `ShopFeignClient.java`
- Create: `AuthInternalFeignClient.java`

---

## Phase 3: 内部接口

### Task 3.1: auth-service 新增内部接口

**Files:**
- Modify: `auth-service/.../AuthController.java` 或创建 `AuthInternalController.java`

### Task 3.2: product-service 新增内部接口

**Files:**
- Modify: `product-service/.../ProductController.java` 或创建 `ProductInternalController.java`

### Task 3.3: order-service 新增内部接口

**Files:**
- Modify: `order-service/.../OrderController.java` 或创建 `OrderInternalController.java`

---

## Phase 4: 网关与现有服务改造

### Task 4.1: gateway-service 新增路由规则

**Files:**
- Modify: `gateway-service/src/main/resources/application.yml`

### Task 4.2: order-service 改造

**Files:**
- Modify: `order-service/.../OrderServiceImpl.java`
- 调用 shop-service 关联订单

---

## Phase 5: 前端实现

### Task 5.1: 商家端前端

**Files:**
- Create: `frontier-seller/src/api/shop.js`
- Create: `frontier-seller/src/views/shop/ShopRegister.vue`
- Create: `frontier-seller/src/views/shop/ShopList.vue`
- Create: 其他页面组件

---

**Plan saved to:** `docs/superpowers/plans/2026-05-08-shop-service-plan.md`

---

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**