<template>
  <el-card shadow="hover" class="order-card">
    <div class="order-card__abstract" @click="toggle">
      <div class="order-card__info">
        <div class="order-card__id">{{ T.LABEL_ORDER_ID }}: {{ order.orderId }}</div>
        <p class="order-card__product">{{ order.productName || `${T.LABEL_PRODUCT} #${order.productId}` }}</p>
        <div class="order-card__meta">
          <span>{{ T.LABEL_QUANTITY }}: {{ order.quantity }}</span>
          <span class="order-card__price">{{ formatPrice(order.totalPrice || order.price) }}</span>
        </div>
      </div>
      <div class="order-card__status">
        <el-tag :type="order.orderStatus === 'PAID' ? 'warning' : 'info'">{{ statusText }}</el-tag>
      </div>
      <el-button text class="order-card__toggle">
        <svg :class="{ 'is-expanded': expanded }" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
      </el-button>
    </div>

    <div v-if="expanded" class="order-card__concrete">
      <el-divider />
      <div class="order-card__detail-row">
        <span>{{ T.LABEL_DATE }}: {{ formatDate(order.orderDate) }}</span>
        <span v-if="order.contact">{{ T.LABEL_CUSTOMER_INFO }}: {{ order.contact.name }} {{ order.contact.phone }}</span>
      </div>
      <div class="order-card__actions">
        <el-button @click.stop="handleDetail">{{ T.BTN_DETAIL }}</el-button>
        <el-button v-if="isPendingShip" type="primary" @click.stop="handleShip">{{ T.BTN_SHIP }}</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { useOrderCard } from './OrderCard.js'

const props = defineProps({ order: { type: Object, required: true } })
const emit = defineEmits(['detail', 'ship'])

const { T, expanded, statusText, isPendingShip, toggle, handleDetail, handleShip, formatPrice, formatDate } = useOrderCard(props, emit)
</script>

<style scoped src="./OrderCard.css"></style>
