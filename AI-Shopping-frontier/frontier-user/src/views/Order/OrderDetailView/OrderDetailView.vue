<template>
  <div class="order-detail-view">
    <div class="order-detail-view__inner">
      <div class="order-detail-view__back" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        {{ T.BACK }}
      </div>

      <div v-if="loading" class="order-detail-view__loading">
        <div class="order-list-view__skeleton"></div>
      </div>

      <template v-else-if="order">
        <OrderCard
          variant="detail"
          :order="order"
          @cancel="showActionSheet = true; actionType = 'cancel'"
          @delete="handleDelete"
          @pay="handlePay"
          @viewLogistics="handleViewLogistics"
          @confirm="showActionSheet = true; actionType = 'confirm'"
          @review="handleReview"
        />
      </template>

      <div v-else class="order-detail-view__empty">
        <p>{{ T.NOT_FOUND }}</p>
      </div>

      <Teleport to="body">
        <div v-if="showActionSheet" class="action-sheet-overlay" @click="showActionSheet = false">
          <div class="action-sheet" @click.stop>
            <div class="action-sheet__content">
              <h3 class="action-sheet__title">{{ actionTitle }}</h3>
              <p class="action-sheet__desc">{{ actionDesc }}</p>
            </div>
            <div class="action-sheet__actions">
              <button class="action-sheet__btn action-sheet__btn--danger" @click="confirmAction">{{ actionConfirmText }}</button>
              <button class="action-sheet__btn action-sheet__btn--cancel" @click="showActionSheet = false">{{ T.RETHINK }}</button>
            </div>
          </div>
        </div>
      </Teleport>

      <PaymentModal :visible="showPaymentModal" :orderId="order?.orderId" :order="order" :orderDate="order?.orderDate" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
    <LogisticsModal :visible="logisticsVisible" :loading="logisticsLoading" :logisticsList="logisticsList" @close="logisticsVisible = false" />
    </div>
  </div>
</template>
<script setup>
import { ORDER_DETAIL_TEXT as T } from './Text'
import { watch } from "vue"
import { useOrderDetail } from "./useOrderDetail"
import OrderCard from '@/components/OrderCard/OrderCard.vue'
import PaymentModal from '@/components/PaymentModal/PaymentModal.vue'
import LogisticsModal from '@/components/LogisticsModal/LogisticsModal.vue'
import { useLogisticsModal } from '@/components/LogisticsModal/useLogisticsModal'

const {
  order,
  loading,
  showActionSheet,
  actionType,
  actionTitle,
  actionDesc,
  actionConfirmText,
  goBack,
    handlePay,
    handleDelete,
  handleViewLogistics,
  logisticsVisible,
  handleReview,
  confirmAction,
  showPaymentModal,
  onPaymentSuccess,
  onPayLater,
} = useOrderDetail()

const { loading: logisticsLoading, logisticsList, loadLogistics } = useLogisticsModal()

watch(logisticsVisible, (v) => {
  if (v && order.value?.orderId) loadLogistics(order.value.orderId)
})
</script>

<style scoped>
  .order-detail-view {
  padding: 32px;
  font-family: var(--font-body);
  }
  .order-detail-view__inner {
    width: 60%;
    margin: 0 auto;
  }
.order-detail-view__back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  color: var(--color-text-secondary);
  cursor: pointer;
  margin-bottom: 20px;
  transition: color 250ms ease-out;
}
.order-detail-view__back svg {
  width: 24px;
  height: 24px;
}
.order-detail-view__back:hover {
  color: var(--color-accent);
}
.order-detail-view__loading {
  padding: 24px 0;
}
.order-list-view__skeleton {
  height: 600px;
  border-radius: var(--radius-xl);
  background: linear-gradient(90deg, #f1f5f9 25%, #f8fafc 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
.order-detail-view__empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  color: var(--color-text-tertiary);
  font-size: 22px;
}

.action-sheet-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.4);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 250ms ease-out;
}
.action-sheet {
  width: 100%;
  max-width: 500px;
  background: #fff;
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  padding: 40px 32px calc(env(safe-area-inset-bottom) + 24px);
  animation: slideUp 300ms ease-out;
}
.action-sheet__content {
  text-align: center;
  margin-bottom: 32px;
}
.action-sheet__title {
  font-family: var(--font-heading);
  font-size: 26px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 12px;
}
.action-sheet__desc {
  font-size: 20px;
  color: var(--color-text-tertiary);
  margin: 0;
}
.action-sheet__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.action-sheet__btn {
  width: 100%;
  padding: 18px;
  border-radius: var(--radius-md);
  font-size: 20px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 250ms ease-out;
  border: none;
  font-family: var(--font-body);
}
.action-sheet__btn--danger {
  background: var(--color-danger);
  color: #fff;
}
.action-sheet__btn--cancel {
  background: var(--color-bg);
  color: var(--color-text-secondary);
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
@media (prefers-reduced-motion: reduce) {
  .action-sheet-overlay { animation: none; }
  .action-sheet { animation: none; }
}

</style>
