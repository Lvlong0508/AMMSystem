<template>
  <div class="after-sale-view">
    <div class="after-sale-view__back" @click="goBack">
      {{ T.BACK }}
    </div>

    <div v-if="loading" class="after-sale-view__loading">{{ T.LOADING }}</div>

    <div v-else-if="list.length === 0" class="after-sale-view__empty">{{ T.NO_RETURN_RECORD }}</div>

    <div v-else class="after-sale-view__list">
      <div v-for="item in list" :key="item.orderId" class="after-sale-card">
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
            @click="handleSubmitLogistics(item)"
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
  shopReturnPhone
} = useAfterSale()
</script>

<style scoped>
@import './AfterSaleView.css';
</style>
