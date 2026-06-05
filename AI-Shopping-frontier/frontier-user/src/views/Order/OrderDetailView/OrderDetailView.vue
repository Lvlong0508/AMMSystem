<template>
  <div class="order-detail-view">
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
  </div>
</template>

<script setup>
import { ORDER_DETAIL_TEXT as T } from './Text'
import { useOrderDetail } from './useOrderDetail'
import OrderCard from '@/components/OrderCard/OrderCard.vue'

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
  handleViewLogistics,
  handleReview,
  confirmAction
} = useOrderDetail()
</script>

<style scoped>
@import './OrderDetailView.css';
</style>
