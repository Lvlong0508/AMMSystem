<template>
  <div class="shop-orders">
    <div class="shop-orders__toolbar">
      <h2 class="shop-orders__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <el-button size="small" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <div class="shop-orders__filters">
      <el-select v-model="filterStatus" size="small" style="width: 130px" clearable :placeholder="T.LABEL_STATUS">
        <el-option label="全部" value="" />
        <el-option v-for="[k, v] in Object.entries(STATUS_TEXT)" :key="k" :label="v" :value="k" />
      </el-select>
      <el-input v-model="searchKeyword" size="small" :placeholder="T.LABEL_SEARCH" style="width: 200px" clearable />
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
        @ship="handleShip"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import { useShopOrders } from './ShopOrders.js'
const props = useShopOrders()
const { T, shopInfo, orders, loading, filterStatus, searchKeyword, filteredOrders, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, handleShip, ORDER_STATUS, STATUS_TEXT } = props
</script>

<style scoped src="./ShopOrders.css"></style>
