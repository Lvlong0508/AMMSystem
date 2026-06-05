<template>
  <div v-if="variant === 'abstract'" class="order-card order-card--abstract">
    <div class="order-card__main" @click="$emit('click')">
      <div class="order-card__thumb">
        <svg width="52" height="52" viewBox="0 0 52 52" fill="none"><rect width="52" height="52" rx="8" fill="#f1f5f9"/><path d="M18 22h16l-2 14H20l-2-14z" stroke="#94a3b8" stroke-width="1.5"/><circle cx="22" cy="36" r="2" fill="#94a3b8"/><circle cx="32" cy="36" r="2" fill="#94a3b8"/></svg>
      </div>
      <div class="order-card__info">
        <div class="order-card__top">
          <span class="order-card__product-name">{{ order.productName || T.PRODUCT_PLACEHOLDER }}</span>
          <span class="order-card__price">¥{{ order.totalPrice.toFixed(2) }}</span>
        </div>
        <div class="order-card__meta">
          <span>{{ T.QTY_LABEL }}{{ order.quantity }} · {{ order.shopName || T.SHOP_PLACEHOLDER }}</span>
          <StatusTag :status="order.orderStatus" />
        </div>
        <div class="order-card__id-row">
          <span class="order-card__id">{{ order.orderId }}</span>
          <span class="order-card__date">{{ formatDate(order.orderDate) }}</span>
        </div>
      </div>
    </div>
    <div class="order-card__divider"></div>
    <div class="order-card__actions">
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('cancel', order)">{{ T.CANCEL }}</button>
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('pay', order)">{{ T.PAY }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('confirm', order)">{{ T.CONFIRM }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('review', order)">{{ T.REVIEW }}</button>
    </div>
  </div>

  <div v-else class="order-card order-card--detail">
    <div class="order-card__detail-header">
      <span class="order-card__detail-title">{{ T.ORDER_DETAIL }}</span>
      <StatusTag :status="order.orderStatus" />
    </div>

    <div class="order-card__detail-section">
      <div class="order-card__thumb">
        <svg width="52" height="52" viewBox="0 0 52 52" fill="none"><rect width="52" height="52" rx="8" fill="#f1f5f9"/><path d="M18 22h16l-2 14H20l-2-14z" stroke="#94a3b8" stroke-width="1.5"/><circle cx="22" cy="36" r="2" fill="#94a3b8"/><circle cx="32" cy="36" r="2" fill="#94a3b8"/></svg>
      </div>
      <div class="order-card__detail-product-info">
        <span class="order-card__product-name">{{ order.productName || T.PRODUCT_PLACEHOLDER }}</span>
        <span class="order-card__price">¥{{ order.totalPrice.toFixed(2) }}</span>
        <span class="order-card__qty">{{ T.QTY_LABEL }}{{ order.quantity }} · {{ order.shopName || T.SHOP_PLACEHOLDER }}</span>
      </div>
    </div>

    <div class="order-card__info-grid">
      <div class="order-card__info-item">
        <span class="order-card__info-label">{{ T.ORDER_DATE }}</span>
        <span class="order-card__info-value">{{ formatDate(order.orderDate) }}</span>
      </div>
      <div class="order-card__info-item">
        <span class="order-card__info-label">{{ T.TRACKING }}</span>
        <span class="order-card__info-value">{{ order.trackingNumber || '-' }}</span>
      </div>
      <div class="order-card__info-item">
        <span class="order-card__info-label">{{ T.SHOP }}</span>
        <span class="order-card__info-value">{{ order.shopName || T.SHOP_PLACEHOLDER }}</span>
      </div>
    </div>

    <div class="order-card__address">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
      <div>
        <span class="order-card__contact-name">{{ order.contactName }}</span>
        <span class="order-card__contact-phone">{{ order.contactPhone }}</span>
        <p class="order-card__contact-addr">{{ order.contactAddress }}</p>
      </div>
    </div>

    <div class="order-card__timeline">
      <div class="order-card__timeline-header">
        <span>{{ T.TIMELINE_TITLE }}</span>
        <span class="order-card__timeline-progress">{{ timelineProgress }}</span>
      </div>
      <div class="order-card__steps">
        <div v-for="(step, i) in steps" :key="i" class="order-card__step" :class="{ 'order-card__step--done': step.done, 'order-card__step--active': step.active }">
          <div class="order-card__step-circle">
            <svg v-if="step.done" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <span class="order-card__step-label">{{ step.label }}</span>
          <span v-if="i === 0" class="order-card__step-date">{{ formatDate(order.orderDate) }}</span>
        </div>
        <div v-if="steps.length > 1" class="order-card__step-line"></div>
      </div>
    </div>

    <div class="order-card__divider"></div>
    <div class="order-card__actions">
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('cancel', order)">{{ T.CANCEL }}</button>
      <button v-if="order.orderStatus === 'PENDING'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('pay', order)">{{ T.PAY }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'SHIPPED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('confirm', order)">{{ T.CONFIRM }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--outline" @click="$emit('viewLogistics', order)">{{ T.VIEW_LOGISTICS }}</button>
      <button v-if="order.orderStatus === 'DELIVERED'" class="order-card__action-btn order-card__action-btn--primary" @click="$emit('review', order)">{{ T.REVIEW }}</button>
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

defineEmits(['click', 'cancel', 'pay', 'viewLogistics', 'confirm', 'review'])

const { formatDate, timelineProgress, steps } = useOrderCard(props)
</script>

<style scoped>
@import './OrderCard.css';
</style>
