<template>
  <el-card
    v-if="variant === 'abstract'"
    shadow="hover"
    class="order-card order-card--abstract"
    @click="$emit('click', order)"
  >
    <div class="order-card__abstract">
      <div class="order-card__info">
        <div class="order-card__id">{{ T.LABEL_ORDER_ID }}: {{ order.orderId }}</div>
        <p class="order-card__product">{{ order.productName || `${T.LABEL_PRODUCT} #${order.productId}` }}</p>
        <div class="order-card__meta">
          <span>{{ T.LABEL_QUANTITY }}: {{ order.quantity }}</span>
          <span class="order-card__price">{{ order.totalPrice ? formatPrice(order.totalPrice) : '-' }}</span>
        </div>
      </div>
      <div class="order-card__status">
        <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
      </div>
    </div>
  </el-card>

  <el-card v-else shadow="never" class="order-card order-card--detail">
    <div class="order-card__detail-header">
      <h3>{{ T.LABEL_ORDER_ID }}: {{ order.orderId }}</h3>
      <el-tag :type="statusType">{{ statusText }}</el-tag>
    </div>
    <el-divider />
    <div class="order-card__detail-grid">
      <div class="order-card__detail-section">
        <h4 class="order-card__section-title">{{ T.SECTION_ORDER }}</h4>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_PRODUCT }}</span>
          <span>{{ order.productName || `${T.LABEL_PRODUCT} #${order.productId}` }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_QUANTITY }}</span>
          <span>{{ order.quantity }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_TOTAL }}</span>
          <span>{{ formatPrice(order.totalPrice) }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_DATE }}</span>
          <span>{{ formatDate(order.orderDate) }}</span>
        </div>
      </div>
      <div v-if="order.contactName" class="order-card__detail-section">
        <h4 class="order-card__section-title">{{ T.SECTION_CONTACT }}</h4>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_CONTACT_NAME }}</span>
          <span>{{ order.contactName }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_CONTACT_PHONE }}</span>
          <span>{{ order.contactPhone }}</span>
        </div>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_CONTACT_ADDRESS }}</span>
          <span>{{ order.contactAddress }}</span>
        </div>
      </div>
      <div v-if="order.trackingNumber" class="order-card__detail-section">
        <h4 class="order-card__section-title">{{ T.SECTION_LOGISTICS }}</h4>
        <div class="order-card__detail-row">
          <span class="order-card__label">{{ T.LABEL_TRACKING }}</span>
          <span>{{ order.trackingNumber }}</span>
        </div>
      </div>
    </div>
    <el-divider v-if="actionVisible" />
    <div v-if="actionVisible" class="order-card__actions">
      <el-button type="primary" @click="$emit('ship', order)">{{ T.BTN_SHIP }}</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { useOrderCard } from './OrderCard.js'

const props = defineProps({
  order: { type: Object, required: true },
  variant: { type: String, default: 'abstract', validator: v => ['abstract', 'detail'].includes(v) }
})

defineEmits(['click', 'ship'])

const { T, statusText, statusType, actionVisible, formatPrice, formatDate } = useOrderCard(props)
</script>

<style scoped src="./OrderCard.css"></style>
