# 发货地址专用API实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增后端API仅返回发货地址(addressType=1)，前端发货弹窗调用新API，并将"选择发货联系人"文案改为"选择发货信息"

**Architecture:** 后端在contact-service新增发货地址查询接口，前端新增API并修改发货弹窗调用

**Tech Stack:** Java Spring Boot, Vue.js

---

### Task 1: 后端Mapper新增发货地址查询方法

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/mapper/ShopAddressMapper.java`

- [ ] **Step 1: 新增selectShipAddressesByShopId方法**

在 `ShopAddressMapper.java` 第49行后添加新方法：

```java
@Select("SELECT id, name, phone, address, address_type, is_default, created_at, updated_at " +
        "FROM shop_address WHERE id IN (SELECT address_id FROM shop_address_rel WHERE shop_id = #{shopId}) " +
        "AND address_type = 1")
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
List<ShopAddress> selectShipAddressesByShopId(String shopId);
```

- [ ] **Step 2: 提交代码**

---

### Task 2: 后端Service层新增方法

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/service/ShopAddressService.java:12`

- [ ] **Step 1: 在接口中添加方法声明**

在 `ShopAddressService.java` 第12行后添加：

```java
List<ShopAddress> getShipAddressesByShopId(String shopId);
```

- [ ] **Step 2: 在实现类中添加实现**

修改 `ShopAddressServiceImpl.java` 第67行后添加：

```java
@Override
public List<ShopAddress> getShipAddressesByShopId(String shopId) {
    return shopAddressMapper.selectShipAddressesByShopId(shopId);
}
```

- [ ] **Step 3: 提交代码**

---

### Task 3: 后端Controller新增API端点

**Files:**
- Modify: `AI-Shopping-backend_Eureka/contact-service/src/main/java/com/gzasc/aishopping/contact/controller/ShopAddressSellerController.java:18`

- [ ] **Step 1: 添加新接口**

在 `ShopAddressSellerController.java` 第30行后添加：

```java
@GetMapping("/ship-list")
public Map<String, Object> getShipAddressList(
        @RequestHeader(value = "X-Shop-Id", required = false) String shopId) {
    if (shopId == null || shopId.trim().isEmpty()) {
        return Map.of("message", "查询发货地址错误：未获取到店铺ID");
    }
    try {
        List<ShopAddress> addresses = shopAddressService.getShipAddressesByShopId(shopId);
        return Map.of("message", "查询成功", "data", addresses, "total", addresses.size());
    } catch (Exception e) {
        return Map.of("message", "查询发货地址列表错误：" + e.getMessage());
    }
}
```

需要添加 import:
```java
import java.util.List;
import java.util.Map;
```

- [ ] **Step 2: 提交代码**

---

### Task 4: 前端新增API方法

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/api/contact.js:36`

- [ ] **Step 1: 添加getShipAddressList方法**

在 `contact.js` 第37行后添加：

```javascript
// 获取发货地址列表（仅发货地址，不含退货地址）
export const getShipAddressList = (shopId) =>
    request.get('/api/seller/address/ship-list', { headers: { 'X-Shop-Id': shopId } })
```

- [ ] **Step 2: 提交代码**

---

### Task 5: 前端修改发货弹窗API调用

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/merchant/MerchantShip/useMerchantShip.js:8`

- [ ] **Step 1: 修改import**

在 `useMerchantShip.js` 第8行，将：
```javascript
import { getAllContacts, getContactById, getAddressList } from '../../api/contact.js'
```

改为：
```javascript
import { getAllContacts, getContactById, getShipAddressList } from '../../api/contact.js'
```

- [ ] **Step 2: 修改loadContacts方法**

将第135行的 `getAddressList` 改为 `getShipAddressList`：

```javascript
const res = await getShipAddressList(currentShopId.value)
```

- [ ] **Step 3: 提交代码**

---

### Task 6: 前端修改文案

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/merchant/MerchantShip/MerchantShip.vue:224`

- [ ] **Step 1: 修改标签文案**

在 `MerchantShip.vue` 第224行，将：
```html
<label>选择发货联系人 <span class="required">*</span></label>
```

改为：
```html
<label>选择发货信息 <span class="required">*</span></label>
```

- [ ] **Step 2: 修改提示文案**

第244行，将：
```html
共 {{ contacts.length }} 个发货地址可用
```

改为：
```html
共 {{ contacts.length }} 个发货信息可用
```

- [ ] **Step 3: 提交代码**

---

**执行方式选择：**
1. Subagent-Driven (推荐) - 每个任务由独立子代理执行，快速迭代
2. Inline Execution - 在当前会话中执行任务

请选择执行方式。