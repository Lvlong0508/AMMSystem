# Chat 输入框固定底部布局 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chat 页面输入框空状态居中、有消息时 `position: fixed` 固定在视口底部，永不跟随消息滚动。

**Architecture:** 三层 z-index（bg:0 / page:1 / input-bar:100）；AppLayout 通过 inline style 暴露 `--sidebar-width` 变量给子组件；空状态 flex 居中，有消息时输入框切 `position: fixed` 并附带入场动画。

**Tech Stack:** Vue 3, CSS (scoped), CSS custom properties, CSS keyframes

---

### Task 1: AppLayout 暴露侧边栏宽度 CSS 变量

**Files:**
- Modify: `frontier-user/src/layout/AppLayout/AppLayout.vue:4`（template 中的 `.app-layout` 行）

- [ ] **Step 1: 在 `.app-layout` 上添加 inline style 绑定**

在 `AppLayout.vue` 模板中找到 `<div class="app-layout">` 行，添加 `:style`：

```diff
-    <div class="app-layout" :class="{ 'app-layout--sidebar-collapsed': sidebarCollapsed }">
+    <div class="app-layout" :class="{ 'app-layout--sidebar-collapsed': sidebarCollapsed }" :style="{ '--sidebar-width': sidebarCollapsed ? '72px' : '240px' }">
```

- [ ] **Step 2: 验证构建**

Run: `cd frontier-user; npm run build`
Expected: 构建成功，无报错

- [ ] **Step 3: Commit**

```bash
git add frontier-user/src/layout/AppLayout/AppLayout.vue
git commit -m "feat: expose --sidebar-width CSS variable from AppLayout"
```

---

### Task 2: 重写 ChatView 输入框定位方案

**Files:**
- Modify: `frontier-user/src/views/Chat/ChatView/ChatView.css`（全量替换核心布局段）

- [ ] **Step 1: 重写 CSS 布局段**

将 ChatView.css 中 `.chat-view`、`.chat-view__messages`、`.chat-view__input-bar` 相关规则替换为以下内容（保留气泡/欢迎语/按钮/动画等不涉及的规则不动）：

```css
/* ---- 容器 ---- */
.chat-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  min-height: 0;
  background: transparent;
  font-family: var(--font-body);
}

.chat-view--empty {
  justify-content: center;
}

/* ---- 消息列表（可滚动） ---- */
.chat-view__messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.chat-view__messages::-webkit-scrollbar {
  display: none;
}
.chat-view--empty .chat-view__messages {
  flex: 0 0 auto;
  overflow: visible;
  padding-bottom: 24px;
}
.chat-view--has-messages .chat-view__messages {
  padding-bottom: 112px;
}

/* ---- 输入框 ---- */
.chat-view__input-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px 20px;
  background: transparent;
}
.chat-view--empty .chat-view__input-bar {
  justify-content: center;
}
.chat-view--has-messages .chat-view__input-bar {
  position: fixed;
  bottom: 0;
  left: var(--sidebar-width);
  right: 0;
  z-index: 100;
  transition: left 250ms ease-out;
  animation: inputBarFix 300ms ease-out;
}

@keyframes inputBarFix {
  from { opacity: 0.5; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
```

**具体操作**：在 ChatView.css 中：
1. 删除 `.chat-view--has-messages` 原有的空规则块（如果有）
2. 将 `.chat-view__input-bar` 及其相关规则替换为上方的版本
3. 保留第 37 行之后的所有规则（welcome、bubble、send-btn、keyframes 等）不变

- [ ] **Step 2: 验证构建**

Run: `cd frontier-user; npm run build`
Expected: 构建成功，无报错

- [ ] **Step 3: Commit**

```bash
git add frontier-user/src/views/Chat/ChatView/ChatView.css
git commit -m "feat: fixed input bar at viewport bottom with animation"
```
