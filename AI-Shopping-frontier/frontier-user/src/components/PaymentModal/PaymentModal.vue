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
              <div class="payment-modal__summary-row">
                <span class="payment-modal__summary-label">商品</span>
                <span class="payment-modal__summary-value">{{ order?.productName || '-' }}</span>
              </div>
              <div class="payment-modal__summary-row">
                <span class="payment-modal__summary-label">数量</span>
                <span class="payment-modal__summary-value">x{{ order?.quantity || '-' }}</span>
              </div>
              <div class="payment-modal__summary-divider"></div>
              <div class="payment-modal__total-row">
                <span class="payment-modal__total-label">{{ T.TOTAL }}</span>
                <span class="payment-modal__total-amount">¥{{ (order?.totalPrice || 0).toFixed(2) }}</span>
              </div>
            </div>

            <div v-if="remainingMinutes <= 10" class="payment-modal__timeout">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              <span>{{ T.TIMEOUT_WARNING.replace('{minutes}', remainingMinutes) }}</span>
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
  order: { type: Object, default: null },
  orderDate: { type: [String, Date], default: null }
})

const emit = defineEmits(['close', 'pay-success', 'pay-later', 'timeout'])

const { selectedMethod, paying, paid, methods, remainingMinutes, handlePay, handlePayLater } = usePaymentModal(props, { emit })
</script>

<style scoped>
@import './PaymentModal.css';
</style>
