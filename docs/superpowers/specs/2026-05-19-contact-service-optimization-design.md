# Contact Service 优化设计方案

## 优化目标
参考 auth-service 架构，全面提升 contact-service 代码质量，保持功能不变。

## 优化内容

### 1. 统一响应格式
```java
// 现有：Map.of("message", "xxx")
// 改为：{code: 200/400/500, message: "xxx", data: {...}}
```

### 2. 全局异常处理
- 添加 GlobalExceptionHandler
- 定义 ContactException 业务异常
- 移除各方法 try-catch

### 3. 参数校验
- Contact 实体添加 @NotBlank 等注解
- Controller 使用 @Valid 校验
- 移除手动校验代码

### 4. 事务管理
- Service 方法添加 @Transactional

### 5. 日志
- 替换 System.out.println 为 Slf4j log

### 6. 认证
- 保持 X-User-Id 头解析方式（与现有网关兼容）

## 文件变更

| 文件 | 变更 |
|------|------|
| Contact.java | 添加校验注解 |
| ContactController.java | 统一响应格式，使用@Valid |
| ContactServiceImpl.java | 添加@Transactional，改为log |
| GlobalExceptionHandler.java | 新建 |
| ContactException.java | 新建 |

## 实施顺序
1. 创建 ContactException
2. 创建 GlobalExceptionHandler
3. 修改 Contact.java 添加校验注解
4. 修改 ContactController.java
5. 修改 ContactServiceImpl.java