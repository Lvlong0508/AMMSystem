# 商家端卡片式重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**目标：** 将商家端 ProductCard/OrderCard 改为 variant prop 模式（abstract/detail），以卡片可视化替代表格/表单管理页面

**技术栈：** Vue 3 + Element Plus + Pinia

---

### Task 1: ProductCard 重构

**文件：**
- Modify: `frontier-seller/src/components/ProductCard/ProductCard.vue`
- Modify: `frontier-seller/src/components/ProductCard/ProductCard.js`
- Modify: `frontier-seller/src/components/ProductCard/ProductCard.css`
- Modify: `frontier-seller/src/components/ProductCard/Text.js`

- [ ] **Step 1: 更新 Text.js** — 添加新常量

```js
export const BTN_EDIT = '编辑'
export const BTN_DELETE = '删除'
export const BTN_LIST = '上架'
export const BTN_UNLIST = '下架'
export const STATUS_ON = '在售'
export const STATUS_OFF = '已下架'
export const LABEL_STOCK = '库存'
export const LABEL_CREATED = '创建时间'
export const LABEL_UPDATED = '更新时间'
```

- [ ] **Step 2: 重写 ProductCard.js** — variant 模式逻辑

```js
import { computed } from 'vue'
import * as T from './Text.js'

export function useProductCard(props) {
  const parsedTags = computed(() => {
    if (!props.product?.tags) return []
    if (Array.isArray(props.product.tags)) return props.product.tags
    return String(props.product.tags).split(',').map(t => t.trim()).filter(Boolean)
  })

  function formatDate(dateStr) {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleString('zh-CN')
  }

  return { T, parsedTags, formatDate }
}
```

- [ ] **Step 3: 重写 ProductCard.vue** — abstract/detail 双模板

```vue
<template>
  <el-card
    v-if="variant === 'abstract'"
    shadow="hover"
    class="product-card product-card--abstract"
    @click="$emit('click', product)"
  >
    <div class="product-card__abstract">
      <div class="product-card__image">
        <el-image v-if="product.imageUrl" :src="product.imageUrl" style="width: 80px; height: 80px" fit="cover" />
        <div v-else class="product-card__image-placeholder">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
        </div>
      </div>
      <div class="product-card__info">
        <h3 class="product-card__name">{{ product.name }}</h3>
        <div class="product-card__meta">
          <span class="product-card__price">¥{{ product.price?.toFixed(2) }}</span>
          <span>{{ T.LABEL_STOCK }}: {{ product.stock }}</span>
        </div>
      </div>
      <div class="product-card__status">
        <el-tag v-if="product.isSale" type="success" size="small">{{ T.STATUS_ON }}</el-tag>
        <el-tag v-else type="info" size="small">{{ T.STATUS_OFF }}</el-tag>
      </div>
    </div>
  </el-card>

  <el-card v-else shadow="never" class="product-card product-card--detail">
    <div class="product-card__detail-layout">
      <div class="product-card__detail-image">
        <el-image v-if="product.imageUrl" :src="product.imageUrl" style="width: 240px; height: 240px" fit="cover" />
        <div v-else class="product-card__image-placeholder product-card__image-placeholder--lg">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
        </div>
      </div>
      <div class="product-card__detail-info">
        <div v-if="parsedTags.length" class="product-card__tags">
          <span v-for="tag in parsedTags" :key="tag" class="product-card__tag">{{ tag }}</span>
        </div>
        <h3 class="product-card__detail-name">{{ product.name }}</h3>
        <div class="product-card__price">¥{{ product.price?.toFixed(2) }}</div>
        <p class="product-card__desc">{{ product.description }}</p>
        <el-divider />
        <div class="product-card__detail-meta">
          <span>{{ T.LABEL_STOCK }}: {{ product.stock }}</span>
          <span>{{ T.LABEL_CREATED }}: {{ formatDate(product.createdAt) }}</span>
          <span>{{ T.LABEL_UPDATED }}: {{ formatDate(product.updatedAt) }}</span>
        </div>
        <div class="product-card__status">
          <el-tag v-if="product.isSale" type="success">{{ T.STATUS_ON }}</el-tag>
          <el-tag v-else type="info">{{ T.STATUS_OFF }}</el-tag>
        </div>
        <div class="product-card__actions">
          <el-button type="primary" @click="$emit('edit', product)">{{ T.BTN_EDIT }}</el-button>
          <el-button :type="product.isSale ? 'warning' : 'success'" @click="$emit('toggle-sale', product)">
            {{ product.isSale ? T.BTN_UNLIST : T.BTN_LIST }}
          </el-button>
          <el-button type="danger" @click="$emit('delete', product)">{{ T.BTN_DELETE }}</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { useProductCard } from './ProductCard.js'

const props = defineProps({
  product: { type: Object, required: true },
  variant: { type: String, default: 'abstract', validator: v => ['abstract', 'detail'].includes(v) }
})

defineEmits(['click', 'edit', 'toggle-sale', 'delete'])

const { T, parsedTags, formatDate } = useProductCard(props)
</script>

<style scoped src="./ProductCard.css"></style>
```

- [ ] **Step 4: 重写 ProductCard.css** — abstract/detail 样式

```css
.product-card--abstract {
  cursor: pointer;
}

.product-card__abstract {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.product-card__image {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-card);
}

.product-card__image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
}

.product-card__image-placeholder--lg {
  width: 240px;
  height: 240px;
}

.product-card__info {
  flex: 1;
  min-width: 0;
}

.product-card__name {
  font-size: var(--text-base);
  font-weight: 600;
  margin: 0 0 var(--space-1);
  color: var(--color-text);
}

.product-card__meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.product-card__price {
  font-size: var(--text-md);
  font-weight: 700;
  color: var(--color-danger);
}

.product-card__status {
  flex-shrink: 0;
}

.product-card__detail-layout {
  display: flex;
  gap: var(--space-6);
}

.product-card__detail-image {
  flex-shrink: 0;
}

.product-card__detail-info {
  flex: 1;
  min-width: 0;
}

.product-card__detail-name {
  font-size: var(--text-xl);
  font-weight: 600;
  margin: var(--space-2) 0;
  color: var(--color-text);
}

.product-card__desc {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
  margin: var(--space-2) 0;
}

.product-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.product-card__tag {
  font-size: var(--text-xs);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.product-card__detail-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-3);
}

.product-card__actions {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.el-divider {
  margin: var(--space-3) 0;
}
```

- [ ] **Step 5: 验证** — 确认组件无编译错误

---

### Task 2: OrderCard 重构

**文件：**
- Modify: `frontier-seller/src/components/OrderCard/OrderCard.vue`
- Modify: `frontier-seller/src/components/OrderCard/OrderCard.js`
- Modify: `frontier-seller/src/components/OrderCard/OrderCard.css`
- Modify: `frontier-seller/src/components/OrderCard/Text.js`

- [ ] **Step 1: 更新 Text.js** — 添加 section 常量

```js
export const LABEL_ORDER_ID = '订单编号'
export const LABEL_PRODUCT = '商品'
export const LABEL_QUANTITY = '数量'
export const LABEL_TOTAL = '总价'
export const LABEL_DATE = '下单时间'
export const LABEL_STATUS = '状态'
export const LABEL_CONTACT_NAME = '收货人'
export const LABEL_CONTACT_PHONE = '电话'
export const LABEL_CONTACT_ADDRESS = '地址'
export const LABEL_TRACKING = '物流单号'
export const BTN_DETAIL = '详情'
export const BTN_SHIP = '发货'
export const SECTION_ORDER = '订单信息'
export const SECTION_CONTACT = '收货信息'
export const SECTION_LOGISTICS = '物流信息'
```

- [ ] **Step 2: 重写 OrderCard.js** — variant 模式逻辑

```js
import { computed } from 'vue'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useOrderCard(props) {
  const statusText = computed(() => STATUS_TEXT[props.order.orderStatus] || props.order.orderStatus)

  const statusType = computed(() => {
    const m = { PENDING: 'info', PAID: 'warning', SHIPPED: 'primary', DELIVERED: 'success', CANCELLED: 'danger', RETURNED: 'danger', RETURN_REQUESTED: 'warning', RETURN_APPROVED: 'warning' }
    return m[props.order.orderStatus] || 'info'
  })

  const actionVisible = computed(() => {
    return props.order.orderStatus === ORDER_STATUS.PAID
  })

  function formatPrice(price) {
    return price != null ? `¥${Number(price).toFixed(2)}` : '-'
  }

  function formatDate(dateStr) {
    return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-'
  }

  return { T, statusText, statusType, actionVisible, formatPrice, formatDate }
}
```

- [ ] **Step 3: 重写 OrderCard.vue**

```vue
<template>
  <el-card
    v-if="variant === 'abstract'"
    shadow="hover"
    class="order-card order-card--abstract"
    @click="$emit('click', order)"
  >
    <div class="order-card__abstract">
      <div class="order-card__info">
        <div class="order-card__id">{{ T.LABEL_ORDER_ID }}: {{ order.orderId }}</div>
        <p class="order-card__product">{{ order.productName || `${T.LABEL_PRODUCT} #${order.productId}` }}</p>
        <div class="order-card__meta">
          <span>{{ T.LABEL_QUANTITY }}: {{ order.quantity }}</span>
          <span class="order-card__price">{{ order.totalPrice ? formatPrice(order.totalPrice) : '-' }}</span>
        </div>
      </div>
      <div class="order-card__status">
        <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
      </div>
    </div>
  </el-card>

  <el-card v-else shadow="never" class="order-card order-card--detail">
    <div class="order-card__detail-header">
      <h3>{{ T.LABEL_ORDER_ID }}: {{ order.orderId }}</h3>
      <el-tag :type="statusType">{{ statusText }}</el-tag>
    </div>
    <el-divider />
    <div class="order-card__detail-grid">
      <div class="order-card__detail-section">
        <h4 class="order-card__section-title">{{ T.SECTION_ORDER }}</h4>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_PRODUCT }}</span>
          <span>{{ order.productName || `${T.LABEL_PRODUCT} #${order.productId}` }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_QUANTITY }}</span>
          <span>{{ order.quantity }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_TOTAL }}</span>
          <span>{{ formatPrice(order.totalPrice) }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_DATE }}</span>
          <span>{{ formatDate(order.orderDate) }}</span>
        </div>
      </div>
      <div v-if="order.contactName" class="order-card__detail-section">
        <h4 class="order-card__section-title">{{ T.SECTION_CONTACT }}</h4>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_CONTACT_NAME }}</span>
          <span>{{ order.contactName }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_CONTACT_PHONE }}</span>
          <span>{{ order.contactPhone }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_CONTACT_ADDRESS }}</span>
          <span>{{ order.contactAddress }}</span>
        </div>
      </div>
      <div v-if="order.trackingNumber" class="order-card__detail-section">
        <h4 class="order-card__section-title">{{ T.SECTION_LOGISTICS }}</h4>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_TRACKING }}</span>
          <span>{{ order.trackingNumber }}</span>
        </div>
      </div>
    </div>
    <el-divider v-if="actionVisible" />
    <div v-if="actionVisible" class="order-card__actions">
      <el-button type="primary" @click="$emit('ship', order)">{{ T.BTN_SHIP }}</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { useOrderCard } from './OrderCard.js'

const props = defineProps({
  order: { type: Object, required: true },
  variant: { type: String, default: 'abstract', validator: v => ['abstract', 'detail'].includes(v) }
})

defineEmits(['click', 'ship'])

const { T, statusText, statusType, actionVisible, formatPrice, formatDate } = useOrderCard(props)
</script>

<style scoped src="./OrderCard.css"></style>
```

- [ ] **Step 4: 重写 OrderCard.css**

```css
.order-card--abstract {
  cursor: pointer;
}

.order-card__abstract {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.order-card__info {
  flex: 1;
  min-width: 0;
}

.order-card__id {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--space-1);
  font-family: var(--font-mono);
}

.order-card__product {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin: 0 0 var(--space-1);
}

.order-card__meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.order-card__price {
  font-size: var(--text-md);
  font-weight: 700;
  color: var(--color-danger);
}

.order-card__status {
  flex-shrink: 0;
}

.order-card__detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-card__detail-header h3 {
  font-size: var(--text-base);
  font-weight: 600;
  margin: 0;
  font-family: var(--font-mono);
}

.order-card__detail-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.order-card__detail-section {
  background: var(--color-card);
  padding: var(--space-3);
  border-radius: var(--radius-md);
}

.order-card__section-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-2);
  padding-bottom: var(--space-1);
  border-bottom: 1px solid var(--color-border-light);
}

.order-card__detail-row {
  display: flex;
  gap: var(--space-3);
  font-size: var(--text-sm);
  padding: var(--space-1) 0;
}

.order-card__label {
  flex-shrink: 0;
  color: var(--color-text-secondary);
  min-width: 80px;
}

.order-card__actions {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
}
```

- [ ] **Step 5: 验证组件**

---

### Task 3: ShopProducts 页面改造

**文件：**
- Modify: `frontier-seller/src/views/ShopProducts/ShopProducts.vue`
- Modify: `frontier-seller/src/views/ShopProducts/ShopProducts.js`
- Modify: `frontier-seller/src/views/ShopProducts/ShopProducts.css`

- [ ] **Step 1: 更新 ShopProducts.vue** — 使用 abstract ProductCard + detail 弹窗

```vue
<template>
  <div class="shop-products">
    <div class="shop-products__toolbar">
      <h2 class="shop-products__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <div>
        <el-button @click="loadProducts">{{ T.BTN_REFRESH }}</el-button>
        <el-button type="primary" @click="showAddDialog">{{ T.BTN_ADD }}</el-button>
      </div>
    </div>

    <div class="shop-products__filters">
      <el-input v-model="searchKeyword" :placeholder="T.SEARCH_PLACEHOLDER" style="width: 280px" clearable />
    </div>

    <div v-loading="loading" class="product-grid">
      <ProductCard
        v-for="product in filteredProducts"
        :key="product.id || product.productId"
        :product="product"
        variant="abstract"
        @click="showDetail"
      />
    </div>

    <el-empty v-if="!loading && filteredProducts.length === 0" :description="T.EMPTY_TEXT" />

    <!-- 商品详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="680px" destroy-on-close>
      <ProductCard
        v-if="selectedProduct"
        :product="selectedProduct"
        variant="detail"
        @edit="handleEditFromDetail"
        @toggle-sale="handleToggleSaleFromDetail"
        @delete="handleDeleteFromDetail"
      />
    </el-dialog>

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? T.DIALOG_EDIT : T.DIALOG_ADD" width="600px">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="500" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="T.LABEL_PRICE">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="T.LABEL_STOCK">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="T.LABEL_IMAGE">
          <input type="file" accept="image/jpeg,image/png" @change="handleFileChange" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? (isEdit ? T.BTN_SAVING : T.BTN_ADD_SUBMITTING) : (isEdit ? T.BTN_SAVE : T.BTN_ADD_SUBMIT) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import ProductCard from '@/components/ProductCard/ProductCard.vue'
import { useShopProducts } from './ShopProducts.js'
const { T, shopInfo, products, loading, searchKeyword, filteredProducts, detailVisible, selectedProduct, dialogVisible, isEdit, submitting, form, showAddDialog, showEditDialog, closeDialog, handleFileChange, handleSubmit, handleToggleSale, handleDelete, loadProducts, showDetail, closeDetail, handleEditFromDetail, handleToggleSaleFromDetail, handleDeleteFromDetail } = useShopProducts()
</script>

<style scoped src="./ShopProducts.css"></style>
```

- [ ] **Step 2: 重写 ShopProducts.js** — 修复 loadProducts + 新增 detail 逻辑

```js
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShopDetail } from '@/api/shop'
import { getProductsByShop, createProduct, updateProduct, deleteProduct, listProduct, unlistProduct, getProductById } from '@/api/product'
import * as T from './Text.js'

export function useShopProducts() {
  const route = useRoute()
  const shopId = computed(() => route.params.shopId)
  const shopInfo = ref(null)
  const products = ref([])
  const loading = ref(false)
  const searchKeyword = ref('')

  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const editingProductId = ref(null)

  const detailVisible = ref(false)
  const selectedProduct = ref(null)

  const form = ref({
    name: '', description: '', price: '', stock: '', image: null
  })

  const filteredProducts = computed(() => {
    if (!searchKeyword.value.trim()) return products.value
    const kw = searchKeyword.value.trim().toLowerCase()
    return products.value.filter(p => p.name?.toLowerCase().includes(kw))
  })

  async function loadShopInfo() {
    try {
      const res = await getShopDetail(shopId.value)
      if (res?.data) shopInfo.value = res.data
    } catch (e) { console.error(e) }
  }

  async function loadProducts() {
    loading.value = true
    try {
      const res = await getProductsByShop(shopId.value)
      products.value = res?.data || []
    } catch (e) {
      ElMessage.error('加载失败')
      products.value = []
    } finally {
      loading.value = false
    }
  }

  async function showDetail(product) {
    try {
      const res = await getProductById(product.id || product.productId)
      selectedProduct.value = res?.data || product
    } catch {
      selectedProduct.value = product
    }
    detailVisible.value = true
  }

  function closeDetail() {
    detailVisible.value = false
    selectedProduct.value = null
  }

  async function handleEditFromDetail(product) {
    closeDetail()
    showEditDialog(product)
  }

  async function handleToggleSaleFromDetail(product) {
    closeDetail()
    await handleToggleSale(product)
  }

  async function handleDeleteFromDetail(product) {
    closeDetail()
    await handleDelete(product)
  }

  function showAddDialog() {
    isEdit.value = false
    editingProductId.value = null
    form.value = { name: '', description: '', price: '', stock: '', image: null }
    dialogVisible.value = true
  }

  function showEditDialog(product) {
    isEdit.value = true
    editingProductId.value = product.id || product.productId
    form.value = {
      name: product.name || '',
      description: product.description || '',
      price: product.price || '',
      stock: product.stock || 0,
      image: null
    }
    dialogVisible.value = true
  }

  function closeDialog() { dialogVisible.value = false }

  function handleFileChange(e) {
    if (e.target.files.length > 0) form.value.image = e.target.files[0]
  }

  function validate() {
    if (!form.value.name.trim()) { ElMessage.warning(T.NAME_REQUIRED); return false }
    if (!form.value.price || form.value.price <= 0) { ElMessage.warning(T.PRICE_REQUIRED); return false }
    if (form.value.stock === '' || form.value.stock < 0) { ElMessage.warning(T.STOCK_REQUIRED); return false }
    return true
  }

  async function handleSubmit() {
    if (!validate()) return
    submitting.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updateProduct(editingProductId.value, {
          name: form.value.name.trim(),
          description: form.value.description.trim(),
          price: parseFloat(form.value.price),
          stock: parseInt(form.value.stock)
        })
      } else {
        const fd = new FormData()
        fd.append('product', JSON.stringify({
          name: form.value.name.trim(),
          description: form.value.description.trim(),
          price: parseFloat(form.value.price),
          stock: parseInt(form.value.stock),
          shopId: Number(shopId.value)
        }))
        if (form.value.image) fd.append('image', form.value.image)
        res = await createProduct(fd)
      }
      if (res?.message?.includes('成功')) {
        ElMessage.success(isEdit.value ? T.SUCCESS_EDIT : T.SUCCESS_ADD)
        closeDialog()
        await loadProducts()
      } else {
        ElMessage.error(res?.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  }

  async function handleToggleSale(product) {
    try {
      if (product.isSale) {
        await unlistProduct(product.id || product.productId)
      } else {
        await listProduct(product.id || product.productId)
      }
      ElMessage.success(product.isSale ? T.UNLIST_SUCCESS : T.LIST_SUCCESS)
      await loadProducts()
    } catch (e) {
      ElMessage.error(T.OPERATION_FAILED)
    }
  }

  async function handleDelete(product) {
    try {
      await ElMessageBox.confirm(T.CONFIRM_DELETE, { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    } catch { return }
    try {
      const res = await deleteProduct(product.id || product.productId)
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_DELETE)
        await loadProducts()
      } else {
        ElMessage.error(res?.message || '删除失败')
      }
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }

  onMounted(() => { loadShopInfo(); loadProducts() })

  return {
    T, shopInfo, products, loading, searchKeyword, filteredProducts,
    detailVisible, selectedProduct,
    dialogVisible, isEdit, submitting, form,
    showAddDialog, showEditDialog, closeDialog, handleFileChange, handleSubmit,
    handleToggleSale, handleDelete, loadProducts,
    showDetail, closeDetail, handleEditFromDetail, handleToggleSaleFromDetail, handleDeleteFromDetail
  }
}
```

- [ ] **Step 3: 更新 ShopProducts.css** — 补充 detail 弹窗样式

```css
.shop-products__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.shop-products__title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
}

.shop-products__filters {
  margin-bottom: var(--space-4);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: var(--space-4);
}
```

- [ ] **Step 4: 更新 ShopProducts/Text.js** — 添加 DIALOG_DETAIL

```js
// 追加
export const DIALOG_DETAIL = '商品详情'
```

---

### Task 4: ShopOrders 页面改造

**文件：**
- Modify: `frontier-seller/src/views/ShopOrders/ShopOrders.vue`
- Modify: `frontier-seller/src/views/ShopOrders/ShopOrders.js`
- Modify: `frontier-seller/src/views/ShopOrders/ShopOrders.css`

- [ ] **Step 1: 更新 ShopOrders.vue** — 使用 OrderCard 替换表格

```vue
<template>
  <div class="shop-orders">
    <div class="shop-orders__toolbar">
      <h2 class="shop-orders__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <el-button size="small" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <div class="shop-orders__filters">
      <el-select v-model="filterStatus" size="small" style="width: 130px" clearable :placeholder="T.LABEL_STATUS">
        <el-option label="全部" value="" />
        <el-option v-for="[k, v] in Object.entries(STATUS_TEXT)" :key="k" :label="v" :value="k" />
      </el-select>
      <el-input v-model="searchKeyword" size="small" :placeholder="T.LABEL_SEARCH" style="width: 200px" clearable />
    </div>

    <div v-loading="loading" class="order-list">
      <OrderCard
        v-for="order in filteredOrders"
        :key="order.orderId"
        :order="order"
        variant="abstract"
        @click="showDetail"
      />
    </div>

    <el-empty v-if="!loading && filteredOrders.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="680px" destroy-on-close>
      <OrderCard
        v-if="selectedOrder"
        :order="selectedOrder"
        variant="detail"
        @ship="handleShip"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import { useShopOrders } from './ShopOrders.js'
const props = useShopOrders()
const { T, shopInfo, orders, loading, filterStatus, searchKeyword, filteredOrders, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, handleShip, ORDER_STATUS, STATUS_TEXT } = props
</script>

<style scoped src="./ShopOrders.css"></style>
```

- [ ] **Step 2: 更新 ShopOrders.js** — 添加 detail 加载逻辑

```js
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderListByShop, getOrderDetail } from '@/api/order'
import { getShopDetail } from '@/api/shop'
import { ORDER_STATUS, STATUS_TEXT } from '@/config/orderStatus'
import * as T from './Text.js'

export function useShopOrders() {
  const route = useRoute()
  const router = useRouter()
  const shopId = computed(() => route.params.shopId)
  const shopInfo = ref(null)
  const orders = ref([])
  const loading = ref(false)
  const filterStatus = ref('')
  const searchKeyword = ref('')

  const detailVisible = ref(false)
  const selectedOrder = ref(null)

  const filteredOrders = computed(() => {
    let r = orders.value
    if (filterStatus.value) r = r.filter(o => o.orderStatus === filterStatus.value)
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase()
      r = r.filter(o => o.orderId?.toLowerCase().includes(kw) || o.productName?.toLowerCase().includes(kw))
    }
    return r
  })

  async function loadShopInfo() {
    try { const res = await getShopDetail(shopId.value); if (res?.data) shopInfo.value = res.data } catch (e) { console.error(e) }
  }

  async function loadOrders() {
    loading.value = true
    try {
      const res = await getOrderListByShop(shopId.value)
      orders.value = res?.data ? (Array.isArray(res.data) ? res.data : []) : res?.orders || []
    } catch (e) {
      ElMessage.error('加载失败')
      orders.value = []
    } finally { loading.value = false }
  }

  async function showDetail(order) {
    try {
      const res = await getOrderDetail(shopId.value, order.orderId)
      selectedOrder.value = res?.data || order
    } catch {
      selectedOrder.value = order
    }
    detailVisible.value = true
  }

  function closeDetail() {
    detailVisible.value = false
    selectedOrder.value = null
  }

  function handleShip(order) {
    router.push({ path: '/ship', query: { orderId: order.orderId } })
  }

  function getStatusType(status) {
    const m = { PENDING: 'info', PAID: 'warning', SHIPPED: 'primary', DELIVERED: 'success', CANCELLED: 'danger', RETURNED: 'danger' }
    return m[status] || 'info'
  }

  function getStatusText(s) { return STATUS_TEXT[s] || s }
  function formatDate(d) { return d ? new Date(d).toLocaleString('zh-CN') : '-' }
  function formatPrice(p) { return p != null ? `¥${Number(p).toFixed(2)}` : '-' }

  onMounted(() => { loadShopInfo(); loadOrders() })

  return { T, shopInfo, orders, loading, filterStatus, searchKeyword, filteredOrders, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, handleShip, ORDER_STATUS, STATUS_TEXT }
}
```

- [ ] **Step 3: 更新 ShopOrders.css**

```css
.shop-orders__toolbar {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-4);
}

.shop-orders__filters {
  display: flex; align-items: center; gap: var(--space-3); margin-bottom: var(--space-4);
}

.shop-orders__title {
  font-size: var(--text-lg); font-weight: 600; margin: 0;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
```

---

### Task 5: ShopEmployees 页面改造

**文件：**
- Modify: `frontier-seller/src/views/ShopEmployees/ShopEmployees.vue`
- Modify: `frontier-seller/src/views/ShopEmployees/ShopEmployees.js`
- Modify: `frontier-seller/src/views/ShopEmployees/ShopEmployees.css`
- Modify: `frontier-seller/src/views/ShopEmployees/Text.js`

- [ ] **Step 1: 更新 ShopEmployees.vue** — 卡片网格替代表格

```vue
<template>
  <div class="shop-employees">
    <div class="shop-employees__toolbar">
      <h2 class="shop-employees__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <div>
        <el-button @click="loadEmployees">{{ T.BTN_REFRESH }}</el-button>
        <el-button type="primary" @click="showAddDialog">{{ T.BTN_ADD }}</el-button>
      </div>
    </div>

    <div v-loading="loading" class="employee-grid">
      <el-card v-for="emp in employees" :key="emp.merchantId" shadow="hover" class="employee-card">
        <div class="employee-card__header">
          <el-avatar :size="48">{{ getAvatarText(emp.name || emp.username) }}</el-avatar>
          <div class="employee-card__info">
            <span class="employee-card__name">{{ emp.name || emp.username }}</span>
            <el-tag size="small" class="employee-card__role">{{ getRoleText(emp.role) }}</el-tag>
          </div>
        </div>
        <div class="employee-card__body">
          <div class="employee-card__detail-row">
            <span class="employee-card__label">{{ T.LABEL_USERNAME }}</span>
            <span>{{ emp.username }}</span>
          </div>
          <div class="employee-card__detail-row">
            <span class="employee-card__label">{{ T.LABEL_PHONE }}</span>
            <span>{{ emp.phone || '-' }}</span>
          </div>
        </div>
        <div class="employee-card__footer">
          <el-button text type="danger" size="small" @click="handleRemove(emp)">{{ T.BTN_REMOVE }}</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && employees.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="dialogVisible" :title="T.DIALOG_ADD" width="480px">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :maxlength="50" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE">
          <el-input v-model="form.phone" :maxlength="20" />
        </el-form-item>
        <el-form-item :label="T.LABEL_USERNAME">
          <el-input v-model="form.username" :maxlength="50" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PASSWORD">
          <el-input v-model="form.password" type="password" :maxlength="50" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? T.BTN_SUBMITTING : T.BTN_SUBMIT }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShopEmployees } from './ShopEmployees.js'
const props = useShopEmployees()
const { T, shopInfo, employees, loading, dialogVisible, submitting, form, getAvatarText, getRoleText, showAddDialog, closeDialog, handleSubmit, handleRemove, loadEmployees } = props
</script>

<style scoped src="./ShopEmployees.css"></style>
```

- [ ] **Step 2: 更新 ShopEmployees.css**

```css
.shop-employees__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.shop-employees__title {
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0;
}

.employee-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-4);
}

.employee-card__header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.employee-card__info {
  flex: 1;
  min-width: 0;
}

.employee-card__name {
  display: block;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--space-1);
}

.employee-card__role {
  display: inline-block;
}

.employee-card__body {
  padding: var(--space-3) 0;
  border-top: 1px solid var(--color-border-light);
  border-bottom: 1px solid var(--color-border-light);
}

.employee-card__detail-row {
  display: flex;
  gap: var(--space-2);
  font-size: var(--text-sm);
  padding: var(--space-1) 0;
}

.employee-card__label {
  color: var(--color-text-secondary);
  min-width: 60px;
}

.employee-card__footer {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-2);
}
```

---

### 验证方式

1. `cd frontier-seller && npm run dev` — 确认开发服务器正常启动
2. 依次访问各页面检查渲染效果：
   - `/shop/:shopId/products` — 商品卡片网格，点击弹出详情
   - `/shop/:shopId/orders` — 订单卡片列表，点击弹出详情
   - `/shop/:shopId/employees` — 员工卡片网格
3. 检查浏览器控制台无报错
