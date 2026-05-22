# contact-service 架构统一规范 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 contact-service 的响应格式、userId 类型、日志方式对齐到 auth-service 的统一标准，并引入 common-api 依赖。

**Architecture:** contact-service 和 auth-service 是同一父项目下的平行微服务。本次不改变业务逻辑，仅做代码规范对齐：Controller 返回 `Map<String, Object>` → `ApiResponse<T>`（common-api）；`X-User-Id` 解析从 `Integer` → `Long`；`GlobalExceptionHandler` 同步对齐；本地自实现的 `ApiResponse` 删除。

**Tech Stack:** Spring Boot 3, MyBatis 3, common-api (ApiResponse), Lombok

**Spec ref:** `docs/superpowers/specs/2026-05-22-contact-service-unify-design.md`

---

### 文件变更总览

| # | 文件 | 操作 | 变更内容 |
|---|------|------|---------|
| 1 | `common-api/.../ContactFeignClient.java` | 修改 | 修复路径（`/api/seller/contact/get/` → `/internal/contact/`），保持返回 `Map<String, Object>` 以兼容现有消费者 |
| 2 | `contact-service/pom.xml` | 修改 | 追加 common-api 依赖 |
| 3 | `contact-service/.../dto/ApiResponse.java` | **删除** | 本地自实现，被 common-api 替换 |
| 4 | `contact-service/.../service/UserContactService.java` | 修改 | `int userId` → `Long userId` |
| 5 | `contact-service/.../service/impl/UserContactServiceImpl.java` | 修改 | `int userId` → `Long userId` |
| 6 | `contact-service/.../mapper/UserContactMapper.java` | 修改 | `int userId` → `Long userId` |
| 7 | `contact-service/.../controller/UserContactController.java` | 修改 | 响应 `Map` → `ApiResponse`，userId `Integer` → `Long` |
| 8 | `contact-service/.../controller/MerchantContactController.java` | 修改 | 响应 `Map` → `ApiResponse`（shopId 保持 `String`）|
| 9 | `contact-service/.../controller/GlobalExceptionHandler.java` | 修改 | 响应 `Map` → `ApiResponse<Void>`，`LoggerFactory` → `@Slf4j` |
| 10 | `contact-service/.../controller/internal/InternalContactController.java` | 修改 | 响应 `Map` → `ApiResponse`，移除 try-catch |
| 11 | `contact-service/.../resources/application.yml` | 修改 | 追加 MyBatis SQL 日志配置 |
| 12 | 数据库 `eureka_contact.user_contact.user_id` | 迁移 | INT → BIGINT |

---

### Task 1: common-api — 修复 ContactFeignClient 路径

**Files:**
- Modify: `common-api/src/main/java/com/gzasc/aishopping/common/feign/contact/ContactFeignClient.java`

**背景：** 当前 `ContactFeignClient` 映射到 `/api/seller/contact/get/{id}` 路径，但 contact-service 无此端点。实际内部端点是 `/internal/contact/{id}`。且两个方法映射到同一路径，存在冲突。

- [ ] **Step 1: 修改 FeignClient**

```java
package com.gzasc.aishopping.common.feign.contact;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "contact-service")
public interface ContactFeignClient {

    @GetMapping("/internal/contact/{id}")
    Map<String, Object> getContactById(@PathVariable("id") Integer id);

    @GetMapping("/internal/contact/{id}")
    Map<String, Object> getContactByIdWithUser(
            @PathVariable("id") Integer id,
            @RequestHeader("X-User-Id") String userId);
}
```

变更说明：
- 路径从 `/api/seller/contact/get/{id}` → `/internal/contact/{id}`（真实存在的端点）
- 返回类型保持 `Map<String, Object>`（不破坏现有消费者 shop-service、chat-service）
- 两个方法映射到同一路径的问题依然存在，需后续统一重构 —— 但因 Feign 实际按方法签名调用，Spring Cloud 会按方法各自独立处理，当前不阻塞功能
- ⚠️ `getContactByIdWithUser` 中的 `@RequestHeader("X-User-Id") String userId` 由网关在请求中注入，contact-service 内部端点 `GET /internal/contact/{id}` 会忽略该头（有则用、无则略），不影响功能

- [ ] **Step 2: 验证编译**

```bash
cd AI-Shopping-backend_Eureka
mvn compile -pl common-api -am
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add common-api/src/main/java/com/gzasc/aishopping/common/feign/contact/ContactFeignClient.java
git commit -m "fix(common-api): 修复ContactFeignClient路径为/internal/contact/{id}"
```

---

### Task 2: contact-service — 引入 common-api 依赖 + 删除本地 ApiResponse

**Files:**
- Modify: `contact-service/pom.xml`
- Delete: `contact-service/src/main/java/com/gzasc/aishopping/contact/dto/ApiResponse.java`

- [ ] **Step 1: pom.xml 追加 common-api 依赖**

在 `<dependencies>` 末尾（`spring-boot-starter-validation` 下方）追加：

```xml
        <dependency>
            <groupId>com.gzasc</groupId>
            <artifactId>common-api</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
```

- [ ] **Step 2: 删除本地 ApiResponse**

删除文件：`contact-service/src/main/java/com/gzasc/aishopping/contact/dto/ApiResponse.java`

- [ ] **Step 3: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add contact-service/pom.xml
git add -A contact-service/src/main/java/com/gzasc/aishopping/contact/dto/ApiResponse.java
git commit -m "feat(contact-service): 引入common-api依赖，删除本地ApiResponse"
```

---

### Task 3: contact-service — userId 类型 int → Long（Service 接口 + 实现）

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/UserContactService.java`
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/UserContactServiceImpl.java`

**背景：** auth-service 使用雪花算法生成 Long 类型 ID，contact-service 的 `user_contact.user_id` 需改为 BIGINT，对应 Java 参数从 `int` 改为 `Long`。

- [ ] **Step 1: 修改 UserContactService 接口**

所有 `int userId` → `Long userId`：

```java
public interface UserContactService {
    int createContact(Contact contact, Long userId);
    int deleteContact(int id, Long userId);
    int updateContact(Contact contact, Long userId);
    List<Contact> getContactsByUserId(Long userId);
    int setDefaultContact(int id, Long userId);
    Contact g(int id);
}
```

- [ ] **Step 2: 修改 UserContactServiceImpl**

所有 `int userId` → `Long userId`。`selectUserIdsByContactId` 返回值也改为 `List<Long>`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class UserContactServiceImpl implements UserContactService {

    private final UserContactMapper userContactMapper;

    @Override
    @Transactional
    public int createContact(Contact contact, Long userId) {
        int result = userContactMapper.insertContact(contact);
        if (result > 0) {
            userContactMapper.insertUserRelContact(userId, contact.getId());
        }
        return result;
    }

    @Override
    @Transactional
    public int deleteContact(int id, Long userId) {
        List<Long> userIds = userContactMapper.selectUserIdsByContactId(id);
        if (userIds.isEmpty() || !userIds.contains(userId)) {
            return 0;
        }
        userContactMapper.deleteRelByContactId(id);
        return userContactMapper.deleteContactById(id);
    }

    @Override
    @Transactional
    public int updateContact(Contact contact, Long userId) {
        List<Long> userIds = userContactMapper.selectUserIdsByContactId(contact.getId());
        if (userIds.isEmpty() || !userIds.contains(userId)) {
            return 0;
        }
        return userContactMapper.updateContact(contact);
    }

    @Override
    public List<Contact> getContactsByUserId(Long userId) {
        return userContactMapper.selectByUserId(userId);
    }

    @Override
    public int setDefaultContact(int id, Long userId) {
        List<Long> userIds = userContactMapper.selectUserIdsByContactId(id);
        if (userIds.isEmpty() || !userIds.contains(userId)) {
            return 0;
        }
        return userContactMapper.setDefaultById(id);
    }

    @Override
    public Contact g(int id) {
        return userContactMapper.selectContactById(id);
    }
}
```

- [ ] **Step 3: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS（Mapper 尚未改，此处会有编译错误，属于正常中间状态）

- [ ] **Step 4: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/service/UserContactService.java
git add contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/UserContactServiceImpl.java
git commit -m "refactor(contact-service): userId类型 int→Long (Service层)"
```

---

### Task 4: contact-service — userId 类型 int → Long（Mapper）

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/UserContactMapper.java`

- [ ] **Step 1: 修改 UserContactMapper**

将所有 `int userId` 参数改为 `Long userId`，`selectUserIdsByContactId` 返回值从 `List<Integer>` → `List<Long>`：

```java
@Mapper
public interface UserContactMapper {

    @Results(id = "CONTACT_RESULT_MAPPING", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })

    @Select("SELECT c.id, c.name, c.phone, c.address, c.is_default, c.created_at, c.updated_at " +
            "FROM t_contact c " +
            "INNER JOIN user_contact uc ON c.id = uc.contact_id " +
            "WHERE uc.user_id = #{userId}")
    @ResultMap("CONTACT_RESULT_MAPPING")
    List<Contact> selectByUserId(Long userId);

    @Select("SELECT * FROM t_contact WHERE id = #{id}")
    Contact selectContactById(int id);

    @Insert("INSERT INTO user_contact (user_id, contact_id) VALUES (#{userId}, #{contactId})")
    int insertUserRelContact(@Param("userId") Long userId, @Param("contactId") int contactId);

    @Delete("DELETE FROM user_contact WHERE contact_id = #{contactId}")
    int deleteRelByContactId(int contactId);

    @Insert("INSERT INTO t_contact (name, phone, address, is_default) VALUES (#{name}, #{phone}, #{address}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContact(Contact contact);

    @Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address}, is_default = #{isDefault} WHERE id = #{id}")
    int updateContact(Contact contact);

    @Delete("DELETE FROM t_contact WHERE id = #{id}")
    int deleteContactById(int id);

    @Update("UPDATE t_contact SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);

    @Select("SELECT user_id FROM user_contact WHERE contact_id = #{contactId}")
    List<Long> selectUserIdsByContactId(int contactId);
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/UserContactMapper.java
git commit -m "refactor(contact-service): userId类型 int→Long (Mapper层)"
```

---

### Task 5: contact-service — UserContactController 响应格式 + userId Long

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/UserContactController.java`

**变更：** 返回 `Map<String, Object>` → `ApiResponse<T>`；userId 解析 `Integer.parseInt()` → `Long.parseLong()`

- [ ] **Step 1: 修改 UserContactController**

```java
package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.dto.ContactResponse;
import com.gzasc.aishopping.contact.dto.CreateContactRequest;
import com.gzasc.aishopping.contact.dto.UpdateContactRequest;
import com.gzasc.aishopping.contact.service.UserContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/contact")
@RequiredArgsConstructor
public class UserContactController {

    private final UserContactService userContactService;

    @PostMapping("/create")
    public ApiResponse<?> createContact(
            @RequestBody @Valid CreateContactRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return ApiResponse.error(401, "未登录");
        }
        if (bindingResult.hasErrors()) {
            return ApiResponse.error(400, "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return ApiResponse.error(401, "未登录");
        }

        Contact contact = toContact(request);
        int id = userContactService.createContact(contact, userId);
        return ApiResponse.success("创建地址成功", java.util.Map.of("id", id));
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> deleteContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        int result = userContactService.deleteContact(id, userId);
        if (result <= 0) {
            return ApiResponse.error(400, "删除地址失败：地址不存在");
        }
        return ApiResponse.success("删除地址成功", null);
    }

    @PutMapping("/update")
    public ApiResponse<?> updateContact(
            @RequestBody @Valid UpdateContactRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return ApiResponse.error(401, "未登录");
        }
        if (bindingResult.hasErrors()) {
            return ApiResponse.error(400, "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return ApiResponse.error(401, "未登录");
        }

        Contact contact = toContact(request);
        int result = userContactService.updateContact(contact, userId);
        if (result <= 0) {
            return ApiResponse.error(400, "更新联系人失败：地址不存在");
        }
        return ApiResponse.success("更新地址成功", null);
    }

    @GetMapping("/list")
    public ApiResponse<?> getContacts(
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<Contact> contacts = userContactService.getContactsByUserId(userId);
        List<ContactResponse> data = contacts.stream()
                .map(ContactResponse::fromContact)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", java.util.Map.of("contacts", data, "total", data.size()));
    }

    @PutMapping("/set-default/{id}")
    public ApiResponse<?> setDefaultContact(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
        Long userId = getUserId(userIdStr);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        int result = userContactService.setDefaultContact(id, userId);
        if (result <= 0) {
            return ApiResponse.error(400, "设置失败：地址不存在");
        }
        return ApiResponse.success("设置成功", null);
    }

    private Long getUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userIdStr);
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
        Contact contact = toContact(request);
        contact.setId(request.getId());
        return contact;
    }
}
```

变更要点：
- 所有返回类型 `Map<String, Object>` → `ApiResponse<?>`
- `Integer.parseInt()` → `Long.parseLong()`
- `Map.of("code", ..., "message", ..., "data", ...)` → `ApiResponse.success()` / `ApiResponse.error()`

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/controller/UserContactController.java
git commit -m "refactor(contact-service): UserContactController 统一ApiResponse + userId Long"
```

---

### Task 6: contact-service — MerchantContactController 响应格式统一

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/MerchantContactController.java`

**变更：** 仅替换返回格式（shopId 保持 `String`，不修改类型）

- [ ] **Step 1: 修改 MerchantContactController**

```java
package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.contact.dto.AddressResponse;
import com.gzasc.aishopping.contact.dto.CreateAddressRequest;
import com.gzasc.aishopping.contact.dto.UpdateAddressRequest;
import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/address")
@RequiredArgsConstructor
public class MerchantContactController {

    private final ShopAddressService shopAddressService;

    @PostMapping("/create")
    public ApiResponse<?> createAddress(
            @RequestBody @Valid CreateAddressRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return ApiResponse.error(401, "未获取到店铺ID");
        }
        if (bindingResult.hasErrors()) {
            return ApiResponse.error(400, "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        ShopAddress address = toShopAddress(request);
        int id = shopAddressService.createAddress(address, shopId);
        return ApiResponse.success("新增成功", java.util.Map.of("id", id));
    }

    @PutMapping("/update/{id}")
    public ApiResponse<?> updateAddress(
            @PathVariable("id") int id,
            @RequestBody @Valid UpdateAddressRequest request,
            BindingResult bindingResult,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return ApiResponse.error(401, "未获取到店铺ID");
        }
        if (bindingResult.hasErrors()) {
            return ApiResponse.error(400, "参数错误：" + bindingResult.getFieldError().getDefaultMessage());
        }

        ShopAddress address = toShopAddress(request);
        address.setId(id);
        int rows = shopAddressService.updateAddress(address, shopId);
        if (rows > 0) {
            return ApiResponse.success("修改成功", null);
        }
        return ApiResponse.error(400, "修改失败：地址不存在或不属于该店铺");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return ApiResponse.error(401, "未获取到店铺ID");
        }

        int rows = shopAddressService.deleteAddress(id, shopId);
        if (rows > 0) {
            return ApiResponse.success("删除成功", null);
        }
        return ApiResponse.error(400, "删除失败：地址不存在或不属于该店铺");
    }

    @GetMapping("/list")
    public ApiResponse<?> getAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return ApiResponse.error(401, "未获取到店铺ID");
        }

        List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
        List<AddressResponse> data = addresses.stream()
                .map(AddressResponse::fromShopAddress)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", java.util.Map.of("addresses", data, "total", data.size()));
    }

    @GetMapping("/ship-default")
    public ApiResponse<?> getDefaultShipAddress(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return ApiResponse.error(401, "未获取到店铺ID");
        }

        ShopAddress address = shopAddressService.getDefaultShipAddressByShopId(shopId);
        return ApiResponse.success("查询成功", address != null ? AddressResponse.fromShopAddress(address) : null);
    }

    @PutMapping("/set-default/{id}")
    public ApiResponse<?> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopIdStr) {
        String shopId = getShopId(shopIdStr);
        if (shopId == null) {
            return ApiResponse.error(401, "未获取到店铺ID");
        }

        int rows = shopAddressService.setDefaultAddress(id, shopId);
        if (rows > 0) {
            return ApiResponse.success("设置成功", null);
        }
        return ApiResponse.error(400, "设置失败：地址不存在或不属于该店铺");
    }

    private String getShopId(String shopIdStr) {
        if (shopIdStr == null || shopIdStr.trim().isEmpty()) {
            return null;
        }
        return shopIdStr;
    }

    private ShopAddress toShopAddress(CreateAddressRequest request) {
        return toShopAddress(request.getName(), request.getPhone(), request.getAddress(),
                request.getAddressType() != null ? request.getAddressType() : 1,
                request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
    }

    private ShopAddress toShopAddress(UpdateAddressRequest request) {
        return toShopAddress(request.getName(), request.getPhone(), request.getAddress(),
                request.getAddressType(),
                request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
    }

    private ShopAddress toShopAddress(String name, String phone, String address, Integer addressType, Integer isDefault) {
        ShopAddress shopAddress = new ShopAddress();
        shopAddress.setName(name);
        shopAddress.setPhone(phone);
        shopAddress.setAddress(address);
        shopAddress.setAddressType(addressType);
        shopAddress.setIsDefault(isDefault);
        return shopAddress;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/controller/MerchantContactController.java
git commit -m "refactor(contact-service): MerchantContactController 统一ApiResponse响应格式"
```

---

### Task 7: contact-service — GlobalExceptionHandler 对齐

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/GlobalExceptionHandler.java`

**变更：** 返回 `Map<String, Object>` → `ApiResponse<Void>`；`LoggerFactory` → `@Slf4j`

- [ ] **Step 1: 修改 GlobalExceptionHandler**

```java
package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.contact.exception.ContactException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ContactException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleContactException(ContactException e) {
        log.warn("联系人业务异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数验证失败";
        log.warn("参数验证失败: {}", message);
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统错误", e);
        return ApiResponse.error(500, "系统错误，请稍后重试");
    }
}
```

变更要点：
- `import java.util.Map` → 删除
- `import org.slf4j.Logger / LoggerFactory` → `import lombok.extern.slf4j.Slf4j`
- 返回类型 `Map<String, Object>` → `ApiResponse<Void>`
- `Map.of("code", ..., "message", ...)` → `ApiResponse.error(...)`
- 类上增加 `@Slf4j`，删除 `private static final Logger log = LoggerFactory.getLogger(...)`

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/controller/GlobalExceptionHandler.java
git commit -m "refactor(contact-service): GlobalExceptionHandler 统一ApiResponse + @Slf4j"
```

---

### Task 8: contact-service — InternalContactController 对齐

**Files:**
- Modify: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/internal/InternalContactController.java`

**变更：** 返回 `Map` → `ApiResponse`；移除 try-catch 让全局异常处理器统一处理

- [ ] **Step 1: 修改 InternalContactController**

```java
package com.gzasc.aishopping.contact.controller.internal;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.contact.model.Contact;
import com.gzasc.aishopping.contact.service.UserContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/contact")
@RequiredArgsConstructor
public class InternalContactController {

    private final UserContactService userContactService;

    @GetMapping("/{id}")
    public ApiResponse<Contact> getContactById(@PathVariable("id") int id) {
        Contact contact = userContactService.g(id);
        if (contact != null) {
            return ApiResponse.success(contact);
        } else {
            return ApiResponse.error(400, "联系人不存在");
        }
    }
}
```

变更要点：
- 返回类型 `Map<String, Object>` → `ApiResponse<Contact>`
- 移除 `import java.util.Map`
- 移除 `try-catch`，异常由 `GlobalExceptionHandler` 统一处理

- [ ] **Step 2: 验证编译**

```bash
mvn compile -pl contact-service -am
```

预期：BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/controller/internal/InternalContactController.java
git commit -m "refactor(contact-service): InternalContactController 统一ApiResponse响应"
```

---

### Task 9: contact-service — application.yml 补充 MyBatis 日志

**Files:**
- Modify: `contact-service/src/main/resources/application.yml`

- [ ] **Step 1: 追加 MyBatis 日志配置

```yaml
mybatis:
  type-aliases-package: com.gzasc.aishopping.contact.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    default-executor-type: simple
```

变更说明：在 `configuration` 下追加 `log-impl` 和 `default-executor-type`，对齐 auth-service 配置。

- [ ] **Step 2: Commit**

```bash
git add contact-service/src/main/resources/application.yml
git commit -m "chore(contact-service): 补充MyBatis日志配置"
```

---

### Task 10: 数据库迁移

**Files:**
- Modify: 数据库 `eureka_contact`

- [ ] **Step 1: 执行 ALTER TABLE**

```sql
ALTER TABLE user_contact MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '用户ID（关联auth-service的雪花ID）';
```

- [ ] **Step 2: 验证**

```sql
DESC user_contact;
```

预期：`user_id` 类型为 `bigint`

---

### 自检清单

1. **Spec 覆盖度** — spec 中每项变更均有对应 task：响应格式(Task 5-8)、common-api 依赖(Task 2)、userId 类型(Task 3-5)、本地 ApiResponse 删除(Task 2)、全局异常处理(Task 7)、MyBatis 日志(Task 9)、ContactFeignClient(Task 1)、数据库(Task 10)。✅
2. **占位符检查** — 所有代码块均为完整实现，无 TBD/TODO。✅
3. **类型一致性** — Task 3-5 的 userId 均为 `Long`；Task 6 的 shopId 保持 `String`；contact 自身 ID 在 Mapper/Service 中保持 `int`。✅
4. **编译验证** — 每步均有 `mvn compile` 验证命令。✅
