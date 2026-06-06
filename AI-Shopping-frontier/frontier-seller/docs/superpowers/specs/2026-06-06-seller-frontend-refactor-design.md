# Seller Frontend Refactor: Login Flow & No-Shop UX

## 目标

重构商家端前端核心流程，解决登录过渡、无店铺状态、创建店铺入口等问题。

## 路由结构

| 路径 | 组件 | 说明 |
|------|------|------|
| `/login` | LoginPage | 独立页面，无 AppLayout 包裹 |
| `/register` | ShopRegister | 独立页面，带退出按钮（已实现） |
| `/` | AppLayout | 主框架，含 sidebar + topbar + content |
| `/` 的子路由 | Ship / Shop* 等 | 管理功能页面 |

- Login 和 Register 是顶级路由（`meta: { public: true }`）
- AppLayout 作为父路由 `/`，所有管理页作为 children

## 登录 → App 过渡流程

```
[登录表单] → 提交成功 → [验证中全屏状态] → initShops 完成 → [AppLayout 淡入]
                           ↑ 动画持续中         ↑ 动画结束
```

### 实现细节

1. **Login.js** 新增 `verifying` ref
2. 登录 API 成功后：`verifying = true` → 并行调用 `shop.initShops()` → 完成后 `router.push('/')`
3. **Login.vue**: 当 `verifying` 为 true 时，隐藏登录表单，显示全屏 loading 状态（品牌 Logo + spinner + "正在验证店铺信息…"）
4. **App.vue** 的 `<transition name="el-fade-in-linear" mode="out-in">` 负责 Login → AppLayout 的切换。Login 退出时显示的是 verifying 状态，不是空白表单
5. 用户感知：提交登录 → loading → App 出现，中间无闪烁

## 无店铺状态（AppLayout）

当 `shop.hasNoShops === true`：

| 区域 | 行为 |
|------|------|
| 左侧栏 `<AppSidebar />` | `v-if="!shop.hasNoShops"` 完全隐藏 |
| 功能区 | 显示空状态插画 + "您还没有店铺，创建后即可管理" |
| 弹窗 | `el-dialog`，`:show-close="false"` `:close-on-click-modal="false"`，按钮"前往创建"→ `window.open('/register', '_blank')` |
| 顶部导航面包屑 | 已有 `v-if="!shop.hasNoShops"`，正常隐藏 |

## 创建店铺页面（ShopRegister）

- 独立路由 `/register`，无 AppLayout
- 右上角"退出登录"按钮（已实现）
- 创建成功后跳转 `/shop/list`（路由守卫自动刷新店铺列表）

## 涉及文件

| 文件 | 改动 |
|------|------|
| `src/views/Login/Login.js` | 新增 `verifying` ref，登录成功后调用 `initShops` 再导航 |
| `src/views/Login/Login.vue` | 新增 verifying 全屏状态模板，替换表单视图 |
| `src/views/Login/Login.css` | 新增 verifying 状态样式 |
| `src/views/Login/Text.js` | 如有需要，添加验证中文本 |
| `src/layout/AppLayout.vue` | 左侧栏和内容区按 `hasNoShops` 条件渲染；添加无店铺弹窗 |
| `src/layout/AppSidebar.vue` | 移除 `hasNoShops` 相关代码（由 AppLayout 控制整个 sidebar 显隐） |

## 不变的文件

- `src/App.vue` — router-view + transition，无需改动
- `src/router/index.js` — 路由结构不变
- `src/store/shop.js` — 逻辑不变
- `src/store/auth.js` — 逻辑不变
- `src/views/ShopRegister/` — 已独立可用
- `src/layout/AppTopBar.vue` — 已有 `hasNoShops` 判断
