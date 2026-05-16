# 商家端地址信息管理实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为商家端添加地址信息管理功能，支持发货/退货地址的CRUD操作

**Architecture:** 在 contact-service 中新增地址管理模块，使用 MyBatis 注解方式实现 Mapper，遵循现有代码风格

**Tech Stack:** Spring Boot, MyBatis, MySQL

---

## 文件结构

- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/model/ShopAddress.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressMapper.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressRelMapper.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/ShopAddressService.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ShopAddressServiceImpl.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java`
- Modify: `AI-Shopping-backend_Eureka/sql/init/03-contact-init.sql`

---

### Task 1: 创建数据库表 SQL

**Files:**
- Modify: `AI-Shopping-backend_Eureka/sql/init/03-contact-init.sql`

- [ ] **Step 1: 添加地址表 SQL**

在 `03-contact-init.sql` 文件末尾添加：

```sql
-- 商家地址表
CREATE TABLE IF NOT EXISTS shop_address (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    name VARCHAR(100) NOT NULL COMMENT '收货人姓名',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    address VARCHAR(500) NOT NULL COMMENT '完整地址',
    address_type TINYINT NOT NULL DEFAULT 1 COMMENT '地址类型：1-发货地址 2-退货地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认：0-否 1-是',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_address_type (address_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家地址表';

-- 商店地址关联表
CREATE TABLE IF NOT EXISTS shop_address_rel (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    shop_id VARCHAR(16) NOT NULL COMMENT '店铺ID',
    address_id INT NOT NULL COMMENT '地址ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_shop_id (shop_id),
    INDEX idx_address_id (address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商店地址关联表';

SELECT '商家地址表初始化完成' AS message;
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-backend_Eureka/sql/init/03-contact-init.sql
git commit -m "feat(contact): add shop_address and shop_address_rel tables"
```

---

### Task 2: 创建 Model 实体类

**Files:**
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/model/ShopAddress.java`

- [ ] **Step 1: 创建 ShopAddress.java**

```java
package com.gzasc.aishopping.contact.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShopAddress {
    private int id;
    private String name;
    private String phone;
    private String address;
    private int addressType;
    private int isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 提交**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/model/ShopAddress.java
git commit -m "feat(contact): add ShopAddress model"
```

---

### Task 3: 创建 Mapper 接口

**Files:**
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressMapper.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressRelMapper.java`

- [ ] **Step 1: 创建 ShopAddressMapper.java**

```java
package com.gzasc.aishopping.contact.mapper;

import com.gzasc.aishopping.contact.model.ShopAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShopAddressMapper {

    @Insert("INSERT INTO shop_address (name, phone, address, address_type, is_default) " +
            "VALUES (#{name}, #{phone}, #{address}, #{addressType}, #{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAddress(ShopAddress address);

    @Delete("DELETE FROM shop_address WHERE id = #{id}")
    int deleteAddressById(int id);

    @Update("UPDATE shop_address SET name = #{name}, phone = #{phone}, address = #{address}, " +
            "address_type = #{addressType}, is_default = #{isDefault} WHERE id = #{id}")
    int updateAddress(ShopAddress address);

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "addressType", column = "address_type"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ShopAddress selectAddressById(int id);

    @Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
            "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "addressType", column = "address_type"),
            @Result(property = "isDefault", column = "is_default"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    List<ShopAddress> selectAddressesByShopId(String shopId);

    @Update("UPDATE shop_address SET is_default = 0 WHERE address_type = #{addressType} " +
            "AND id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId})")
    int clearDefaultByType(@Param("shopId") String shopId, @Param("addressType") int addressType);

    @Update("UPDATE shop_address SET is_default = 1 WHERE id = #{id}")
    int setDefaultById(int id);
}
```

- [ ] **Step 2: 创建 ShopAddressRelMapper.java**

```java
package com.gzasc.aishopping.contact.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ShopAddressRelMapper {

    @Insert("INSERT INTO shop_address_rel (shop_id, address_id) VALUES (#{shopId}, #{addressId})")
    int insertRel(@Param("shopId") String shopId, @Param("addressId") int addressId);

    @Delete("DELETE FROM shop_address_rel WHERE address_id = #{addressId}")
    int deleteRelByAddressId(int addressId);

    @Select("SELECT shop_id FROM shop_address_rel WHERE address_id = #{addressId}")
    String selectShopIdByAddressId(int addressId);
}
```

- [ ] **Step 3: 提交**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressMapper.java
git add contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressRelMapper.java
git commit -m "feat(contact): add ShopAddressMapper and ShopAddressRelMapper"
```

---

### Task 4: 创建 Service 层

**Files:**
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/ShopAddressService.java`
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ShopAddressServiceImpl.java`

- [ ] **Step 1: 创建 ShopAddressService.java**

```java
package com.gzasc.aishopping.contact.service;

import com.gzasc.aishopping.contact.model.ShopAddress;

import java.util.List;

public interface ShopAddressService {
    int createAddress(ShopAddress address, String shopId);
    int deleteAddress(int id, String shopId);
    int updateAddress(ShopAddress address, String shopId);
    ShopAddress getAddressById(int id, String shopId);
    List<ShopAddress> getAddressesByShopId(String shopId);
    int setDefaultAddress(int id, String shopId);
}
```

- [ ] **Step 2: 创建 ShopAddressServiceImpl.java**

```java
package com.gzasc.aishopping.contact.service.impl;

import com.gzasc.aishopping.contact.mapper.ShopAddressMapper;
import com.gzasc.aishopping.contact.mapper.ShopAddressRelMapper;
import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopAddressServiceImpl implements ShopAddressService {

    private final ShopAddressMapper shopAddressMapper;
    private final ShopAddressRelMapper shopAddressRelMapper;

    @Override
    @Transactional
    public int createAddress(ShopAddress address, String shopId) {
        if (address.getIsDefault() == 1) {
            shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        }
        int rows = shopAddressMapper.insertAddress(address);
        if (rows > 0) {
            shopAddressRelMapper.insertRel(shopId, address.getId());
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteAddress(int id, String shopId) {
        String checkShopId = shopAddressRelMapper.selectShopIdByAddressId(id);
        if (!shopId.equals(checkShopId)) {
            return 0;
        }
        shopAddressRelMapper.deleteRelByAddressId(id);
        return shopAddressMapper.deleteAddressById(id);
    }

    @Override
    @Transactional
    public int updateAddress(ShopAddress address, String shopId) {
        String checkShopId = shopAddressRelMapper.selectShopIdByAddressId(address.getId());
        if (!shopId.equals(checkShopId)) {
            return 0;
        }
        if (address.getIsDefault() == 1) {
            shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        }
        return shopAddressMapper.updateAddress(address);
    }

    @Override
    public ShopAddress getAddressById(int id, String shopId) {
        String checkShopId = shopAddressRelMapper.selectShopIdByAddressId(id);
        if (!shopId.equals(checkShopId)) {
            return null;
        }
        return shopAddressMapper.selectAddressById(id);
    }

    @Override
    public List<ShopAddress> getAddressesByShopId(String shopId) {
        return shopAddressMapper.selectAddressesByShopId(shopId);
    }

    @Override
    @Transactional
    public int setDefaultAddress(int id, String shopId) {
        ShopAddress address = getAddressById(id, shopId);
        if (address == null) {
            return 0;
        }
        shopAddressMapper.clearDefaultByType(shopId, address.getAddressType());
        return shopAddressMapper.setDefaultById(id);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/service/ShopAddressService.java
git add contact-service/src/main/java/com/gzasc/aishopping/contact/service/impl/ShopAddressServiceImpl.java
git commit -m "feat(contact): add ShopAddressService and implementation"
```

---

### Task 5: 创建 Controller 层

**Files:**
- Create: `contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java`

- [ ] **Step 1: 创建 ShopAddressSellerController.java**

```java
package com.gzasc.aishopping.contact.controller;

import com.gzasc.aishopping.contact.model.ShopAddress;
import com.gzasc.aishopping.contact.service.ShopAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/address")
@RequiredArgsConstructor
public class ShopAddressSellerController {

    private final ShopAddressService shopAddressService;

    @GetMapping("/list")
    public Map<String, Object> getAddressList(
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "查询地址错误：未获取到店铺ID");
        }
        try {
            List<ShopAddress> addresses = shopAddressService.getAddressesByShopId(shopId);
            return Map.of("message", "查询成功", "data", addresses, "total", addresses.size());
        } catch (Exception e) {
            return Map.of("message", "查询地址列表错误：" + e.getMessage());
        }
    }

    @PostMapping("/add")
    public Map<String, Object> addAddress(
            @RequestBody ShopAddress address,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "新增地址错误：未获取到店铺ID");
        }
        if (address.getName() == null || address.getPhone() == null || address.getAddress() == null) {
            return Map.of("message", "新增地址错误：信息不完整");
        }
        if (address.getAddressType() != 1 && address.getAddressType() != 2) {
            address.setAddressType(1);
        }
        if (address.getIsDefault() != 1) {
            address.setIsDefault(0);
        }
        try {
            int rows = shopAddressService.createAddress(address, shopId);
            if (rows > 0) {
                return Map.of("message", "新增成功", "data", address);
            } else {
                return Map.of("message", "新增失败");
            }
        } catch (Exception e) {
            return Map.of("message", "新增地址错误：" + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateAddress(
            @PathVariable("id") int id,
            @RequestBody ShopAddress address,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "修改地址错误：未获取到店铺ID");
        }
        address.setId(id);
        if (address.getName() == null || address.getPhone() == null || address.getAddress() == null) {
            return Map.of("message", "修改地址错误：信息不完整");
        }
        try {
            int rows = shopAddressService.updateAddress(address, shopId);
            if (rows > 0) {
                return Map.of("message", "修改成功");
            } else {
                return Map.of("message", "修改失败：地址不存在或不属于该店铺");
            }
        } catch (Exception e) {
            return Map.of("message", "修改地址错误：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "删除地址错误：未获取到店铺ID");
        }
        try {
            int rows = shopAddressService.deleteAddress(id, shopId);
            if (rows > 0) {
                return Map.of("message", "删除成功");
            } else {
                return Map.of("message", "删除失败：地址不存在或不属于该店铺");
            }
        } catch (Exception e) {
            return Map.of("message", "删除地址错误：" + e.getMessage());
        }
    }

    @PutMapping("/set-default/{id}")
    public Map<String, Object> setDefaultAddress(
            @PathVariable("id") int id,
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
        if (shopId == null || shopId.trim().isEmpty()) {
            return Map.of("message", "设置默认地址错误：未获取到店铺ID");
        }
        try {
            int rows = shopAddressService.setDefaultAddress(id, shopId);
            if (rows > 0) {
                return Map.of("message", "设置成功");
            } else {
                return Map.of("message", "设置失败：地址不存在或不属于该店铺");
            }
        } catch (Exception e) {
            return Map.of("message", "设置默认地址错误：" + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java
git commit -m "feat(contact): add ShopAddressSellerController for seller address management"
```

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-05-16-shop-address.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - 调度子代理逐任务执行，快速迭代

**2. Inline Execution** - 在当前会话中执行任务，使用 executing-plans

**Which approach?**