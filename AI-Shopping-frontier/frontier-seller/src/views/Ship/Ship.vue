<template>
  <div class="ship-page">
    <div class="ship-page__toolbar">
      <div class="ship-page__toolbar-left">
        <h2 class="ship-page__title">{{ T.PAGE_TITLE }}</h2>
        <el-tag v-if="pendingShipCount > 0" type="warning" class="ship-page__badge">
          {{ pendingShipCount }} 笔待发货
        </el-tag>
      </div>
      <div class="ship-page__toolbar-right">
        <el-button :loading="loading" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
      </div>
    </div>

    <div class="ship-page__filters">
      <el-input
        v-model="searchKeyword"
        :placeholder="T.LABEL_CUSTOMER"
        style="width: 200px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button @click="handleSearch">{{ T.BTN_SEARCH }}</el-button>
    </div>

    <div v-loading="loading" class="order-grid">
      <div
        v-for="order in orders"
        :key="order.orderId"
        class="ship-card"
        @click="showOrderDetail(order)"
      >
        <div class="ship-card__body">
          <div class="ship-card__left">
            <div class="ship-card__thumb">
              <el-image
                v-if="order.productImageUrl"
                :src="order.productImageUrl"
                style="width: 64px; height: 64px; border-radius: 6px"
                fit="cover"
              />
              <div v-else class="ship-card__thumb-empty" />
            </div>
            <div class="ship-card__info">
              <div class="ship-card__id">{{ order.orderId }}</div>
              <div class="ship-card__name">{{ order.productName || T.LABEL_PRODUCT }}</div>
              <div class="ship-card__qty">{{ T.LABEL_QUANTITY }}: {{ order.quantity }}</div>
            </div>
          </div>
          <div class="ship-card__side">
            <el-tag type="warning" size="small">{{ getStatusText(order.orderStatus) }}</el-tag>
            <span class="ship-card__date">{{ formatDate(order.orderDate) }}</span>
            <div class="ship-card__contact">
              <div>{{ order.contactName }}</div>
              <div>{{ order.contactPhone }}</div>
              <div class="ship-card__caddr">{{ order.contactAddress }}</div>
            </div>
          </div>
        </div>
        <div class="ship-card__bar">
          <el-button type="primary" size="default" @click.stop="showShipDialog(order)">
            {{ T.BTN_SHIP }}
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && orders.length === 0" :description="T.EMPTY_TEXT" />
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="600px" :close-on-click-modal="false">
      <div v-if="detailLoading" v-loading="detailLoading" style="height: 100px" />
      <template v-else-if="selectedOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="T.LABEL_ORDER_ID" :span="2">{{ selectedOrder.orderId }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_STATUS_COL">
            <el-tag :type="getStatusType(selectedOrder.orderStatus)">{{ getStatusText(selectedOrder.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_DATE">{{ formatDate(selectedOrder.orderDate) }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_PRODUCT" :span="2">{{ selectedOrder.productName || T.LABEL_PRODUCT }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_QUANTITY">{{ selectedOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_TOTAL">{{ formatPrice(selectedOrder.totalPrice) }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-section">
          <h4 class="detail-section__title">{{ T.SECTION_CONTACT }}</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="T.LABEL_CONTACT_NAME">{{ selectedOrder.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_CONTACT_PHONE">{{ selectedOrder.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_CONTACT_ADDRESS">{{ selectedOrder.contactAddress || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
      <template #footer>
        <el-button @click="closeDetail">{{ T.BTN_CANCEL }}</el-button>
        <el-button v-if="selectedOrder && selectedOrder.orderStatus === ORDER_STATUS.PAID" type="primary" @click="closeDetail(); showShipDialog(selectedOrder)">{{ T.BTN_SHIP }}</el-button>
      </template>
    </el-dialog>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" :title="T.DIALOG_SHIP" width="500px" :close-on-click-modal="false">
      <div v-if="selectedOrder" style="margin-bottom: 16px">
        <p><strong>{{ T.LABEL_ORDER_ID }}:</strong> {{ selectedOrder.orderId }}</p>
        <p><strong>{{ T.LABEL_CONTACT_NAME }}:</strong> {{ selectedOrder.contactName }} {{ selectedOrder.contactPhone }}</p>
        <p><strong>{{ T.LABEL_CONTACT_ADDRESS }}:</strong> {{ selectedOrder.contactAddress }}</p>
      </div>
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_SELECT_CONTACT">
          <el-select v-model="shipForm.selectedContactId" style="width: 100%" :loading="contactsLoading" :placeholder="T.LABEL_SELECT_CONTACT">
            <el-option v-for="c in contacts" :key="c.id" :label="`${c.name} - ${c.phone} - ${c.address}`" :value="c.id" />
          </el-select>
          <div v-if="!contactsLoading && contacts.length === 0" style="color: var(--color-text-secondary); font-size: var(--text-xs); margin-top: 4px">{{ T.NO_CONTACTS }}</div>
        </el-form-item>
        <el-form-item :label="T.LABEL_TRACKING">
          <el-input v-model="shipForm.trackingNumber" :placeholder="T.PLACEHOLDER_TRACKING" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeShipDialog">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="shipping" :disabled="!shipForm.trackingNumber || !shipForm.selectedContactId" @click="handleShip">{{ shipping ? T.BTN_SHIPPING : T.BTN_CONFIRM_SHIP }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShip } from "./Ship.js"
const { T, orders, loading, searchKeyword, pendingShipCount, contacts, contactsLoading, detailVisible, detailLoading, selectedOrder, shipVisible, shipForm, shipping, ORDER_STATUS, loadOrders, handleSearch, getStatusType, getStatusText, formatPrice, formatDate, showOrderDetail, closeDetail, showShipDialog, closeShipDialog, handleShip } = useShip()
</script>

<style scoped src="./Ship.css"></style>
