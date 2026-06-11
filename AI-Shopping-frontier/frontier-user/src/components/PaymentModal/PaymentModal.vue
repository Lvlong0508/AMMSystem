<template>
  <Teleport to="body">
    <div v-if="visible" class="payment-modal-overlay" @click="handlePayLater">
      <div class="payment-modal" @click.stop>
        <template v-if="paid">
          <div class="payment-modal__success">
            <div class="payment-modal__success-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
            </div>
            <div class="payment-modal__success-text">{{ T.PAY_SUCCESS }}</div>
            <button class="payment-modal__success-btn" @click="$emit('close')">确定</button>
          </div>
        </template>
        <template v-else>
          <div class="payment-modal__header">
            <span class="payment-modal__title">{{ T.TITLE }}</span>
            <button class="payment-modal__close" @click="handlePayLater">✕</button>
          </div>
          <div class="payment-modal__body">
            <div class="payment-modal__order-summary">
              <div class="payment-modal__summary-row">
                <span class="payment-modal__summary-label">{{ T.ORDER_ID }}</span>
                <span class="payment-modal__summary-value">{{ orderId }}</span>
              </div>
              <div class="payment-modal__product-summary">
                <img v-if="productImage" class="payment-modal__product-img" :src="productImage" :alt="order.productName" />
                <div v-else class="payment-modal__product-img-placeholder">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
                </div>
                <div class="payment-modal__product-info">
                  <span class="payment-modal__product-name">{{ order?.productName || '-' }}</span>
                  <span class="payment-modal__product-quantity">x{{ order?.quantity || '-' }}</span>
                </div>
              </div>
              <div class="payment-modal__summary-divider"></div>
              <div class="payment-modal__total-row">
                <span class="payment-modal__total-label">{{ T.TOTAL }}</span>
                <span class="payment-modal__total-amount">¥{{ Number(order?.totalPrice || 0).toFixed(2) }}</span>
              </div>
            </div>

            <span class="payment-modal__section-label">{{ T.PAY_METHOD }}</span>
            <div class="payment-modal__method-list">
              <div
                v-for="m in methods"
                :key="m.key"
                class="payment-modal__method-item"
                :class="{ 'payment-modal__method-item--selected': selectedMethod === m.key }"
                @click="selectedMethod = m.key"
              >
                <div class="payment-modal__method-radio">
                  <div v-if="selectedMethod === m.key" class="payment-modal__method-radio-inner"></div>
                </div>
                <span class="payment-modal__method-label">{{ m.label }}</span>
              </div>
            </div>

            <div class="payment-modal__actions">
              <button class="payment-modal__pay-btn" :disabled="paying" @click="handlePay">
                {{ paying ? T.PAYING : T.PAY_NOW }}
              </button>
              <button class="payment-modal__pay-later-btn" @click="handlePayLater">
                {{ T.PAY_LATER }}
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { PAYMENT_MODAL_TEXT as T } from './Text'
import { usePaymentModal } from './usePaymentModal'

const props = defineProps({
  visible: { type: Boolean, default: false },
  orderId: { type: String, default: '' },
  order: { type: Object, default: null }
})

const emit = defineEmits(['close', 'pay-success', 'pay-later'])

const { selectedMethod, paying, paid, methods, handlePay, handlePayLater, productImage } = usePaymentModal(props, { emit })
</script>

<style scoped>
@import './PaymentModal.css';
</style>
