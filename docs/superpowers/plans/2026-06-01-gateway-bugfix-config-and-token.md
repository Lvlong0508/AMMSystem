# Gateway Bug 修复：配置注入与 Token 验证一致性

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 gateway-service 中 `RedisRateLimitService` 硬编码配置 + `AuthServiceImpl.validateToken()` 绕过 Sa-Token 的问题

**Architecture:** 两个独立的 Bug 修复，涉及 3 个文件。Bug #5 将 `IpRateLimitProperties` 注入 `RedisRateLimitService` 替换硬编码常量；Bug #4 将 `validateToken()` 的手动 Redis 查询改为 `StpUtil.getLoginIdByToken()`，并在 `getAccountType()` 返回 null 时抛 401 而非 403

**Tech Stack:** Java 17, Spring Cloud Gateway 2023.0.0, Sa-Token 1.39.0

---

### Task 1: RedisRateLimitService 配置注入

**Files:**
- Modify: `gateway-service/src/main/java/com/gzasc/aishopping/gateway/service/RedisRateLimitService.java`
- Config (reference): `gateway-service/src/main/java/com/gzasc/aishopping/gateway/config/IpRateLimitProperties.java`

- [ ] **Step 1: 修改 RedisRateLimitService，删除硬编码常量，构造器注入 IpRateLimitProperties**

```java
package com.gzasc.aishopping.gateway.service;

import com.gzasc.aishopping.gateway.config.IpRateLimitProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisRateLimitService {

    private static final String KEY_PREFIX = "rate_limit:ip:";

    private final StringRedisTemplate redisTemplate;
    private final IpRateLimitProperties ipRateLimitProperties;

    public RedisRateLimitService(StringRedisTemplate redisTemplate,
                                 IpRateLimitProperties ipRateLimitProperties) {
        this.redisTemplate = redisTemplate;
        this.ipRateLimitProperties = ipRateLimitProperties;
    }

    public boolean isAllowed(String ip) {
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, ipRateLimitProperties.getTimeWindowSeconds(), TimeUnit.SECONDS);
        }
        return count != null && count <= ipRateLimitProperties.getMaxRequests();
    }
}
```

变更要点：
1. 删除 `import org.springframework.beans.factory.annotation.Autowired`
2. 删除 `private static final long MAX_REQUESTS = 300`
3. 删除 `private static final long WINDOW_SECONDS = 60`
4. 删除 `@Autowired private StringRedisTemplate redisTemplate`
5. 新增 `import com.gzasc.aishopping.gateway.config.IpRateLimitProperties`
6. 新增 `private final IpRateLimitProperties ipRateLimitProperties`
7. 构造器注入 `redisTemplate` 和 `ipRateLimitProperties`
8. `WINDOW_SECONDS` → `ipRateLimitProperties.getTimeWindowSeconds()`
9. `MAX_REQUESTS` → `ipRateLimitProperties.getMaxRequests()`

- [ ] **Step 2: 编译验证**

```bash
cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\gateway-service
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行限流过滤器测试**

```bash
mvn test -pl . -Dtest="IpRateLimitFilterTest" -q
```

Expected: 6 个测试全部通过（GW-IP-009 用 properties 覆盖 `ip-rate-limit.max-requests=1`，现在配置真正生效，测试路径更真实）

- [ ] **Step 4: Commit**

```bash
git add gateway-service/src/main/java/com/gzasc/aishopping/gateway/service/RedisRateLimitService.java
git commit -m "fix: RedisRateLimitService 注入 IpRateLimitProperties 替代硬编码常量"
```

---

### Task 2: AuthServiceImpl.validateToken 改用 StpUtil

**Files:**
- Modify: `gateway-service/src/main/java/com/gzasc/aishopping/gateway/service/impl/AuthServiceImpl.java`

- [ ] **Step 1: 修改 validateToken 方法**

变更要点：
1. 删除 `import org.springframework.data.redis.core.StringRedisTemplate`
2. 删除 `private final StringRedisTemplate stringRedisTemplate` 字段
3. 从构造器删除 `StringRedisTemplate` 参数
4. `validateToken()` 方法实现替换

```java
package com.gzasc.aishopping.gateway.service.impl;

import com.gzasc.aishopping.gateway.config.AuthWhitelistProperties;
import com.gzasc.aishopping.gateway.exception.GatewayAuthException;
import com.gzasc.aishopping.gateway.service.AuthService;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.exception.SaTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final AuthWhitelistProperties whitelistProperties;

    public AuthServiceImpl(AuthWhitelistProperties whitelistProperties) {
        this.whitelistProperties = whitelistProperties;
    }

    // ... isPreFlightRequest, isWhiteList unchanged ...

    @Override
    public String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new GatewayAuthException(401, "未登录");
        }
        try {
            String loginId = StpUtil.getLoginIdByToken(token);
            log.debug("用户 {} 认证通过", loginId);
            return loginId;
        } catch (SaTokenException e) {
            log.warn("Token无效或已过期: {}", token);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }
    }

    // ... rest of the class unchanged ...
}
```

- [ ] **Step 2: 编译验证**

```bash
cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\gateway-service
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行 AuthServiceImplTest**

```bash
mvn test -pl . -Dtest="AuthServiceImplTest" -q
```

Expected: 29 个测试全部通过（`validateToken` 没有独立单测，原有测试不依赖此方法实现）

- [ ] **Step 4: Commit**

```bash
git add gateway-service/src/main/java/com/gzasc/aishopping/gateway/service/impl/AuthServiceImpl.java
git commit -m "fix: validateToken 改用 StpUtil.getLoginIdByToken 替代手动 Redis 查询"
```

---

### Task 3: SaTokenAuthGlobalFilter 处理 null accountType

**Files:**
- Modify: `gateway-service/src/main/java/com/gzasc/aishopping/gateway/filter/SaTokenAuthGlobalFilter.java`

- [ ] **Step 1: 在 getAccountType 返回 null 时抛 401**

```java
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (authService.isPreFlightRequest(request)) {
            return chain.filter(exchange);
        }

        if (authService.isWhiteList(path)) {
            log.debug("WhiteList path: {}, skip auth", path);
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("satoken");
        String loginId = authService.validateToken(token);

        String accountType = authService.getAccountType(token);
        if (accountType == null) {
            log.warn("Token session 已过期: {}", loginId);
            throw new GatewayAuthException(401, "登录已过期，请重新登录");
        }
        if (!authService.hasPermission(accountType, path, request)) {
            log.warn("{} 无权限访问路径 {}", loginId, path);
            throw new GatewayAuthException(403, "无权限访问该资源");
        }

        ServerHttpRequest newRequest = request.mutate()
                .header("userId", loginId)
                .header("satoken", token)
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }
```

变更要点：在 `getAccountType()` 之后、`hasPermission()` 之前，插入 `accountType == null` 检查，抛出 401 而非走到 403 路径。

- [ ] **Step 2: 编译验证**

```bash
cd F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\gateway-service
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 运行所有测试确认无回归**

```bash
mvn test -q
```

Expected: BUILD SUCCESS，所有 ~67 个测试通过

- [ ] **Step 4: Commit**

```bash
git add gateway-service/src/main/java/com/gzasc/aishopping/gateway/filter/SaTokenAuthGlobalFilter.java
git commit -m "fix: getAccountType 返回 null 时抛出 401 而非 403"
```
