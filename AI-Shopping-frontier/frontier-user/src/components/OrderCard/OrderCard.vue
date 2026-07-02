<template>
  <div v-if="variant === 'abstract'" class="order-card order-card--abstract">
    <div class="order-card__main" @click="$emit('click')">
      <div class="order-card__shop-row">
        <div class="order-card__shop-info">
          <img v-if="order.shopLogoUrl" class="order-card__shop-logo" :src="order.shopLogoUrl" :alt="order.shopName" />
          <span class="order-card__shop-name">{{ order.shopName || T.SHOP_PLACEHOLDER }}</span>
        </div>
        <StatusTag :status="order.orderStatus" />
      </div>

      <div class="order-card__section-divider"></div>

      <div class="order-card__product-row">
        <div class="order-card__thumb">
          <img v-if="order.productImageUrl" class="order-card__thumb-img" :src="order.productImageUrl" :alt="order.productName" />
          <svg v-else width="88" height="88" viewBox="0 0 52 52" fill="none"><rect width="88" height="88" rx="8" fill="#f1f5f9"/><path d="M18 22h16l-2 14H20l-2-14z" stroke="#94a3b8" stroke-width="1.5"/><circle cx="22" cy="36" r="2" fill="#94a3b8"/><circle cx="32" cy="36" r="2" fill="#94a3b8"/></svg>
        </div>
        <div class="order-card__product-details">
          <span class="order-card__product-name">{{ order.productName || T.PRODUCT_PLACEHOLDER }}</span>
          <div v-if="order.productType" class="order-card__tag-row">
            <span v-for="tag in order.productType.split(',')" :key="tag" class="order-card__tag">{{ tag.trim() }}</span>
          </div>
          <span class="order-card__qty">{{ T.QTY_LABEL }}{{ order.quantity }}</span>
        </div>
      </div>

      <div class="order-card__section-divider"></div>

      <div class="order-card__price-row">
        <div class="order-card__price-left">
          <span class="order-card__id">{{ order.orderId }}</span>
          <span v-if="order.orderDate" class="order-card__date">{{ formatDate(order.orderDate) }}</span>
        </div>
        <div class="order-card__price-right">
          <span class="order-card__price">¥{{ order.totalPrice.toFixed(2) }}</span>
        </div>
      </div>
    </div>

    <div class="order-card__divider"></div>

    <div class="order-card__actions">
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--danger" @click="$emit('cancel', order)">{{ T.CANCEL }}</button>
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('pay', order)">{{ T.PAY }}</button>
      <button v-if="order.orderStatus === 'PAID'" class="order-card__action-btn order-card__action-btn--danger" @click="$emit('cancel', order)">{{ T.CANCEL }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('confirm', order)">{{ T.CONFIRM }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('return', order)">{{ T.APPLY_RETURN }}</button>
      <button v-if="['RETURN_PENDING', 'RETURNING'].includes(order.orderStatus)" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('afterSale', order)">{{ T.AFTER_SALE }}</button>
      <button v-if="['DELIVERED', 'CANCELLED', 'RETURN_PENDING', 'RETURNING', 'RETURNED'].includes(order.orderStatus)" class="order-card__action-btn order-card__action-btn--danger" @click="$emit('delete', order)">{{ T.DELETE }}</button>
    </div>
  </div>

  
  <div v-else class="order-card order-card--detail">
    <!-- 状态行 + 地址行 -->
    <div class="order-card__top-row" v-if="order.orderStatus || order.contactName">
      <div class="order-card__top-left" v-if="order.orderStatus">
        <StatusTag :status="order.orderStatus" />
      </div>
      <div class="order-card__top-right" v-if="order.contactName">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
        <div class="order-card__top-contact">
          <span class="order-card__contact-name">{{ order.contactName }}</span>
          <span class="order-card__contact-phone">{{ order.contactPhone }}</span>
          <p class="order-card__contact-addr">{{ order.contactAddress }}</p>
        </div>
      </div>
    </div>
    <div class="order-card__divider"></div>
    <!-- 店铺信息 -->
    <div class="order-card__shop-section" v-if="order.shopName">
      <div class="order-card__shop-info">
        <img v-if="order.shopLogoUrl" class="order-card__shop-logo" :src="order.shopLogoUrl" :alt="order.shopName" />
        <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="12" cy="12" r="3"/></svg>
        <span class="order-card__shop-name">{{ order.shopName }}</span>
      </div>
    </div>

    <div class="order-card__divider" v-if="order.shopName"></div>

    <!-- 商品信息 -->
    <div class="order-card__detail-section">
      <div class="order-card__section-label">{{ T.PRODUCT_INFO }}</div>
      <div class="order-card__product-row">
        <div class="order-card__thumb">
          <img v-if="order.productImageUrl" class="order-card__thumb-img" :src="order.productImageUrl" :alt="order.productName" />
          <svg v-else width="88" height="88" viewBox="0 0 52 52" fill="none"><rect width="88" height="88" rx="8" fill="#f1f5f9"/><path d="M18 22h16l-2 14H20l-2-14z" stroke="#94a3b8" stroke-width="1.5"/><circle cx="22" cy="36" r="2" fill="#94a3b8"/><circle cx="32" cy="36" r="2" fill="#94a3b8"/></svg>
        </div>
        <div class="order-card__product-details">
          <span class="order-card__product-name">{{ order.productName || T.PRODUCT_PLACEHOLDER }}</span>
          <div v-if="order.productType" class="order-card__tag-row">
            <span v-for="tag in String(order.productType).split(',')" :key="tag" class="order-card__tag">{{ tag.trim() }}</span>
          </div>
          <span class="order-card__qty">{{ T.QTY_LABEL }}{{ order.quantity }}</span>
        </div>
      </div>
    </div>

    <div class="order-card__divider"></div>

    <!-- 价格信息 -->
    <div class="order-card__detail-section" v-if="order.totalPrice">
      <div class="order-card__section-label">{{ T.TOTAL }}</div>
      <div class="order-card__price-section">
        <span class="order-card__price">¥{{ Number(order.totalPrice).toFixed(2) }}</span>
      </div>
    </div>

    <div class="order-card__divider" v-if="order.totalPrice"></div>

    <!-- 订单信息 -->
    <div class="order-card__detail-section">
      <div class="order-card__section-label">{{ T.ORDER_INFO }}</div>
      <div class="order-card__info-grid">
        <div class="order-card__info-item">
          <span class="order-card__info-label">{{ T.ORDER_ID }}</span>
          <span class="order-card__info-value">{{ order.orderId }}</span>
        </div>
        <div class="order-card__info-item" v-if="order.orderDate">
          <span class="order-card__info-label">{{ T.ORDER_DATE }}</span>
          <span class="order-card__info-value">{{ formatDate(order.orderDate) }}</span>
        </div>
        <div class="order-card__info-item" v-if="order.trackingNumber">
          <span class="order-card__info-label">{{ T.TRACKING }}</span>
          <span class="order-card__info-value">{{ order.trackingNumber }}</span>
        </div>
      </div>
    </div>

    <div class="order-card__divider"></div>

    <!-- 操作栏 -->
    <div class="order-card__actions">
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--danger" @click="$emit('cancel', order)">{{ T.CANCEL }}</button>
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('pay', order)">{{ T.PAY }}</button>
      <button v-if="order.orderStatus === 'PAID'" class="order-card__action-btn order-card__action-btn--danger" @click="$emit('cancel', order)">{{ T.CANCEL }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('confirm', order)">{{ T.CONFIRM }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('return', order)">{{ T.APPLY_RETURN }}</button>
      <button v-if="['RETURN_PENDING', 'RETURNING'].includes(order.orderStatus)" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('afterSale', order)">{{ T.AFTER_SALE }}</button>
      <button v-if="['DELIVERED', 'CANCELLED', 'RETURN_PENDING', 'RETURNING', 'RETURNED'].includes(order.orderStatus)" class="order-card__action-btn order-card__action-btn--danger" @click="$emit('delete', order)">{{ T.DELETE }}</button>
    </div>
  </div>
</template>

<script setup>
import { ORDER_CARD_TEXT as T } from './Text'
import { useOrderCard } from './useOrderCard'
import StatusTag from '@/components/StatusTag/StatusTag.vue'

const props = defineProps({
  variant: { type: String, default: 'abstract', validator: v => ['abstract', 'detail'].includes(v) },
  order: { type: Object, required: true }
})

defineEmits(['click', 'cancel', 'pay', 'viewLogistics', 'confirm', 'return', 'delete', 'submitLogistics', 'afterSale'])

const { formatDate, timelineProgress, steps } = useOrderCard(props)
</script>

<style scoped>
@import './OrderCard.css';
</style>


