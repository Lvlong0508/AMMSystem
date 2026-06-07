# ShopInfo 数据库自增 ID 实现计划

> **Goal:** 将 shop_info 表的 ID 生成从 SafeIdGenerator 切换为数据库自增

**改动范围：** 3 个文件
- sql/init/shop-init.sql — DDL 加 AUTO_INCREMENT
- ShopInfoMapper.java — INSERT 改用 useGeneratedKeys
- ShopServiceImpl.java — 移除 SafeIdGenerator 调用

---

### Task 1: shop-init.sql DDL 修改

**Modify:** sql/init/shop-init.sql

- [ ] shop_info 表 DDL 中 id BIGINT PRIMARY KEY 改为 id BIGINT AUTO_INCREMENT PRIMARY KEY

### Task 2: ShopInfoMapper 修改

**Modify:** shop-service/.../mapper/ShopInfoMapper.java

- [ ] INSERT 去掉 id 列和 #{id} 值
- [ ] 添加 @Options(useGeneratedKeys = true, keyProperty = "id")

### Task 3: ShopServiceImpl 修改

**Modify:** shop-service/.../service/impl/ShopServiceImpl.java

- [ ] 移除 shopInfo.setId(SafeIdGenerator.nextId()) 这一行

### Task 4: 编译验证

- [ ] Maven 编译 shop-service 模块验证无错误
