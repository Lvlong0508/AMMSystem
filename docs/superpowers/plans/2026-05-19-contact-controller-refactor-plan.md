# contact-service Controller 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 将 contact-service 的 Controller 层简化为两个（UserContactController + MerchantContactController）

**架构：** 
- UserContactController：用户端联系人 CRUD，基于 X-User-Id 隔离
- MerchantContactController：商户端地址管理，基于 X-Shop-Id 隔离
- 统一使用 ApiResponse 和 DTO 模式

**技术栈：** Spring Boot + MyBatis + Lombok

---

## 文件结构

```
contact-service/src/main/java/com/gzasc/aishopping/contact/
├── controller/
│   ├── UserContactController.java        # 重命名自 ContactController
│   ├── MerchantContactController.java   # 重命名自 ShopAddressSellerController
│   ├── InternalContactController.java   # 保留，内部接口
│   └── GlobalExceptionHandler.java      # 保留
├── model/
│   ├── Contact.java                     # 修改：增加 isDefault 字段
│   ├── ShopAddress.java                 # 保留
│   ├── UserContact.java                 # 保留
│   └── dto/
│       ├── ApiResponse.java             # 已存在
│       ├── CreateContactRequest.java    # 已存在
│       ├── UpdateContactRequest.java    # 已存在
│       ├── ContactResponse.java         # 修改：增加 isDefault 字段
│       ├── CreateAddressRequest.java    # 新增
│       ├── UpdateAddressRequest.java    # 新增
│       └── AddressResponse.java         # 新增
├── mapper/
│   ├── ContactMapper.java               # 修改：增加字段映射
│   └── ...
└── service/
    ├── ContactService.java              # 修改：增加 setDefaultContact 方法
    ├── ContactServiceImpl.java          # 修改：实现 setDefaultContact
    └── ...
```

---

## Task 1: Contact.java 增加 isDefault 字段

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/Contact.java`

- [ ] **Step 1: 添加 isDefault 字段**

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
    private Integer id;              // 联系人ID

    @NotBlank(message = "姓名为空")
    private String name;             // 联系人姓名

    @NotBlank(message = "电话为空")
    private String phone;            // 联系电话

    @NotBlank(message = "地址为空")
    private String address;          // 联系地址

    private Integer isDefault;       // 是否默认联系人：0-否 1-是
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/Contact.java
git commit -m "feat(contact): add isDefault field to Contact"
```

---

## Task 2: ContactMapper.java 增加字段映射

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ContactMapper.java`

- [ ] **Step 1: 更新 @Results 映射和 SQL**

```java
@Results(id = "CONTACT_RESULT_MAPPING", value = {
        @Result(property = "id", column = "id"),
        @Result(property = "name", column = "name"),
        @Result(property = "phone", column = "phone"),
        @Result(property = "address", column = "address"),
        @Result(property = "isDefault", column = "is_default"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at")
})
```

- [ ] **Step 2: 更新 Insert 和 Update SQL**

```java
@Insert("INSERT INTO t_contact (name, phone, address, is_default) " +
        "VALUES (#{name}, #{phone}, #{address}, #{isDefault})")
@Options(useGeneratedKeys = true, keyProperty = "id")
int insertContact(Contact contact);

@Update("UPDATE t_contact SET name = #{name}, phone = #{phone}, address = #{address}, " +
        "is_default = #{isDefault} WHERE id = #{id}")
int updateContact(Contact contact);

@Update("UPDATE t_contact SET is_default = 0 WHERE user_id = #{userId}")
int clearDefaultByUserId(int userId);

@Update("UPDATE t_contact SET is_default = 1 WHERE id = #{id}")
int setDefaultById(int id);
```

- [ ] **Step 3: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ContactMapper.java
git commit -m "feat(contact): add isDefault mapping to ContactMapper"
```

---

## Task 3: ContactService.java 增加 setDefaultContact 方法

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/ContactService.java`

- [ ] **Step 1: 添加方法声明**

```java
/**
 * 设置默认联系人
 * @param id 联系人ID
 * @param userId 用户ID
 * @return 影响行数
 */
int setDefaultContact(int id, int userId);
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/ContactService.java
git commit -m "feat(contact): add setDefaultContact to ContactService"
```

---

## Task 4: ContactServiceImpl.java 实现 setDefaultContact

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ContactServiceImpl.java`

- [ ] **Step 1: 实现方法**

```java
@Override
public int setDefaultContact(int id, int userId) {
    log.info("setDefaultContact, id={}, userId={}", id, userId);
    Contact contact = getContactById(id, userId);
    if (contact == null) {
        return 0;
    }
    contactMapper.clearDefaultByUserId(userId);
    return contactMapper.setDefaultById(id);
}
```

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ContactServiceImpl.java
git commit -m "feat(contact): implement setDefaultContact"
```

---

## Task 5: ContactResponse.java 增加 isDefault 字段

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/ContactResponse.java`

- [ ] **Step 1: 更新 DTO**

```java
@Data
public class ContactResponse {
    private Integer id;
    private String name;
    private String phone;
    private String address;
    private Integer isDefault;       // 是否默认：0-否 1-是
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
        response.setIsDefault(contact.getIsDefault());
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
git commit -m "feat(contact): add isDefault to ContactResponse"
```

---

## Task 6: 创建商户地址相关 DTO

**Files:**
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/CreateAddressRequest.java`
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/UpdateAddressRequest.java`
- Create: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/AddressResponse.java`

- [ ] **Step 1: 创建 CreateAddressRequest.java**

```java
package com.gzasc.aishopping.contact.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建地址请求 DTO
 */
@Data
public class CreateAddressRequest {
    @NotBlank(message = "收货人不能为空")
    private String name;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "地址不能为空")
    private String address;

    @NotNull(message = "地址类型不能为空")
    private Integer addressType;    // 1-发货地址 2-退货地址

    private Integer isDefault;      // 0-否 1-是
}
```

- [ ] **Step 2: 创建 UpdateAddressRequest.java**

```java
package com.gzasc.aishopping.contact.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新地址请求 DTO
 */
@Data
public class UpdateAddressRequest {
    @NotNull(message = "ID不能为空")
    private Integer id;

    @NotBlank(message = "收货人不能为空")
    private String name;

    @NotBlank(message = "电话不能为空")
    private String phone;

    @NotBlank(message = "地址不能为空")
    private String address;

    @NotNull(message = "地址类型不能为空")
    private Integer addressType;

    private Integer isDefault;
}
```

- [ ] **Step 3: 创建 AddressResponse.java**

```java
package com.gzasc.aishopping.contact.model.dto;

import com.gzasc.aishopping.contact.model.ShopAddress;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 地址响应 DTO
 */
@Data
public class AddressResponse {
    private Integer id;
    private String name;
    private String phone;
    private String address;
    private Integer addressType;
    private Integer isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AddressResponse fromShopAddress(ShopAddress shopAddress) {
        if (shopAddress == null) {
            return null;
        }
        AddressResponse response = new AddressResponse();
        response.setId(shopAddress.getId());
        response.setName(shopAddress.getName());
        response.setPhone(shopAddress.getPhone());
        response.setAddress(shopAddress.getAddress());
        response.setAddressType(shopAddress.getAddressType());
        response.setIsDefault(shopAddress.getIsDefault());
        response.setCreatedAt(shopAddress.getCreatedAt());
        response.setUpdatedAt(shopAddress.getUpdatedAt());
        return response;
    }
}
```

- [ ] **Step 4: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/CreateAddressRequest.java
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/UpdateAddressRequest.java
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/model/dto/AddressResponse.java
git commit -m "feat(contact): add merchant address DTOs"
```

---

## Task 7: 重命名 ContactController.java → UserContactController.java

**Files:**
- Rename: `ContactController.java` → `UserContactController.java`
- Modify: 修改 @RequestMapping 为 `/api/user/contact`
- Modify: 添加 set-default 接口

- [ ] **Step 1: 重命名并更新类名和 RequestMapping**

```java
@RestController
@RequestMapping("/api/user/contact")
@RequiredArgsConstructor
public class UserContactController {
    // 保持所有现有方法不变
}
```

- [ ] **Step 2: 添加 set-default 接口**

```java
@PutMapping("/set-default/{id}")
public ApiResponse<Void> setDefaultContact(
        @PathVariable int id,
        @RequestHeader(value = "X-User-Id", required = false) String userIdStr) {
    Integer userId = parseUserId(userIdStr);
    if (userId == null) {
        throw new ContactException("设置默认联系人错误：未登录");
    }
    int result = contactService.setDefaultContact(id, userId);
    if (result <= 0) {
        throw new ContactException("设置失败：联系人不存在或不属于该用户");
    }
    return ApiResponse.success("设置成功");
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/UserContactController.java
git commit -m "refactor(contact): rename ContactController to UserContactController and add set-default"
```

---

## Task 8: 删除 ContactSellerController.java

**Files:**
- Delete: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ContactSellerController.java`

- [ ] **Step 1: 删除文件**

Run: `rm AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ContactSellerController.java`

- [ ] **Step 2: 提交**

```bash
git rm AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ContactSellerController.java
git commit -m "refactor(contact): remove ContactSellerController"
```

---

## Task 9: 重命名并重构 ShopAddressSellerController.java → MerchantContactController.java

**Files:**
- Rename: `ShopAddressSellerController.java` → `MerchantContactController.java`
- Modify: 使用 ApiResponse + DTO

- [ ] **Step 1: 创建 MerchantContactController.java**

```java
package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.model.dto.*;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import com.gzasc.aishopping.contact.service.impl.ContactException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/merchant/address")
@RequiredArgsConstructor
public class MerchantContactController {

    private final ShopAddressService shopAddressService;

    @PostMapping("/create")
    public ApiResponse<AddressResponse> createAddress(
            @RequestBody @Valid CreateAddressRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("新增地址错误：未获取到店铺ID");
        }
        ShopAddress address = toShopAddress(request);
        shopAddressService.createAddress(address, shopId);
        return ApiResponse.success("新增成功", AddressResponse.fromShopAddress(address));
    }

    @PutMapping("/update/{id}")
    public ApiResponse<Void> updateAddress(
            @PathVariable("id") int id,
            @RequestBody @Valid UpdateAddressRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("修改地址错误：未获取到店铺ID");
        }
        ShopAddress address = toShopAddress(request);
        address.setId(id);
        int rows = shopAddressService.updateAddress(address, shopId);
        if (rows > 0) {
            return ApiResponse.success("修改成功");
        }
        throw new ContactException("修改失败：地址不存在或不属于该店铺");
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<Void> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("删除地址错误：未获取到店铺ID");
        }
        int rows = shopAddressService.deleteAddress(id, shopId);
        if (rows > 0) {
            return ApiResponse.success("删除成功");
        }
        throw new ContactException("删除失败：地址不存在或不属于该店铺");
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("查询地址错误：未获取到店铺ID");
        }
        List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
        List<AddressResponse> data = addresses.stream()
                .map(AddressResponse::fromShopAddress)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("addresses", data, "total", data.size()));
    }

    @GetMapping("/ship-list")
    public ApiResponse<Map<String, Object>> getShipAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("查询发货地址错误：未获取到店铺ID");
        }
        List<ShopAddress> addresses = shopAddressService.getShipAddressesByShopId(shopId);
        List<AddressResponse> data = addresses.stream()
                .map(AddressResponse::fromShopAddress)
                .collect(Collectors.toList());
        return ApiResponse.success("查询成功", Map.of("addresses", data, "total", data.size()));
    }

    @PutMapping("/set-default/{id}")
    public ApiResponse<Void> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            throw new ContactException("设置默认地址错误：未获取到店铺ID");
        }
        int rows = shopAddressService.setDefaultAddress(id, shopId);
        if (rows > 0) {
            return ApiResponse.success("设置成功");
        }
        throw new ContactException("设置失败：地址不存在或不属于该店铺");
    }

    private ShopAddress toShopAddress(CreateAddressRequest request) {
        ShopAddress address = new ShopAddress();
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setAddressType(request.getAddressType() != null ? request.getAddressType() : 1);
        address.setIsDefault(request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
        return address;
    }

    private ShopAddress toShopAddress(UpdateAddressRequest request) {
        ShopAddress address = new ShopAddress();
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());
        address.setAddressType(request.getAddressType());
        address.setIsDefault(request.getIsDefault() != null && request.getIsDefault() == 1 ? 1 : 0);
        return address;
    }
}
```

- [ ] **Step 2: 删除旧文件 ShopAddressSellerController.java**

Run: `rm AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java`

- [ ] **Step 3: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/MerchantContactController.java
git rm AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java
git commit -m "refactor(contact): rename ShopAddressSellerController to MerchantContactController with DTO"
```

---

## Task 10: 最终验证

**Files:**
- 无文件变更

- [ ] **Step 1: 验证编译通过**

Run: `cd AI-Shopping-backend_Eureka/contact-service && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 检查 Controller 结构**

确认 controller 目录下只有：
- UserContactController.java
- MerchantContactController.java
- InternalContactController.java
- GlobalExceptionHandler.java

- [ ] **Step 3: 提交**

```bash
git commit -m "chore(contact): final verification"
```

---

## 验收检查

- [ ] Contact.java 有 isDefault 字段
- [ ] ContactMapper.java 支持 isDefault 字段
- [ ] ContactService.java 有 setDefaultContact 方法
- [ ] ContactResponse.java 有 isDefault 字段
- [ ] 创建了商户地址 DTO（CreateAddressRequest, UpdateAddressRequest, AddressResponse）
- [ ] ContactController.java 重命名为 UserContactController.java
- [ ] ContactSellerController.java 已删除
- [ ] ShopAddressSellerController.java 重命名为 MerchantContactController.java，使用 DTO + ApiResponse
- [ ] 编译通过

---

## 自检清单

1. **Spec 覆盖**：所有需求都有对应 Task ✓
2. **占位符扫描**：无 TBD/TODO ✓
3. **类型一致性**：方法签名和字段名一致 ✓