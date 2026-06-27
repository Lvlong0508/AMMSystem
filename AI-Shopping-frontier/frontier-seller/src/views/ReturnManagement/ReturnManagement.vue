<template>
  <div class="return-management">
    <div class="return-management__toolbar">
      <h2 class="return-management__title">{{ T.PAGE_TITLE }}</h2>
      <el-button @click="loadOrders">{{ T.BTN_REFRESH }}</el-button>
    </div>

    <div class="return-management__tabs">
      <button
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === 'pending' }"
        @click="activeTab = 'pending'"
      >
        {{ T.TAB_PENDING }}
      </button>
      <button
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === 'processed' }"
        @click="activeTab = 'processed'"
      >
        {{ T.TAB_PROCESSED }}
      </button>
    </div>

    <div class="return-management__filters">
      <el-input v-model="searchKeyword" size="small" :placeholder="T.LABEL_SEARCH" style="width: 200px" clearable @keyup.enter="handleSearch" />
      <el-button size="small" @click="handleSearch">{{ T.BTN_SEARCH }}</el-button>
    </div>

    <div v-loading="loading" class="return-list">
      <el-card
        v-for="item in list"
        :key="item.orderId"
        shadow="hover"
        class="return-card"
      >
        <div class="return-card__body" @click="showDetail(item)">
          <div class="return-card__info">
            <div class="return-card__order-id">{{ T.LABEL_ORDER_ID }}: {{ item.orderId }}</div>
            <p class="return-card__reason">{{ T.LABEL_REASON }}: {{ item.returnReason }}</p>
            <div class="return-card__meta">
              <span>{{ T.LABEL_DATE }}: {{ formatDate(item.createdDate) }}</span>
            </div>
          </div>
          <div class="return-card__status">
            <el-tag :type="getStatusType(item.status)" size="small">
              {{ getStatusText(item.status) }}
            </el-tag>
          </div>
        </div>
        <div v-if="item.status === 'applying'" class="return-card__actions">
          <el-button @click="handleReject(item)">
            {{ T.BTN_REJECT }}
          </el-button>
          <el-button type="warning" @click="handleApprove(item)">
            {{ T.BTN_APPROVE }}
          </el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && list.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="680px" destroy-on-close>
      <template v-if="selectedOrder">
        <div class="detail-section">
          <h4 class="detail-section__title">{{ T.SECTION_RETURN }}</h4>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_ORDER_ID }}</span>
            <span>{{ selectedOrder.orderId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_REASON }}</span>
            <span>{{ selectedOrder.returnReason }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_STATUS }}</span>
            <el-tag :type="getStatusType(selectedOrder.status)" size="small">
              {{ getStatusText(selectedOrder.status) }}
            </el-tag>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_DATE }}</span>
            <span>{{ formatDate(selectedOrder.createdDate) }}</span>
          </div>
        </div>

        <div v-if="selectedOrder.productName" class="detail-section">
          <h4 class="detail-section__title">{{ T.SECTION_ORDER }}</h4>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_PRODUCT }}</span>
            <span>{{ selectedOrder.productName || selectedOrder.productId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_QUANTITY }}</span>
            <span>{{ selectedOrder.quantity }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">{{ T.LABEL_TOTAL }}</span>
            <span>{{ selectedOrder.totalPrice ? `¥${Number(selectedOrder.totalPrice).toFixed(2)}` : '-' }}</span>
          </div>
        </div>
      </template>

      <div v-if="selectedOrder" class="return-actions">
        <el-button
          v-if="selectedOrder.orderStatus === 'RETURNING'"
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
import { useReturnManagement } from './ReturnManagement.js'
const { T, list, loading, activeTab, pendingList, processedList, searchKeyword, detailVisible, selectedOrder, loadOrders, handleSearch, handleApprove, handleReject, handleConfirm, showDetail, closeDetail, getStatusText, getStatusType, formatDate } = useReturnManagement()
</script>

<style scoped src="./ReturnManagement.css"></style>
