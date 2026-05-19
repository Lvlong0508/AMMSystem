# contact-service 代码规范化设计方案

**日期**: 2026-05-19
**状态**: 已批准
**版本**: 1.0

---

## 1. 目标

将 contact-service 的代码风格统一为与 auth-service 一致，采用渐进式优化，分两阶段实施：
- **第一阶段**：代码风格统一化
- **第二阶段**：DTO 分层重构

**约束**：不改变现有 API 契约和业务逻辑，仅优化代码质量和架构。

---

## 2. 第一阶段：代码风格统一化

### 2.1 Model 层优化

#### `Contact.java`
- 添加类级别 javadoc，说明用途
- 字段补充注释
- 使用 `@NoArgsConstructor` + `@AllArgsConstructor` + `@Data`

**当前**:
```java
@Data
public class Contact {
    private int id;
    @NotBlank(message = "姓名为空")
    private String name;
    // ...
}
```

**目标**:
```java
/**
 * 联系人实体类
 * 对应数据库 t_contact 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    private Integer id;           // 联系人ID

    @NotBlank(message = "姓名为空")
    private String name;          // 联系人姓名

    @NotBlank(message = "电话为空")
    private String phone;         // 联系电话

    @NotBlank(message = "地址为空")
    private String address;        // 联系地址
    private LocalDateTime createdAt;  // 创建时间
    private LocalDateTime updatedAt;  // 更新时间
}
```

#### `UserContact.java`
- 同样添加类注释和字段注释

#### `ShopAddress.java`
- 同样添加类注释和字段注释

### 2.2 Mapper 层优化

#### `ContactMapper.java`
- 每个方法添加 Javadoc
- 提取重复的 `@Results` 映射为公共映射

**当前**:
```java
@Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE id = #{id}")
@Results({
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        // ... 重复 5 次
})
Contact selectContactById(int id);
```

**目标**:
```java
/**
 * 根据ID查询联系人
 */
@Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE id = #{id}")
@Results(CONTACT_RESULT_MAPPING)
Contact selectContactById(int id);

// 公共映射常量
@Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact")
@Results(CONTACT_RESULT_MAPPING)
List<Contact> selectAllContacts();
```

### 2.3 Service 层优化

#### `ContactService.java` / `ContactServiceImpl.java`
- 保留现有结构
- 添加类级别注释
- 保留 `@Slf4j` 日志

#### `ShopAddressService.java` / `ShopAddressServiceImpl.java`
- 同上

### 2.4 Controller 层优化

#### `ContactController.java`
- 每个 endpoint 添加日志（已有）
- 响应格式保持一致

#### 其他 Controller
- 保持现有逻辑

### 2.5 Exception 类优化

#### `ContactException.java`
- 添加类 javadoc

---

## 3. 第二阶段：DTO 分层重构

### 3.1 目录结构

```
model/
├── Contact.java              # 保留，作为数据库实体
├── UserContact.java          # 保留，作为关联实体
├── ShopAddress.java          # 保留
├── dto/
│   ├── CreateContactRequest.java
│   ├── UpdateContactRequest.java
│   ├── ContactResponse.java
│   └── ApiResponse.java      # 统一响应封装
```

### 3.2 DTO 定义

#### `ApiResponse.java`
```java
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

#### `CreateContactRequest.java`
```java
@Data
public class CreateContactRequest {
    @NotBlank(message = "姓名为空")
    private String name;

    @NotBlank(message = "电话为空")
    private String phone;

    @NotBlank(message = "地址为空")
    private String address;
}
```

#### `UpdateContactRequest.java`
```java
@Data
public class UpdateContactRequest {
    private Integer id;  // 必填

    @NotBlank(message = "姓名为空")
    private String name;

    @NotBlank(message = "电话为空")
    private String phone;

    @NotBlank(message = "地址为空")
    private String address;
}
```

#### `ContactResponse.java`
```java
@Data
public class ContactResponse {
    private Integer id;
    private String name;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContactResponse fromContact(Contact contact) {
        // 转换逻辑
    }
}
```

### 3.3 Controller 适配

**当前**:
```java
@PostMapping("/create")
public Map<String, Object> createContact(@RequestBody @Valid Contact contact, ...) {
    return Map.of("code", 200, "message", "创建联系人成功", "data", Map.of("id", id));
}
```

**目标**:
```java
@PostMapping("/create")
public ApiResponse<Map<String, Integer>> createContact(
        @RequestBody @Valid CreateContactRequest request,
        @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
    Integer userId = parseUserId(userIdStr);
    if (userId == null) {
        throw new ContactException("未登录");
    }
    Contact contact = toContact(request);
    int id = contactService.createContact(contact, userId);
    return ApiResponse.success("创建联系人成功", Map.of("id", id));
}
```

### 3.4 Model 角色转变

| 对象 | 角色 | 说明 |
|------|------|------|
| `Contact.java` | 数据库实体 | 仅与 MyBatis 交互 |
| `*Request.java` | 请求 DTO | Controller 入参 |
| `*Response.java` | 响应 DTO | Controller 出参 |

---

## 4. 实施顺序

### 第一阶段（代码风格统一化）
1. `Contact.java` → `UserContact.java` → `ShopAddress.java`
2. `ContactMapper.java` → `UserContactMapper.java` → `ShopAddressMapper.java` → `ShopAddressRelMapper.java`
3. `ContactService.java` / `ContactServiceImpl.java`
4. `ShopAddressService.java` / `ShopAddressServiceImpl.java`
5. `ContactException.java`

### 第二阶段（DTO 重构）
1. 创建 `model/dto/` 目录
2. 创建 `ApiResponse.java`
3. 创建 `CreateContactRequest.java`
4. 创建 `UpdateContactRequest.java`
5. 创建 `ContactResponse.java`
6. Controller 适配
7. 删除 Model 中的验证注解（或保留用于内部校验）

---

## 5. 验收标准

### 第一阶段验收
- [ ] 所有 Model 有完整 javadoc
- [ ] 所有 Mapper 方法有 Javadoc
- [ ] 字段注释完整（与 auth-service 一致）
- [ ] `@Results` 映射已提取为常量
- [ ] 编译通过，无新增警告

### 第二阶段验收
- [ ] 无 `Map<String, Object>` 作为 Controller 返回值
- [ ] 所有请求参数使用 DTO
- [ ] 所有响应使用 ApiResponse 封装
- [ ] API 契约保持不变（入参/出参字段一致）
- [ ] 单元测试通过（如有）

---

## 6. 风险与回滚

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| DTO 重构改变 API 契约 | 高 | 确保字段名一致，使用 ApiResponse.data 兼容 |
| Mapper 映射提取出错 | 中 | 先验证单个查询，提交前测试 |
| 验证注解位置变更 | 低 | Model 保留注解，DTO 明确标注 |

回滚策略：每个阶段完成后，如发现问题，通过 Git revert 单阶段提交。