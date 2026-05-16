# 商家端地址信息管理设计

## 概述

为商家端添加地址信息管理功能，支持发货/退货地址的CRUD操作。

## 数据库设计

### 表1: shop_address（地址信息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK, AUTO_INCREMENT) | 地址ID |
| name | VARCHAR(100) NOT NULL | 收货人姓名 |
| phone | VARCHAR(20) NOT NULL | 联系电话 |
| address | VARCHAR(500) NOT NULL | 完整地址 |
| address_type | TINYINT NOT NULL | 地址类型：1-发货地址 2-退货地址 |
| is_default | TINYINT DEFAULT 0 | 是否默认：0-否 1-是 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### 表2: shop_address_rel（商店地址关联表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK, AUTO_INCREMENT) | ID |
| shop_id | VARCHAR(16) NOT NULL | 店铺ID |
| address_id | INT NOT NULL | 地址ID |
| created_at | TIMESTAMP | 创建时间 |
| INDEX idx_shop_id (shop_id) | | |
| INDEX idx_address_id (address_id) | | |

## 后端设计

### 新增文件

1. **Model**: `ShopAddress.java`
2. **Mapper**: `ShopAddressMapper.java`, `ShopAddressRelMapper.java`
3. **Service**: `ShopAddressService.java` + `ShopAddressServiceImpl.java`
4. **Controller**: `ShopAddressSellerController.java`

### API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/seller/address/list | 获取店铺地址列表 |
| POST | /api/seller/address/add | 新增地址 |
| PUT | /api/seller/address/update/{id} | 修改地址 |
| DELETE | /api/seller/address/delete/{id} | 删除地址 |
| PUT | /api/seller/address/set-default/{id} | 设置默认地址 |

## 实现顺序

1. 创建数据库表 SQL 脚本
2. 创建 Model 实体类
3. 创建 Mapper 接口和 XML
4. 创建 Service 层
5. 创建 Controller 层