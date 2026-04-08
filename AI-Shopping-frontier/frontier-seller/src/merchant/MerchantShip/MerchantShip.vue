<!-- src/merchant/MerchantShip/MerchantShip.vue -->
<template>
  <div class="merchant-ship">
    <!-- 标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">{{ T.PAGE_TITLE }}</h2>
        <span v-if="pendingShipCount > 0" class="badge">{{ pendingShipCount }}</span>
      </div>
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
          <option :value="ORDER_STATUS.PAID">{{ T.OPTION_PAID }}</option>
          <option :value="ORDER_STATUS.SHIPPED">{{ T.OPTION_SHIPPED }}</option>
          <option :value="ORDER_STATUS.RETURNED">{{ T.OPTION_RETURNED }}</option>
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
            <div class="product-section">
              <span class="section-icon">{{ T.ICON_PRODUCT }}</span>
              <div class="product-details">
                <span class="product-id">{{ T.LABEL_PRODUCT_ID }}{{ order.productId }}</span>
                <span class="quantity">{{ T.LABEL_QUANTITY }}{{ order.quantity }}</span>
              </div>
            </div>

            <div class="price-section">
              <span class="price-label">{{ T.LABEL_TOTAL_PRICE }}</span>
              <span class="price-value">¥{{ order.totalPrice?.toFixed(2) }}</span>
            </div>

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

            <!-- 物流信息 - 已发货及以上状态显示 -->
            <div class="tracking-section" v-if="order.orderStatus === ORDER_STATUS.SHIPPED || order.orderStatus === ORDER_STATUS.DELIVERED || order.orderStatus === ORDER_STATUS.RETURNED">
              <div class="tracking-item">
                <span class="tracking-label">发货物流单号:</span>
                <span class="tracking-value">{{ order.logistics?.trackingNumber || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 订单操作 -->
          <div class="order-actions">
            <button
              class="action-btn view-btn"
              @click="showOrderDetail(order)"
            >
              {{ T.BTN_DETAIL }}
            </button>
            <button
              v-if="order.orderStatus === ORDER_STATUS.PAID"
              class="action-btn ship-btn"
              @click="showShipDialog(order)"
            >
              {{ T.BTN_SHIP }}
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
            <span class="detail-value status-badge" :class="getStatusClass(selectedOrder.orderStatus)">
              {{ getStatusText(selectedOrder.orderStatus) }}
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
          <div class="detail-section" v-if="selectedOrder.logistics?.shipper">
            <h4>发货人信息</h4>
            <div class="detail-row" v-if="selectedOrder.logistics.shipper.name">
              <span class="detail-label">姓名</span>
              <span class="detail-value">{{ selectedOrder.logistics.shipper.name }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.logistics.shipper.phone">
              <span class="detail-label">电话</span>
              <span class="detail-value">{{ selectedOrder.logistics.shipper.phone }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.logistics.shipper.address">
              <span class="detail-label">地址</span>
              <span class="detail-value">{{ selectedOrder.logistics.shipper.address }}</span>
            </div>
          </div>
          <div class="detail-section" v-if="selectedOrder.orderStatus === ORDER_STATUS.SHIPPED || selectedOrder.orderStatus === ORDER_STATUS.DELIVERED || selectedOrder.orderStatus === ORDER_STATUS.RETURNED">
            <h4>物流信息</h4>
            <div class="detail-row">
              <span class="detail-label">发货物流单号</span>
              <span class="detail-value">{{ selectedOrder.logistics?.trackingNumber || '-' }}</span>
            </div>
            <div class="detail-row" v-if="selectedOrder.logistics?.shippingDate">
              <span class="detail-label">发货时间</span>
              <span class="detail-value">{{ selectedOrder.logistics.shippingDate }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发货弹窗 -->
    <div v-if="shipVisible" class="dialog-overlay" @click="closeShipDialog">
      <div class="ship-dialog" @click.stop>
        <div class="dialog-header">
          <h3>{{ T.DIALOG_TITLE_SHIP }}</h3>
          <button class="close-btn" @click="closeShipDialog">{{ T.BTN_CLOSE }}</button>
        </div>
        <div class="dialog-body">
          <div class="order-info-summary" v-if="selectedOrder">
            <p><strong>订单编号:</strong> {{ selectedOrder.orderId }}</p>
            <p><strong>收货人:</strong> {{ selectedOrder.contact?.name }}</p>
            <p><strong>收货地址:</strong> {{ selectedOrder.contact?.address }}</p>
          </div>
          <div class="form-group">
            <label>选择发货联系人 <span class="required">*</span></label>
            <select 
              v-model="shipForm.selectedContactId" 
              class="form-input"
              :disabled="contactsLoading"
            >
              <option value="">{{ contactsLoading ? '加载中...' : '请选择发货联系人' }}</option>
              <option 
                v-for="contact in contacts" 
                :key="contact.id" 
                :value="contact.id"
              >
                {{ contact.name }} - {{ contact.phone }} - {{ contact.address }}
              </option>
            </select>
            <p v-if="contacts.length === 0 && !contactsLoading" class="contact-hint">
              暂无联系人，请先<a href="#/contacts">添加联系人</a>
            </p>
          </div>
          <div class="form-group">
            <label>{{ T.LABEL_TRACKING_INPUT }} <span class="required">{{ T.REQUIRED_MARK }}</span></label>
            <input
              type="text"
              v-model="shipForm.trackingNumber"
              :placeholder="T.PLACEHOLDER_TRACKING"
              class="form-input"
            />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="closeShipDialog">{{ T.BTN_CANCEL }}</button>
          <button
            class="btn-submit"
            :disabled="!shipForm.trackingNumber || !shipForm.selectedContactId || shipping"
            @click="handleShip"
          >
            {{ shipping ? T.BTN_SHIPPING : T.BTN_CONFIRM_SHIP }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useMerchantShip } from './useMerchantShip.js'
import { MERCHANT_TEXT as T } from './Text.js'
import { ORDER_STATUS } from '../../config/orderStatus.js'

const {
  orders,
  loading,
  filterStatus,
  searchCustomer,
  pendingShipCount,
  detailVisible,
  selectedOrder,
  shipVisible,
  shipForm,
  shipping,
  contacts,
  contactsLoading,
  loadOrders,
  handleStatusFilter,
  handleCustomerSearch,
  getStatusClass,
  getStatusText,
  formatDate,
  showOrderDetail,
  closeDetail,
  showShipDialog,
  closeShipDialog,
  handleShip
} = useMerchantShip()
</script>

<style scoped>
@import './MerchantShip.css';
</style>
