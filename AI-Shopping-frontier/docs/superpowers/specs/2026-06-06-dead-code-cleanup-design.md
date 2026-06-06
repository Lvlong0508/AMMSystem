# Dead Code Cleanup — frontier-user

## Overview
Remove dead/unused code from the Vue 3 frontend to improve maintainability and reduce bundle size.

## Items to Clean

### 1. Delete build artifact
- `src/output.css` (951 lines) — generated Tailwind build output, duplicates `style.css`
- Add `output.css` to `.gitignore`

### 2. Remove dead composable call
- `ChatBubble.vue:33` — `const {} = useChatBubble(...)` destructures nothing, entire call is dead
- `useChatBubble.js` — composable file becomes unused

### 3. Delete empty Text.js files
Files exporting empty objects, never referenced:
- `components/ChatBubble/Text.js`
- `components/Skeleton/Text.js`
- `components/Toast/Text.js`
- `components/StatusTag/Text.js` — also remove import from `StatusTag.vue`

### 4. Uninstall unused npm dependencies
- `motion-v` (production)
- `class-variance-authority` (dev)
- `clsx` (dev)
- `tailwind-merge` (dev)
- `vite-plugin-vue-devtools` (dev)

### 5. Remove empty stub functions
- `useContactView.js` — `handleEdit`
- `useOrderList.js` — `handleViewLogistics`, `handleReview`
- `useOrderDetail.js` — `handleViewLogistics`, `handleReview`

### 6. Delete unused ActionSheet component
- `components/ActionSheet/` — entire component, never imported anywhere

### 7. Remove unused CSS
- `ContactView.css:456` — `.form-modal__textarea` class definition

### 8. Remove unused methods
- `ContactView.vue` — `onProvinceChange()`, `onCityChange()`

## Excluded (保留)
- `auth.js` exports `checkUsername`/`checkPhone` — kept for future use
