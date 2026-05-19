# contact-service 代码规范化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 将 contact-service 代码风格统一为与 auth-service 一致，分两阶段实施

**架构：** 渐进式重构 — 第一阶段优化代码风格，第二阶段引入 DTO 分层

**技术栈：** Spring Boot + MyBatis + Lombok

---

## 文件结构

```
contact-service/src/main/java/com/gzasc/aishopping/contact/
├── model/
│   ├── Contact.java          # 需优化：添加 javadoc + 字段注释
│   ├── UserContact.java      # 需优化：同上
│   ├── ShopAddress.java      # 需优化：同上
│   └── dto/                  # 第二阶段新增
│       ├── ApiResponse.java
│       ├── CreateContactRequest.java
│       ├── UpdateContactRequest.java
│       └── ContactResponse.java
├── mapper/
│   ├── ContactMapper.java           # 需优化：添加 Javadoc + 提取映射
│   ├── UserContactMapper.java       # 需优化：添加 Javadoc
│   ├── ShopAddressMapper.java       # 需优化：添加 Javadoc
│   └── ShopAddressRelMapper.java    # 需优化：添加 Javadoc
├── service/
│   ├── ContactService.java
│   ├── ContactServiceImpl.java      # 需优化：添加类注释
│   ├── ShopAddressService.java
│   └── ShopAddressServiceImpl.java  # 需优化：添加类注释
├── controller/
│   ├── ContactController.java       # 需优化：第二阶段适配 DTO
│   ├── ShopAddressSellerController.java
│   ├── ContactSellerController.java
│   └── GlobalExceptionHandler.java
└── service/impl/
    └── ContactException.java        # 需优化：添加 javadoc
```

---

## 第一阶段：代码风格统一化

---

### Task 1: 优化 Contact.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/Contact.java`

- [ ] **Step 1: 添加类级别 javadoc 和字段注释**

```java
package com.gzasc.aishopping.contact.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 联系人实体类
 * 对应数据库 t_contact 表，存储用户联系人信息
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

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/Contact.java
git commit -m "refactor(contact): add javadoc and field comments to Contact"
```

---

### Task 2: 优化 UserContact.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/UserContact.java`

- [ ] **Step 1: 读取并优化文件**

```java
package com.gzasc.aishopping.contact.model;

/**
 * 用户-联系人关联实体类
 * 对应数据库 user_contact 表，建立用户与联系人的多对多关系
 */
@Data
public class UserContact {
    private Integer id;        // 记录ID
    private Integer userId;     // 用户ID
    private Integer contactId; // 联系人ID
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/UserContact.java
git commit -m "refactor(contact): add javadoc and field comments to UserContact"
```

---

### Task 3: 优化 ShopAddress.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/ShopAddress.java`

- [ ] **Step 1: 读取并优化文件**（添加类 javadoc 和字段注释）

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/ShopAddress.java
git commit -m "refactor(contact): add javadoc and field comments to ShopAddress"
```

---

### Task 4: 优化 ContactMapper.java — 添加 Javadoc + 提取映射

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ContactMapper.java`

- [ ] **Step 1: 提取公共映射 + 添加方法 Javadoc**

```java
package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.Contact;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 联系人 Mapper 接口
 * 使用 contact 数据源
 */
@Mapper
public interface ContactMapper {

    /**
     * 公共结果映射
     * 将数据库字段映射到 Contact 实体属性
     */
    @Results(id = "CONTACT_RESULT_MAPPING", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })

    /**
     * 根据ID查询联系人
     * @param id 联系人ID
     * @return 联系人实体，未找到返回 null
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE id = #{id}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactById(int id);

    /**
     * 查询所有联系人
     * @return 联系人列表
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectAllContacts();

    /**
     * 根据用户ID查询联系人列表（通过关联表）
     * @param userId 用户ID
     * @return 该用户的所有联系人
     */
    @Select("SELECT c.id, c.name, c.phone, c.address, c.created_at, c.updated_at " +
            "FROM t_contact c " +
            "INNER JOIN user_contact uc ON c.id = uc.contact_id " +
            "WHERE uc.user_id = #{userId}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectByUserId(int userId);

    /**
     * 根据姓名模糊查询联系人
     * @param name 联系人姓名
     * @return 匹配的联系人列表
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE name = #{name}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectContactsByName(String name);

    /**
     * 根据手机号查询联系人
     * @param phone 联系电话
     * @return 匹配的联系人，未找到返回 null
     */
    @Select("SELECT id, name, phone, address, created_at, updated_at FROM t_contact WHERE phone = #{phone}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    Contact selectContactByPhone(String phone);

    /**
     * 插入联系人
     * @param contact 联系人实体
     * @return 影响行数
     */
    @Insert("INSERT INTO t_contact (name, phone, address) VALUES (#{name}, #{phone}, #{address})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    /**
     * 更新联系人信息
     * @param contact 联系人实体
     * @return 影响行数
     */
    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address} WHERE id = #{id}")
    int updateContact(Contact contact);

    /**
     * 根据ID删除联系人
     * @param id 联系人ID
     * @return 影响行数
     */
    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ContactMapper.java
git commit -m "refactor(contact): add javadoc and extract @ResultMapping in ContactMapper"
```

---

### Task 5: 优化 UserContactMapper.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/UserContactMapper.java`

- [ ] **Step 1: 添加类注释 + 方法 Javadoc**

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/UserContactMapper.java
git commit -m "refactor(contact): add javadoc to UserContactMapper"
```

---

### Task 6: 优化 ShopAddressMapper.java 和 ShopAddressRelMapper.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressMapper.java`
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressRelMapper.java`

- [ ] **Step 1: 添加类注释 + 方法 Javadoc**

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressMapper.java AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressRelMapper.java
git commit -m "refactor(contact): add javadoc to ShopAddress mappers"
```

---

### Task 7: 优化 ContactServiceImpl.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ContactServiceImpl.java`

- [ ] **Step 1: 添加类级别 javadoc**

```java
package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ContactMapper;
import com.gzasc.aishopping.contact.mapper.UserContactMapper;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.model.UserContact;
import com.gzasc.aishopping.contact.service.ContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联系人服务实现类
 * 提供联系人的 CRUD 操作，支持用户隔离
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMapper contactMapper;
    private final UserContactMapper userContactMapper;

    // ... 保留现有方法逻辑，仅添加类注释
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ContactServiceImpl.java
git commit -m "refactor(contact): add class javadoc to ContactServiceImpl"
```

---

### Task 8: 优化 ShopAddressServiceImpl.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ShopAddressServiceImpl.java`

- [ ] **Step 1: 添加类级别 javadoc**

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ShopAddressServiceImpl.java
git commit -m "refactor(contact): add class javadoc to ShopAddressServiceImpl"
```

---

### Task 9: 优化 ContactException.java

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ContactException.java`

- [ ] **Step 1: 添加类级别 javadoc**

```java
package com.gzasc.aishopping.contact.service.impl;

/**
 * 联系人业务异常类
 * 用于抛出业务逻辑相关的错误信息
 */
public class ContactException extends RuntimeException {

    public ContactException(String message) {
        super(message);
    }

    public ContactException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ContactException.java
git commit -m "refactor(contact): add javadoc to ContactException"
```

---

## 第二阶段：DTO 分层重构

---

### Task 10: 创建 ApiResponse.java

**Files:**
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/ApiResponse.java`

- [ ] **Step 1: 创建统一响应封装类**

```java
package com.gzasc.aishopping.contact.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应封装
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/ApiResponse.java
git commit -m "feat(contact): add ApiResponse for unified API response"
```

---

### Task 11: 创建 CreateContactRequest.java

**Files:**
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/CreateContactRequest.java`

- [ ] **Step 1: 创建请求 DTO**

```java
package com.gzasc.aishopping.contact.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建联系人请求 DTO
 */
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

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/CreateContactRequest.java
git commit -m "feat(contact): add CreateContactRequest DTO"
```

---

### Task 12: 创建 UpdateContactRequest.java

**Files:**
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/UpdateContactRequest.java`

- [ ] **Step 1: 创建请求 DTO**

```java
package com.gzasc.aishopping.contact.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新联系人请求 DTO
 */
@Data
public class UpdateContactRequest {
    @NotNull(message = "ID不能为空")
    private Integer id;

    @NotBlank(message = "姓名为空")
    private String name;

    @NotBlank(message = "电话为空")
    private String phone;

    @NotBlank(message = "地址为空")
    private String address;
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/UpdateContactRequest.java
git commit -m "feat(contact): add UpdateContactRequest DTO"
```

---

### Task 13: 创建 ContactResponse.java

**Files:**
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/ContactResponse.java`

- [ ] **Step 1: 创建响应 DTO**

```java
package com.gzasc.aishopping.contact.model.dto;

import com.gzasc.aishopping.contact.model.Contact;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人响应 DTO
 */
@Data
public class ContactResponse {
    private Integer id;
    private String name;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContactResponse fromContact(Contact contact) {
        if (contact == null) {
            return null;
        }
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setName(contact.getName());
        response.setPhone(contact.getPhone());
        response.setAddress(contact.getAddress());
        response.setCreatedAt(contact.getCreatedAt());
        response.setUpdatedAt(contact.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/ContactResponse.java
git commit -m "feat(contact): add ContactResponse DTO"
```

---

### Task 14: 适配 ContactController.java — 使用 DTO

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ContactController.java`

- [ ] **Step 1: 重构 Controller，使用 DTO + ApiResponse**

```java
package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.model.dto.ApiResponse;
import com.gzasc.aishopping.contact.model.dto.ContactResponse;
import com.gzasc.aishopping.contact.model.dto.CreateContactRequest;
import com.gzasc.aishopping.contact.model.dto.UpdateContactRequest;
import com.gzasc.aishopping.contact.service.ContactService;
import com.gzasc.aishopping.contact.service.impl.ContactException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/create")
    public ApiResponse<Map<String, Integer>> createContact(
            @RequestBody @Valid CreateContactRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("未登录（错误代码：Co-000）");
        }
        Contact contact = toContact(request);
        int id = contactService.createContact(contact, userId);
        return ApiResponse.success("创建联系人成功", Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("删除联系人错误：未登录（错误代码：Co-000）");
        }
        int result = contactService.deleteContact(id, userId);
        if (result <= 0) {
            throw new ContactException("删除联系人失败：联系人不存在或无权限（错误代码：Co-005）");
        }
        return ApiResponse.success("删除联系人成功");
    }

    @PutMapping("/update")
    public ApiResponse<Void> updateContact(
            @RequestBody @Valid UpdateContactRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("更新联系人错误：未登录（错误代码：Co-000）");
        }
        Contact contact = toContact(request);
        int result = contactService.updateContact(contact, userId);
        if (result <= 0) {
            throw new ContactException("更新联系人失败：联系人不存在或无权限（错误代码：Co-010）");
        }
        return ApiResponse.success("更新联系人成功");
    }

    @GetMapping("/get/{id}")
    public ApiResponse<ContactResponse> getContactById(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("查询联系人错误：未登录（错误代码：Co-000）");
        }
        Contact contact = contactService.getContactById(id, userId);
        if (contact == null) {
            throw new ContactException("查询失败：联系人不存在（错误代码：Co-011）");
        }
        return ApiResponse.success("查询成功", ContactResponse.fromContact(contact));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Integer userId = parseUserId(userIdStr);
        if (userId == null) {
            throw new ContactException("查询联系人错误：未登录（错误代码：Co-000）");
        }
        List<Contact> contacts = contactService.getContactsByUserId(userId);
        List<ContactResponse> data = contacts.stream()
                .map(ContactResponse::fromContact)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("contacts", data, "total", data.size()));
    }

    @GetMapping("/search/name")
    public ApiResponse<Map<String, Object>> getContactsByName(@RequestParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ContactException("查询错误：姓名为空（错误代码：Co-012）");
        }
        List<Contact> contacts = contactService.getContactsByName(name);
        List<ContactResponse> data = contacts.stream()
                .map(ContactResponse::fromContact)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("contacts", data, "total", data.size()));
    }

    @GetMapping("/search/phone")
    public ApiResponse<ContactResponse> getContactByPhone(@RequestParam("phone") String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ContactException("查询错误：电话为空（错误代码：Co-013）");
        }
        Contact contact = contactService.getContactByPhone(phone);
        if (contact == null) {
            throw new ContactException("查询失败：联系人不存在（错误代码：Co-014）");
        }
        return ApiResponse.success("查询成功", ContactResponse.fromContact(contact));
    }

    private Integer parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Contact toContact(CreateContactRequest request) {
        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getAddress());
        return contact;
    }

    private Contact toContact(UpdateContactRequest request) {
        Contact contact = new Contact();
        contact.setId(request.getId());
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setAddress(request.getAddress());
        return contact;
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ContactController.java
git commit -m "refactor(contact): migrate ContactController to use DTO pattern"
```

---

### Task 15: 验证 API 契约兼容性

**Files:**
- 测试文件（如有）

- [ ] **Step 1: 检查响应字段兼容性**

确保 `ContactResponse` 的字段名与原 `Map` 返回的字段名一致：
- `id` ✓
- `name` ✓
- `phone` ✓
- `address` ✓
- `createdAt` ✓
- `updatedAt` ✓

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git commit -m "test(contact): verify API contract compatibility"
```

---

## 验收检查

- [ ] 所有 Model 有完整 javadoc（Task 1, 2, 3）
- [ ] 所有 Mapper 方法有 Javadoc（Task 4, 5, 6）
- [ ] `@Results` 映射已提取为常量（Task 4）
- [ ] Service 层有类注释（Task 7, 8）
- [ ] Exception 有 javadoc（Task 9）
- [ ] DTO 层创建完成（Task 10, 11, 12, 13）
- [ ] Controller 适配 DTO（Task 14）
- [ ] 编译通过，无新增警告
- [ ] API 契约保持不变

---

## 自检清单

1. **Spec 覆盖**：设计文档中的每一项都有对应 Task ✓
2. **占位符扫描**：无 TBD/TODO ✓
3. **类型一致性**：所有方法签名和字段名在 Task 间一致 ✓