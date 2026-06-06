# Dead Code Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all dead/unused code from frontier-user frontend

**Architecture:** 8 independent cleanup steps, each producing a working state. Order: delete artifact → remove dead JS → remove dead components → uninstall deps → remove dead CSS → commit.

**Tech Stack:** Vue 3, Vite, vanilla JS

---

### Task 1: Delete generated build artifact + .gitignore

**Files:**
- Delete: `frontier-user/src/output.css`
- Modify: `frontier-user/.gitignore`

- [ ] **Step 1: Delete output.css and update .gitignore**

Delete the file:
```bash
git rm frontier-user/src/output.css
```

Add to `frontier-user/.gitignore`:
```
# build output
output.css
```

- [ ] **Step 2: Commit**

```bash
git add frontier-user/.gitignore
git commit -m "chore: remove committed build artifact output.css"
```

---

### Task 2: Remove dead composable in ChatBubble + delete empty Text.js files

**Files:**
- Modify: `frontier-user/src/components/ChatBubble/ChatBubble.vue` — remove dead `useChatBubble` call
- Delete: `frontier-user/src/components/ChatBubble/useChatBubble.js`
- Delete: `frontier-user/src/components/ChatBubble/Text.js`
- Delete: `frontier-user/src/components/Skeleton/Text.js`
- Delete: `frontier-user/src/components/Toast/Text.js`
- Modify: `frontier-user/src/components/StatusTag/StatusTag.vue` — remove unused import of `STATUS_TAG_TEXT`
- Delete: `frontier-user/src/components/StatusTag/Text.js`

- [ ] **Step 1: Remove dead useChatBubble call in ChatBubble.vue**

In `ChatBubble.vue`:
- Delete line: `const {} = useChatBubble({ role: 'ai' })`
- Remove `useChatBubble` from import line

- [ ] **Step 2: Delete unused files**

```bash
git rm frontier-user/src/components/ChatBubble/useChatBubble.js
git rm frontier-user/src/components/ChatBubble/Text.js
git rm frontier-user/src/components/Skeleton/Text.js
git rm frontier-user/src/components/Toast/Text.js
```

- [ ] **Step 3: Remove STATUS_TAG_TEXT import from StatusTag.vue**

In `StatusTag.vue`, remove the line:
```js
import { STATUS_TAG_TEXT as T } from './Text';
```
And delete the now-empty import.

Then delete Text.js:
```bash
git rm frontier-user/src/components/StatusTag/Text.js
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove dead composable call and empty Text.js files"
```

---

### Task 3: Remove empty stub functions

**Files:**
- Modify: `frontier-user/src/views/Contact/ContactView/useContactView.js` — remove `handleEdit`
- Modify: `frontier-user/src/views/Order/OrderListView/useOrderList.js` — remove `handleViewLogistics`, `handleReview`
- Modify: `frontier-user/src/views/Order/OrderDetailView/useOrderDetail.js` — remove `handleViewLogistics`, `handleReview`

- [ ] **Step 1: Remove empty functions from useContactView.js**

Delete the `handleEdit` function and its return entry.

- [ ] **Step 2: Remove empty functions from useOrderList.js**

Delete `handleViewLogistics` and `handleReview` functions and their return entries.

- [ ] **Step 3: Remove empty functions from useOrderDetail.js**

Delete `handleViewLogistics` and `handleReview` functions and their return entries.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove empty stub functions"
```

---

### Task 4: Delete unused ActionSheet component

**Files:**
- Delete: `frontier-user/src/components/ActionSheet/` (entire directory)

- [ ] **Step 1: Delete ActionSheet component**

```bash
git rm -r frontier-user/src/components/ActionSheet/
```

- [ ] **Step 2: Commit**

```bash
git commit -m "refactor: remove unused ActionSheet component"
```

---

### Task 5: Remove unused CSS

**Files:**
- Modify: `frontier-user/src/views/Contact/ContactView/ContactView.css` — remove `.form-modal__textarea` class

- [ ] **Step 1: Remove unused CSS class**

In `ContactView.css`, delete the `.form-modal__textarea { ... }` block.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "refactor: remove unused .form-modal__textarea CSS"
```

---

### Task 6: Remove unused methods in ContactView.vue

**Files:**
- Modify: `frontier-user/src/views/Contact/ContactView/ContactView.vue` — remove `onProvinceChange`, `onCityChange`

- [ ] **Step 1: Remove unused methods**

In `ContactView.vue`, delete the `onProvinceChange()` and `onCityChange()` method definitions and their script references.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "refactor: remove unused onProvinceChange/onCityChange methods"
```

---

### Task 7: Uninstall unused npm dependencies

**Files:**
- Modify: `frontier-user/package.json` (via npm uninstall)

- [ ] **Step 1: Uninstall dependencies**

```bash
npm uninstall motion-v
npm uninstall class-variance-authority clsx tailwind-merge vite-plugin-vue-devtools
```

- [ ] **Step 2: Verify project still runs**

```bash
npm run build
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove 5 unused npm dependencies"
```
