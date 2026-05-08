<!-- ShopRegister.vue -->
<template>
  <div class="shop-register">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">创建店铺</h2>
      </div>
      <button class="back-btn" @click="$router.push('/shop/list')">
        返回店铺列表
      </button>
    </div>

    <div class="form-container">
      <div class="form-card">
        <h3 class="form-title">店铺信息</h3>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>店铺名称 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.name"
              placeholder="请输入店铺名称"
              class="form-input"
              maxlength="50"
            />
          </div>

          <div class="form-group">
            <label>店铺简介</label>
            <textarea
              v-model="form.description"
              placeholder="请输入店铺简介（可选）"
              class="form-textarea"
              rows="3"
              maxlength="200"
            ></textarea>
          </div>

          <div class="form-group">
            <label>店铺地址 <span class="required">*</span></label>
            <input
              type="text"
              v-model="form.address"
              placeholder="请输入店铺地址"
              class="form-input"
              maxlength="100"
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
            <label>营业时间</label>
            <input
              type="text"
              v-model="form.businessHours"
              placeholder="例如：09:00-22:00"
              class="form-input"
              maxlength="50"
            />
          </div>

          <div class="form-actions">
            <button type="button" class="btn-cancel" @click="$router.push('/shop/list')">
              取消
            </button>
            <button type="submit" class="btn-submit" :disabled="submitting">
              {{ submitting ? '创建中...' : '创建店铺' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { shopApi } from '../../api/shop.js'
import { showSuccess, showError } from '../../utils/swal.js'

const router = useRouter()

const form = ref({
  name: '',
  description: '',
  address: '',
  phone: '',
  businessHours: ''
})

const submitting = ref(false)

const validate = () => {
  if (!form.value.name.trim()) {
    showError('请输入店铺名称')
    return false
  }
  if (!form.value.address.trim()) {
    showError('请输入店铺地址')
    return false
  }
  if (!form.value.phone.trim()) {
    showError('请输入联系电话')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validate()) return

  submitting.value = true
  try {
    const res = await shopApi.register(form.value)
    if (res?.message?.includes('成功')) {
      showSuccess('店铺创建成功')
      router.push('/shop/list')
    } else {
      showError(res?.message || '创建失败')
    }
  } catch (error) {
    console.error('创建店铺失败:', error)
    showError('创建失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.shop-register {
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

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.back-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(100, 116, 139, 0.35);
}

.form-container {
  max-width: 600px;
}

.form-card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.05);
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 24px 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
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
  background: white;
  transition: all 0.2s;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-submit {
  padding: 10px 24px;
  border: none;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.shop-register::-webkit-scrollbar {
  width: 8px;
}

.shop-register::-webkit-scrollbar-track {
  background: transparent;
  margin: 8px 0;
}

.shop-register::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>