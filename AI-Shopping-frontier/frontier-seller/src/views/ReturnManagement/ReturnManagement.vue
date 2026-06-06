<template>
  <div class="return-management">
    <div class="return-management__toolbar">
      <h2 class="return-management__title">{{ T.PAGE_TITLE }}</h2>
      <el-button @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <div v-loading="loading" class="return-list">
      <OrderCard
        v-for="order in orders"
        :key="order.orderId"
        :order="order"
        variant="abstract"
        @click="showDetail"
      />
    </div>

    <el-empty v-if="!loading && orders.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="680px" destroy-on-close>
      <OrderCard
        v-if="selectedOrder"
        :order="selectedOrder"
        variant="detail"
      />
      <div v-if="selectedOrder" class="return-actions">
        <el-button
          v-if="selectedOrder.orderStatus === 'RETURN_REQUESTED'"
          type="warning"
          @click="handleApprove(selectedOrder)"
        >
          {{ T.BTN_APPROVE }}
        </el-button>
        <el-button
          v-if="selectedOrder.orderStatus === 'RETURN_APPROVED'"
          type="success"
          @click="handleConfirm(selectedOrder)"
        >
          {{ T.BTN_CONFIRM }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import { useReturnManagement } from './ReturnManagement.js'
const { T, orders, loading, detailVisible, selectedOrder, loadOrders, handleApprove, handleConfirm, showDetail, closeDetail, getStatusType, getStatusText, formatDate, formatPrice } = useReturnManagement()
</script>

<style scoped src="./ReturnManagement.css"></style>
