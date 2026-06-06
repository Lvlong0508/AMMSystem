# 商家端卡片式重构设计方案

## 概述

对商家端 3 个核心管理页面进行卡片式可视化重构，将 ProductCard 和 OrderCard 改为 `variant` prop 模式（abstract/detail），并提升员工管理和商店信息的视觉展示。同时补充缺失的按店铺查询商品列表 API 接口。

## 涉及范围

| 模块 | 文件 | 改动类型 |
|------|------|----------|
| ProductCard | `components/ProductCard/` | 重构 |
| OrderCard | `components/OrderCard/` | 重构 |
| ShopProducts | `views/ShopProducts/` | 重构 |
| ShopOrders | `views/ShopOrders/` | 重构 |
| ShopEmployees | `views/ShopEmployees/` | 重构 |
| ShopInfo | `views/ShopInfo/` | 本次不涉及 |

## 1. ProductCard 重构

### Props

```ts
{
  product: Object,   // required
  variant: String,   // 'abstract' | 'detail', default 'abstract'
}
```

### 字段映射

| variant | 展示字段 |
|---------|---------|
| `abstract` | `imageUrl`(80×80缩略图), `name`, `price`, `isSale`(状态标签), `stock` |
| `detail` | 大图(240×240) + `name`, `price`, `tags`, `description`, `stock`, `isSale`, `createdAt`, `updatedAt` + 操作按钮(编辑/上架下架/删除) |

### 事件

| 事件 | 触发时机 |
|------|---------|
| `click` | 点击 abstract 卡片 |
| `edit` | 点击编辑按钮 |
| `toggle-sale` | 点击上架/下架按钮 |
| `delete` | 点击删除按钮 |

### 视觉风格

- 使用 Element Plus `el-card`，保持现有商家端设计语言
- abstract: 横排布局，左侧图片右侧信息，hover 阴影
- detail: 左右分栏布局（图片列+信息列），操作按钮在底部
- 颜色、间距、字体沿用 `styles/variables.css` 的 CSS 变量

## 2. OrderCard 重构

### Props

```ts
{
  order: Object,    // required
  variant: String,  // 'abstract' | 'detail', default 'abstract'
}
```

### 字段映射

| variant | 展示字段 |
|---------|---------|
| `abstract` | `orderId`, `productId`, `quantity`, `orderStatus`(状态标签) |
| `detail` | 全部字段: `orderId`, `productId`, `quantity`, `orderStatus`, `totalPrice`, `orderDate`, `contactName`, `contactPhone`, `contactAddress`, `trackingNumber` + 发货按钮 |

### 事件

| 事件 | 触发时机 |
|------|---------|
| `click` | 点击 abstract 卡片 |
| `ship` | 点击发货按钮(detail 中) |

### 视觉风格

- abstract: 横排布局，左侧订单概要右侧状态标签，hover 阴影
- detail: 分区块展示（订单信息、收货信息、物流信息），底部操作按钮
- 状态标签使用 Element Plus `el-tag` 彩色标签

## 3. ShopProducts 页面改造

- abstract ProductCard 网格排列 (`grid-template-columns: repeat(auto-fill, minmax(380px, 1fr))`)
- 点击卡片 → `el-dialog` 内展示 detail ProductCard
- 顶部工具栏：页面标题 + 刷新按钮 + 添加商品按钮
- 搜索框过滤商品名称
- 添加/编辑商品仍使用表单弹窗（与现有逻辑一致）

## 4. ShopOrders 页面改造

- abstract OrderCard 列表排列（纵向列表）
- 点击卡片 → `el-dialog` 内展示 detail OrderCard
- 顶部工具栏：页面标题 + 刷新按钮
- 筛选栏：状态筛选 + 订单号/商品搜索
- 待发货订单在 detail 中显示"发货"按钮

## 5. ShopEmployees 页面改造

- 员工卡片网格排列，每张卡片展示：
  - 头像（首字母 avatar）
  - 姓名、用户名、电话
  - 角色标签
  - 移除按钮
- 顶部工具栏：标题 + 刷新 + 添加店员按钮
- 添加店员使用表单弹窗（与现有逻辑一致）

## 6. 新增 API: `getProductsByShop`

`product.js` 缺少按店铺查询商品列表的接口，当前 `ShopProducts.js` 的 `loadProducts` 调用有误，需新增：

```js
/**
 * 查询指定店铺的商品列表
 *
 * @param {string|number} shopId - 店铺ID
 * @returns {Promise<Array<{id: number, name: string, price: number, tags: string, description: string, stock: number, isSale: boolean, imageId: number, imageUrl: string, createdAt: string, updatedAt: string}>>}
 *
 * @example
 * // 请求
 * getProductsByShop(1)
 * // 响应
 * // [{ id: 1, name: "商品名称", price: 99.99, isSale: true, ... }]
 */
export const getProductsByShop = (shopId) =>
  request.get(`${PRODUCT_BASE}/shop/${shopId}`)
```

## 数据结构

### Product 对象

```ts
{
  id: number,
  name: string,
  price: number,
  tags: string,
  description: string,
  stock: number,
  isSale: boolean,
  imageId: number,
  imageUrl: string,
  createdAt: string,
  updatedAt: string
}
```

### Order 对象

```ts
{
  orderId: string,
  userId: number,
  shopId: number,
  productId: number,
  quantity: number,
  totalPrice: number,
  orderStatus: string,
  orderDate: string,
  contactId: number,
  contactName: string,
  contactPhone: string,
  contactAddress: string,
  trackingNumber: string|null
}
```

### Employee 对象

```ts
{
  merchantId: string,
  shopId: string,
  username: string,
  name: string,
  phone: string,
  role: string,  // 'CLERK' | 'MANAGER' | 'ADMIN'
  assignedBy: string
}
```

## 文件结构（保持不变）

每个组件/页面保持 4 文件模式：
- `Xxx.vue` — 渲染层
- `Xxx.js` — 逻辑层
- `Xxx.css` — 样式层
- `Text.js` — 文本层

## 样式约定

- 使用 Element Plus 组件库 + CSS 变量系统
- 命名遵循 BEM-like: `block__element--modifier`
- 间距、颜色、字体、阴影等从 `styles/variables.css` 取值

## 实现顺序（含依赖关系）

```
第1步: ProductCard 重构          ← 无前置依赖
第2步: OrderCard 重构            ← 无前置依赖
第3步: 新增 getProductsByShop API ← 无前置依赖
第4步: ShopProducts 页面改造      ← 依赖 第1步 + 第3步
第5步: ShopOrders 页面改造        ← 依赖 第2步
第6步: ShopEmployees 页面改造     ← 无前置依赖
```

第1-3步可并行。第4-6步在各依赖完成后可并行。
