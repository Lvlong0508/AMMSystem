<!-- src/components/Payment/PaymentDialog.vue -->
<template>
  <div v-if="visible" class="payment-dialog-overlay" @click="handleClose">
    <div class="payment-dialog" @click.stop>
      <!-- Header -->
      <div class="dialog-header">
        <h2 class="dialog-title">{{ T.DIALOG_TITLE }}</h2>
        <button class="close-btn" @click="handleClose">{{ T.BTN_CLOSE }}</button>
      </div>

      <!-- Content -->
      <div class="dialog-content">
        <!-- 订单信息卡片 -->
        <div class="order-info-card">
          <div class="order-header">
            <span class="order-icon">{{ T.ICON_ORDER }}</span>
            <div class="order-meta">
              <h3 class="order-name">{{ orderInfo?.product?.name }}</h3>
              <span class="order-id">{{ T.LABEL_ORDER_ID }}{{ orderInfo?.orderId }}</span>
            </div>
          </div>
          <div class="order-details">
            <div class="detail-item">
              <span class="label">{{ T.LABEL_QUANTITY }}</span>
              <span class="value">{{ orderInfo?.quantity }}</span>
            </div>
            <div class="detail-item">
              <span class="label">{{ T.LABEL_CONTACT }}</span>
              <span class="value">{{ orderInfo?.contact?.name }}</span>
            </div>
          </div>
        </div>

        <!-- 支付金额 -->
        <div class="payment-amount">
          <span class="amount-label">{{ T.LABEL_AMOUNT }}</span>
          <span class="amount-value">¥{{ orderInfo?.totalPrice?.toFixed(2) }}</span>
        </div>

        <!-- 支付方式选择 -->
        <div class="payment-methods">
          <h4 class="section-title">{{ T.SECTION_PAYMENT_METHOD }}</h4>
          <div class="method-list">
            <div
              v-for="method in paymentMethods"
              :key="method.id"
              class="method-item"
              :class="{ 'selected': selectedMethod === method.id }"
              @click="selectMethod(method.id)"
            >
              <div class="method-radio">
                <span v-if="selectedMethod === method.id" class="radio-checked">●</span>
                <span v-else class="radio-unchecked">○</span>
              </div>
              <div class="method-icon">{{ method.icon }}</div>
              <div class="method-info">
                <span class="method-name">{{ method.name }}</span>
                <span class="method-desc">{{ method.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="dialog-footer">
        <button class="btn-cancel" @click="handleClose">{{ T.BTN_CANCEL }}</button>
        <button
          class="btn-confirm"
          :disabled="!selectedMethod || processing"
          @click="handleConfirmPayment"
        >
          {{ processing ? T.BTN_PROCESSING : T.BTN_CONFIRM }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { usePaymentLogic } from './usePaymentLogic.js'
import { PAYMENT_TEXT as T } from './Text.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  orderInfo: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'success', 'cancel'])

const {
  selectedMethod,
  processing,
  paymentMethods,
  selectMethod,
  handleClose,
  handleConfirmPayment
} = usePaymentLogic(props, emit, T)
</script>

<style scoped>
@import './PaymentDialog.css';
</style>
