<template>
  <Teleport to="body">
    <div v-if="visible" class="order-modal-overlay" @click="$emit('close')">
      <div class="order-modal" @click.stop>
        <div class="order-modal__header">
          <span class="order-modal__title">{{ T.TITLE }}</span>
          <button class="order-modal__close" @click="$emit('close')">✕</button>
        </div>

        <div class="order-modal__body">
          <div class="order-modal__product">
            <img v-if="product.imageUrl" class="order-modal__product-img" :src="product.imageUrl" :alt="product.name" />
            <div v-else class="order-modal__product-img-placeholder">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
            </div>
            <div class="order-modal__product-info">
              <span class="order-modal__product-name">{{ product.name }}</span>
              <div class="order-modal__product-price">¥{{ product.price.toFixed(2) }}</div>
            </div>
          </div>

          <div class="order-modal__section">
            <span class="order-modal__section-label">{{ T.QUANTITY }}</span>
            <div class="order-modal__qty-selector">
              <button class="order-modal__qty-btn" :disabled="quantity <= 1" @click="decrement">−</button>
              <input class="order-modal__qty-input" type="number" :min="1" :max="maxQuantity" v-model.number="quantity" @input="onQtyInput" />
              <button class="order-modal__qty-btn" :disabled="quantity >= maxQuantity" @click="increment">+</button>
            </div>
          </div>

          <div class="order-modal__section">
            <span class="order-modal__section-label">{{ T.ADDRESS }}</span>
            <div v-if="loadingAddress" class="order-modal__loading-address">加载中...</div>
            <div v-else-if="contacts.length === 0" class="order-modal__no-address">
              <p>{{ T.NO_ADDRESS }}</p>
              <a class="order-modal__manage-address" @click="goManageAddress">{{ T.MANAGE_ADDRESS }}</a>
            </div>
            <div v-else class="order-modal__address-list">
              <div
                v-for="c in contacts"
                :key="c.id"
                class="order-modal__address-item"
                :class="{ 'order-modal__address-item--selected': selectedContactId === c.id }"
                @click="selectedContactId = c.id"
              >
                <div class="order-modal__address-radio">
                  <div v-if="selectedContactId === c.id" class="order-modal__address-radio-inner"></div>
                </div>
                <div class="order-modal__address-detail">
                  <span class="order-modal__address-name">{{ c.name }}</span>
                  <span class="order-modal__address-phone">{{ c.phone }}</span>
                  <div class="order-modal__address-text">{{ c.address }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="order-modal__footer">
          <div>
            <span class="order-modal__total-label">{{ T.TOTAL }}: </span>
            <span class="order-modal__total-amount">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <button class="order-modal__submit" :disabled="!canSubmit" @click="handleSubmit">
            {{ submitting ? T.SUBMITTING : T.SUBMIT }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ORDER_MODAL_TEXT as T } from './Text'
import { useOrderModal } from './useOrderModal'

const props = defineProps({
  visible: { type: Boolean, default: false },
  product: { type: Object, required: true }
})

const emit = defineEmits(['close', 'order-placed'])

const { quantity, contacts, selectedContactId, totalPrice, maxQuantity, canSubmit, submitting, loadingAddress, decrement, increment, handleSubmit } = useOrderModal(props, { emit })

const onQtyInput = () => {
  if (!quantity.value || quantity.value < 1) quantity.value = 1
  if (quantity.value > maxQuantity.value) quantity.value = maxQuantity.value
}

const goManageAddress = () => {
  emit('close')
}
</script>

<style scoped>
@import './OrderModal.css';
</style>
