<template>
  <div class="return-management">
    <div class="return-management__toolbar">
      <h2 class="return-management__title">{{ T.PAGE_TITLE }}</h2>
      <el-button size="small" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="orders" v-loading="loading" stripe border size="small" style="width: 100%">
        <el-table-column prop="orderId" :label="T.LABEL_ORDER_ID" min-width="160" />
        <el-table-column :label="T.LABEL_DATE" min-width="160">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_PRODUCT" min-width="120">
          <template #default="{ row }">{{ row.productName || `商品 #${row.productId}` }}</template>
        </el-table-column>
        <el-table-column prop="quantity" :label="T.LABEL_QUANTITY" width="80" />
        <el-table-column :label="T.LABEL_TOTAL" width="100">
          <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_STATUS" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" size="small">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="T.LABEL_ACTIONS" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">{{ T.BTN_DETAIL }}</el-button>
            <el-button v-if="row.orderStatus === 'RETURN_REQUESTED'" text type="warning" size="small" @click="handleApprove(row)">{{ T.BTN_APPROVE }}</el-button>
            <el-button v-if="row.orderStatus === 'RETURN_APPROVED'" text type="success" size="small" @click="handleConfirm(row)">{{ T.BTN_CONFIRM }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && orders.length === 0" :description="T.EMPTY_TEXT" />
    </el-card>
  </div>
</template>

<script setup>
import { useReturnManagement } from './ReturnManagement.js'
const { T, orders, loading, detailVisible, selectedOrder, filterStatus, loadOrders, handleApprove, handleConfirm, showDetail, getStatusType, getStatusText, formatDate, formatPrice } = useReturnManagement()
</script>

<style scoped src="./ReturnManagement.css"></style>
