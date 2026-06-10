<template>
  <div class="chat-view" :class="{ 'chat-view--empty': messages.length === 0, 'chat-view--has-messages': messages.length > 0 }">
    <div class="chat-view__messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="chat-view__welcome">
        <div class="chat-view__welcome-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
        </div>
        <h2 class="chat-view__welcome-title">{{ T.WELCOME_TITLE }}</h2>
        <p class="chat-view__welcome-sub">{{ T.WELCOME_SUB }}</p>
      </div>

      <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="chat-view__bubble-wrap"
          :class="[msg.role === 'user' ? 'chat-view__bubble-wrap--user' : 'chat-view__bubble-wrap--ai']"
          :style="{ animationDelay: `${idx * 0.1}s` }"
      >
        <div class="chat-view__bubble" :class="msg.role === 'user' ? 'chat-view__bubble--user' : 'chat-view__bubble--ai'">
          {{ msg.text }}
          <div v-if="msg.products && msg.products.length" class="chat-view__product-row">
            <ProductCard
                v-for="p in msg.products"
                :key="p.id"
                variant="abstract"
                :product="p"
                @viewDetail="(prod) => handleViewDetail(prod)"
                @buyNow="(prod) => handleBuyNow(prod)"
            />
          </div>
        </div>
      </div>

      <div v-if="loading" class="chat-view__bubble-wrap chat-view__bubble-wrap--ai">
        <div class="chat-view__bubble chat-view__bubble--ai">
          <div class="chat-view__typing">
            <span class="chat-view__dot"></span>
            <span class="chat-view__dot"></span>
            <span class="chat-view__dot"></span>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-view__input-bar">
      <div class="chat-view__input-wrap">
        <IInput
            ref="inputRef"
            v-model="inputText"
            :placeholder="T.INPUT_PLACEHOLDER"
            :disabled="loading"
            @keydown.enter="handleSend"
        />
      </div>
      <button
          class="chat-view__send-btn"
          :class="{ 'chat-view__send-btn--active': inputText.trim() }"
          :disabled="!inputText.trim() || loading"
          @click="handleSend"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
      </button>
    </div>

    <OrderModal :visible="showOrderModal" :product="selectedProduct" @close="showOrderModal = false" @order-placed="onOrderPlaced" />
    <PaymentModal :visible="showPaymentModal" :orderId="placedOrderId" :order="selectedProduct" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
  </div>
</template>

<script setup>
import { CHAT_VIEW_TEXT as T } from './Text'
import { useChatView } from './useChatView'
import ProductCard from '@/components/ProductCard/ProductCard.vue'
import IInput from '@/components/IInput/IInput.vue'
import OrderModal from '@/components/OrderModal/OrderModal.vue'
import PaymentModal from '@/components/PaymentModal/PaymentModal.vue'

const { messages, loading, inputText, inputRef, messagesRef, handleSend, handleViewDetail, handleBuyNow, showOrderModal, showPaymentModal, selectedProduct, placedOrderId, onOrderPlaced, onPaymentSuccess, onPayLater } = useChatView()
</script>

<style scoped>
@import './ChatView.css';
</style>