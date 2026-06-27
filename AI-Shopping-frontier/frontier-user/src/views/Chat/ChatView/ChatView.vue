<template>
  <div class="chat-view" :class="{ 'chat-view--empty': messages.length === 0, 'chat-view--has-messages': messages.length > 0 }">
    <div class="chat-view__messages" ref="messagesRef">
      <div v-if="messages.length === 0" class="chat-view__welcome">
        <div class="chat-view__welcome-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
        </div>
        <h2 class="chat-view__welcome-title">{{ T.WELCOME_TITLE }}</h2>
        <p class="chat-view__welcome-sub">{{ T.WELCOME_SUB }}</p>
        <div class="chat-view__quick-replies">
          <button
            v-for="(text, i) in T.QUICK_REPLIES"
            :key="i"
            class="chat-view__quick-btn"
            @click="handleQuickReply(text)"
          >{{ text }}</button>
        </div>
      </div>

      <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="chat-view__bubble-wrap"
          :class="[msg.role === 'user' ? 'chat-view__bubble-wrap--user' : 'chat-view__bubble-wrap--ai']"
          :style="{ animationDelay: `${idx * 0.1}s` }"
      >
        <div v-if="msg.role === 'user'" class="chat-view__bubble chat-view__bubble--user">
          {{ msg.text }}
        </div>

        <div v-else class="chat-view__ai-response">
          <div class="chat-view__ai-text">{{ msg.text }}</div>
          <div v-if="msg.products && msg.products.length" class="chat-view__data-divider"></div>
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

      <div v-if="loading" class="chat-view__loading">
        <div class="chat-view__typing">
          <span class="chat-view__dot"></span>
          <span class="chat-view__dot"></span>
          <span class="chat-view__dot"></span>
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

    <div v-if="showDetailModal" class="detail-overlay" @click.self="showDetailModal = false">
      <div v-if="detailLoading" class="detail-modal" style="text-align:center;padding:48px 32px">
        <div class="chat-view__typing">
          <span class="chat-view__dot"></span>
          <span class="chat-view__dot"></span>
          <span class="chat-view__dot"></span>
        </div>
      </div>
      <div v-else-if="detailProduct" class="detail-modal">
        <div class="detail-modal__img-wrap">
          <img class="detail-modal__img" :src="detailProduct.imageUrl" :alt="detailProduct.name" />
        </div>
        <div class="detail-modal__body">
          <h3 class="detail-modal__name">{{ detailProduct.name }}</h3>
          <div class="detail-modal__price">¥{{ detailProduct.price?.toFixed(2) }}</div>
          <div v-if="detailProduct.tags" class="detail-modal__tags">
            <span v-for="tag in (typeof detailProduct.tags === 'string' ? detailProduct.tags.split(',') : detailProduct.tags)" :key="tag" class="detail-modal__tag">{{ tag }}</span>
          </div>
          <div class="detail-modal__stock">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
            <span>{{ T.DETAIL_STOCK }}：<strong>{{ detailProduct.stock ?? '-' }}</strong></span>
          </div>
          <div v-if="detailProduct.description" class="detail-modal__desc">
            <p>{{ detailProduct.description }}</p>
          </div>
        </div>
        <div class="detail-modal__actions">
          <button class="detail-modal__btn detail-modal__btn--primary" @click="showDetailModal = false; handleBuyNow(detailProduct)">{{ T.DETAIL_BUY }}</button>
          <button class="detail-modal__btn detail-modal__btn--secondary" @click="showDetailModal = false">{{ T.DETAIL_CLOSE }}</button>
        </div>
      </div>
    </div>

    <OrderModal :visible="showOrderModal" :product="selectedProduct" @close="showOrderModal = false" @order-placed="onOrderPlaced" />
    <PaymentModal :visible="showPaymentModal" :orderId="placedOrderId" :order="placedOrder" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
  </div>
</template>

<script setup>
import { CHAT_VIEW_TEXT as T } from './Text'
import { useChatView } from './useChatView'
import ProductCard from '@/components/ProductCard/ProductCard.vue'
import IInput from '@/components/IInput/IInput.vue'
import OrderModal from '@/components/OrderModal/OrderModal.vue'
import PaymentModal from '@/components/PaymentModal/PaymentModal.vue'

const { messages, loading, inputText, inputRef, messagesRef, handleSend, handleQuickReply, handleViewDetail, handleBuyNow, showDetailModal, detailProduct, detailLoading, showOrderModal, showPaymentModal, selectedProduct, placedOrderId, placedOrder, onOrderPlaced, onPaymentSuccess, onPayLater } = useChatView()
</script>

<style scoped>
@import './ChatView.css';
</style>