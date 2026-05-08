<!-- ShopProducts.vue -->
<template>
  <div class="shop-products">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.push('/shop/list')">
          返回
        </button>
        <h2 class="page-title">商品管理</h2>
        <span v-if="shopInfo" class="shop-name">- {{ shopInfo.name }}</span>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadProducts" :disabled="loading">
          刷新
        </button>
        <button class="add-btn" @click="showAddDialog">
          添加商品
        </button>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <input
          type="text"
          v-model="searchKeyword"
          placeholder="搜索商品名称"
          class="filter-input"
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
    </div>

    <div class="products-container">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="products.length === 0" class="empty-state">
        <span class="empty-icon">📦</span>
        <p>暂无商品</p>
        <button class="add-first-btn" @click="showAddDialog">
          添加第一个商品
        </button>
      </div>
      <div v-else class="products-grid">
        <div
          v-for="product in filteredProducts"
          :key="product.productId"
          class="product-card"
        >
          <div class="product-image">
            <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.name" />
            <span v-else class="no-image">📷</span>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p v-if="product.description" class="product-desc">{{ product.description }}</p>
            <div class="product-meta">
              <span class="price">¥{{ product.price?.toFixed(2) }}</span>
              <span class="stock">库存: {{ product.stock || 0 }}</span>
            </div>
          </div>
          <div class="product-actions">
            <button class="edit-btn" @click="showEditDialog(product)">编辑</button>
            <button class="delete-btn" @click="handleDelete(product)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑商品弹窗 -->
    <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ isEdit ? '编辑商品' : '添加商品' }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>商品名称 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.name"
              placeholder="请输入商品名称"
              class="form-input"
              maxlength="100"
            />
          </div>

          <div class="form-group">
            <label>商品描述</label>
            <textarea
              v-model="form.description"
              placeholder="请输入商品描述（可选）"
              class="form-textarea"
              rows="3"
              maxlength="500"
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>价格 (元) <span class="required">*</span></label>
              <input
                type="number"
                v-model="form.price"
                placeholder="0.00"
                class="form-input"
                min="0"
                step="0.01"
              />
            </div>

            <div class="form-group">
              <label>库存数量 <span class="required">*</span></label>
              <input
                type="number"
                v-model="form.stock"
                placeholder="0"
                class="form-input"
                min="0"
              />
            </div>
          </div>

          <div class="form-group">
            <label>图片URL</label>
            <input
              type="text"
              v-model="form.imageUrl"
              placeholder="请输入图片链接（可选）"
              class="form-input"
            />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="closeDialog">取消</button>
          <button
            class="btn-submit"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '保存中...' : (isEdit ? '保存' : '添加') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shopApi } from '../../api/shop.js'
import { showSuccess, showError } from '../../utils/swal.js'
import Swal from 'sweetalert2'

const route = useRoute()

const shopId = computed(() => route.params.shopId)
const shopInfo = ref(null)
const products = ref([])
const loading = ref(false)

const searchKeyword = ref('')
const filteredProducts = computed(() => {
  if (!searchKeyword.value.trim()) return products.value
  const keyword = searchKeyword.value.trim().toLowerCase()
  return products.value.filter(p =>
    p.name?.toLowerCase().includes(keyword)
  )
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const form = ref({
  name: '',
  description: '',
  price: '',
  stock: '',
  imageUrl: ''
})
const editingProductId = ref(null)

const loadShopInfo = async () => {
  try {
    const res = await shopApi.detail(shopId.value)
    if (res?.data) {
      shopInfo.value = res.data
    }
  } catch (error) {
    console.error('加载店铺信息失败:', error)
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await shopApi.products(shopId.value)
    if (res?.data) {
      products.value = Array.isArray(res.data) ? res.data : []
    } else if (res?.products) {
      products.value = res.products
    } else {
      products.value = []
    }
  } catch (error) {
    console.error('加载商品列表失败:', error)
    showError('加载失败')
    products.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {}

const showAddDialog = () => {
  isEdit.value = false
  form.value = {
    name: '',
    description: '',
    price: '',
    stock: '',
    imageUrl: ''
  }
  editingProductId.value = null
  dialogVisible.value = true
}

const showEditDialog = (product) => {
  isEdit.value = true
  form.value = {
    name: product.name || '',
    description: product.description || '',
    price: product.price || '',
    stock: product.stock || 0,
    imageUrl: product.imageUrl || ''
  }
  editingProductId.value = product.productId
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
}

const validateForm = () => {
  if (!form.value.name.trim()) {
    showError('请输入商品名称')
    return false
  }
  if (!form.value.price || form.value.price <= 0) {
    showError('请输入有效的价格')
    return false
  }
  if (form.value.stock === '' || form.value.stock < 0) {
    showError('请输入有效的库存数量')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) return

  submitting.value = true
  try {
    const data = {
      name: form.value.name.trim(),
      description: form.value.description.trim(),
      price: parseFloat(form.value.price),
      stock: parseInt(form.value.stock),
      imageUrl: form.value.imageUrl.trim()
    }

    let res
    if (isEdit.value) {
      res = await shopApi.updateProduct(shopId.value, editingProductId.value, data)
    } else {
      res = await shopApi.createProduct(shopId.value, data)
    }

    if (res?.message?.includes('成功')) {
      showSuccess(isEdit.value ? '商品更新成功' : '商品添加成功')
      closeDialog()
      await loadProducts()
    } else {
      showError(res?.message || (isEdit.value ? '更新失败' : '添加失败'))
    }
  } catch (error) {
    console.error('保存商品失败:', error)
    showError('保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (product) => {
  const result = await Swal.fire({
    title: '确认删除',
    text: `确定要删除商品 "${product.name}" 吗？`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonColor: '#ef4444'
  })

  if (!result.isConfirmed) return

  try {
    const res = await shopApi.deleteProduct(shopId.value, product.productId)
    if (res?.message?.includes('成功')) {
      showSuccess('商品删除成功')
      await loadProducts()
    } else {
      showError(res?.message || '删除失败')
    }
  } catch (error) {
    console.error('删除商品失败:', error)
    showError('删除失败，请稍后重试')
  }
}

onMounted(() => {
  loadShopInfo()
  loadProducts()
})
</script>

<style scoped>
.shop-products {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.back-btn:hover {
  transform: translateY(-1px);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.shop-name {
  font-size: 16px;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.refresh-btn,
.add-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  color: white;
}

.add-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.refresh-btn:hover:not(:disabled),
.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.filter-bar {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-input {
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  width: 220px;
  outline: none;
}

.filter-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-btn {
  padding: 10px 18px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.products-container {
  min-height: 300px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
  display: block;
}

.add-first-btn {
  margin-top: 16px;
  padding: 10px 24px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-card {
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.product-image {
  width: 100%;
  height: 160px;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 48px;
}

.product-info {
  margin-bottom: 12px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.product-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 10px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.stock {
  font-size: 13px;
  color: #64748b;
}

.product-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.edit-btn,
.delete-btn {
  flex: 1;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.delete-btn {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
}

.edit-btn:hover,
.delete-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 弹窗样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.dialog {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.15);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #334155;
  margin-bottom: 8px;
  font-weight: 500;
}

.required {
  color: #ef4444;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #000000;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-row .form-group {
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.btn-cancel,
.btn-submit {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: white;
  border: 1px solid #e2e8f0;
  color: #475569;
}

.btn-submit {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.btn-cancel:hover {
  background: #f8fafc;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.35);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.shop-products::-webkit-scrollbar {
  width: 8px;
}

.shop-products::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>