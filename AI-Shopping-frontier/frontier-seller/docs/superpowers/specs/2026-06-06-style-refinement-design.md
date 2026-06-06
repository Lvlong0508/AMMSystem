# AI-Mart 商家端 · 样式视觉精化设计

## 概述

在 Element Plus 全面引入的基础上，针对目前样式粗放的问题进行系统性精化。目标：从"能用"提升到"精致、连贯、高级"（Premium Admin 方向）。

## 变更范围

| 文件 | 操作 |
|------|------|
| `src/styles/variables.css` | 重写：补充完整的设计令牌体系 |
| `src/App.vue` | 更新 `<style>`：Element Plus 全局主题 + 过渡设置 |
| `src/layout/AppSidebar.vue` | 更新 `<style>`：深层样式覆盖，视觉精化 |
| `src/layout/AppTopBar.vue` | 更新 `<style>`：尺寸与线条精化 |
| `src/layout/AppLayout.vue` | 更新 `<style>`：微调布局 |
| `src/styles/base.css` | 简化，移除非必要的 Tailwind 导入 |
| `index.html` | 添加 Inter 字体 CDN 链接、JetBrains Mono |

不变的文件：所有页面级 `*.vue` / `*.css` 文件（页面内部布局和组件组合不受影响）。

## 设计令牌

### 颜色系统

| 令牌 | 值 | 用途 |
|------|-----|------|
| `--color-primary` | `oklch(0.45 0.12 260)` | 主色 — 深蓝灰 |
| `--color-primary-hover` | `oklch(0.4 0.12 260)` | 主色 hover |
| `--color-primary-light` | `oklch(0.65 0.08 260)` | 主色浅变体 |
| `--color-primary-bg` | `oklch(0.94 0.03 260)` | 主色浅背景 |
| `--color-bg` | `oklch(0.97 0.005 260)` | 页面背景 |
| `--color-surface` | `oklch(0.99 0 0)` | 内容区域 |
| `--color-card` | `oklch(0.985 0.003 260)` | 卡片背景 |
| `--color-border` | `oklch(0.91 0.008 260)` | 主要边框 |
| `--color-border-light` | `oklch(0.94 0.005 260)` | 次要边框 |
| `--color-text` | `oklch(0.15 0.015 260)` | 主文字 |
| `--color-text-secondary` | `oklch(0.5 0.02 260)` | 次要文字 |
| `--color-text-tertiary` | `oklch(0.68 0.015 260)` | 辅助文字 |
| `--color-accent` | `oklch(0.6 0.2 40)` | 强调色 |
| `--color-success` | `oklch(0.55 0.16 150)` | 成功 |
| `--color-danger` | `oklch(0.5 0.2 25)` | 危险 |
| `--color-warning` | `oklch(0.65 0.16 80)` | 警告 |

### 排版

- 字体：Inter（标题/正文），JetBrains Mono（等宽数据）
- 基础字号：14px（`--text-base: 0.875rem`）
- 完整层级：xs(12) / sm(13) / base(14) / md(16) / lg(18) / xl(20) / 2xl(24) / 3xl(30)
- 行高令牌：`--leading-tight(1.25)` / `--leading-normal(1.5)` / `--leading-relaxed(1.75)`
- 字距令牌：`--tracking-tight(-0.01em)` / `--tracking-normal(0)` / `--tracking-wide(0.02em)`

### 间距（8pt 网格）

`--space-1` 到 `--space-20`，新增 `--space-5(1.25rem)`、`--space-10(2.5rem)`、`--space-16(4rem)`、`--space-20(5rem)`

### 圆角

sm(4px) / md(8px) / lg(12px) / xl(16px) / 2xl(20px)

### 阴影层级

sm / md / lg / xl —— 使用 `oklch(0 0 0 / opacity)` 代替 rgba

### 过渡

- `--transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1)`
- `--transition-normal: 250ms ...`
- `--easing-spring: cubic-bezier(0.34, 1.56, 0.64, 1)`

## Element Plus 全局主题覆盖

在 `App.vue` 的 `<style>` 中覆盖 Element Plus CSS 变量（见 Part 3 的具体变量表），覆盖范围包括：

- 颜色令牌全部映射到 `--color-*` 变量
- 表格：表头加粗小字 + 交替行底色 + hover 行品牌色背景
- 对话框：头部/主体/底部清晰分界，圆角 16px
- 表单：输入框边框 hover 品牌色、聚焦品牌色
- 按钮：主按钮映射品牌色 + hover 变深
- 卡片：1px 细边框替代纯阴影
- 分页器：精简样式

## 布局视觉精化

### 侧栏
- 保持白色背景，底部加细分隔线
- 菜单紧凑模式
- 选中态左侧 2px `--color-primary` 色条
- 底部用户区浅灰背景区分

### 顶栏
- 高度紧凑 48px
- 面包屑 + 店铺选择器右对齐

### 页面内容
- `el-card` 保持默认内边距
- 表格直接放在卡片内，形成清晰层级

## 执行计划

共 3 个独立任务，可并行执行：

1. **Task A**: 更新 `variables.css` + `index.html`（字体 CDN）
2. **Task B**: 更新 `App.vue`（Element Plus 全局主题 + 过渡）
3. **Task C**: 更新布局组件样式（AppSidebar / AppTopBar / AppLayout CSS）
4. **清理**: 更新 `base.css`（移除 Tailwind 中不再需要的部分）

## 验收标准

- [ ] `npm run build` 通过
- [ ] Inter 字体正确加载
- [ ] 表格表头小字大写 + 交替行
- [ ] 输入框 hover/focus 品牌色边框
- [ ] 对话框圆角 16px + 头体尾分界
- [ ] 侧栏选中态色条
- [ ] 所有 Element Plus 组件颜色与设计令牌一致
