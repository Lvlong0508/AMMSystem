# Chat 页面输入框固定底部布局设计

## 问题

Chat 页面输入框在有消息时跟随消息列表滚动，未固定在视口底部。要求：
- 空状态：欢迎语 + 输入框整体垂直居中
- 有消息：输入框永不滚动，始终固定在浏览器视口底部
- 侧边栏折叠/展开时输入框宽度自适应

## 架构

### 三层 z-index

| 层 | 元素 | z-index | 定位 |
|---|---|---|---|
| Layer 1 | `.app-bg` (Vanta) | 0 | `position: fixed; inset: 0` |
| Layer 2 | `.app-layout` | 1 | `position: relative` |
| Layer 3 | `.chat-view__input-bar` | 100 | `position: fixed; bottom: 0` |

### 侧边栏宽度传递

AppLayout 通过 inline style 将侧边栏宽度暴露为 CSS 自定义属性：

```html
<div class="app-layout" :style="{ '--sidebar-width': sidebarCollapsed ? '72px' : '240px' }">
```

ChatView 的 scoped CSS 用 `var(--sidebar-width)` 读取，实现响应式宽度。

## 状态设计

### 空状态 (`chat-view--empty`)

```
.chat-view (display: flex; flex-direction: column; justify-content: center)
  └── .chat-view__messages (flex: 0 0 auto)  ← 欢迎语，内容自适应
  └── .chat-view__input-bar (normal flow)     ← 正常流，随 flex 居中
```

- 输入框在正常 flex 流中，作为第二个子元素与欢迎语一起垂直居中
- `justify-content: center` 将两者作为一组居中

### 有消息 (`chat-view--has-messages`)

```
.chat-view (display: flex; flex-direction: column)
  └── .chat-view__messages (flex: 1; overflow-y: auto)  ← 消息列表滚动
  └── .chat-view__input-bar (position: fixed)           ← 脱离文档流，固定在视口底部
```

- 输入框 `position: fixed; bottom: 0; left: var(--sidebar-width); right: 0; z-index: 100`
- `.chat-view__messages` 加 `padding-bottom: 112px` 防止最后一条消息被输入框遮挡
- 消息列表内部滚动，输入框不受影响

### 状态切换动画

输入框从正常流切换到 `position: fixed` 时，CSS transition 无法跨定位模式插值。使用 `@keyframes` 动画做柔和过渡：

```css
.chat-view--has-messages .chat-view__input-bar {
  animation: inputBarFix 300ms ease-out;
}
@keyframes inputBarFix {
  from { opacity: 0.5; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}
```

侧边栏折叠/展开时 `left` 值通过 CSS transition 平滑变化：

```css
.chat-view--has-messages .chat-view__input-bar {
  transition: left 250ms ease-out;
}
```

## 变更文件

| 文件 | 变更内容 |
|---|---|
| `src/layout/AppLayout/AppLayout.vue` | `.app-layout` 添加 `:style` 绑定 `--sidebar-width` |
| `src/views/Chat/ChatView/ChatView.css` | 输入框 `position: fixed` + 动画；空状态 flex 居中；messages padding-bottom |
| 其余文件 | 无变更 |

## 不涉及的变更

- `useChatView.js`：已清理干净，Vanta 逻辑已在 AppLayout
- `AppLayout.css`：z-index 分层已就绪
- `ChatView.vue`：模板/script 无需改动

## 边界情况

- **侧边栏折叠中发送消息**：`--sidebar-width` 实时更新，`transition` 平滑跟随
- **快速切换路由**：输入框仅 Chat 路由下显示，其他路由不受影响
- **欢迎语→消息切换**：输入框从居中位置通过动画过渡到视口底部
- **消息→清空（新对话）**：输入框从 fixed 回到正常流，由 Vue 的 class 切换自动处理
