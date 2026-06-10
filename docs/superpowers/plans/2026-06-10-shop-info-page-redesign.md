# 商店信息页重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将商家端商店信息页从内联编辑改为「纯文本展示 + 弹窗编辑」模式，Logo 居中顶部展示

**Architecture:** 保持 Vue 3 + Element Plus 架构，展示层用 el-descriptions 替代 el-form，编辑层用 el-dialog + el-form，开关店用 el-switch 替代按钮

**Tech Stack:** Vue 3 (Composition API), Element Plus, Vue Router, Axios

---

### Task 1: Text.js — 新增常量

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/Text.js`

- [ ] **Step 1: 在 Text.js 末尾新增以下常量**

```js
export const BTN_EDIT = '编辑'
export const DIALOG_TITLE = '编辑商店信息'
export const BTN_CANCEL = '取消'
export const LABEL_STATUS = '营业状态'
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/Text.js
git commit -m "feat: add new constants for shop info redesign"
```

---

### Task 2: ShopInfo.css — 重写样式

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.css`

- [ ] **Step 1: 将原有 CSS 替换为以下内容**

```css
.shop-info {
  max-width: 720px;
  margin: 0 auto;
}

.shop-info__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.shop-info__title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
  margin-right: auto;
}

.shop-info__logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.shop-info__logo-avatar {
  border: 2px solid var(--border-color, #dcdfe6);
}

.shop-info__edit-btn {
  flex-shrink: 0;
}

.shop-info__edit-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.shop-info__edit-logo-preview {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 1px solid var(--border-color, #dcdfe6);
  overflow: hidden;
  flex-shrink: 0;
}

.shop-info__edit-logo-name {
  font-size: var(--text-sm, 14px);
  color: var(--text-secondary, #909399);
}
```

- [ ] **Step 2: 提交**

```bash
git add AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.css
git commit -m "feat: rewrite ShopInfo styles for new layout"
```

---

### Task 3: ShopInfo.js — 新增弹窗与切换逻辑

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.js`

- [ ] **Step 1: 修改 import，新增 ref 和 reactive 导入**

保持现有 imports，新增不需要额外导入（ref, reactive, onMounted 已存在）。

- [ ] **Step 2: 在 useShopInfo 函数中新增弹窗状态变量**

在 `const logoPreview = ref('')` 之后添加：

```js
const dialogVisible = ref(false)
const editForm = reactive({
  name: '',
  description: '',
  phone: '',
  region: [],
  addressDetail: ''
})
const editLogoFile = ref(null)
const editLogoInputRef = ref(null)
```

- [ ] **Step 3: 新增 openEditDialog 函数**

在 `clearLogo` 函数之后添加：

```js
function openEditDialog() {
  editForm.name = form.name
  editForm.description = form.description
  editForm.phone = form.phone
  editForm.region = [...form.region]
  editForm.addressDetail = form.addressDetail
  editLogoFile.value = null
  dialogVisible.value = true
}
```

- [ ] **Step 4: 新增 handleEditSave 函数**

在 `handleToggleStatus` 之前添加：

```js
async function handleEditSave() {
  saving.value = true
  try {
    const fd = new FormData()
    const shopData = {
      name: editForm.name,
      description: editForm.description,
      phone: editForm.phone,
      address: buildAddressString(editForm.region, editForm.addressDetail)
    }
    fd.append('shop', new Blob([JSON.stringify(shopData)], { type: 'application/json' }))
    if (editLogoFile.value) {
      fd.append('logo', editLogoFile.value)
    }
    const res = await updateShop(shopId, fd)
    ElMessage.success(res?.message || T.SAVE_SUCCESS)
    // sync back to display form
    form.name = editForm.name
    form.description = editForm.description
    form.phone = editForm.phone
    form.region = [...editForm.region]
    form.addressDetail = editForm.addressDetail
    if (editLogoFile.value) {
      // refresh logo from server
      loadShopInfo()
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error(T.SAVE_FAILED)
  } finally {
    saving.value = false
  }
}
```

- [ ] **Step 5: 将 handleToggleStatus 改为 el-switch 风格**

替换原有 `handleToggleStatus`：

```js
async function handleToggleStatus() {
  toggling.value = true
  try {
    if (shopStatus.value === 1) {
      await closeShop(shopId)
      shopStatus.value = 0
    } else {
      await openShop(shopId)
      shopStatus.value = 1
    }
    ElMessage.success(T.STATUS_TOGGLED)
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    toggling.value = false
  }
}
```

- [ ] **Step 6: 新增 handleEditLogoChange 和 clearEditLogo 函数**

在 `openEditDialog` 之后添加：

```js
function handleEditLogoChange(e) {
  const file = e.target.files[0]
  if (!file) {
    editLogoFile.value = null
    return
  }
  const ext = getExtension(file.name)
  if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
    ElMessage.warning(T.LOGO_TYPE_INVALID)
    clearEditLogo()
    return
  }
  editLogoFile.value = file
}

function clearEditLogo() {
  editLogoFile.value = null
  if (editLogoInputRef.value) {
    editLogoInputRef.value.value = ''
  }
}
```

- [ ] **Step 7: 更新 return 语句**

```js
return {
  T, form, loading, saving, toggling, shopStatus, logoPreview, logoFile, logoInputRef,
  handleLogoChange, clearLogo, handleSave, handleToggleStatus,
  dialogVisible, editForm, editLogoFile, editLogoInputRef,
  openEditDialog, handleEditSave, handleEditLogoChange, clearEditLogo
}
```

- [ ] **Step 8: 提交**

```bash
git add AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.js
git commit -m "feat: add edit dialog and switch toggle logic to ShopInfo"
```

---

### Task 4: ShopInfo.vue — 重写模板

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.vue`

- [ ] **Step 1: 将 template 替换为以下内容**

```vue
<template>
  <div class="shop-info">
    <div class="shop-info__header">
      <h2 class="shop-info__title">{{ T.PAGE_TITLE }}</h2>
      <el-switch
        :model-value="shopStatus === 1"
        :loading="toggling"
        :disabled="toggling"
        inline-prompt
        active-text="营业中"
        inactive-text="已关闭"
        @change="handleToggleStatus"
      />
      <el-button class="shop-info__edit-btn" type="primary" @click="openEditDialog">
        {{ T.BTN_EDIT }}
      </el-button>
    </div>

    <el-card v-loading="loading" shadow="never">
      <div v-if="logoPreview" class="shop-info__logo">
        <el-avatar :size="100" shape="circle" class="shop-info__logo-avatar">
          <el-image :src="logoPreview" fit="cover" style="width: 100%; height: 100%" />
        </el-avatar>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item :label="T.LABEL_NAME">
          {{ form.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_DESC">
          {{ form.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_PHONE">
          {{ form.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_REGION">
          {{ form.region.length ? form.region.join(' / ') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_ADDRESS">
          {{ form.addressDetail || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="T.DIALOG_TITLE" width="600px">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="editForm.name" :placeholder="T.PLACEHOLDER_NAME" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="editForm.description" type="textarea" :rows="3" :placeholder="T.PLACEHOLDER_DESC" :maxlength="500" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE">
          <el-input v-model="editForm.phone" :placeholder="T.PLACEHOLDER_PHONE" :maxlength="20" />
        </el-form-item>
        <el-form-item :label="T.LABEL_REGION">
          <el-cascader
            v-model="editForm.region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover' }"
            :placeholder="T.PLACEHOLDER_REGION"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_ADDRESS">
          <el-input v-model="editForm.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS" :maxlength="200" />
        </el-form-item>
        <el-form-item :label="T.LABEL_LOGO">
          <div class="shop-info__edit-logo">
            <el-image
              v-if="logoPreview && !editLogoFile"
              :src="logoPreview"
              class="shop-info__edit-logo-preview"
              fit="cover"
            />
            <input ref="editLogoInputRef" type="file" accept=".jpg,.png" @change="handleEditLogoChange" />
            <span v-if="editLogoFile" class="shop-info__edit-logo-name">{{ editLogoFile.name }}</span>
            <el-button v-if="editLogoFile" type="danger" link @click="clearEditLogo">{{ T.BTN_CLEAR_LOGO }}</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleEditSave">
          {{ saving ? T.BTN_SAVING : T.BTN_SAVE }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 2: 更新 script 标签中的解构**

更新 `const { ... } = useShopInfo()` 解构行，加入新增的返回值：

```vue
<script setup>
import { useShopInfo } from './ShopInfo.js'
import { regionData } from 'element-china-area-data'

const regionOptions = regionData

const {
  T, form, loading, saving, toggling, shopStatus, logoPreview,
  dialogVisible, editForm, editLogoFile, editLogoInputRef,
  openEditDialog, handleEditSave, handleEditLogoChange, clearEditLogo, handleToggleStatus
} = useShopInfo()
</script>
```

- [ ] **Step 3: 提交**

```bash
git add AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.vue
git commit -m "feat: rewrite ShopInfo template with descriptions display and edit dialog"
```

---

### Task 5: 验证

- [ ] **Step 1: 启动开发服务器并检查页面**

```bash
# 在 frontier-seller 目录下
npm run dev
```

检查：
- 页面加载正常，无报错
- Logo 在顶部居中展示（圆形头像）
- 信息以 el-descriptions 纯文本展示
- el-switch 显示当前营业状态并可切换
- 点击「编辑」弹出 dialog，表单自动填充当前数据
- 保存后数据正确更新
- 取消后数据不变

- [ ] **Step 2: 提交最终验证**

```bash
git add -A
git commit -m "feat: redesign shop info page with display mode and edit dialog"
```
