# Shop 服务 Controller 层重构设计

## 1. 概述

本文档描述 shop 服务 Controller 层的重构设计方案，将现有的单一 `ShopSellerController` 拆分为多个职责明确的 Controller，并按查询/管理路径分离以便于网关权限控制。

**约束**: 只修改 Controller 层，复用现有 Service、Mapper 等。

## 2. 角色定义

| 角色 | role值 | 权限 |
|------|--------|------|
| 店长 | 1 | 全部权限 |
| 店员 | 2 | 查询订单、发货 |

## 3. 用户端接口 (ShopUserController)

**路径前缀**: `/api/user/shop`

| 接口 | 方法 | 功能 | 权限 |
|------|------|------|------|
| `/list` | GET | 店铺列表（分页） | 需登录 |
| `/{shopId}` | GET | 店铺详情 | 需登录 |
| `/{shopId}/products` | GET | 店铺商品列表 | 需登录 |

## 4. 商家端接口 - 查询类 (ShopQueryController)

**路径前缀**: `/api/seller/shop/query`

| 接口 | 方法 | 功能 | 返回数据 | 权限 |
|------|------|------|----------|------|
| `/shop/{shopId}` | GET | 店铺信息 | 店铺详情 | 店铺成员 |
| `/{shopId}/products` | GET | 商品列表 | 商品信息 | 店铺成员 |
| `/{shopId}/orders` | GET | 订单列表 | 订单+商品+收货地址+物流 | 店铺成员 |
| `/{shopId}/orders/{orderId}` | GET | 订单详情 | 订单+商品+收货地址+物流 | 店铺成员 |
| `/{shopId}/employees` | GET | 员工列表 | 员工信息 | 店铺成员 |
| `/{shopId}/addresses` | GET | 地址列表 | 全部地址 | 店铺成员 |
| `/{shopId}/addresses/shipping` | GET | 发货地址 | 发货地址 | 店铺成员 |
| `/{shopId}/addresses/return` | GET | 退货地址 | 退货地址 | 店铺成员 |

### 4.1 订单列表返回数据格式

```json
{
  "orders": [
    {
      "orderId": "xxx",
      "productId": "xxx",
      "productName": "商品名称",
      "quantity": 1,
      "totalPrice": 100.00,
      "orderStatus": "PAID",
      "orderDate": "2026-05-17T10:00:00",
      "contact": {
        "name": "收货人",
        "phone": "电话",
        "address": "地址"
      },
      "logistics": {
        "id": 1,
        "trackingNumber": "单号",
        "shippingDate": "发货时间"
      }
    }
  ]
}
```

## 5. 商家端接口 - 管理类 (ShopManageController)

**路径前缀**: `/api/seller/shop/manage`

| 接口 | 方法 | 功能 | 权限 |
|------|------|------|------|
| `/shop/register` | POST | 创建店铺 | 店长 |
| `/shop/{shopId}` | PUT | 更新店铺 | 店长 |
| `/shop/{shopId}` | DELETE | 关闭店铺 | 店长 |
| `/{shopId}/products` | POST | 添加商品 | 店长 |
| `/{shopId}/products/{productId}` | PUT | 更新商品 | 店长 |
| `/{shopId}/products/{productId}` | DELETE | 删除商品 | 店长 |
| `/{shopId}/addresses` | POST | 添加地址 | 店长 |
| `/{shopId}/addresses/{addressId}` | PUT | 更新地址 | 店长 |
| `/{shopId}/addresses/{addressId}` | DELETE | 删除地址 | 店长 |
| `/{shopId}/orders/ship` | POST | 发货 | 店员+店长 |
| `/{shopId}/employees/register` | POST | 添加员工 | 店长 |
| `/{shopId}/employees/{merchantId}` | DELETE | 移除员工 | 店长 |

## 6. 网关权限配置

### 6.1 路径匹配规则

| 路径模式 | 权限要求 |
|----------|----------|
| `/api/user/**` | 需登录 |
| `/api/seller/shop/query/**` | 需登录 (店员/店长) |
| `/api/seller/shop/manage/**` | 需店长 (role=1) |
| `/api/seller/shop/manage/**/ship` | 需登录 (店员/店长) |

### 6.2 特殊处理

发货接口 (`/manage/**/ship`) 需要允许店员访问，但其他 manage 接口仅限店长。

## 7. 实现计划

1. 创建 ShopUserController (用户端)
2. 创建 ShopQueryController (商家端查询)
3. 创建 ShopManageController (商家端管理)
4. 修改网关权限配置
5. 更新前端 API 调用

## 8. 向后兼容

重构期间保持原有接口兼容，逐步迁移。