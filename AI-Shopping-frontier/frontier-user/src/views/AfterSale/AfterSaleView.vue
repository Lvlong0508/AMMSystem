<template>
  <div class="after-sale-view">
    <div class="after-sale-view__back" @click="goBack">
      {{ T.BACK }}
    </div>

    <div v-if="loading" class="after-sale-view__loading">{{ T.LOADING }}</div>

    <div v-else-if="list.length === 0" class="after-sale-view__empty">{{ T.NO_RETURN_RECORD }}</div>

    <div v-else class="after-sale-view__list">
      <div v-for="item in list" :key="item.orderId" class="after-sale-card" @click="openDetail(item)">
        <div class="after-sale-card__header">
          <div class="after-sale-card__shop-info">
            <img v-if="item.shopLogoUrl" class="after-sale-card__shop-logo" :src="item.shopLogoUrl" :alt="item.shopName" />
            <span class="after-sale-card__shop-name">{{ item.shopName }}</span>
          </div>
          <span class="after-sale-card__status-tag" :class="'after-sale-card__status-tag--' + getReturnStatusClass(item)">
            {{ getReturnStatusTag(item) }}
          </span>
        </div>

        <div class="after-sale-card__divider"></div>

        <div class="after-sale-card__product-row">
          <div class="after-sale-card__thumb">
            <img v-if="item.productImageUrl" class="after-sale-card__thumb-img" :src="item.productImageUrl" :alt="item.productName" />
          </div>
          <div class="after-sale-card__product-details">
            <span class="after-sale-card__product-name">{{ item.productName }}</span>
            <div v-if="item.productType" class="after-sale-card__tag-row">
              <span v-for="tag in item.productType.split(',')" :key="tag" class="after-sale-card__tag">{{ tag.trim() }}</span>
            </div>
            <span class="after-sale-card__qty">x{{ item.quantity }}</span>
          </div>
          <span class="after-sale-card__price">¥{{ Number(item.totalPrice).toFixed(2) }}</span>
        </div>

        <div v-if="item.returnReason" class="after-sale-card__divider"></div>
        <div v-if="item.returnReason" class="after-sale-card__reason">
          <span class="after-sale-card__reason-label">{{ T.RETURN_REASON }}:</span>
          {{ item.returnReason }}
        </div>

        <div v-if="item.returnTrackingNumber" class="after-sale-card__divider"></div>
        <div v-if="item.returnTrackingNumber" class="after-sale-card__reason">
          <span class="after-sale-card__reason-label">{{ T.RETURN_TRACKING }}:</span>
          {{ item.returnTrackingNumber }}
        </div>

        <div class="after-sale-card__divider"></div>

        <div class="after-sale-card__actions">
          <button
            v-if="showSubmitLogisticsBtn(item)"
            class="after-sale-card__btn after-sale-card__btn--primary"
            @click.stop="handleSubmitLogistics(item)"
          >
            {{ T.SUBMIT_LOGISTICS }}
          </button>
        </div>
      </div>
    </div>

    <ReturnLogisticsModal
      :visible="showReturnLogisticsModal"
      :contacts="contacts"
      :loading-address="loadingAddress"
      :shop-return-address="shopReturnAddress"
      :shop-return-phone="shopReturnPhone"
      @close="showReturnLogisticsModal = false"
      @submit="onLogisticsSubmit"
    />

    <Transition name="detail-fade">
      <div v-if="showDetailModal && detailItem" class="after-sale-detail-overlay" @click.self="closeDetail">
      <div class="after-sale-detail-modal">
        <div class="after-sale-detail-modal__header">
          <span class="after-sale-detail-modal__title">{{ T.DETAIL_TITLE }}</span>
          <button class="after-sale-detail-modal__close" @click="closeDetail">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="after-sale-detail-modal__body">
          <div class="after-sale-detail-modal__row">
            <span class="after-sale-detail-modal__label">{{ T.ORDER_ID }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.orderId }}</span>
          </div>
          <div class="after-sale-detail-modal__row">
            <span class="after-sale-detail-modal__label">{{ T.SHOP_NAME }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.shopName || '-' }}</span>
          </div>
          <div class="after-sale-detail-modal__row">
            <span class="after-sale-detail-modal__label">{{ T.PRODUCT_NAME }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.productName || '-' }}</span>
          </div>
          <div class="after-sale-detail-modal__row" v-if="detailItem.productType">
            <span class="after-sale-detail-modal__label">{{ T.PRODUCT_TYPE }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.productType }}</span>
          </div>
          <div class="after-sale-detail-modal__row">
            <span class="after-sale-detail-modal__label">{{ T.QUANTITY }}</span>
            <span class="after-sale-detail-modal__value">x{{ detailItem.quantity }}</span>
          </div>
          <div class="after-sale-detail-modal__row">
            <span class="after-sale-detail-modal__label">{{ T.TOTAL_PRICE }}</span>
            <span class="after-sale-detail-modal__value">¥{{ Number(detailItem.totalPrice).toFixed(2) }}</span>
          </div>
          <div class="after-sale-detail-modal__row">
            <span class="after-sale-detail-modal__label">{{ T.RETURN_STATUS }}</span>
            <span class="after-sale-detail-modal__value">{{ getReturnStatusTag(detailItem) }}</span>
          </div>
          <div class="after-sale-detail-modal__row" v-if="detailItem.returnReason">
            <span class="after-sale-detail-modal__label">{{ T.RETURN_REASON }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.returnReason }}</span>
          </div>
          <div class="after-sale-detail-modal__row" v-if="detailItem.returnTrackingNumber">
            <span class="after-sale-detail-modal__label">{{ T.RETURN_TRACKING }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.returnTrackingNumber }}</span>
          </div>
          <div class="after-sale-detail-modal__row" v-if="detailItem.orderStatus">
            <span class="after-sale-detail-modal__label">{{ T.ORDER_STATUS }}</span>
            <span class="after-sale-detail-modal__value">{{ detailItem.orderStatus }}</span>
          </div>
        </div>
        <div class="after-sale-detail-modal__footer">
          <button class="after-sale-detail-modal__btn" @click="closeDetail">{{ T.CLOSE }}</button>
        </div>
      </div>
    </div>
    </Transition>
  </div>
</template>

<script setup>
import { AFTER_SALE_TEXT as T } from './Text'
import { useAfterSale } from './useAfterSale'
import ReturnLogisticsModal from '@/components/ReturnLogisticsModal/ReturnLogisticsModal.vue'

const {
  list,
  loading,
  goBack,
  showSubmitLogisticsBtn,
  getReturnStatusTag,
  getReturnStatusClass,
  handleSubmitLogistics,
  onLogisticsSubmit,
  showReturnLogisticsModal,
  contacts,
  loadingAddress,
  shopReturnAddress,
  shopReturnPhone,
  detailItem,
  showDetailModal,
  openDetail,
  closeDetail
} = useAfterSale()
</script>

<style scoped>
@import './AfterSaleView.css';
</style>
