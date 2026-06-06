# 样式视觉精化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI-Mart 商家端视觉从"可用"提升到"精致的 Premium Admin"水准

**Architecture:** 纯 CSS 变更，无功能修改。涉及 4 个核心文件：`variables.css`（设计令牌）、`index.html`（字体）、`App.vue`（Element Plus 主题）、布局组件 CSS

**Tech Stack:** CSS3 (oklch), Element Plus CSS 变量覆盖, Google Fonts (Inter/JetBrains Mono)

---

### Task A: 更新设计令牌 + 字体 CDN

**Files:**
- Modify: `src/styles/variables.css` — 完整设计令牌体系
- Modify: `index.html` — Inter + JetBrains Mono CDN
- Modify: `src/styles/base.css` — 简化

- [ ] **Step 1: 覆盖 `src/styles/variables.css`**

写入完整的令牌体系，包含颜色、排版、间距、圆角、阴影、过渡、z-index：

```css
:root {
  --color-primary: oklch(0.45 0.12 260);
  --color-primary-hover: oklch(0.4 0.12 260);
  --color-primary-light: oklch(0.65 0.08 260);
  --color-primary-bg: oklch(0.94 0.03 260);
  --color-bg: oklch(0.97 0.005 260);
  --color-surface: oklch(0.99 0 0);
  --color-card: oklch(0.985 0.003 260);
  --color-border: oklch(0.91 0.008 260);
  --color-border-light: oklch(0.94 0.005 260);
  --color-text: oklch(0.15 0.015 260);
  --color-text-secondary: oklch(0.5 0.02 260);
  --color-text-tertiary: oklch(0.68 0.015 260);
  --color-accent: oklch(0.6 0.2 40);
  --color-success: oklch(0.55 0.16 150);
  --color-danger: oklch(0.5 0.2 25);
  --color-warning: oklch(0.65 0.16 80);

  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;

  --text-xs: 0.75rem;
  --text-sm: 0.8125rem;
  --text-base: 0.875rem;
  --text-md: 1rem;
  --text-lg: 1.125rem;
  --text-xl: 1.25rem;
  --text-2xl: 1.5rem;
  --text-3xl: 1.875rem;

  --leading-tight: 1.25;
  --leading-normal: 1.5;
  --leading-relaxed: 1.75;

  --tracking-tight: -0.01em;
  --tracking-normal: 0;
  --tracking-wide: 0.02em;

  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-10: 2.5rem;
  --space-12: 3rem;
  --space-16: 4rem;
  --space-20: 5rem;

  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-2xl: 20px;

  --shadow-sm: 0 1px 3px oklch(0 0 0 / 0.06);
  --shadow-md: 0 4px 12px oklch(0 0 0 / 0.07);
  --shadow-lg: 0 8px 30px oklch(0 0 0 / 0.09);
  --shadow-xl: 0 20px 60px oklch(0 0 0 / 0.12);

  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 250ms cubic-bezier(0.4, 0, 0.2, 1);
  --easing-spring: cubic-bezier(0.34, 1.56, 0.64, 1);

  --z-dropdown: 100;
  --z-sticky: 200;
  --z-modal: 300;
  --z-toast: 400;
}
```

- [ ] **Step 2: 更新 `index.html` 添加字体 CDN**

在 `<head>` 中添加：

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
```

- [ ] **Step 3: 简化 `base.css`**

移除 Tailwind 导入（已不依赖 Tailwind）：

```css
*, *::before, *::after {
  box-sizing: border-box;
}

body {
  font-family: var(--font-sans);
  font-size: var(--text-base);
  line-height: var(--leading-normal);
  color: var(--color-text);
  background: var(--color-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
```

---

### Task B: Element Plus 全局主题覆盖

**Files:**
- Modify: `src/App.vue` — `<style>` 块替换

- [ ] **Step 1: 更新 `App.vue` 全局样式**

将 `<style>` 块替换为完整的 Element Plus 主题覆盖：

```css
:root {
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: var(--color-primary-light);
  --el-color-primary-light-5: oklch(0.75 0.06 260);
  --el-color-primary-light-7: oklch(0.85 0.04 260);
  --el-color-primary-dark-2: var(--color-primary-hover);
  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger: var(--color-danger);
  --el-color-info: var(--color-text-tertiary);

  --el-text-color-primary: var(--color-text);
  --el-text-color-regular: var(--color-text-secondary);
  --el-text-color-secondary: var(--color-text-tertiary);
  --el-text-color-placeholder: var(--color-text-tertiary);

  --el-bg-color: white;
  --el-bg-color-page: var(--color-bg);
  --el-bg-color-overlay: white;
  --el-border-color: var(--color-border);
  --el-border-color-light: var(--color-border-light);
  --el-border-color-lighter: oklch(0.95 0.003 260);
  --el-border-radius-base: var(--radius-md);
  --el-border-radius-small: var(--radius-sm);

  --el-font-size-base: var(--text-base);
  --el-font-size-small: var(--text-sm);
  --el-font-size-large: var(--text-md);
  --el-font-family: var(--font-sans);
  --el-font-weight-primary: 500;

  --el-fill-color: var(--color-card);
  --el-fill-color-light: oklch(0.96 0.005 260);
  --el-fill-color-blank: white;
}

.el-table {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: var(--color-card);
  --el-table-row-hover-bg-color: var(--color-primary-bg);
  --el-table-header-text-color: var(--color-text-secondary);
}

.el-table th.el-table__cell {
  font-weight: 600;
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--color-text-secondary);
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background: oklch(0.985 0.003 260);
}

.el-dialog {
  --el-dialog-border-radius: var(--radius-xl);
}

.el-dialog__header {
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
  margin: 0;
}

.el-dialog__title {
  font-size: var(--text-lg);
  font-weight: 600;
}

.el-dialog__body {
  padding: var(--space-6);
}

.el-dialog__footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-card);
}

.el-form-item__label {
  font-weight: 500;
  font-size: var(--text-sm);
  color: var(--color-text);
  padding-bottom: var(--space-1);
}

.el-input__wrapper {
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
  border-radius: var(--radius-md);
}

.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--color-primary-light) inset;
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
  --el-button-active-bg-color: var(--color-primary-hover);
}

.el-button--small {
  border-radius: var(--radius-md);
}

.el-tag {
  --el-tag-border-radius: var(--radius-md);
}

.el-card {
  --el-card-border-radius: var(--radius-lg);
  --el-card-padding: var(--space-6);
  border: 1px solid var(--color-border-light);
}

.el-pagination {
  --el-pagination-button-bg-color: transparent;
}

/* 全局过渡 */
* {
  transition-property: background-color, border-color, color, opacity, box-shadow;
  transition-duration: var(--transition-fast);
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

a, button, [role="button"] {
  cursor: pointer;
}
```

---

### Task C: 更新布局组件样式

**Files:**
- Modify: `src/layout/AppLayout.vue` — 布局调整
- Modify: `src/layout/AppSidebar.vue` — 精化侧栏
- Modify: `src/layout/AppTopBar.vue` — 精化顶栏

- [ ] **Step 1: 更新 `AppSidebar.vue` 样式**

侧栏保持白色背景，加入视觉层次。关键变化：
- 左侧选中态使用 2px 品牌色条（覆盖 el-menu 的默认指示器）
- 底部用户区域浅色背景分隔

```scss
.sidebar {
  width: 220px;
  display: flex;
  flex-direction: column;
  background: white;
  border-right: 1px solid var(--color-border-light);
  transition: width var(--transition-fast);
  overflow: hidden;
}

.sidebar--collapsed { width: 64px; }

.sidebar__header {
  padding: var(--space-4) var(--space-5);
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--color-border-light);
}

.sidebar__logo {
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--color-primary);
  white-space: nowrap;
}

.sidebar__menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

/* el-menu 紧凑 + 选中态左侧色条 */
.sidebar__menu .el-menu-item {
  height: 40px;
  line-height: 40px;
  margin: 1px var(--space-2);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.sidebar__menu .el-menu-item.is-active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-weight: 500;
  box-shadow: inset 2px 0 0 var(--color-primary);
}

.sidebar__menu .el-menu-item:hover {
  background: var(--color-card);
}

.sidebar__menu .el-sub-menu__title {
  height: 40px;
  line-height: 40px;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 1px var(--space-2);
  border-radius: var(--radius-md);
}

.sidebar__footer {
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-card);
}

.sidebar__user {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.sidebar__user-name {
  font-size: var(--text-sm);
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

- [ ] **Step 2: 更新 `AppTopBar.vue` 样式**

```scss
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-4);
  background: white;
  border-bottom: 1px solid var(--color-border-light);
  min-height: 48px;
}

.topbar__left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.topbar__right {
  display: flex;
  align-items: center;
}
```

- [ ] **Step 3: 更新 `AppLayout.vue` 样式**

```scss
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 0;
  background: var(--color-bg);
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
}
```

---

### Task D: 构建验证

- [ ] **Step 1: 运行构建**

```bash
npm run build
```

Expected: exit 0, 无错误

- [ ] **Step 2: 检查字体加载**

确认 `index.html` 中 Inter + JetBrains Mono 的 `<link>` 标签存在且格式正确。
