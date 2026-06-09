# 用户端商品卡片重构设计

## 目标

重构 `frontier-user` 的 `ProductCard` 组件 abstract 变体，采用 A1 极简暗钮方案，仅展示：商品图片、商品名、库存、价格。

## 数据源

AI 工具返回的 `ProductWithImageAbstractDTO` 使用字段：
- `imageUrl` — 商品图片
- `name` — 商品名
- `stock` — 库存（若 DTO 无则默认）
- `price` — 价格

## 布局结构

```
┌──────────────────────────────┐
│                              │
│         商品图片              │  ← 顶部：大图
│                              │
├──────────────────────────────┤
│ 商品名字                      │  ← 中部：商品名
├──────────────────────────────┤
│ 库存        ¥99.00          │  ← 中下：库存 + 价格
├──────────────────────────────┤
│        [去下单]              │  ← 底部：按钮
└──────────────────────────────┘
```

## 设计规格（A1 极简暗钮）

- **卡片**：白底，圆角 18px，轻阴影，宽 240px，聊天气泡内水平排列
- **顶部图片**：高 180px，圆角 14px（内边距 14px），浅灰占位，object-fit: contain
- **商品名**：padding 0 14px，16px 深色粗体，单行截断
- **库存+价格**：padding 0 14px，flex 左右分布，左侧"库存: 128 件"（12px 灰色），右侧价格（22px 红色粗体）
- **按钮**：margin 10px 14px 14px，全宽纯黑（#0f172a），圆角 12px，12px 内边距，白色粗体"去下单"

## 影响范围

- `frontier-user/src/components/ProductCard/ProductCard.vue` — 重写 abstract 模板
- `frontier-user/src/components/ProductCard/ProductCard.css` — 重写 abstract 样式
- `frontier-user/src/views/Chat/ChatView/ChatView.vue` — 确认传入数据兼容

## 约束

- 使用纯 CSS（项目现有变量体系），不引入新依赖
- `detail` 变体保持不动