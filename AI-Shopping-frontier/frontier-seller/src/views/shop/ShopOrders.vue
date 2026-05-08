<!-- ShopOrders.vue -->
<template>
  <div class="shop-orders">
    <div class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.push('/shop/list')">
          返回
        </button>
        <h2 class="page-title">订单管理</h2>
        <span v-if="shopInfo" class="shop-name">- {{ shopInfo.name }}</span>
      </div>
      <button class="refresh-btn" @click="loadOrders" :disabled="loading">
        刷新
      </button>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">状态</span>
        <select v-model="filterStatus" class="filter-select" @change="handleFilter">
          <option value="">全部</option>
          <option value="PENDING">待支付</option>
          <option value="PAID">待发货</option>
          <option value="SHIPPED">已发货</option>
          <option value="DELIVERED">已送达</option>
          <option value="CANCELLED">已取消</option>
          <option value="RETURNED">已退货</option>
        </select>
      </div>
      <div class="filter-item">
        <input
          type="text"
          v-model="searchKeyword"
          placeholder="搜索订单号或商品"
          class="filter-input"
        />
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
    </div>

    <div class="orders-container">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="orders.length === 0" class="empty-state">
        <span class="empty-icon">📋</span>
        <p>暂无订单</p>
      </div>
      <div v-else class="orders-list">
        <div
          v-for="order in filteredOrders"
          :key="order.orderId"
          class="order-card"
          :class="getStatusClass(order.orderStatus)"
        >
          <div class="order-header">
            <div class="order-info">
              <span class="order-id">订单: {{ order.orderId }}</span>
              <span class="order-date">{{ formatDate(order.orderDate) }}</span>
            </div>
            <span class="status-badge" :class="getStatusClass(order.orderStatus)">
              {{ getStatusText(order.orderStatus) }}
            </span>
          </div>

          <div class="order-body">
            <div class="product-section">
              <span class="section-icon">📦</span>
              <div class="product-info-left">
                <span class="product-id">商品ID: {{ order.productId }}</span>
                <span class="product-name" v-if="order.productName">{{ order.productName }}</span>
              </div>
              <div class="product-info-right">
                <span class="quantity-label">x</span>
                <span class="quantity-value">{{ order.quantity }}</span>
              </div>
            </div>

            <div class="price-section">
              <span class="price-label">总价</span>
              <span class="price-value">¥{{ order.totalPrice?.toFixed(2) }}</span>
            </div>

            <div class="contact-section" v-if="order.contact">
              <div class="contact-item">
                <span class="contact-icon">👤</span>
                <span>{{ order.contact.name }}</span>
              </div>
              <div class="contact-item">
                <span class="contact-icon">📞</span>
                <span>{{ order.contact.phone }}</span>
              </div>
              <div class="contact-item">
                <span class="contact-icon">📍</span>
                <span class="address">{{ order.contact.address }}</span>
              </div>
            </div>

            <div class="tracking-section" v-if="order.logistics?.trackingNumber">
              <div class="tracking-item">
                <span class="tracking-label">物流单号:</span>
                <span class="tracking-value">{{ order.logistics.trackingNumber }}</span>
              </div>
            </div>
          </div>

          <div class="order-actions">
            <button class="action-btn view-btn" @click="showDetail(order)">
              👁️ 详情
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detailVisible" class="dialog-overlay" @click="closeDetail">
      <div class="detail-dialog" @click.stop>
        <div class="dialog-header">
          <h3>订单详情</h3>
          <button class="close-btn" @click="closeDetail">×</button>
        </div>
        <div class="dialog-body" v-if="selectedOrder">
          <div class="detail-row">
            <span class="detail-label">订单编号</span>
            <span class="detail-value">{{ selectedOrder.orderId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">订单状态</span>
            <span class="detail-value status-badge" :class="getStatusClass(selectedOrder.orderStatus)">
              {{ getStatusText(selectedOrder.orderStatus) }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">商品ID</span>
            <span class="detail-value">{{ selectedOrder.productId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">商品名称</span>
            <span class="detail-value">{{ selectedOrder.productName || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">数量</span>
            <span class="detail-value">{{ selectedOrder.quantity }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">总价</span>
            <span class="detail-value price">¥{{ selectedOrder.totalPrice?.toFixed(2) }}</span>
          </div>
          <div class="detail-section" v-if="selectedOrder.contact">
            <h4>收货人信息</h4>
            <div class="detail-row">
              <span class="detail-label">姓名</span>
              <span class="detail-value">{{ selectedOrder.contact.name }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">电话</span>
              <span class="detail-value">{{ selectedOrder.contact.phone }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">地址</span>
              <span class="detail-value">{{ selectedOrder.contact.address }}</span>
            </div>
          </div>
          <div class="detail-section" v-if="selectedOrder.logistics">
            <h4>物流信息</h4>
            <div class="detail-row">
              <span class="detail-label">物流单号</span>
              <span class="detail-value">{{ selectedOrder.logistics.trackingNumber || '-' }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.logistics.shippingDate">
              <span class="detail-label">发货时间</span>
              <span class="detail-value">{{ selectedOrder.logistics.shippingDate }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shopApi } from '../../api/shop.js'
import { showError } from '../../utils/swal.js'

const route = useRoute()

const shopId = computed(() => route.params.shopId)
const shopInfo = ref(null)
const orders = ref([])
const loading = ref(false)

const filterStatus = ref('')
const searchKeyword = ref('')
const filteredOrders = computed(() => {
  let result = orders.value
  if (filterStatus.value) {
    result = result.filter(o => o.orderStatus === filterStatus.value)
  }
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.trim().toLowerCase()
    result = result.filter(o =>
      o.orderId?.toLowerCase().includes(keyword) ||
      o.productName?.toLowerCase().includes(keyword)
    )
  }
  return result
})

const detailVisible = ref(false)
const selectedOrder = ref(null)

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

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await shopApi.orders(shopId.value)
    if (res?.data) {
      orders.value = Array.isArray(res.data) ? res.data : []
    } else if (res?.orders) {
      orders.value = res.orders
    } else {
      orders.value = []
    }
  } catch (error) {
    console.error('加载订单失败:', error)
    showError('加载失败')
    orders.value = []
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {}

const handleSearch = () => {}

const getStatusClass = (status) => {
  const classMap = {
    'PENDING': 'status-pending',
    'PAID': 'status-paid',
    'SHIPPED': 'status-shipped',
    'DELIVERED': 'status-delivered',
    'CANCELLED': 'status-cancelled',
    'RETURNED': 'status-returned'
  }
  return classMap[status] || ''
}

const getStatusText = (status) => {
  const textMap = {
    'PENDING': '待支付',
    'PAID': '待发货',
    'SHIPPED': '已发货',
    'DELIVERED': '已送达',
    'CANCELLED': '已取消',
    'RETURNED': '已退货'
  }
  return textMap[status] || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const showDetail = (order) => {
  selectedOrder.value = order
  detailVisible.value = true
}

const closeDetail = () => {
  detailVisible.value = false
  selectedOrder.value = null
}

onMounted(() => {
  loadShopInfo()
  loadOrders()
})
</script>

<style scoped>
.shop-orders {
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

.refresh-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
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

.filter-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.filter-select,
.filter-input {
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.filter-select:focus,
.filter-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.filter-input {
  width: 220px;
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

.orders-container {
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

.orders-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.order-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.order-card::before {
  content: '';
  display: block;
  height: 4px;
  border-radius: 2px 2px 0 0;
  margin: -20px -20px 16px;
  background: linear-gradient(90deg, #3b82f6 0%, #8b5cf6 100%);
}

.order-card.status-paid::before { background: linear-gradient(90deg, #f59e0b 0%, #fbbf24 100%); }
.order-card.status-shipped::before { background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%); }
.order-card.status-delivered::before { background: linear-gradient(90deg, #10b981 0%, #34d399 100%); }
.order-card.status-cancelled::before { background: linear-gradient(90deg, #ef4444 0%, #f87171 100%); }

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;
}

.order-id {
  font-weight: 700;
  color: #0f172a;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.order-date {
  font-size: 13px;
  color: #94a3b8;
}

.status-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status-badge.status-pending { background: #fef3c7; color: #b45309; }
.status-badge.status-paid { background: #dbeafe; color: #1d4ed8; }
.status-badge.status-shipped { background: #dbeafe; color: #1d4ed8; }
.status-badge.status-delivered { background: #d1fae5; color: #047857; }
.status-badge.status-cancelled { background: #fee2e2; color: #b91c1c; }
.status-badge.status-returned { background: #e5e7eb; color: #374151; }

.order-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 12px;
}

.section-icon {
  font-size: 24px;
}

.product-info-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.product-id {
  font-size: 13px;
  color: #64748b;
}

.product-name {
  font-size: 15px;
  color: #0f172a;
  font-weight: 600;
}

.product-info-right {
  padding: 6px 12px;
  background: white;
  border-radius: 8px;
}

.quantity-label {
  font-size: 14px;
  color: #94a3b8;
}

.quantity-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.price-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  background: #fef2f2;
  border-radius: 12px;
  border: 1px solid #fecaca;
}

.price-label {
  font-size: 14px;
  color: #64748b;
}

.price-value {
  font-size: 20px;
  font-weight: 700;
  color: #ef4444;
}

.contact-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  background: #f0f9ff;
  border-radius: 12px;
  border: 1px solid #bae6fd;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #475569;
}

.contact-icon {
  font-size: 16px;
}

.address {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tracking-section {
  padding: 14px;
  background: #eff6ff;
  border-radius: 12px;
  border: 1px solid #bfdbfe;
}

.tracking-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.tracking-label {
  font-weight: 500;
  color: #64748b;
}

.tracking-value {
  color: #1e293b;
  font-weight: 600;
  font-family: monospace;
  background: rgba(255, 255, 255, 0.7);
  padding: 4px 10px;
  border-radius: 6px;
}

.order-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.action-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.view-btn {
  background: #f1f5f9;
  color: #475569;
}

.view-btn:hover {
  background: #e2e8f0;
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

.detail-dialog {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
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
  overflow-y: auto;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;
}

.detail-label {
  font-size: 14px;
  color: #64748b;
}

.detail-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}

.detail-value.price {
  color: #ef4444;
  font-size: 20px;
}

.detail-section {
  margin-top: 20px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.detail-section h4 {
  margin: 0 0 14px 0;
  font-size: 15px;
  color: #334155;
}

.shop-orders::-webkit-scrollbar {
  width: 8px;
}

.shop-orders::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
</style>