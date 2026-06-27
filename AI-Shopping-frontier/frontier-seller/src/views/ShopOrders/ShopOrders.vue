<template>
  <div class="shop-orders">
    <div class="shop-orders__toolbar">
      <h2 class="shop-orders__title">{{ T.PAGE_TITLE }}</h2>
      <el-button size="small" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <div class="shop-orders__filters">
      <el-select v-model="filterStatus" size="small" style="width: 130px" clearable :placeholder="T.LABEL_STATUS">
        <el-option label="全部" value="" />
        <el-option v-for="[k, v] in Object.entries(STATUS_TEXT)" :key="k" :label="v" :value="k" />
      </el-select>
      <el-input v-model="searchKeyword" size="small" :placeholder="T.LABEL_SEARCH" style="width: 200px" clearable @keyup.enter="handleSearch" />
      <el-button size="small" @click="handleSearch">搜索</el-button>
    </div>

    <div v-loading="loading" class="order-list">
      <OrderCard
        v-for="order in filteredOrders"
        :key="order.orderId"
        :order="order"
        variant="abstract"
        @click="showDetail"
      />
    </div>

    <el-empty v-if="!loading && filteredOrders.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="680px" destroy-on-close>
      <OrderCard
        v-if="selectedOrder"
        :order="selectedOrder"
        variant="detail"
        @ship="handleShipFromDetail"
      />
    </el-dialog>

    <el-dialog v-model="shipVisible" title="订单发货" width="520px" :close-on-click-modal="false">
      <div v-if="selectedOrder" style="margin-bottom: 16px">
        <p><strong>订单编号：</strong>{{ selectedOrder.orderId }}</p>
        <p><strong>收货人：</strong>{{ selectedOrder.contactName }} {{ selectedOrder.contactPhone }}</p>
        <p><strong>收货地址：</strong>{{ selectedOrder.contactAddress }}</p>
      </div>
      <el-form ref="shipFormRef" :model="shipForm" :rules="{ trackingNumber: trackingRule }" label-position="top">
        <el-form-item label="选择发货地址">
          <el-select v-model="shipForm.selectedContactId" style="width: 100%" :loading="contactsLoading" placeholder="选择发货地址">
            <el-option v-for="c in contacts" :key="c.id" :label="`${c.name} - ${c.phone} - ${c.address}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item prop="trackingNumber" label="物流单号">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入物流单号" />
          <div style="color: #c0c4cc; font-size: 12px; margin-top: 4px">6-20位字母、数字或连字符</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="shipping" :disabled="!shipForm.trackingNumber || !shipForm.selectedContactId" @click="confirmShip">{{ shipping ? '发货中...' : '确认发货' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import { useShopOrders } from './ShopOrders.js'
const props = useShopOrders()
const { T, orders, loading, filterStatus, searchKeyword, searchQuery, filteredOrders, handleSearch, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, handleShip, confirmShip, ORDER_STATUS, STATUS_TEXT, shipVisible, shipFormRef, shipForm, shipping, contacts, contactsLoading, trackingRule } = props
</script>

<style scoped src="./ShopOrders.css"></style>
