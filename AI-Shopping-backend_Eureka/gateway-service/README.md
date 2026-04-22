# Gateway Service - 网关服务

## 功能说明

本服务作为统一入口，负责：
1. **IP限流** - 同一IP 1分钟内最多30次请求
2. **统一认证** - 使用SaToken验证登录状态
3. **路由分发** - 根据URL前缀区分用户/商家，转发到对应微服务
4. **用户信息传递** - 通过Header将userId传递给下游服务

## 路由规则

### 用户端路由 (`/api/user/**`)
| 路由ID | 目标服务 | 路径 |
|-------|---------|------|
| user-auth | auth-service | /api/user/auth/** |
| user-product | product-service | /api/user/product/** |
| user-order | order-service | /api/user/order/** |
| user-contact | contact-service | /api/user/contact/** |
| user-logistics | logistics-service | /api/user/logistics/** |
| user-chat | chat-service | /api/user/chat/** |

### 商家端路由 (`/api/seller/**`)
| 路由ID | 目标服务 | 路径 |
|-------|---------|------|
| seller-auth | auth-service | /api/seller/auth/** |
| seller-product | product-service | /api/seller/product/** |
| seller-order | order-service | /api/seller/order/** |
| seller-contact | contact-service | /api/seller/contact/** |
| seller-logistics | logistics-service | /api/seller/logistics/** |
| seller-chat | chat-service | /api/seller/chat/** |

## 认证白名单

以下路径无需认证：
- /api/user/auth/login
- /api/user/auth/register
- /api/seller/auth/login
- /api/seller/auth/register

## 测试方法

### 运行单元测试
```bash
mvn test
```

### 手动测试（使用test-requests.http文件）
1. 在IDEA中打开 `src/test/resources/test-requests.http`
2. 确保Eureka、Redis和auth-service已启动
3. 依次执行HTTP请求进行验证

## 过滤器执行顺序

1. `IpRateLimitFilter` (order=-200) - IP限流
2. `SaTokenAuthFilter` (order=-100) - 认证检查
3. `UserIdHeaderFilter` (order=0) - 添加userId到Header

## 下游服务获取userId

```java
@GetMapping("/api/user/order/list")
public Result listOrders(HttpServletRequest request) {
    String userId = request.getHeader("userId");
    // 使用userId查询...
}
```
