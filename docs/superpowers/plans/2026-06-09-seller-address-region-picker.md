# 卖家端地址改为省市区选择器 + 详细地址 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将卖家端三个页面（店铺注册、店铺信息编辑、地址管理）的纯文本地址输入框，改为省市区级联选择器 + 详细地址输入框的拆分模式

**Architecture:** 新增 `element-china-area-data` 依赖，`el-cascader` 做省市区三级联动，提交时将选中标签 + 详细地址拼接为完整地址字符串

**Tech Stack:** Vue 3 + Element Plus + element-china-area-data

---

### Task 1: 安装依赖 + 创建地址工具函数

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/package.json`
- Create: `AI-Shopping-frontier/frontier-seller/src/utils/region.js`

- [ ] **Step 1: 安装 element-china-area-data**

```bash
npm install element-china-area-data
```

Expected: added to `dependencies` in `package.json`.

- [ ] **Step 2: 创建 `src/utils/region.js`**

```js
import { regionData } from 'element-china-area-data'

function getRegionLabels(values) {
  let current = regionData
  const labels = []
  for (const v of values) {
    const node = current.find(n => n.value === v)
    if (!node) break
    labels.push(node.label)
    current = node.children || []
  }
  return labels.join('')
}

export function parseAddress(address) {
  if (!address) return { region: [], detail: '' }
  for (const p of regionData) {
    if (address.startsWith(p.label)) {
      let rest = address.slice(p.label.length)
      for (const c of (p.children || [])) {
        if (rest.startsWith(c.label)) {
          rest = rest.slice(c.label.length)
          for (const d of (c.children || [])) {
            if (rest.startsWith(d.label)) {
              return { region: [p.value, c.value, d.value], detail: rest.slice(d.label.length) }
            }
          }
          return { region: [p.value, c.value], detail: rest }
        }
      }
      return { region: [p.value], detail: rest }
    }
  }
  return { region: [], detail: address }
}

export function buildAddressString(values, detail) {
  if (!values || values.length === 0) return detail || ''
  const regionStr = getRegionLabels(values)
  return regionStr + (detail || '')
}
```

- [ ] **Step 3: 确认 `element-china-area-data` 已在 package.json 中**

```bash
node -e "console.log(require('./package.json').dependencies['element-china-area-data'])"
```

Expected: version string printed.

---

### Task 2: 更新 Register.vue — 店铺注册表单

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/Register/Register.vue`
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/Register/Register.js`
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/Register/Text.js`

- [ ] **Step 1: Text.js — 新增省市区相关文本常量**

```js
export const LABEL_SHOP_REGION = '省/市/区'
export const PLACEHOLDER_SHOP_REGION = '请选择省/市/区'
export const PLACEHOLDER_SHOP_ADDRESS_DETAIL = '选填，填写详细地址（街道、门牌号等）'
export const REGION_REQUIRED = '请选择省/市/区'
```

- [ ] **Step 2: Register.js — 表单模型拆分和提交调整**

修改 `shopForm`，将单字段 `address` 拆分为 `region` + `addressDetail`：

```js
const shopForm = reactive({ name: "", description: "", region: [], addressDetail: "", phone: "" })
```

更新校验规则，`address` 改为 `addressDetail`：

```js
const shopRules = {
  name: [
    { required: true, message: T.SHOP_NAME_REQUIRED, trigger: "blur" },
    { max: 50, message: T.SHOP_NAME_MAX, trigger: "blur" }
  ],
  description: [{ max: 200, message: T.SHOP_DESC_MAX, trigger: "blur" }],
  region: [{ required: true, message: T.REGION_REQUIRED, trigger: "change" }],
  addressDetail: [{ max: 200, message: T.SHOP_ADDRESS_MAX, trigger: "blur" }],
  phone: [
    { max: 20, message: T.SHOP_PHONE_MAX, trigger: "blur" },
    { pattern: /^$|^(1\d{10}|0\d{2,3}-?\d{7,8})$/, message: T.SHOP_PHONE_INVALID, trigger: "blur" }
  ]
}
```

修改 `buildShopFormData` 拼接地址：

```js
import { buildAddressString } from '@/utils/region'

function buildShopFormData() {
  const formData = new FormData()
  const shopData = {
    name: shopForm.name,
    description: shopForm.description || undefined,
    address: shopForm.region.length > 0 ? buildAddressString(shopForm.region, shopForm.addressDetail) : (shopForm.addressDetail || undefined),
    phone: shopForm.phone || undefined
  }
  formData.append("shop", new Blob([JSON.stringify(shopData)], { type: "application/json" }))
  if (logoFile.value) {
    formData.append("logo", logoFile.value)
  }
  return formData
}
```

- [ ] **Step 3: Register.vue — 地址字段改为 Cascader + Input**

替换第86-93行的 `<el-form-item :label="T.LABEL_SHOP_ADDRESS" ...>` 为：

```vue
<el-form-item :label="T.LABEL_SHOP_REGION" prop="region">
  <el-cascader
    v-model="shopForm.region"
    :options="regionOptions"
    :props="{ expandTrigger: 'hover' }"
    :placeholder="T.PLACEHOLDER_SHOP_REGION"
    clearable
    style="width: 100%"
  />
</el-form-item>
<el-form-item :label="T.LABEL_SHOP_ADDRESS" prop="addressDetail">
  <el-input
    v-model="shopForm.addressDetail"
    :placeholder="T.PLACEHOLDER_SHOP_ADDRESS_DETAIL"
    :maxlength="200"
    show-word-limit
  />
</el-form-item>
```

在 `<script setup>` 中引入 `regionData`：

```vue
<script setup>
import { useRegister } from './Register.js'
import { regionData } from 'element-china-area-data'
import { ref } from 'vue'

const regionOptions = ref(regionData)

const {
  T, currentStep,
  accountFormRef, accountForm, accountRules,
  shopFormRef, shopForm, shopRules,
  logoInputRef, logoFile,
  submittingAccount, submittingShop, registeredUsername,
  handleNext, handleLogoChange, clearLogo, handleSubmitShop
} = useRegister()
</script>
```

---

### Task 3: 更新 ShopInfo.vue — 店铺信息编辑

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.vue`
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/ShopInfo.js`
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopInfo/Text.js`

- [ ] **Step 1: Text.js — 新增文本常量**

```js
export const LABEL_REGION = '省/市/区'
export const PLACEHOLDER_REGION = '请选择省/市/区'
```

- [ ] **Step 2: ShopInfo.js — 拆分表单字段 + 加载时解析 + 保存时拼接**

在顶部添加导入：

```js
import { parseAddress, buildAddressString } from '@/utils/region'
```

表单字段拆分：

```js
const form = reactive({
  name: '',
  description: '',
  phone: '',
  region: [],
  addressDetail: '',
  address: '',  // 保留，用于 parseAddress 前暂存和后端原始字段
  businessHours: ''
})
```

修改 `loadShopInfo` 中加载地址的逻辑：

```js
form.name = shop.name || ''
form.description = shop.description || ''
form.phone = shop.phone || ''
form.address = shop.address || ''  // 保留原始完整地址
const parsed = parseAddress(shop.address || '')
form.region = parsed.region
form.addressDetail = parsed.detail
form.businessHours = shop.businessHours || ''
```

修改 `handleSave` 中保存时拼接地址：

```js
async function handleSave() {
  saving.value = true
  try {
    const payload = {
      name: form.name,
      description: form.description,
      phone: form.phone,
      address: form.region.length > 0 ? buildAddressString(form.region, form.addressDetail) : (form.addressDetail || ''),
      businessHours: form.businessHours
    }
    const res = await updateShop(shopId, payload)
    ElMessage.success(res?.message || T.SAVE_SUCCESS)
  } catch (e) {
    ElMessage.error(T.SAVE_FAILED)
  } finally {
    saving.value = false
  }
}
```

- [ ] **Step 3: ShopInfo.vue — 地址字段拆分**

替换 `<el-form-item :label="T.LABEL_ADDRESS">` 为：

```vue
<el-form-item :label="T.LABEL_REGION">
  <el-cascader
    v-model="form.region"
    :options="regionOptions"
    :props="{ expandTrigger: 'hover' }"
    :placeholder="T.PLACEHOLDER_REGION"
    clearable
    style="width: 100%"
  />
</el-form-item>
<el-form-item :label="T.LABEL_ADDRESS">
  <el-input v-model="form.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS" :maxlength="200" />
</el-form-item>
```

在 `<script setup>` 中引入 `regionData`：

```vue
<script setup>
import { useShopInfo } from './ShopInfo.js'
import { regionData } from 'element-china-area-data'
import { ref } from 'vue'

const regionOptions = ref(regionData)

const { T, form, loading, saving, toggling, shopStatus, handleSave, handleToggleStatus } = useShopInfo()
</script>
```

---

### Task 4: 更新 ShopAddresses.vue — 地址管理弹窗

**Files:**
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopAddresses/ShopAddresses.vue`
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopAddresses/ShopAddresses.js`
- Modify: `AI-Shopping-frontier/frontier-seller/src/views/ShopAddresses/Text.js`

- [ ] **Step 1: Text.js — 新增文本常量**

```js
export const LABEL_REGION = '省/市/区'
export const REGION_REQUIRED = '请选择省/市/区'
export const PLACEHOLDER_ADDRESS_DETAIL = '请输入详细地址（街道、门牌号等）'
```

- [ ] **Step 2: ShopAddresses.js — 拆分表单字段 + 编辑时解析 + 提交时拼接**

在顶部添加导入：

```js
import { parseAddress, buildAddressString } from '@/utils/region'
```

修改 `form` 初始化和相关方法：

```js
const form = ref({ addressType: 1, name: '', phone: '', region: [], addressDetail: '', isDefault: 0 })
```

修改 `showEditDialog` 解析地址：

```js
function showEditDialog(addr) {
  isEdit.value = true; editingId.value = addr.id
  const parsed = parseAddress(addr.address || '')
  form.value = {
    addressType: addr.addressType,
    name: addr.name,
    phone: addr.phone,
    region: parsed.region,
    addressDetail: parsed.detail,
    isDefault: addr.isDefault || 0
  }
  dialogVisible.value = true
}
```

修改 `validate`：

```js
function validate() {
  if (!form.value.name.trim()) { ElMessage.warning(T.NAME_REQUIRED); return false }
  if (!form.value.phone.trim()) { ElMessage.warning(T.PHONE_REQUIRED); return false }
  if (!form.value.region || form.value.region.length === 0) { ElMessage.warning(T.REGION_REQUIRED); return false }
  return true
}
```

修改 `showAddDialog` 重置表单：

```js
function showAddDialog() {
  isEdit.value = false; editingId.value = null
  form.value = { addressType: activeTab.value, name: '', phone: '', region: [], addressDetail: '', isDefault: 0 }
  dialogVisible.value = true
}
```

修改 `handleSubmit` 拼接地址：

```js
async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const payload = {
      addressType: form.value.addressType,
      name: form.value.name,
      phone: form.value.phone,
      address: form.value.region.length > 0 ? buildAddressString(form.value.region, form.value.addressDetail) : (form.value.addressDetail || ''),
      isDefault: form.value.isDefault
    }
    const res = isEdit.value
      ? await updateAddress(editingId.value, payload)
      : await createAddress(payload)
    if (res?.message?.includes('成功')) {
      ElMessage.success(isEdit.value ? T.SUCCESS_EDIT : T.SUCCESS_ADD)
      closeDialog()
      await loadAddresses()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally { submitting.value = false }
}
```

- [ ] **Step 3: ShopAddresses.vue — 弹窗中地址字段拆分**

在 `<script setup>` 中引入 `regionData` 和 `ref`：

```vue
<script setup>
import { useShopAddresses } from './ShopAddresses.js'
import { regionData } from 'element-china-area-data'
import { ref } from 'vue'

const regionOptions = ref(regionData)

const props = useShopAddresses()
const { T, ... } = props
</script>
```

替换弹窗中 `<el-form-item :label="T.LABEL_ADDRESS">` 为：

```vue
<el-form-item :label="T.LABEL_REGION">
  <el-cascader
    v-model="form.region"
    :options="regionOptions"
    :props="{ expandTrigger: 'hover' }"
    placeholder="请选择省/市/区"
    clearable
    style="width: 100%"
  />
</el-form-item>
<el-form-item :label="T.LABEL_ADDRESS">
  <el-input v-model="form.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS_DETAIL" :maxlength="200" />
</el-form-item>
```

---

### Task 5: 验证构建

- [ ] **Step 1: 运行 dev 服务器验证无编译错误**

```bash
cd AI-Shopping-frontier/frontier-seller && npm run dev
```

Expected: Vite dev server starts without errors.

- [ ] **Step 2: 运行生产构建**

```bash
cd AI-Shopping-frontier/frontier-seller && npm run build
```

Expected: Build succeeds with no errors.
