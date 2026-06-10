# 商店信息页重构设计

## 概述

重构商家端商店信息页，将当前内联可编辑表单改为「纯文本展示 + 弹窗编辑」模式，并将店铺 Logo 移至页面顶部居中展示。

## 布局结构

```
┌─────────────────────────────────────────────┐
│  商店信息        [●] 营业中       [编辑]   │  ← 标题栏
├─────────────────────────────────────────────┤
│              ╭─────╮                        │
│              │ logo │                       │  ← el-avatar 圆形居中
│              ╰─────╯                        │
│  ┌───────────────────────────────────────┐  │
│  │  商店名称    │  xxx                   │  │
│  │  商店简介    │  xxx                   │  │  ← el-descriptions border
│  │  联系电话    │  xxx                   │  │     column="1"
│  │  所在地区    │  xxx                   │  │
│  │  详细地址    │  xxx                   │  │
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## 组件变更

### ShopInfo.vue

| 区域 | 变更 |
|------|------|
| 标题栏 | 左侧「商店信息」标题，中间 `el-switch`（显示状态+切换），右侧「编辑」按钮 |
| Logo 区域 | 从 `el-form-item` 中移出，置于卡片顶部居中，使用 `el-avatar shape="circle"` 包裹 |
| 信息展示 | 将所有 `el-form-item` + 输入框替换为 `el-descriptions border column="1"` 纯文本展示 |
| 编辑按钮 | 新增，点击弹出 `el-dialog` |
| 保存按钮 | 从页面移除，移至弹窗 footer |
| 开/关店按钮 | 移除，由 `el-switch` 替代 |

### ShopInfo.js 新增

| 功能 | 说明 |
|------|------|
| `dialogVisible` (ref) | 控制编辑弹窗显隐 |
| `editForm` (reactive) | 弹窗内使用的表单副本 |
| `openEditDialog()` | 打开弹窗，将 `form` 深拷贝到 `editForm` |
| `closeEditDialog()` | 关闭弹窗，丢弃变更 |
| `handleEditSave()` | 用 `editForm` 数据提交 API，成功后更新 `form` |
| `handleToggleStatus()` | `el-switch` change 事件，调用开/关店 API |

### ShopInfo.css

| 样式 | 说明 |
|------|------|
| `.shop-info__header` | 标题栏 flex 布局 |
| `.shop-info__logo` | Logo 容器，居中 + 下边距 |
| `.shop-info__logo-avatar` | el-avatar 包裹样式 |
| 移除原有 `.shop-info__logo-upload` / `.shop-info__logo-preview` 等 |

### Text.js 新增常量

- `BTN_EDIT = '编辑'`
- `DIALOG_TITLE = '编辑商店信息'`
- `BTN_CANCEL = '取消'`

移除不再使用的常量（原表单相关的按钮文字视情况保留）。

## 编辑弹窗

```
┌─────────────────────────────────────┐
│  编辑商店信息                    ✕  │
├─────────────────────────────────────┤
│  商店名称：  [________________]     │
│  商店简介：  [________________]     │
│              [________________]     │
│              [________________]     │
│  联系电话：  [________________]     │
│  省/市/区：  [请选择 ▼]            │
│  详细地址：  [________________]     │
│  店铺 Logo： [选择文件]  预览图     │
├─────────────────────────────────────┤
│          [取消]    [保存]           │
└─────────────────────────────────────┘
```

- `el-dialog width="600px"`，与 ShopProducts 统一
- `el-form label-position="top"`
- 表单字段：name, description, phone, region(cascader), addressDetail, logo
- Logo 上传使用原生 `<input type="file">`，仅 `.jpg,.png`

## 数据流

1. **页面加载：** `onMounted` → `getShopDetail(shopId)` → 填充 `form` 用于展示
2. **打开弹窗：** 深拷贝 `form` → `editForm`，弹窗绑定 `editForm`
3. **保存：** `updateShop(shopId, FormData)` → 成功 → `form = editForm` → 关闭弹窗
4. **取消：** 关闭弹窗，`editForm` 不写回 `form`
5. **开关店：** `el-switch` → `closeShop/openShop` API → 更新 `shopStatus`

## 涉及文件

| 文件 | 操作 |
|------|------|
| `frontier-seller/src/views/ShopInfo/ShopInfo.vue` | 重写 template |
| `frontier-seller/src/views/ShopInfo/ShopInfo.js` | 新增弹窗逻辑 |
| `frontier-seller/src/views/ShopInfo/ShopInfo.css` | 重写样式 |
| `frontier-seller/src/views/ShopInfo/Text.js` | 新增常量 |
