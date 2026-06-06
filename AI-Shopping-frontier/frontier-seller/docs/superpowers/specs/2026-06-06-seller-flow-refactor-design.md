# 商家端流程重构设计文档

## 1. 概述

### 1.1 目标

重构商家端（frontier-seller）的登录后流程，引入全局店铺上下文管理，重构导航结构，新增退货管理和商店信息管理页面。

### 1.2 核心变更

- 店铺上下文从页面本地提升到全局 Pinia store
- 导航栏改为两级分组结构
- 新增退货管理、商店信息管理两个页面
- 路由守卫增强，确保店铺上下文一致性

---

## 2. 角色与权限模型

### 2.1 角色判定

| 角色 | `currentRole.role` 值 | 说明 |
|------|----------------------|------|
| 店长 | `"1"` | 可拥有多个店铺，可管理员工 |
| 店员 | `"2"` (或其他非`"1"`值) | 仅关联一个店铺，无员工管理权 |

### 2.2 自审查缺口处理 #1 — 角色设置时机

**现状问题**：`authStore.login()` 没有设置 `currentRole`，`setRole()` 方法暴露在外但未被调用。

**解决方案**：登录响应中增加 `role` 字段。`authStore.login()` 在接收到登录响应后，自动设置 `currentRole`：

```js
// login 成功后追加
if (res.data?.role) {
  currentRole.value = { role: res.data.role, shopId: null }
  localStorage.setItem('currentRole', JSON.stringify(currentRole.value))
}
```

如后端暂未返回 `role`，前端可在 `merchantInfo` 中增加 `role` 字段作为过渡。

### 2.3 自审查缺口处理 #2 — 员工店铺查询

**现状问题**：不确定 `getShopByMerchant(merchantId)` 对员工是否返回店铺。

**解决方案**：假设该接口对店主和店员均有效——店主返回多 shopIds，店员返回单 shopIds。如后端不支持，后续单独为店员增加接口或调整后端。

---

## 3. 全局店铺上下文

### 3.1 Store 增强（`src/store/shop.js`）

```js
// 新增状态
const loaded = ref(false)         // 标记已加载
const pendingShipCount = ref(0)   // 待发货数（侧栏角标用）

// 新增方法
async function initShops(merchantId)  // 初始化加载 shops
function ensureShopSelected()         // 确保有一个当前店铺
```

### 3.2 初始化流程

```
App.vue created/mounted
  ↓
authStore.isLoggedIn === true
  ↓
shopStore.initShops(authStore.merchantInfo.id)
  ↓
shops 加载完成
  ↓
  ├─ shops.length === 1 → 自动设置 currentShopId = shops[0].id
  └─ shops.length > 1   → 保持 currentShopId 不变（如无则取第一个）
```

### 3.3 自审查缺口处理 #5 — 未选店铺兜底

**解决方案**：路由守卫拦截。访问 `/shop/:shopId/xxx` 时，如 `currentShopId` 与 URL 中的 `shopId` 不匹配，静默更新 store。如 `currentShopId` 为 `null`，跳转提示页或 `/ship`。

具体路由守卫逻辑（在 `router/index.js` 中增强）：

```js
router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  const shop = useShopStore()

  // 已有逻辑：public / login check ...

  // 新增：shop 路径检查
  if (to.params.shopId) {
    if (!shop.currentShopId) {
      // 未选店铺，尝试用 URL 中的 shopId
      shop.switchShop(to.params.shopId)
    } else if (String(shop.currentShopId) !== to.params.shopId) {
      shop.switchShop(to.params.shopId)
    }
  }

  next()
})
```

---

## 4. 导航栏重构

### 4.1 结构

```
订单管理（子菜单）
├── 订单发货        /ship                         (始终可见)
├── 订单管理        /shop/:shopId/orders           (需当前店铺)
└── 退货管理        /shop/:shopId/returns  [NEW]   (需当前店铺)

商品管理（单菜单项）
└── 商品管理        /shop/:shopId/products         (需当前店铺)

店铺管理（子菜单，仅店主可见）
├── 商店信息        /shop/:shopId/info      [NEW]  (需当前店铺)
├── 地址管理        /shop/:shopId/addresses        (需当前店铺，仅店主)
└── 员工管理        /shop/:shopId/employees        (需当前店铺，仅店主)
```

### 4.2 控制规则

| 菜单项 | 显示条件 |
|--------|---------|
| 订单发货 | 始终可见 |
| 订单管理 / 商品管理 / 退货管理 / 商店信息 | `currentShopId !== null` |
| 地址管理 / 员工管理 | `currentShopId !== null && isOwner` |

### 4.3 当前店铺未选时的兼容行为

当 `currentShopId === null` 时，所有需店铺的菜单项置灰（或自动隐藏），并展示"请选择店铺"的提示区域。

---

## 5. 页面改动

### 5.1 Ship.vue（发货页）— 改造

**现状**：Ship.js 自己调用 `getShopByMerchant` 加载店铺列表并管理 `currentShopId`。

**改造要点**：
- 移除 Ship.js 中的 `loadShops()`、`shops`、`currentShopId`、`hasMultipleShops`、`switchShop`
- 改为从 `shopStore` 读取全局 `currentShopId`
- Ship.vue 顶部保留店铺选择器（与 AppTopBar 逻辑统一，但 Ship 页可以显示更醒目的选择提示）
- `onMounted` 简化为：若 `shopStore.currentShopId` 已存在则直接 `loadOrders()`

### 5.2 ShopList.vue（店铺列表页）— 改造

**现状**：展示所有店铺卡片，点击跳转到各管理页。

**改造要点**：
- 新增"进入管理"按钮，点击时 `shopStore.switchShop(shopId)` 后跳转到 `/ship`
- 保持原有卡片展示，但作为店长的店铺概览入口

### 5.3 其他现有页面

- **ShopProducts**：从 `route.params.shopId` 读取，无需大改
- **ShopOrders**：同上
- **ShopAddresses**：同上
- **ShopEmployees**：同上

---

## 6. 新页面

### 6.1 退货管理（`/shop/:shopId/returns`）

**视图结构**：
- 工具栏：页面标题 + 刷新按钮 + 状态筛选下拉
- 表格列：订单编号、下单时间、商品、数量、总价、退货原因、状态、操作
- 操作按钮：
  - 状态为 `PAID`（待发货 → 申请退货）→ **审核通过**（调用 `approveReturn`）
  - 状态为已审核 → **确认退货完成**（调用 `confirmReturn`）
- 详情弹窗：查看订单详情 + 退货物流信息

**路由**：`/shop/:shopId/returns`，名称 `shop-returns`

### 6.2 自审查缺口处理 #3 — 退货流程状态

**现状**：`ORDER_STATUS` 中无退货申请中的中间状态，现有流程为 `PENDING → PAID → SHIPPED → DELIVERED → RETURNED`。

**分析**：退货涉及两段操作（卖家审核 → 卖家确认收货），中间需要一个过渡状态。假设后端存在 `RETURN_REQUESTED`（已申请退货）状态，并增加 `RETURN_APPROVED`（已审核通过，待买家寄回）或直接沿用简化的两段流程。

**前端定义扩展**（`src/config/orderStatus.js`）：

```js
RETURN_REQUESTED: 'RETURN_REQUESTED', // 已申请退货（待审核）
RETURN_APPROVED: 'RETURN_APPROVED',   // 已审核通过（待卖家确认收货）
```

**退货管理页面的订单过滤范围**：
| Tab/筛选 | 订单状态 | 可执行操作 |
|----------|---------|-----------|
| 待审核 | `RETURN_REQUESTED` | 审核通过（`approveReturn`）|
| 待确认 | `RETURN_APPROVED` | 确认退货完成（`confirmReturn`）|
| 已退货 | `RETURNED` | 查看物流 |

如后端暂未实现中间状态，则先简化为：退货管理页面展示所有 `RETURNED` + `SHIPPED` 状态的订单，仅在详情中展示退货物流信息，`approveReturn` / `confirmReturn` 按钮暂时隐藏或作为预留。

### 6.3 商店信息管理（`/shop/:shopId/info`）

**视图结构**：
- 基本信息表单：商店名称、简介、Logo（图片上传）、营业时间、联系电话、地址
- 操作：保存修改（调用 `updateShop`）
- 状态切换：开店/关店按钮（调用 `openShop` / `closeShop`）

**路由**：`/shop/:shopId/info`，名称 `shop-info`

### 6.4 自审查缺口处理 #4 — 商店详情字段

**现状**：`getShopDetail` 返回字段有限（无名称、简介等展示信息）。

**说明**：当前 API 返回的 `shop` 对象不包含完整信息。可能出现的情况：
- 方案A：后端存在 `shopInfo` 关联表，需另调接口（如 `getShopInfo(shopInfoId)`）
- 方案B：`getShopDetail` 实际包含更多字段，文档未完整记录

**设计采用方案B**：假设 `getShopDetail` 返回完整信息（含 `name`, `description`, `phone`, `address`, `businessHours`, `logoUrl` 等字段）。如实现后发现缺失，再补充 API 调用。

---

## 7. 路由表变更

| 路径 | 名称 | 组件 | 权限 | 状态 |
|------|------|------|------|------|
| `/shop/:shopId/returns` | `shop-returns` | 新组件 | 登录 | NEW |
| `/shop/:shopId/info` | `shop-info` | 新组件 | shopOwnerOnly | NEW |
| `/shop/:shopId/orders` | `shop-orders` | 不变 | 登录 | 已有 |
| `/shop/:shopId/products` | `shop-products` | 不变 | 登录 | 已有 |
| `/shop/:shopId/employees` | `shop-employees` | 不变 | shopOwnerOnly | 已有 |
| `/shop/:shopId/addresses` | `shop-addresses` | 不变 | shopOwnerOnly | 已有 |
| `/shop/list` | `shop-list` | 不变 | shopOwnerOnly | 已有 |
| `/shop/register` | `shop-register` | 不变 | shopOwnerOnly | 已有 |
| `/ship` | `ship` | 改造 | 登录 | 改造 |
| `/login` | `login` | 不变 | public | 已有 |

---

## 8. 实现顺序

### Phase 1: 基础设施
1. 增强 shop store（合并加载逻辑、初始化方法）
2. 增强 auth store（登录时设置 role）
3. 改造 App.vue（登录后自动初始化 shops）
4. 增强路由守卫（shopId 同步）

### Phase 2: 导航重构
5. 重构 AppSidebar.vue（两级分组菜单）
6. 增强 AppTopBar.vue（店铺选择器升级）
7. 改造 Ship.vue（移除本地 shops 管理）

### Phase 3: 新页面
8. 创建退货管理页面（ReturnManagement）
9. 创建商店信息页面（ShopInfo）

### Phase 4: 收尾
10. 调整 ShopList.vue（新增"进入管理"按钮）
11. 清理各页面中旧的店铺管理逻辑
