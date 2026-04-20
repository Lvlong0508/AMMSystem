<!-- src/components/OrderManager/OrderManager.vue -->
<template>
  <div class="order-manager">
    <!-- 标题栏 -->
    <div class="manager-header">
      <h2 class="manager-title">{{ T.PAGE_TITLE }}</h2>
      <button class="refresh-btn" @click="loadOrders" :disabled="loading">
        {{ loading ? T.BTN_LOADING : T.BTN_REFRESH }}
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">{{ T.LABEL_STATUS }}</span>
        <select v-model="filterStatus" class="filter-select" @change="handleStatusFilter">
          <option value="">{{ T.OPTION_ALL }}</option>
          <option :value="ORDER_STATUS.PENDING">{{ T.OPTION_PENDING }}</option>
          <option :value="ORDER_STATUS.PAID">{{ T.OPTION_PAID }}</option>
          <option :value="ORDER_STATUS.SHIPPED">{{ T.OPTION_SHIPPED }}</option>
          <option :value="ORDER_STATUS.DELIVERED">{{ T.OPTION_DELIVERED }}</option>
          <option :value="ORDER_STATUS.CANCELLED">{{ T.OPTION_CANCELLED }}</option>
        </select>
      </div>
      <div class="filter-item">
        <span class="filter-label">{{ T.LABEL_CUSTOMER }}</span>
        <input
          type="text"
          v-model="searchCustomer"
          :placeholder="T.PLACEHOLDER_CUSTOMER"
          class="filter-input"
          @keyup.enter="handleCustomerSearch"
        />
        <button class="search-btn" @click="handleCustomerSearch">{{ T.BTN_SEARCH }}</button>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="orders-container">
      <div v-if="loading" class="loading-state">{{ T.LOADING_TEXT }}</div>
      <div v-else-if="orders.length === 0" class="empty-state">
        <span class="empty-icon">{{ T.EMPTY_ICON }}</span>
        <p>{{ T.EMPTY_TEXT }}</p>
      </div>
      <div v-else class="orders-list">
        <div
          v-for="order in orders"
          :key="order.orderId"
          class="order-card"
          :class="getStatusClass(order.orderStatus)"
        >
          <!-- 订单头部 -->
          <div class="order-header">
            <div class="order-info">
              <span class="order-id">{{ order.orderId }}</span>
              <span class="order-date">{{ formatDate(order.orderDate) }}</span>
            </div>
            <span class="status-badge" :class="getStatusClass(order.orderStatus)">
              {{ getStatusText(order.orderStatus) }}
            </span>
          </div>

          <!-- 订单内容 -->
          <div class="order-body">
            <!-- 商品信息 -->
            <div class="product-section">
              <span class="section-icon">{{ T.ICON_PRODUCT }}</span>
              <div class="product-info-left">
                <span class="product-id">ID:{{ order.productId }}</span>
                <span class="product-name" v-if="order.productName">{{ order.productName }}</span>
              </div>
              <div class="product-info-right">
                <span class="quantity-label">x</span>
                <span class="quantity-value">{{ order.quantity }}</span>
              </div>
            </div>

            <!-- 价格 -->
            <div class="price-section">
              <span class="price-label">{{ T.LABEL_TOTAL_PRICE }}</span>
              <span class="price-value">¥{{ order.totalPrice?.toFixed(2) }}</span>
            </div>

            <!-- 收货信息 -->
            <div class="contact-section" v-if="order.contact">
              <div class="contact-item">
                <span class="contact-icon">{{ T.ICON_CONTACT }}</span>
                <span>{{ order.contact.name }}</span>
              </div>
              <div class="contact-item">
                <span class="contact-icon">{{ T.ICON_PHONE }}</span>
                <span>{{ order.contact.phone }}</span>
              </div>
              <div class="contact-item">
                <span class="contact-icon">{{ T.ICON_ADDRESS }}</span>
                <span class="address">{{ order.contact.address }}</span>
              </div>
            </div>

            <!-- 物流信息 -->
            <div class="tracking-section" v-if="order.orderStatus === ORDER_STATUS.SHIPPED || order.orderStatus === ORDER_STATUS.DELIVERED || order.orderStatus === ORDER_STATUS.RETURNED">
              <div class="tracking-item">
                <span class="tracking-label">{{ T.LABEL_TRACKING }}</span>
                <span class="tracking-value">{{ order.logistics?.trackingNumber || '-' }}</span>
              </div>
              <div class="tracking-item" v-if="order.logistics?.shippingDate">
                <span class="tracking-label">{{ T.LABEL_SHIPPING_DATE }}</span>
                <span class="tracking-value">{{ formatDate(order.logistics.shippingDate) }}</span>
              </div>
            </div>
          </div>

          <!-- 订单操作按钮 -->
          <div class="order-actions">
            <button class="action-btn view-btn" @click="showOrderDetail(order)">
              👁️ {{ T.BTN_DETAIL }}
            </button>
            <button
              v-if="order.orderStatus === ORDER_STATUS.PENDING"
              class="action-btn pay-btn"
              @click="updateStatus(order.orderId, ORDER_STATUS.PAID)"
            >
              💳 {{ T.BTN_MARK_PAID }}
            </button>
            <button
              v-if="order.orderStatus === ORDER_STATUS.SHIPPED"
              class="action-btn complete-btn"
              @click="updateStatus(order.orderId, ORDER_STATUS.DELIVERED)"
            >
              ✅ {{ T.BTN_COMPLETE }}
            </button>
            <button
              v-if="order.orderStatus === ORDER_STATUS.SHIPPED || order.orderStatus === ORDER_STATUS.DELIVERED"
              class="action-btn return-btn"
              @click="confirmReturn(order.orderId)"
            >
              ↩️ {{ T.BTN_RETURN }}
            </button>
            <button
              v-if="order.orderStatus === ORDER_STATUS.PENDING || order.orderStatus === ORDER_STATUS.PAID"
              class="action-btn cancel-btn"
              @click="updateStatus(order.orderId, ORDER_STATUS.CANCELLED)"
            >
              ❌ {{ T.BTN_CANCEL }}
            </button>
            <button
              v-if="order.orderStatus === ORDER_STATUS.CANCELLED || order.orderStatus === ORDER_STATUS.RETURNED || order.orderStatus === ORDER_STATUS.DELIVERED"
              class="action-btn delete-btn"
              @click="confirmDelete(order.orderId)"
            >
              🗑️ {{ T.BTN_DELETE }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="detailVisible" class="dialog-overlay" @click="closeDetail">
      <div class="detail-dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ T.DIALOG_TITLE_DETAIL }}</h3>
          <button class="close-btn" @click="closeDetail">{{ T.BTN_CLOSE }}</button>
        </div>
        <div class="dialog-body" v-if="selectedOrder">
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_ORDER_ID }}</span>
            <span class="detail-value">{{ selectedOrder.orderId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_ORDER_STATUS }}</span>
            <span class="detail-value" :class="getStatusClass(selectedOrder.orderStatus)">
              <span class="status-badge">
                {{ getStatusText(selectedOrder.orderStatus) }}
              </span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_PRODUCT_ID_DETAIL }}</span>
            <span class="detail-value">{{ selectedOrder.productId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_QUANTITY_DETAIL }}</span>
            <span class="detail-value">{{ selectedOrder.quantity }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_TOTAL_PRICE_DETAIL }}</span>
            <span class="detail-value price">¥{{ selectedOrder.totalPrice?.toFixed(2) }}</span>
          </div>
          <div class="detail-section" v-if="selectedOrder.contact">
            <h4>{{ T.SECTION_CONTACT }}</h4>
            <div class="detail-row">
              <span class="detail-label">{{ T.LABEL_CONTACT_NAME }}</span>
              <span class="detail-value">{{ selectedOrder.contact.name }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">{{ T.LABEL_CONTACT_PHONE }}</span>
              <span class="detail-value">{{ selectedOrder.contact.phone }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">{{ T.LABEL_CONTACT_ADDRESS }}</span>
              <span class="detail-value">{{ selectedOrder.contact.address }}</span>
            </div>
          </div>
          <div class="detail-section" v-if="selectedOrder.orderStatus === ORDER_STATUS.SHIPPED || selectedOrder.orderStatus === ORDER_STATUS.DELIVERED || selectedOrder.orderStatus === ORDER_STATUS.RETURNED">
            <h4>{{ T.SECTION_TRACKING }}</h4>
            <div class="detail-row">
              <span class="detail-label">{{ T.LABEL_TRACKING_NUMBER }}</span>
              <span class="detail-value">{{ selectedOrder.logistics?.trackingNumber || '-' }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.logistics?.shippingDate">
              <span class="detail-label">{{ T.LABEL_SHIPPING_DATE_DETAIL }}</span>
              <span class="detail-value">{{ formatDate(selectedOrder.logistics.shippingDate) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useOrderManager } from './useOrderManager.js'
import { ORDER_TEXT as T } from './Text.js'
import { ORDER_STATUS } from '../../config/orderStatus.js'

const {
  orders,
  loading,
  filterStatus,
  searchCustomer,
  detailVisible,
  selectedOrder,
  loadOrders,
  handleStatusFilter,
  handleCustomerSearch,
  getStatusClass,
  getStatusText,
  formatDate,
  showOrderDetail,
  closeDetail,
  updateStatus,
  confirmDelete,
  confirmReturn
} = useOrderManager()
</script>

<style scoped>
@import './OrderManager.css';
</style>
