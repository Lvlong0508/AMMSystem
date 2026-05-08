<!-- ShopEmployees.vue -->
<template>
  <div class="shop-employees">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.push('/shop/list')">
          返回
        </button>
        <h2 class="page-title">店员管理</h2>
        <span v-if="shopInfo" class="shop-name">- {{ shopInfo.name }}</span>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadEmployees" :disabled="loading">
          刷新
        </button>
        <button class="add-btn" @click="showAddDialog">
          添加店员
        </button>
      </div>
    </div>

    <div class="employees-container">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="employees.length === 0" class="empty-state">
        <span class="empty-icon">👥</span>
        <p>暂无店员</p>
        <button class="add-first-btn" @click="showAddDialog">
          添加第一个店员
        </button>
      </div>
      <div v-else class="employees-list">
        <div
          v-for="employee in employees"
          :key="employee.merchantId"
          class="employee-card"
        >
          <div class="employee-avatar">
            {{ getAvatarText(employee.name) }}
          </div>
          <div class="employee-info">
            <h3 class="employee-name">{{ employee.name || '未命名店员' }}</h3>
            <div class="employee-meta">
              <span class="employee-id">ID: {{ employee.merchantId }}</span>
              <span class="employee-role">{{ getRoleText(employee.role) }}</span>
            </div>
            <div class="employee-contact" v-if="employee.phone">
              <span>📞 {{ employee.phone }}</span>
            </div>
          </div>
          <div class="employee-actions">
            <button class="remove-btn" @click="handleRemove(employee)">
              移除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加店员弹窗 -->
    <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3>添加店员</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>店员名称 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.name"
              placeholder="请输入店员名称"
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
            <label>账号 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.username"
              placeholder="设置店员登录账号"
              class="form-input"
              maxlength="50"
            />
          </div>

          <div class="form-group">
            <label>初始密码 <span class="required">*</span></label>
            <input
              type="password"
              v-model="form.password"
              placeholder="设置初始密码"
              class="form-input"
              maxlength="50"
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
            {{ submitting ? '添加中...' : '添加' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shopApi } from '../../api/shop.js'
import { showSuccess, showError } from '../../utils/swal.js'
import Swal from 'sweetalert2'

const route = useRoute()

const shopId = ref(route.params.shopId)
const shopInfo = ref(null)
const employees = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const submitting = ref(false)
const form = ref({
  name: '',
  phone: '',
  username: '',
  password: ''
})

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

const loadEmployees = async () => {
  loading.value = true
  try {
    const res = await shopApi.employees(shopId.value)
    if (res?.data) {
      employees.value = Array.isArray(res.data) ? res.data : []
    } else if (res?.employees) {
      employees.value = res.employees
    } else {
      employees.value = []
    }
  } catch (error) {
    console.error('加载店员列表失败:', error)
    showError('加载失败')
    employees.value = []
  } finally {
    loading.value = false
  }
}

const getAvatarText = (name) => {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}

const getRoleText = (role) => {
  const roleMap = {
    'CLERK': '店员',
    'MANAGER': '店长',
    'ADMIN': '管理员'
  }
  return roleMap[role] || role || '店员'
}

const showAddDialog = () => {
  form.value = {
    name: '',
    phone: '',
    username: '',
    password: ''
  }
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
}

const validateForm = () => {
  if (!form.value.name.trim()) {
    showError('请输入店员名称')
    return false
  }
  if (!form.value.phone.trim()) {
    showError('请输入联系电话')
    return false
  }
  if (!form.value.username.trim()) {
    showError('请输入账号')
    return false
  }
  if (!form.value.password.trim()) {
    showError('请输入初始密码')
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
      username: form.value.username.trim(),
      password: form.value.password
    }

    const res = await shopApi.registerEmployee(shopId.value, data)
    if (res?.message?.includes('成功')) {
      showSuccess('店员添加成功')
      closeDialog()
      await loadEmployees()
    } else {
      showError(res?.message || '添加失败')
    }
  } catch (error) {
    console.error('添加店员失败:', error)
    showError('添加失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const handleRemove = async (employee) => {
  const result = await Swal.fire({
    title: '确认移除',
    text: `确定要移除店员 "${employee.name || employee.merchantId}" 吗？`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: '移除',
    cancelButtonText: '取消',
    confirmButtonColor: '#ef4444'
  })

  if (!result.isConfirmed) return

  try {
    const res = await shopApi.removeEmployee(shopId.value, employee.merchantId)
    if (res?.message?.includes('成功')) {
      showSuccess('店员移除成功')
      await loadEmployees()
    } else {
      showError(res?.message || '移除失败')
    }
  } catch (error) {
    console.error('移除店员失败:', error)
    showError('移除失败，请稍后重试')
  }
}

onMounted(() => {
  loadShopInfo()
  loadEmployees()
})
</script>

<style scoped>
.shop-employees {
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

.employees-container {
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

.employees-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.employee-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.employee-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.employee-avatar {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.employee-info {
  flex: 1;
  min-width: 0;
}

.employee-name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 6px 0;
}

.employee-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
}

.employee-id {
  font-size: 13px;
  color: #64748b;
}

.employee-role {
  font-size: 12px;
  padding: 2px 8px;
  background: #dbeafe;
  color: #1d4ed8;
  border-radius: 10px;
}

.employee-contact {
  font-size: 13px;
  color: #64748b;
}

.employee-actions {
  flex-shrink: 0;
}

.remove-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.remove-btn:hover {
  transform: translateY(-1px);
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
  max-width: 450px;
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

.form-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #000000;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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

.shop-employees::-webkit-scrollbar {
  width: 8px;
}

.shop-employees::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>