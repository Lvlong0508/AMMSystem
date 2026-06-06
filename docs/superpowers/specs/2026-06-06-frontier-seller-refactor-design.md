# frontier-seller 商家端全面重构设计文档

## 概述

对 `frontier-seller` 商家服务平台进行全面重构，涵盖**视觉设计升级**与**代码架构重组**。

- **项目**: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller`
- **框架**: Vue 3 + Vite + Tailwind CSS
- **代码规格**: 遵循 `F:\IdeaProjects\AI-Shopping\前端代码规格说明.md`
- **风格方向**: 轻量极简风（类似 Linear/Stripe）
- **重构策略**: 渐进式重构（方案 A）

---

## 1. 新目录结构

```
src/
├── api/                接口请求层（保持现有文件结构）
│   ├── request.js      Axios 实例 + 拦截器
│   ├── auth.js
│   ├── shop.js
│   ├── product.js
│   ├── order.js
│   ├── logistics.js
│   └── contact.js
│
├── assets/             静态资源
│   └── icons/          SVG 图标
│
├── components/         通用组件库
│   ├── AppButton.vue
│   ├── AppCard.vue
│   ├── AppModal.vue
│   ├── AppBadge.vue
│   ├── AppEmpty.vue
│   ├── AppSearch.vue
│   ├── AppDropdown.vue
│   ├── AppToast.vue
│   └── index.js        统一导出
│
├── composables/        组合式函数
│   ├── useAuth.js      认证逻辑
│   ├── useShop.js      店铺上下文
│   └── usePagination.js 分页复用
│
├── layout/             布局组件
│   ├── AppLayout.vue   主布局（Sidebar + TopBar + Content）
│   ├── AppLayout.js    布局逻辑
│   ├── AppLayout.css   布局样式
│   ├── AppSidebar.vue  侧边栏
│   ├── AppSidebar.css
│   ├── AppTopBar.vue   顶栏
│   └── AppTopBar.css
│
├── router/
│   └── index.js        路由定义 + 守卫（适配新布局）
│
├── store/              Pinia 状态管理
│   ├── auth.js         认证与角色
│   ├── shop.js         店铺上下文
│   └── app.js          应用 UI 状态
│
├── styles/             全局样式
│   ├── variables.css   设计令牌（CSS 变量）
│   ├── base.css        基础重置 + 排版
│   └── utilities.css   工具类
│
├── utils/
│   └── swal.js         SweetAlert2 封装
│
├── views/              页面（每个页面拆为 4 文件）
│   ├── Login/
│   │   ├── Login.vue
│   │   ├── Login.js
│   │   ├── Login.css
│   │   └── Text.js
│   ├── Ship/
│   │   ├── Ship.vue
│   │   ├── Ship.js
│   │   ├── Ship.css
│   │   └── Text.js
│   ├── ShopList/
│   ├── ShopRegister/
│   ├── ShopProducts/
│   ├── ShopOrders/
│   ├── ShopEmployees/
│   └── ShopAddresses/
│       ├── ShopAddresses.vue
│       ├── ShopAddresses.js
│       ├── ShopAddresses.css
│       └── Text.js
│
├── App.vue             根组件（精简，仅写 layout + router-view）
└── main.js             入口（引入 Pinia + Router + 全局样式）
```

### 文件规格说明（每页面 4 文件）

| 文件 | 职责 | 包含内容 |
|------|------|---------|
| `Filename.vue` | 渲染层 | `<template>` + `<style scoped>` |
| `Filename.js` | 逻辑层 | `<script setup>` 逻辑、API 调用、响应式数据、事件处理 |
| `Filename.css` | 样式层 | 页面级非 scoped 样式（如需 Tailwind 无法覆盖的全局样式） |
| `Text.js` | 文本层 | 导出所有 UI 文本常量，渲染层和逻辑层通过 import 引用 |

---

## 2. 设计系统

### 色彩体系

使用 `oklch` 色彩空间：

| CSS 变量 | 值 | 用途 |
|----------|-----|------|
| `--color-primary` | `oklch(0.55 0.15 250)` | 品牌色（灰蓝） |
| `--color-primary-hover` | `oklch(0.5 0.15 250)` | 品牌色 Hover |
| `--color-surface` | `oklch(0.99 0 0)` | 页面背景 |
| `--color-card` | `oklch(0.97 0.01 240)` | 卡片/区域背景 |
| `--color-border` | `oklch(0.92 0.01 240)` | 分割线、边框 |
| `--color-text` | `oklch(0.2 0.02 240)` | 主要文字 |
| `--color-text-secondary` | `oklch(0.55 0.03 240)` | 次要文字 |
| `--color-text-tertiary` | `oklch(0.7 0.02 240)` | 辅助文字 |
| `--color-accent` | `oklch(0.65 0.2 30)` | 强调色（暖琥珀） |
| `--color-success` | `oklch(0.6 0.18 150)` | 成功状态 |
| `--color-danger` | `oklch(0.55 0.2 25)` | 危险/删除状态 |
| `--color-sidebar` | `oklch(0.25 0.02 240)` | 侧边栏背景（深色） |

### 字体体系

- **标题**: `Inter`（清晰现代、数字对齐好）
- **正文**: 系统字体栈 `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif`
- **字号**: `12 / 14 / 16 / 20 / 24 / 32` px
- **行高**: 正文 `1.5`、标题 `1.2`

### 间距体系

`4 / 8 / 12 / 16 / 24 / 32 / 48 / 64` px

### 圆角

- 按钮/输入框: `6px`
- 卡片: `8px`
- 弹窗/对话框: `12px`
- 徽章: `999px`（圆角胶囊）

### 阴影

```css
--shadow-sm:   0 1px 2px rgba(0,0,0,0.04);
--shadow-md:   0 2px 8px rgba(0,0,0,0.06);
--shadow-lg:   0 8px 24px rgba(0,0,0,0.08);
```

---

## 3. 布局设计

### 页面布局结构

```
┌──────────────────────────────────────────────────┐
│  Sidebar (240px)         │  Main Area            │
│  ┌────────────────────┐  │  ┌──────────────────┐ │
│  │ Logo / 品牌标识     │  │  │ TopBar           │ │
│  │                    │  │  │ 面包屑/搜索/用户  │ │
│  │ 导航菜单           │  │  ├──────────────────┤ │
│  │  📦 订单发货        │  │  │                  │ │
│  │  🏪 店铺管理 ▼     │  │  │  Page Content     │ │
│  │    📋 商品管理     │  │  │  (router-view)    │ │
│  │    📄 订单管理     │  │  │                  │ │
│  │    👥 员工管理     │  │  │                  │ │
│  │    📍 地址管理     │  │  │                  │ │
│  │                    │  │  │                  │ │
│  │ ──────────────     │  │  │                  │ │
│  │ 👤 用户名          │  │  │                  │ │
│  │    退出登录        │  │  └──────────────────┘ │
│  └────────────────────┘  │                       │
└──────────────────────────────────────────────────┘
```

### 侧边栏 (AppSidebar)

- 宽度 `240px`，可折叠到 `64px`（仅显示图标）
- 深色背景（`--color-sidebar`）
- 导航项：高亮当前路由、展开/收起子菜单
- 底部：用户信息 + 退出登录

### 顶栏 (AppTopBar)

- 白色背景 + 底部细边框
- 左侧：面包屑导航
- 右侧：店铺选择器 + 通知（可选）+ 用户头像

---

## 4. 通用组件

| 组件 | Props | 说明 |
|------|-------|------|
| `AppButton` | `variant: 'primary'/'secondary'/'ghost'/'danger'`, `size: 'sm'/'md'/'lg'`, `loading`, `disabled` | 按钮，含 loading 状态 |
| `AppCard` | `padding`, `hoverable` | 内容卡片 |
| `AppModal` | `visible`, `title`, `width`, `confirmText`, `cancelText` | 弹窗，含确认/取消 |
| `AppBadge` | `variant: 'default'/'success'/'warning'/'danger'/'info'` | 状态徽章 |
| `AppEmpty` | `icon`, `title`, `description`, `actionText` | 空状态 |
| `AppSearch` | `v-model`, `placeholder`, `@search` | 搜索输入框 |
| `AppDropdown` | `items`, `v-model` | 下拉菜单 |
| `AppToast` | 由 composable `useToast()` 调用 | 轻提示（success/error/warning） |

---

## 5. Pinia Store

### store/auth.js

```js
状态:
- token (从 localStorage 初始化)
- merchantInfo (从 localStorage 初始化)
- currentRole (从 localStorage 初始化)

计算属性:
- isLoggedIn: !!token
- isOwner: currentRole?.role === '1'
- merchantName: merchantInfo?.username

动作:
- login(credentials) → 调 API → 存状态 + localStorage
- logout() → 调 API → 清状态 + localStorage
- setRole(role) → 更新当前角色
```

### store/shop.js

```js
状态:
- currentShopId (从 localStorage 初始化)
- shops[]

动作:
- loadShops(merchantId) → 调 API → 填充 shops
- switchShop(shopId) → 更新 currentShopId

计算属性:
- currentShop: shops.find(s => s.id === currentShopId)
```

### store/app.js

```js
状态:
- sidebarCollapsed: false

动作:
- toggleSidebar()
```

---

## 6. 数据流

```
用户操作 → 组件方法 → Store Action / 页面逻辑 → API 调用 → 状态更新 → UI 响应

                               ┌──────────────┐
                               │   API 层     │
                               │  (api/*.js)  │
                               └──────┬───────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                  │
              ┌─────▼─────┐   ┌──────▼──────┐   ┌──────▼──────┐
              │  auth     │   │  shop       │   │  Page-local  │
              │  Store    │   │  Store      │   │  State       │
              └───────────┘   └─────────────┘   │  (在 *.js)   │
                                                 └──────────────┘
```

原则：
- **全局状态**（认证、店铺上下文、UI 布局）→ Pinia Store
- **页面级状态**（订单列表、商品列表、表单数据）→ 各页面 `*.js` 中管理，不进入 Store
- **持久化** → 认证信息同步到 localStorage，刷新后 Store 从 localStorage 恢复

---

## 7. 渐进式重构阶段计划

### Phase 1: 骨架搭建

目标：建立新架构的底座，所有页面在新架构下能运行

步骤：
1. 创建 `styles/variables.css` — 设计令牌
2. 创建 `store/auth.js`、`store/shop.js`、`store/app.js` — Pinia Store
3. 创建 `layout/AppLayout.vue/js/css` — 新布局
4. 创建 `layout/AppSidebar.vue/css` — 侧边栏
5. 创建 `layout/AppTopBar.vue/css` — 顶栏
6. 创建基础组件：`AppButton`、`AppCard`、`AppModal`、`AppBadge`、`AppEmpty`、`AppSearch`
7. 更新 `router/index.js` 使用新布局
8. 更新 `App.vue` 和 `main.js` 引入 Pinia + 新布局
9. 删除 `src/style.css` 旧全局样式

### Phase 2: 核心页面迁移

按以下顺序逐页迁移：

10. **Login 页面** — 重构为 `views/Login/Login.vue + js + css + Text.js`
11. **Ship 页面** — 从 `merchant/MerchantShip/` 迁移到 `views/Ship/`，按4文件拆分
12. **ShopList 页面** — 重构为 `views/ShopList/` 4文件

每页迁移步骤：
- 创建 `views/PageName/` 目录
- 从当前文件提取模板 → `.vue`、逻辑 → `.js`、样式 → `.css`、文本 → `Text.js`
- 更新路由指向
- 删除旧文件

### Phase 3: 管理页面迁移

13. **ShopProducts** — 商品管理
14. **ShopOrders** — 订单管理
15. **ShopEmployees** — 员工管理
16. **ShopAddresses** — 地址管理

### Phase 4: 收尾清理

17. 删除 `merchant/` 旧目录
18. 删除所有旧单文件式 vue 文件
19. 更新 `postcss.config.js` / `tailwind.config.js`（如需要）
20. 全局检查：确保无 dead imports、broken routes

---

## 8. 技术选型说明

| 领域 | 选择 | 理由 |
|------|------|------|
| 构建工具 | Vite（已有） | 高速 HMR、原生 ESM |
| CSS 方案 | Tailwind + CSS 变量 + scoped | 原子化开发效率 + 设计令牌统一管理 |
| 状态管理 | Pinia | 官方推荐、轻量、TS 友好 |
| HTTP | Axios（已有） | 拦截器机制成熟 |
| 弹窗 | SweetAlert2（已有） | 封装在 utils/swal.js |
| 图标 | 内联 SVG | 不引入图标库依赖，按需自制 |
| 测试 | 暂不引入 | 项目当前无测试，先完成重构 |

---

## 9. 需注意的技术细节

1. **request.js 适配 Pinia**：响应拦截器的 401 处理需改为调用 auth store 的 logout 方法，避免循环依赖（通过 `useAuthStore()` 延迟调用，或在 store 外挂载回调）
2. **Tailwind 与 CSS 变量共存**：设计令牌以 CSS 变量定义，Tailwind 配置引用这些变量
3. **路由过渡动画**：使用 Vue Router 的 `<RouterView>` 配合 `<Transition>` 实现页面切换动画
4. **localStorage 同步**：Pinia store 初始化时从 localStorage 读取，写操作同时写入 localStorage

---

## 10. 不使用 / 不引入

- ❌ TypeScript（保持 JS）
- ❌ Element Plus / Naive UI（自建轻量组件库）
- ❌ 测试框架（暂不引入）
- ❌ i18n（不需要国际化）
