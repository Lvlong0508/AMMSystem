# 用户端商品卡片重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 frontier-user ProductCard abstract 变体为极简暗钮风格，仅展示图片、名称、库存、价格。

**Architecture:** 纯 CSS 变量 + Vue 3 Composition API，改写 ProductCard.vue 模板和 ProductCard.css 样式，保持 detail 变体不动。

**Tech Stack:** Vue 3, CSS Variables, Vite

---

### Task 1: 重写 ProductCard abstract 模板

**Files:**
- Modify: `AI-Shopping-frontier/frontier-user/src/components/ProductCard/ProductCard.vue`

- [ ] **Step 1: 重写 abstract 变体模板**

将 abstract 分支替换为：

```vue
<div v-if="variant === 'abstract'" class="product-card product-card--abstract" @click="$emit('viewDetail', product)">
  <div class="product-card__image-wrap">
    <img v-if="product.imageUrl" class="product-card__image" :src="product.imageUrl" :alt="product.name" />
    <div v-else class="product-card__image-placeholder">
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
    </div>
  </div>
  <div class="product-card__name">{{ product.name }}</div>
  <div class="product-card__meta">
    <span class="product-card__stock">库存: {{ product.stock ?? '-' }}</span>
    <span class="product-card__price">¥{{ product.price?.toFixed(2) }}</span>
  </div>
  <button class="product-card__cta" @click.stop="$emit('buyNow', product)">去下单</button>
</div>
```

- [ ] **Step 2: 保留 detail 变体和 script 部分不变**

detail 分支模板和 `<script setup>` / `<style scoped>` 保持原样。

---

### Task 2: 重写 ProductCard abstract 样式

**Files:**
- Modify: `AI-Shopping-frontier/frontier-user/src/components/ProductCard/ProductCard.css`

- [ ] **Step 1: 替换 abstract 相关 CSS**

将文件中 `.product-card--abstract` 及其所有子选择器替换为：

```css
.product-card--abstract {
  width: 240px;
  background: var(--color-surface);
  border-radius: 18px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04), 0 12px 36px rgba(15,23,42,.08);
  overflow: hidden;
  cursor: pointer;
  transition: transform 250ms ease-out, box-shadow 250ms ease-out;
}
.product-card--abstract:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,.06), 0 16px 48px rgba(15,23,42,.12);
}
.product-card__image-wrap {
  margin: 14px 14px 0;
  height: 180px;
  border-radius: 14px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.product-card__image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.product-card__image-placeholder {
  color: #cbd5e1;
}
.product-card__name {
  padding: 10px 14px 0;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 750;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.product-card__meta {
  padding: 10px 14px 0;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.product-card__stock {
  font-size: 12px;
  color: var(--color-text-tertiary);
}
.product-card__price {
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 850;
  color: var(--color-danger);
  letter-spacing: -0.5px;
}
.product-card__cta {
  display: block;
  width: calc(100% - 28px);
  margin: 10px 14px 14px;
  padding: 11px 0;
  background: #0f172a;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 200ms ease-out;
}
.product-card__cta:hover {
  opacity: 0.88;
}
```

- [ ] **Step 2: 保留 detail 相关 CSS 不动**

`.product-card--detail` 及其子选择器保持原样。

---

### Task 3: 清理 useProductCard 中不再需要的逻辑

**Files:**
- Modify: `AI-Shopping-frontier/frontier-user/src/components/ProductCard/useProductCard.js`
- Modify: `AI-Shopping-frontier/frontier-user/src/components/ProductCard/ProductCard.vue`

- [ ] **Step 1: 移除 parsedTags**

abstract 变体不再使用标签，从 `useProductCard.js` 中移除 `parsedTags`，从 `ProductCard.vue` 的 `<script setup>` 中移除 `useProductCard` 的导入和调用。若 detail 变体仍需 `parsedTags`，则保留 `useProductCard.js` 但仅 detail 使用。

检查 detail 变体是否使用 `parsedTags`：
- detail 模板中无标签展示 → 可以完全移除
- detail 模板中有标签展示 → 保留给 detail 用

- [ ] **Step 2: 验证编译通过**

Run: `cd AI-Shopping-frontier/frontier-user && npm run build`
Expected: 构建成功无报错

---

### Task 4: 验证视觉效果

- [ ] **Step 1: 启动 dev server 检查卡片渲染**

Run: `cd AI-Shopping-frontier/frontier-user && npm run dev`

在浏览器中进入聊天页，触发 AI 推荐商品，确认 abstract 卡片展示：图片、名称、库存、价格、去下单按钮。

- [ ] **Step 2: 确认 detail 变体未受影响**

点击卡片进入详情视图，确认 detail 变体正常展示。

- [ ] **Step 3: Commit**

```bash
git add AI-Shopping-frontier/frontier-user/src/components/ProductCard/
git commit -m "refactor: redesign user-side product card to minimal style (A1)"
```
