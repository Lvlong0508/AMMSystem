<!-- ShopList.vue -->
<template>
  <div class="shop-list">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的店铺</h2>
        <span v-if="shops.length > 0" class="badge">{{ shops.length }}</span>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadShops" :disabled="loading">
          刷新
        </button>
        <button class="add-btn" @click="$router.push('/shop/register')">
          创建店铺
        </button>
      </div>
    </div>

    <div class="shops-container">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="shops.length === 0" class="empty-state">
        <span class="empty-icon">🏪</span>
        <p>暂无店铺</p>
        <button class="create-btn" @click="$router.push('/shop/register')">
          创建一个店铺
        </button>
      </div>
      <div v-else class="shops-grid">
        <div
          v-for="shop in shops"
          :key="shop.id"
          class="shop-card"
        >
          <div class="shop-header">
            <div class="shop-info">
              <h3 class="shop-name">{{ shop.name }}</h3>
              <span v-if="shop.description" class="shop-desc">{{ shop.description }}</span>
            </div>
            <span class="status-badge" :class="shop.status === 1 ? 'active' : 'closed'">
              {{ shop.status === 1 ? '营业中' : '已关闭' }}
            </span>
          </div>

          <div class="shop-details">
            <div class="detail-item">
              <span class="detail-icon">📍</span>
              <span class="detail-text">{{ shop.address || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📞</span>
              <span class="detail-text">{{ shop.phone || '-' }}</span>
            </div>
            <div class="detail-item" v-if="shop.businessHours">
              <span class="detail-icon">🕐</span>
              <span class="detail-text">{{ shop.businessHours }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-icon">📅</span>
              <span class="detail-text">创建于 {{ formatDate(shop.createdAt) }}</span>
            </div>
          </div>

          <div class="shop-actions">
            <button class="action-btn" @click="goToProducts(shop.id)">
              商品管理
            </button>
            <button class="action-btn" @click="goToOrders(shop.id)">
              订单管理
            </button>
            <button class="action-btn" @click="goToEmployees(shop.id)">
              店员管理
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { shopApi } from '../../api/shop.js'
import { showSuccess, showError } from '../../utils/swal.js'

const router = useRouter()

const shops = ref([])
const loading = ref(false)

const loadShops = async () => {
  loading.value = true
  try {
    const res = await shopApi.list()
    // 后端返回: {success: true, shops: [...]}
    if (res?.success && res?.shops) {
      shops.value = res.shops
    } else if (res?.data?.shops) {
      shops.value = res.data.shops
    } else {
      showError(res?.message || '加载失败')
      shops.value = []
    }
  } catch (error) {
    console.error('加载店铺列表失败:', error)
    showError('加载失败，请稍后重试')
    shops.value = []
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const statusMap = {
    'ACTIVE': '营业中',
    'INACTIVE': '休息中',
    'CLOSED': '已关闭'
  }
  return statusMap[status] || status || '营业中'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const goToProducts = (shopId) => {
  router.push(`/shop/${shopId}/products`)
}

const goToOrders = (shopId) => {
  router.push(`/shop/${shopId}/orders`)
}

const goToEmployees = (shopId) => {
  router.push(`/shop/${shopId}/employees`)
}

onMounted(() => {
  loadShops()
})
</script>

<style scoped>
.shop-list {
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

.badge {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  font-size: 13px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 20px;
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

.refresh-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(100, 116, 139, 0.35);
}

.add-btn {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35);
}

.shops-container {
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

.create-btn {
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

.shops-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.shop-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.shop-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.shop-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.shop-name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.shop-desc {
  font-size: 14px;
  color: #64748b;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.active {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #047857;
}

.status-badge.inactive {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
}

.status-badge.closed {
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  color: #374151;
}

.shop-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #475569;
}

.detail-icon {
  font-size: 16px;
}

.shop-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  flex: 1;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #475569;
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
}

.shop-list::-webkit-scrollbar {
  width: 8px;
}

.shop-list::-webkit-scrollbar-track {
  background: transparent;
  margin: 8px 0;
}

.shop-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .shops-grid {
    grid-template-columns: 1fr;
  }
}
</style>