<template>
  <div class="order-list-view" style="max-width:960px;margin:0 auto">
    <div class="order-list-view__filters">
      <button
        v-for="f in filters"
        :key="f.key"
        class="order-list-view__filter-pill"
        :class="{ 'order-list-view__filter-pill--active': activeFilter === f.key }"
        @click="activeFilter = f.key"
      >
        {{ f.label }}
      </button>
    </div>

    <div v-if="loading" class="order-list-view__loading">
      <div v-for="i in 3" :key="i" class="order-list-view__skeleton" :style="{ animationDelay: `${i * 0.1}s` }"></div>
    </div>

    <div v-else-if="filteredOrders.length === 0" class="order-list-view__empty">
      <svg width="144" height="144" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="order-list-view__empty-icon"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
      <p class="order-list-view__empty-text">{{ T.EMPTY_TEXT }}</p>
      <router-link to="/chat" class="order-list-view__empty-cta">{{ T.EMPTY_CTA }}</router-link>
    </div>

    <div v-else class="order-list-view__list">
      <OrderCard
        v-for="(order, idx) in filteredOrders"
        :key="order.orderId"
        variant="abstract"
        :order="order"
        :style="{ animationDelay: `${idx * 0.08}s` }"
        class="order-list-view__item"
        @click="handleViewDetail(order)"
        @cancel="handleCancel"
        @delete="handleDelete"
        @pay="handlePay"
        @viewLogistics="handleViewLogistics"
        @confirm="handleConfirm"
        @review="handleReview"
      />
    </div>

    <PaymentModal :visible="showPaymentModal" :orderId="payingOrder?.orderId" :order="payingOrder" :orderDate="payingOrder?.orderDate" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
    <LogisticsModal :visible="logisticsVisible" :loading="logisticsLoading" :logisticsList="logisticsList" @close="logisticsVisible = false" />
  </div>
</template>

<script setup>
import { watch } from 'vue'
import { ORDER_LIST_TEXT as T } from './Text'
import { useOrderList } from './useOrderList'
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import PaymentModal from '@/components/PaymentModal/PaymentModal.vue'
import LogisticsModal from '@/components/LogisticsModal/LogisticsModal.vue'
import { useLogisticsModal } from '@/components/LogisticsModal/useLogisticsModal'

const {
  orders,
  loading,
  activeFilter,
  filters,
  filteredOrders,
  handleViewDetail,
    handleCancel,
    handleDelete,
  handlePay,
  handleViewLogistics,
  handleConfirm,
  handleReview,
  payingOrder,
  showPaymentModal,
  onPaymentSuccess,
  onPayLater,
  logisticsVisible,
  logisticsOrderId,
} = useOrderList()

const { loading: logisticsLoading, logisticsList, loadLogistics } = useLogisticsModal()

watch(logisticsVisible, (v) => {
  if (v && logisticsOrderId.value) loadLogistics(logisticsOrderId.value)
})
</script>

<style scoped>
@import './OrderListView.css';
</style>