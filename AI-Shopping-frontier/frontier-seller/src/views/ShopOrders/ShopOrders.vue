<template>
  <div class="shop-orders">
    <div class="shop-orders__toolbar">
      <h2 class="shop-orders__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <el-button size="small" @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <div class="shop-orders__filters">
      <el-select v-model="filterStatus" size="small" style="width: 130px" clearable :placeholder="T.LABEL_STATUS" @change="() => {}">
        <el-option label="全部" value="" />
        <el-option v-for="[k, v] in Object.entries(STATUS_TEXT)" :key="k" :label="v" :value="k" />
      </el-select>
      <el-input v-model="searchKeyword" size="small" :placeholder="T.LABEL_SEARCH" style="width: 200px" clearable />
    </div>

    <el-card shadow="never">
      <el-table :data="filteredOrders" v-loading="loading" stripe border size="small" style="width: 100%">
        <el-table-column prop="orderId" :label="T.LABEL_ORDER_ID" min-width="160" />
        <el-table-column :label="T.LABEL_DATE" min-width="160">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_PRODUCT" min-width="140">
          <template #default="{ row }">{{ row.productName || `商品 #${row.productId}` }}</template>
        </el-table-column>
        <el-table-column prop="quantity" :label="T.LABEL_QUANTITY" width="80" />
        <el-table-column :label="T.LABEL_TOTAL" width="100">
          <template #default="{ row }">{{ formatPrice(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_STATUS_COL" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" size="small">{{ getStatusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="T.LABEL_ACTIONS" width="100" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">{{ T.BTN_DETAIL }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredOrders.length === 0" :description="T.EMPTY_TEXT" />
    </el-card>

    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="600px">
      <template v-if="selectedOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="T.LABEL_ORDER_ID" :span="2">{{ selectedOrder.orderId }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_STATUS_COL">
            <el-tag :type="getStatusType(selectedOrder.orderStatus)" size="small">{{ getStatusText(selectedOrder.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_DATE">{{ formatDate(selectedOrder.orderDate) }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_PRODUCT" :span="2">{{ selectedOrder.productName || `商品 #${selectedOrder.productId}` }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_QUANTITY">{{ selectedOrder.quantity }}</el-descriptions-item>
          <el-descriptions-item :label="T.LABEL_TOTAL">{{ formatPrice(selectedOrder.totalPrice) }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedOrder.contactName" class="detail-section">
          <h4 class="detail-section__title">收货人信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item :label="T.LABEL_CONTACT_NAME">{{ selectedOrder.contactName }}</el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_CONTACT_PHONE">{{ selectedOrder.contactPhone }}</el-descriptions-item>
            <el-descriptions-item :label="T.LABEL_CONTACT_ADDRESS">{{ selectedOrder.contactAddress }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="selectedOrder.trackingNumber" class="detail-section">
          <h4 class="detail-section__title">物流信息</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item :label="T.LABEL_TRACKING">{{ selectedOrder.trackingNumber }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShopOrders } from './ShopOrders.js'
const props = useShopOrders()
const { T, shopInfo, orders, loading, filterStatus, searchKeyword, filteredOrders, detailVisible, selectedOrder, loadOrders, getStatusType, getStatusText, formatDate, formatPrice, showDetail, closeDetail, ORDER_STATUS, STATUS_TEXT } = props
</script>

<style scoped src="./ShopOrders.css"></style>
