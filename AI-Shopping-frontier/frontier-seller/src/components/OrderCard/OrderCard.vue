<template>
  <div
    v-if="variant === 'abstract'"
    class="order-card order-card--abstract"
    @click="$emit('click', order)"
  >
    <div class="order-card__top">
      <span class="order-card__id">{{ order.orderId }}</span>
    </div>
    <div v-if="order.orderDate" class="order-card__date-row">
      <span class="order-card__date">{{ formatDate(order.orderDate) }}</span>
    </div>
    <div class="order-card__body-row">
      <div v-if="order.productImageUrl" class="order-card__thumb">
        <img :src="order.productImageUrl" :alt="order.productName" class="order-card__thumb-img" />
      </div>
      <div class="order-card__info">
        <p v-if="order.productName" class="order-card__product">{{ order.productName }}</p>
        <span class="order-card__qty">{{ T.LABEL_QUANTITY }}: {{ order.quantity }}</span>
        <span v-if="order.contactName" class="order-card__buyer">{{ order.contactName }}</span>
      </div>
    </div>
    <div class="order-card__bottom">
      <el-tag :type="statusType" size="small" effect="plain">{{ statusText }}</el-tag>
      <span v-if="order.totalPrice != null" class="order-card__price">{{ formatPrice(order.totalPrice) }}</span>
    </div>
  </div>

  <div v-else class="order-card order-card--detail">
    <div class="order-card__detail-header">
      <h3 class="order-card__detail-id">{{ T.LABEL_ORDER_ID }}: {{ order.orderId }}</h3>
      <el-tag :type="statusType" effect="plain">{{ statusText }}</el-tag>
    </div>

    <div class="order-card__detail-body">
      <div class="order-card__section">
        <h4 class="order-card__section-title">{{ T.SECTION_ORDER }}</h4>
        <div class="order-card__product-row">
          <div v-if="order.productImageUrl" class="order-card__thumb">
            <img :src="order.productImageUrl" :alt="order.productName" class="order-card__thumb-img" />
          </div>
          <div class="order-card__grid">
            <div v-if="order.productName" class="order-card__field">
              <span class="order-card__label">{{ T.LABEL_PRODUCT }}</span>
              <span class="order-card__value">{{ order.productName }}</span>
            </div>
            <div class="order-card__field">
              <span class="order-card__label">{{ T.LABEL_QUANTITY }}</span>
              <span class="order-card__value">{{ order.quantity }}</span>
            </div>
            <div v-if="order.totalPrice != null" class="order-card__field">
              <span class="order-card__label">{{ T.LABEL_TOTAL }}</span>
              <span class="order-card__value">{{ formatPrice(order.totalPrice) }}</span>
            </div>
            <div v-if="order.orderDate" class="order-card__field">
              <span class="order-card__label">{{ T.LABEL_DATE }}</span>
              <span class="order-card__value">{{ formatDate(order.orderDate) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="order.contactName" class="order-card__section">
        <h4 class="order-card__section-title">{{ T.SECTION_CONTACT }}</h4>
        <div class="order-card__grid">
          <div class="order-card__field">
            <span class="order-card__label">{{ T.LABEL_CONTACT_NAME }}</span>
            <span class="order-card__value">{{ order.contactName }}</span>
          </div>
          <div class="order-card__field">
            <span class="order-card__label">{{ T.LABEL_CONTACT_PHONE }}</span>
            <span class="order-card__value">{{ order.contactPhone }}</span>
          </div>
          <div class="order-card__field order-card__field--full">
            <span class="order-card__label">{{ T.LABEL_CONTACT_ADDRESS }}</span>
            <span class="order-card__value">{{ order.contactAddress }}</span>
          </div>
        </div>
      </div>

      <div v-if="order.trackingNumber" class="order-card__section">
        <h4 class="order-card__section-title">{{ T.SECTION_LOGISTICS }}</h4>
        <div class="order-card__grid">
          <div class="order-card__field">
            <span class="order-card__label">{{ T.LABEL_TRACKING }}</span>
            <span class="order-card__value">{{ order.trackingNumber }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="actionVisible" class="order-card__actions">
      <el-button type="primary" size="default" @click="$emit('ship', order)">{{ T.BTN_SHIP }}</el-button>
    </div>
  </div>
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
