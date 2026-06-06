<template>
  <div class="ship-page">
    <div class="ship-page__toolbar">
      <div class="ship-page__toolbar-left">
        <h2 class="ship-page__title">{{ T.PAGE_TITLE }}</h2>
        <el-tag v-if="pendingShipCount > 0" type="warning"  class="ship-page__badge">
          {{ pendingShipCount }} 笔待发货
        </el-tag>

      </div>
      <div class="ship-page__toolbar-right">
        <el-button  :loading="loading" @click="loadOrders">
          {{ T.BTN_REFRESH }}
        </el-button>
      </div>
    </div>

    <div class="ship-page__filters">
      <el-select
        v-model="filterStatus"
        
        style="width: 130px"
        clearable
        :placeholder="T.LABEL_STATUS"
        @change="loadOrders"
      >
        <el-option :label="T.OPTION_ALL" value="" />
        <el-option :label="T.OPTION_PAID" :value="ORDER_STATUS.PAID" />
        <el-option :label="T.OPTION_SHIPPED" :value="ORDER_STATUS.SHIPPED" />
        <el-option :label="T.OPTION_RETURNED" :value="ORDER_STATUS.RETURNED" />
      </el-select>
      <el-input
        v-model="searchCustomer"
        
        :placeholder="T.LABEL_CUSTOMER"
        style="width: 200px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button  @click="handleSearch">{{ T.BTN_SEARCH }}</el-button>
    </div>

    <div v-loading="loading" class="order-grid">
      <OrderCard
        v-for="order in orders"
        :key="order.orderId"
        :order="order"
        @detail="showOrderDetail"
        @ship="showShipDialog"
      />

      <el-empty v-if="!loading && orders.length === 0" :description="T.EMPTY_TEXT" />
    </div>

    <el-dialog
      v-model="detailVisible"
      :title="T.DIALOG_DETAIL"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="detailLoading" v-loading="detailLoading" style="height: 100px" />
      <template v-else-if="selectedOrder">
        <el-descriptions :column="2" border >
          <el-descriptions-item :label="T.LABEL_ORDER_ID" :span="2">
            {{ selectedOrder.orderId }}
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_STATUS_COL">
            <el-tag :type="getStatusType(selectedOrder.orderStatus)" >
              {{ getStatusText(selectedOrder.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_DATE">
            {{ formatDate(selectedOrder.orderDate) }}
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_PRODUCT" :span="2">
            {{ selectedOrder.productName || `商品 #${selectedOrder.productId}` }}
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_QUANTITY">
            {{ selectedOrder.quantity }}
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_TOTAL">
            {{ formatPrice(selectedOrder.totalPrice) }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedOrder.contact" class="detail-section">
          <h4 class="detail-section__title">收货人信息</h4>
          <el-descriptions :column="1" border >
            <el-descriptions-item :label="T.LABEL_CONTACT_NAME">
              {{ selectedOrder.contact.name }}
            </el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_CONTACT_PHONE">
              {{ selectedOrder.contact.phone }}
            </el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_CONTACT_ADDRESS">
              {{ selectedOrder.contact.address }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div
          v-if="selectedOrder.orderStatus === ORDER_STATUS.SHIPPED || selectedOrder.orderStatus === ORDER_STATUS.DELIVERED || selectedOrder.orderStatus === ORDER_STATUS.RETURNED"
          class="detail-section"
        >
          <h4 class="detail-section__title">物流信息</h4>
          <el-descriptions :column="1" border >
            <el-descriptions-item :label="T.LABEL_TRACKING">
              {{ selectedOrder.logistics?.trackingNumber || '-' }}
            </el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_SHIP_DATE">
              {{ selectedOrder.logistics?.shippingDate || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="shipVisible"
      :title="T.DIALOG_SHIP"
      width="500px"
      :close-on-click-modal="false"
    >
      <div v-if="selectedOrder" style="margin-bottom: 16px;">
        <p><strong>{{ T.LABEL_ORDER_ID }}:</strong> {{ selectedOrder.orderId }}</p>
        <p v-if="selectedOrder.contact">
          <strong>{{ T.LABEL_CONTACT_NAME }}:</strong>
          {{ selectedOrder.contact.name }} {{ selectedOrder.contact.phone }}
        </p>
        <p v-if="selectedOrder.contact">
          <strong>{{ T.LABEL_CONTACT_ADDRESS }}:</strong>
          {{ selectedOrder.contact.address }}
        </p>
      </div>

      <el-form label-position="top">
        <el-form-item :label="T.LABEL_SELECT_CONTACT">
          <el-select
            v-model="shipForm.selectedContactId"
            style="width: 100%"
            :loading="contactsLoading"
            :placeholder="T.LABEL_SELECT_CONTACT"
          >
            <el-option
              v-for="c in contacts"
              :key="c.id"
              :label="`${c.name} - ${c.phone} - ${c.address}`"
              :value="c.id"
            />
          </el-select>
          <div v-if="!contactsLoading && contacts.length === 0" style="color: var(--color-text-secondary); font-size: var(--text-xs); margin-top: 4px;">
            {{ T.NO_CONTACTS }}
          </div>
        </el-form-item>
        <el-form-item :label="T.LABEL_TRACKING">
          <el-input v-model="shipForm.trackingNumber" :placeholder="T.PLACEHOLDER_TRACKING" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeShipDialog">{{ T.BTN_CANCEL }}</el-button>
        <el-button
          type="primary"
          :loading="shipping"
          :disabled="!shipForm.trackingNumber || !shipForm.selectedContactId"
          @click="handleShip"
        >
          {{ shipping ? T.BTN_SHIPPING : T.BTN_CONFIRM_SHIP }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import { useShip } from './Ship.js'

const {
  T, orders, loading, filterStatus, searchCustomer, pendingShipCount,
  contacts, contactsLoading,
  detailVisible, detailLoading, selectedOrder, shipVisible, shipForm, shipping,
  ORDER_STATUS, loadOrders, handleSearch, getStatusType, getStatusText,
  formatPrice, formatDate, showOrderDetail, closeDetail,
  showShipDialog, closeShipDialog, handleShip
} = useShip()
</script>

<style scoped src="./Ship.css"></style>
