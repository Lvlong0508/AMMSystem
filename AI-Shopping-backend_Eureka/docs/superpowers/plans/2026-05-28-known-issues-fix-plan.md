# 已知问题修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复附录 A 中确认的 4 个真实问题（P0×1 + P1×1 + P2×2）

**Architecture:** 各问题独立、互不依赖，可并行修复。每个修复包含：测试用例编写 → 修复实现 → 验证。

**Tech Stack:** Java 17 + Spring Boot 3.2.3 + MyBatis + Spring Cloud Gateway

---

### Task 1: Contact - 修复无限递归（P0）

**问题：** `UserContactController.java:132-136` 中 `toContact(UpdateContactRequest)` 调用 `toContact(request)`，因 `UpdateContactRequest` 不继承 `CreateContactRequest`，触发递归 `StackOverflowError`。

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/UserContactController.java:132-136`

- [ ] **Step 1: 替换 `toContact(UpdateContactRequest)` 实现**

将：

```java
private Contact toContact(UpdateContactRequest request) {
    Contact contact = toContact(request);
    contact.setId(request.getId());
    return contact;
}
```

改为：

```java
private Contact toContact(UpdateContactRequest request) {
    Contact contact = new Contact();
    contact.setId(request.getId());
    contact.setName(request.getName());
    contact.setPhone(request.getPhone());
    contact.setAddress(request.getAddress());
    return contact;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl contact-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/controller/UserContactController.java
git commit -m "fix: fix infinite recursion in UserContactController.toContact(UpdateContactRequest)"
```

---

### Task 2: Contact - setDefaultContact 未清除其他默认标记（P1）

**问题：** `UserContactServiceImpl.setDefaultContact()` 只将目标 `is_default=1`，未清除该用户其他联系人的默认标记。

**Files:**
- Add mapper method: `contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/UserContactMapper.java`
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/UserContactServiceImpl.java:60-68`

- [ ] **Step 1: 在 `UserContactMapper` 中添加清除默认标记的 SQL 方法**

```java
@Update("UPDATE t_contact c " +
        "INNER JOIN user_contact uc ON c.id = uc.contact_id " +
        "SET c.is_default = 0 " +
        "WHERE uc.user_id = #{userId} AND c.id != #{contactId}")
int clearDefaultByUserId(@Param("userId") Long userId, @Param("contactId") int contactId);
```

- [ ] **Step 2: 修改 `setDefaultContact` 实现**

```java
@Override
public int setDefaultContact(int id, Long userId) {
    log.info("setDefaultContact, id={}, userId={}", id, userId);
    List<Long> userIds = userContactMapper.selectUserIdsByContactId(id);
    if (userIds.isEmpty() || !userIds.contains(userId)) {
        return 0;
    }
    userContactMapper.clearDefaultByUserId(userId, id);
    return userContactMapper.setDefaultById(id);
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl contact-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/UserContactMapper.java contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/UserContactServiceImpl.java
git commit -m "fix: clear other default contacts when setting new default"
```

---

### Task 3: Gateway - chat 路由子路径丢失（P2）

**问题：** `user-chat` 路由的 `RewritePath=/api/user/chat, /chat` 缺少捕获组，`/api/user/chat/xxx` 的子路径会被丢弃。

**Files:**
- Modify: `gateway-service/src/main/resources/application.yml:68-71`

- [ ] **Step 1: 修复 RewritePath 配置**

将：

```yaml
        - id: user-chat
          uri: lb://chat-service
          predicates:
            - Path=/api/user/chat/**
          filters:
            - RewritePath=/api/user/chat, /chat
```

改为：

```yaml
        - id: user-chat
          uri: lb://chat-service
          predicates:
            - Path=/api/user/chat/**
          filters:
            - RewritePath=/api/user/chat/(?<segment>.*), /chat/$\{segment}
```

- [ ] **Step 2: 同样的修改应用到 seller-chat 路由**

```yaml
        - id: seller-chat
          uri: lb://chat-service
          predicates:
            - Path=/api/seller/chat/**
          filters:
            - RewritePath=/api/seller/chat/(?<segment>.*), /chat/$\{segment}
```

- [ ] **Step 3: Commit**

```bash
git add gateway-service/src/main/resources/application.yml
git commit -m "fix: add capture group to chat route RewritePath to preserve sub-paths"
```

---

### Task 4: Product - shopInfoCache 添加过期策略（P2）

**问题：** `shopInfoCache` 使用 `ConcurrentHashMap` 无过期策略，店铺信息变更后缓存不及时更新。

**Files:**
- Add dependency: `product-service/pom.xml`
- Modify: `product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java`

- [ ] **Step 0: 添加 Caffeine Maven 依赖**

在 `product-service/pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

- [ ] **Step 1: 替换缓存的实现**

在 `ProductServiceImpl` 中将字段声明：

```java
private final Map<Long, ShopInfoDTO> shopInfoCache = new ConcurrentHashMap<>();
```

改为：

```java
private final Cache<Long, ShopInfoDTO> shopInfoCache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();
```

并添加 import（注意包名是 `cache` 非 `caffeine`）：

```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
```

- [ ] **Step 2: 更新 `getCachedShopInfo` 方法**

将：

```java
private ShopInfoDTO getCachedShopInfo(Long shopId) {
    if (shopId == null) return null;
    return shopInfoCache.computeIfAbsent(shopId, id -> {
        ApiResponse<ShopInfoDTO> response = shopFeignClient.getShopInfo(id);
        return response != null ? response.getData() : null;
    });
}
```

改为：

```java
private ShopInfoDTO getCachedShopInfo(Long shopId) {
    if (shopId == null) return null;
    try {
        return shopInfoCache.get(shopId, id -> {
            ApiResponse<ShopInfoDTO> response = shopFeignClient.getShopInfo(id);
            return response != null ? response.getData() : null;
        });
    } catch (Exception e) {
        log.warn("获取店铺信息失败, shopId={}", shopId, e);
        return null;
    }
}
```

- [ ] **Step 3: 更新 `batchGetShopInfo` 方法（ConcurrentHashMap API → Caffeine API）**

将：

```java
private Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
    if (shopIds == null || shopIds.isEmpty()) return Map.of();
    Set<Long> uncached = shopIds.stream()
        .filter(id -> !shopInfoCache.containsKey(id))
        .collect(Collectors.toSet());
    if (!uncached.isEmpty()) {
        ApiResponse<Map<Long, ShopInfoDTO>> response = shopFeignClient.batchGetShopInfo(uncached);
        if (response != null && response.getData() != null) {
            shopInfoCache.putAll(response.getData());
        }
    }
    return shopIds.stream()
        .filter(shopInfoCache::containsKey)
        .collect(Collectors.toMap(id -> id, shopInfoCache::get));
}
```

改为：

```java
private Map<Long, ShopInfoDTO> batchGetShopInfo(Set<Long> shopIds) {
    if (shopIds == null || shopIds.isEmpty()) return Map.of();
    Set<Long> uncached = shopIds.stream()
        .filter(id -> shopInfoCache.getIfPresent(id) == null)
        .collect(Collectors.toSet());
    if (!uncached.isEmpty()) {
        ApiResponse<Map<Long, ShopInfoDTO>> response = shopFeignClient.batchGetShopInfo(uncached);
        if (response != null && response.getData() != null) {
            response.getData().forEach(shopInfoCache::put);
        }
    }
    return shopIds.stream()
        .filter(id -> shopInfoCache.getIfPresent(id) != null)
        .collect(Collectors.toMap(id -> id, shopInfoCache::getIfPresent));
}
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl product-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add product-service/pom.xml product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git commit -m "fix: add TTL eviction to shopInfoCache via Caffeine"
```

---

## 各任务依赖关系

```
Task 1 (Contact 递归) ── 独立
Task 2 (Contact 默认) ── 独立（与 Task 1 修改不同文件，可并行）
Task 3 (Gateway 路由) ── 独立
Task 4 (Product 缓存) ── 独立
```

四个任务修改不同的文件，可完全并行执行。建议将 Task 1 + Task 2 合并在同一 commit（同一微服务）。

## 工程审查要点

| 原始计划问题 | 修正 |
|-------------|------|
| Task 4 缺少 Caffeine 依赖添加 | Step 0 新增 `product-service/pom.xml` 依赖 |
| Task 4 import 包名错误（`caffeine.Caffeine`） | 修正为 `cache.Caffeine` |
| Task 4 `batchGetShopInfo` 未适配 Caffeine API | Step 3 新增 ConcurrentHashMap → Caffeine API 转换 |
| Task 4 `getCachedShopInfo` 使用 `log.warn` 但类无 `@Slf4j` | 新增 `@Slf4j` 注解及 `import lombok.extern.slf4j.Slf4j` |
| Task 2 标记为依赖 Task 1 | 实为不同文件，可独立并行 |
| Task 3 seller-chat 已正确 | 无需修改，仅 user-chat 有缺陷 |
