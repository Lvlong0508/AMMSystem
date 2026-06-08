# 商家端独立店铺选择流程设计

## 概述

将店铺选择从侧边栏导航中剥离，改为独立的全屏选择页面。用户必须先选择店铺才能进入功能页面，切换店铺需回到选择页重新选择。

## 背景

当前问题：
- 店铺选择与侧边栏功能入口混在一起
- 多店铺时自动默认选中第一个，用户无感知
- /shop/list 页面位于 AppLayout 内，与功能页面同级
- 用户可在功能页面内随时切换店铺，流程不够清晰

## 方案

### 1. 路由结构

新增独立路由 `/shop/select`，不继承 AppLayout（无侧边栏/顶栏），类似登录页的独立全屏布局：

```
/login             → Login          (public)
/shop/register     → ShopRegister   (public)
/shop/select       → ShopSelect     ← NEW, standalone, public
/                  → AppLayout (含侧边栏)
  /ship            → Ship
  /shop/:shopId/products   → ShopProducts
  /shop/:shopId/orders     → ShopOrders
  /shop/:shopId/returns    → ReturnManagement
  /shop/:shopId/info       → ShopInfo (仅店主)
  /shop/:shopId/addresses  → ShopAddresses (仅店主)
  /shop/:shopId/employees  → ShopEmployees (仅店主)
```

`/shop/list` 路由移除。

### 2. 路由守卫流程

```
请求访问任意路由
  ├─ 公开路由（login/register/shop-register/shop-select）→ 放行
  ├─ 未登录 → 重定向 /login
  ├─ 已登录 → 加载店铺列表
  │    ├─ 无店铺 → 放行（AppLayout 显示空状态）
  │    ├─ 单店铺 → 自动选中该店铺 → 放行
  │    └─ 多店铺 + 未选择店铺 → 重定向 /shop/select
  └─ 访问 /shop/:shopId/xxx 时 shopId !== currentShopId → 重定向 /shop/select
```

### 3. ShopSelect 页面

全屏独立页面，无侧边栏、无顶栏，风格与登录页统一：
- 居中布局，顶部显示用户头像+名称
- 标题："请选择要管理的店铺"
- 店铺卡片网格（复用当前 ShopList 的卡片 UI）
- 点击卡片 → switchShop(shopId) → 跳转 /shop/:shopId/products
- 底部显示"退出登录"
- 对于 `ShopList.vue`，将其从 AppLayout 子路由中移出，作为独立路由的页面组件

### 4. Sidebar 变更

- 移除"店铺列表"菜单项
- 其他菜单结构不变

### 5. TopBar 变更

- 右侧添加「切换店铺」文字按钮 → 跳转 /shop/select
- 面包屑中移除「店铺列表」「店铺管理」相关项

### 6. Store 变更

`shop.initShops`：
- 仅加载店铺列表，不自动设置 currentShopId
- 例外：shops.length === 1 时自动选中

## 涉及文件

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| src/router/index.js | 修改 | 添加 /shop/select 路由，移除 /shop/list 子路由，更新守卫逻辑 |
| src/store/shop.js | 修改 | initShops 去掉多店铺自动选择 |
| src/layout/AppSidebar.vue | 修改 | 移除"店铺列表"菜单项 |
| src/layout/AppTopBar.vue | 修改 | 移除店铺列表面包屑，添加切换店铺按钮 |
| src/layout/AppLayout.vue | 修改 | 可能调整空状态逻辑 |
| src/views/ShopList/ShopList.vue | 修改 | 改造为独立全屏布局（移除对 AppLayout 的依赖），作为 /shop/select 页面 |
| src/views/ShopList/ShopList.js | 修改 | 适配新路由守卫逻辑，移除 onMounted 加载（由守卫触发） |
| src/views/ShopList/ShopList.css | 修改 | 添加全屏居中布局样式 |

## 不涉及范围

- 功能页面本身（商品管理、订单管理、退货管理等）不做改动
- 权限控制逻辑（shopOwnerOnly）不做改动
- 登录/注册流程不做改动
