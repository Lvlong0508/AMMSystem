<!-- ShopAddresses.vue -->
<template>
  <div class="shop-addresses">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.push('/shop/list')">
          返回
        </button>
        <h2 class="page-title">地址管理</h2>
        <span v-if="shopInfo" class="shop-name">- {{ shopInfo.name }}</span>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadAddresses" :disabled="loading">
          刷新
        </button>
        <button class="add-btn" @click="showAddDialog">
          新增地址
        </button>
      </div>
    </div>

    <div class="tabs-container">
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 1 }"
          @click="activeTab = 1"
        >
          发货地址 ({{ shippingAddresses.length }})
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 2 }"
          @click="activeTab = 2"
        >
          退货地址 ({{ returnAddresses.length }})
        </button>
      </div>
    </div>

    <div class="addresses-container">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="currentAddresses.length === 0" class="empty-state">
        <span class="empty-icon">📍</span>
        <p>{{ activeTab === 1 ? '暂无发货地址' : '暂无退货地址' }}</p>
        <button class="add-first-btn" @click="showAddDialog">
          添加第一个{{ activeTab === 1 ? '发货' : '退货' }}地址
        </button>
      </div>
      <div v-else class="addresses-list">
        <div
          v-for="address in currentAddresses"
          :key="address.id"
          class="address-card"
          :class="{ 'is-default': address.isDefault === 1 }"
        >
          <div class="address-header">
            <div class="address-type">
              <span class="type-badge" :class="address.addressType === 1 ? 'shipping' : 'return'">
                {{ address.addressType === 1 ? '发货' : '退货' }}
              </span>
              <span v-if="address.isDefault === 1" class="default-badge">默认</span>
            </div>
            <div class="address-actions">
              <button class="edit-btn" @click="showEditDialog(address)">编辑</button>
              <button class="delete-btn" @click="handleDelete(address)">删除</button>
            </div>
          </div>

          <div class="address-content">
            <div class="address-receiver">
              <span class="receiver-name">{{ address.name }}</span>
              <span class="receiver-phone">{{ address.phone }}</span>
            </div>
            <div class="address-detail">{{ address.address }}</div>
            <div class="address-time" v-if="address.updatedAt">
              更新时间: {{ formatDate(address.updatedAt) }}
            </div>
          </div>

          <div class="address-footer" v-if="address.isDefault !== 1">
            <button class="set-default-btn" @click="handleSetDefault(address)">
              设为默认
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ isEdit ? '编辑地址' : '新增地址' }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>地址类型 <span class="required">*</span></label>
            <select v-model="form.addressType" class="form-select">
              <option :value="1">发货地址</option>
              <option :value="2">退货地址</option>
            </select>
          </div>

          <div class="form-group">
            <label>收货人姓名 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.name"
              placeholder="请输入收货人姓名"
              class="form-input"
              maxlength="50"
            />
          </div>

          <div class="form-group">
            <label>联系电话 <span class="required">*</span></label>
            <input
              type="tel"
              v-model="form.phone"
              placeholder="请输入联系电话"
              class="form-input"
              maxlength="20"
            />
          </div>

          <div class="form-group">
            <label>详细地址 <span class="required">*</span></label>
            <textarea
              v-model="form.address"
              placeholder="请输入详细地址"
              class="form-textarea"
              maxlength="200"
              rows="3"
            ></textarea>
          </div>

          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.isDefault" :true-value="1" :false-value="0" />
              <span>设为默认地址</span>
            </label>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="closeDialog">取消</button>
          <button
            class="btn-submit"
            :disabled="submitting"
            @click="handleSubmit"
          >
            {{ submitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '../../api/contact.js'
import { shopApi } from '../../api/shop.js'
import { showSuccess, showError } from '../../utils/swal.js'
import Swal from 'sweetalert2'

const route = useRoute()

const shopId = ref(route.params.shopId)
const shopInfo = ref(null)
const addresses = ref([])
const loading = ref(false)
const activeTab = ref(1)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const form = ref({
  addressType: 1,
  name: '',
  phone: '',
  address: '',
  isDefault: 0
})

const shippingAddresses = computed(() => addresses.value.filter(a => a.addressType === 1))
const returnAddresses = computed(() => addresses.value.filter(a => a.addressType === 2))
const currentAddresses = computed(() => activeTab.value === 1 ? shippingAddresses.value : returnAddresses.value)

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

const loadAddresses = async () => {
  loading.value = true
  try {
    const res = await getAddressList(shopId.value)
    if (res?.data) {
      addresses.value = Array.isArray(res.data) ? res.data : []
    } else if (res?.message?.includes('成功')) {
      addresses.value = []
    } else {
      addresses.value = []
    }
  } catch (error) {
    console.error('加载地址列表失败:', error)
    showError('加载失败')
    addresses.value = []
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const showAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  form.value = {
    addressType: activeTab.value,
    name: '',
    phone: '',
    address: '',
    isDefault: 0
  }
  dialogVisible.value = true
}

const showEditDialog = (address) => {
  isEdit.value = true
  editingId.value = address.id
  form.value = {
    addressType: address.addressType,
    name: address.name,
    phone: address.phone,
    address: address.address,
    isDefault: address.isDefault
  }
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
}

const validateForm = () => {
  if (!form.value.name.trim()) {
    showError('请输入收货人姓名')
    return false
  }
  if (!form.value.phone.trim()) {
    showError('请输入联系电话')
    return false
  }
  if (!form.value.address.trim()) {
    showError('请输入详细地址')
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
      phone: form.value.phone.trim(),
      address: form.value.address.trim(),
      addressType: form.value.addressType,
      isDefault: form.value.isDefault
    }

    let res
    if (isEdit.value) {
      res = await updateAddress(shopId.value, editingId.value, data)
    } else {
      res = await addAddress(shopId.value, data)
    }

    if (res?.message?.includes('成功')) {
      showSuccess(isEdit.value ? '地址修改成功' : '地址新增成功')
      closeDialog()
      await loadAddresses()
    } else {
      showError(res?.message || '操作失败')
    }
  } catch (error) {
    console.error('保存地址失败:', error)
    showError('操作失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (address) => {
  const result = await Swal.fire({
    title: '确认删除',
    text: `确定要删除该地址吗？`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonColor: '#ef4444'
  })

  if (!result.isConfirmed) return

  try {
    const res = await deleteAddress(shopId.value, address.id)
    if (res?.message?.includes('成功')) {
      showSuccess('地址删除成功')
      await loadAddresses()
    } else {
      showError(res?.message || '删除失败')
    }
  } catch (error) {
    console.error('删除地址失败:', error)
    showError('删除失败，请稍后重试')
  }
}

const handleSetDefault = async (address) => {
  try {
    const res = await setDefaultAddress(shopId.value, address.id)
    if (res?.message?.includes('成功')) {
      showSuccess('已设为默认地址')
      await loadAddresses()
    } else {
      showError(res?.message || '设置失败')
    }
  } catch (error) {
    console.error('设置默认地址失败:', error)
    showError('设置失败，请稍后重试')
  }
}

onMounted(() => {
  loadShopInfo()
  loadAddresses()
})
</script>

<style scoped>
.shop-addresses {
  padding: 24px;
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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
}

.tabs-container {
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  gap: 8px;
  background: white;
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.tab-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: #64748b;
  transition: all 0.2s;
}

.tab-btn.active {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
}

.tab-btn:not(.active):hover {
  background: #f1f5f9;
}

.addresses-container {
  min-height: 300px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #64748b;
  background: white;
  border-radius: 12px;
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

.addresses-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.address-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.address-card.is-default {
  border-color: #10b981;
}

.address-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.address-type {
  display: flex;
  gap: 8px;
}

.type-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.type-badge.shipping {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.type-badge.return {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
}

.default-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.address-actions {
  display: flex;
  gap: 8px;
}

.edit-btn,
.delete-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
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
}

.address-content {
  margin-bottom: 12px;
}

.address-receiver {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.receiver-name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.receiver-phone {
  font-size: 14px;
  color: #64748b;
}

.address-detail {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 8px;
}

.address-time {
  font-size: 12px;
  color: #94a3b8;
}

.address-footer {
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.set-default-btn {
  width: 100%;
  padding: 10px;
  border: 1px dashed #10b981;
  border-radius: 8px;
  background: transparent;
  color: #10b981;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.set-default-btn:hover {
  background: #ecfdf5;
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
}

.dialog {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  overflow: hidden;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
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
}

.dialog-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
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
.form-select,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #000000;
  outline: none;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.checkbox-label span {
  font-size: 14px;
  color: #475569;
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
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.shop-addresses::-webkit-scrollbar {
  width: 8px;
}

.shop-addresses::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>