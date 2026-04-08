<!-- src/components/Order/OrderDialog.vue -->
<template>
  <div v-if="visible" class="order-dialog-overlay" @click="handleClose">
    <div class="order-dialog" @click.stop>
      <!-- Header -->
      <div class="dialog-header">
        <h2 class="dialog-title">{{ T.DIALOG_TITLE }}</h2>
        <button class="close-btn" @click="handleClose">{{ T.BTN_CLOSE }}</button>
      </div>

      <!-- Content -->
      <div class="dialog-content">
        <!-- 商品信息卡片 -->
        <div class="product-info-card">
          <div class="product-header">
            <span class="product-icon">{{ T.ICON_PRODUCT }}</span>
            <div class="product-meta">
              <h3 class="product-name">{{ product?.name }}</h3>
              <span class="product-id">{{ T.LABEL_PRODUCT_ID }}{{ product?.id }}</span>
            </div>
          </div>
          <p class="product-desc">{{ product?.description }}</p>
          <div class="product-tags" v-if="parseTags(product?.tags).length > 0">
            <span v-for="tag in parseTags(product?.tags)" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </div>

        <!-- 价格与数量 -->
        <div class="price-quantity-section">
          <div class="unit-price">
            <span class="label">{{ T.LABEL_UNIT_PRICE }}</span>
            <span class="value price">¥{{ product?.price?.toFixed(2) }}</span>
          </div>
          <div class="quantity-selector">
            <span class="label">{{ T.LABEL_QUANTITY }}</span>
            <div class="quantity-control">
              <button class="qty-btn" @click="decreaseQty" :disabled="quantity <= 1">{{ T.BTN_DECREASE }}</button>
              <input
                type="number"
                v-model.number="quantity"
                min="1"
                :max="maxStock"
                class="qty-input"
                @change="validateQuantity"
              />
              <button class="qty-btn" @click="increaseQty" :disabled="quantity >= maxStock">{{ T.BTN_INCREASE }}</button>
            </div>
            <span class="stock-hint">{{ T.LABEL_STOCK }}{{ product?.stock }}</span>
          </div>
        </div>

        <!-- 选择收货地址 -->
        <div class="contact-section">
          <h4 class="section-title">{{ T.SECTION_CONTACT }}</h4>
          
          <!-- 加载中 -->
          <div v-if="loadingContacts" class="loading-contacts">
            {{ T.LOADING_CONTACTS }}
          </div>
          
          <!-- 无地址 -->
          <div v-else-if="contacts.length === 0" class="empty-contacts">
            <p>{{ T.EMPTY_CONTACTS }}</p>
          </div>
          
          <!-- 地址列表 -->
          <div v-else class="contact-list">
            <div
              v-for="contact in contacts"
              :key="contact.id"
              class="contact-item"
              :class="{ 'selected': selectedContact?.id === contact.id }"
              @click="selectContact(contact)"
            >
              <div class="contact-radio">
                <span v-if="selectedContact?.id === contact.id" class="radio-checked">●</span>
                <span v-else class="radio-unchecked">○</span>
              </div>
              <div class="contact-info">
                <div class="contact-header">
                  <span class="contact-name">{{ contact.name }}</span>
                  <span class="contact-phone">{{ contact.phone }}</span>
                </div>
                <div class="contact-address">{{ contact.address }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 订单汇总 -->
        <div class="order-summary">
          <div class="summary-row">
            <span>{{ T.LABEL_SUBTOTAL }}</span>
            <span>¥{{ subtotal.toFixed(2) }}</span>
          </div>
          <!-- <div class="summary-row">
            <span>{{ T.LABEL_SHIPPING }}</span>
            <span class="free">{{ T.LABEL_FREE_SHIPPING }}</span>
          </div> -->
          <div class="summary-row total">
            <span>{{ T.LABEL_TOTAL }}</span>
            <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="dialog-footer">
        <button class="btn-cancel" @click="handleClose">{{ T.BTN_CANCEL }}</button>
        <button
          class="btn-submit"
          :disabled="!isValid || submitting"
          @click="handleSubmit"
        >
          {{ submitting ? T.BTN_SUBMITTING : T.BTN_SUBMIT }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useOrderLogic } from './useOrderLogic.js'
import { ORDER_DIALOG_TEXT as T } from './Text.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  product: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'success'])

const {
  quantity,
  submitting,
  contacts,
  loadingContacts,
  selectedContact,
  maxStock,
  subtotal,
  totalPrice,
  isValid,
  parseTags,
  increaseQty,
  decreaseQty,
  validateQuantity,
  handleClose,
  handleSubmit,
  selectContact
} = useOrderLogic(props, emit)
</script>

<style scoped>
@import './OrderDialog.css';
</style>
