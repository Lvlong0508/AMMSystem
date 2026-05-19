# UserContactMapper 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**目标：** 重构 ContactMapper → UserContactMapper，删除冗余类

**架构：** 将用户-联系人关联逻辑集中到 UserContactMapper，返回简单类型

---

## Task 1: 重命名 ContactMapper.java → UserContactMapper.java

**Files:**
- Rename: `ContactMapper.java` → `UserContactMapper.java`
- Update: package 和类名

- [ ] **Step 1: 重命名文件**
- [ ] **Step 2: 更新 package 声明**
- [ ] **Step 3: 验证编译通过**
- [ ] **Step 4: 提交**

---

## Task 2: 重构 UserContactMapper.java 方法

**Files:**
- Modify: `UserContactMapper.java`

- [ ] **Step 1: 按以下结构重构**

```java
package com.gzasc.aishopping.contact.mapper;

/**
 * 用户-联系人关联 Mapper 接口
 * 对应 user_contact 表，处理用户与联系人的关联关系
 */
@Mapper
public interface UserContactMapper {

    /* ========== 查询操作 ========== */

    /**
     * 根据用户ID查询关联的联系人的ID列表
     * @param userId 用户ID
     * @return 联系人ID列表
     */
    @Select("SELECT contact_id FROM user_contact WHERE user_id = #{userId}")
    List<Integer> selectContactIdsByUserId(int userId);

    /**
     * 根据联系人ID查询关联的用户ID列表
     * @param contactId 联系人ID
     * @return 用户ID列表
     */
    @Select("SELECT user_id FROM user_contact WHERE contact_id = #{contactId}")
    List<Integer> selectUserIdsByContactId(int contactId);

    /* ========== 插入操作 ========== */

    /**
     * 插入用户-联系人关联记录
     * @param userId 用户ID
     * @param contactId 联系人ID
     * @return 影响行数
     */
    @Insert("INSERT INTO user_contact (user_id, contact_id) VALUES (#{userId}, #{contactId})")
    int insertUserContact(@Param("userId") int userId, @Param("contactId") int contactId);

    /* ========== 删除操作 ========== */

    /**
     * 删除指定用户-联系人关联
     * @param userId 用户ID
     * @param contactId 联系人ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_contact WHERE user_id = #{userId} AND contact_id = #{contactId}")
    int deleteByUserIdAndContactId(@Param("userId") int userId, @Param("contactId") int contactId);

    /**
     * 根据联系人ID删除所有关联记录
     * @param contactId 联系人ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_contact WHERE contact_id = #{contactId}")
    int deleteByContactId(int contactId);
}
```

- [ ] **Step 2: 验证编译通过**
- [ ] **Step 3: 提交**

---

## Task 3: 更新 ContactServiceImpl.java

**Files:**
- Modify: `ContactServiceImpl.java`

- [ ] **Step 1: 读取现有代码并更新**

主要变更：
1. 移除 `UserContact` 的 import 和使用
2. 调用方法改为新方法名：
   - `userContactMapper.selectByUserId()` → `userContactMapper.selectContactIdsByUserId()`
   - `userContactMapper.selectByContactId()` → `userContactMapper.selectUserIdsByContactId()`
   - `userContactMapper.insert()` → `userContactMapper.insertUserContact()`
   - `userContactMapper.deleteByContactId()` → `userContactMapper.deleteByContactId()`
3. 权限校验逻辑调整（遍历 ID 列表而非对象列表）

- [ ] **Step 2: 验证编译通过**
- [ ] **Step 3: 提交**

---

## Task 4: 删除 UserContact.java 和原 UserContactMapper.java

**Files:**
- Delete: `UserContact.java`
- Delete: 原 `UserContactMapper.java`（如果存在）

- [ ] **Step 1: 删除文件**
- [ ] **Step 2: 验证编译通过**
- [ ] **Step 3: 提交**

---

## Task 5: 最终验证

- [ ] **Step 1: 验证编译通过**
- [ ] **Step 2: 检查代码结构**
- [ ] **Step 3: 提交**

---

## 验收清单

- [ ] `ContactMapper.java` 已重命名为 `UserContactMapper.java`
- [ ] 新 `UserContactMapper` 方法返回 `List<Integer>`
- [ ] `UserContact.java` 已删除
- [ ] 原 `UserContactMapper.java` 已删除
- [ ] `ContactServiceImpl` 正确调用新方法
- [ ] 编译通过