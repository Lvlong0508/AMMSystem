<!-- src/components/ChatWindow/ChatWindow.vue -->
<template>
  <div class="chat-window flex flex-col h-full bg-gradient-to-b from-blue-50 to-white">
    <!-- Header -->
    <div class="bg-gradient-to-r from-blue-600 to-indigo-600 text-white p-6 shadow-lg flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold">{{ T.HEADER_TITLE }}</h1>
        <p class="text-blue-100 text-sm mt-1">{{ T.HEADER_SUBTITLE }}</p>
      </div>
      <button
        @click="startNewChat"
        class="px-4 py-2 bg-white/20 hover:bg-white/30 text-white rounded-lg transition-colors flex items-center gap-2 text-sm font-medium"
      >
        <span>+</span>
        <span>新对话</span>
      </button>
    </div>

    <!-- Messages container -->
    <div class="flex-1 overflow-y-auto p-6 space-y-4" ref="messagesContainer">
      <!-- 商品推荐模块 -->
      <div v-if="messages.length <= 1" class="flex flex-col items-center justify-center min-h-[400px]">
        <div class="text-center mb-8">
          <div class="text-6xl mb-4">{{ T.WELCOME_ICON }}</div>
          <p class="text-gray-600 text-lg font-semibold">{{ T.WELCOME_TITLE }}</p>
          <p class="text-gray-500 mt-2">{{ T.WELCOME_SUBTITLE }}</p>
        </div>
        
        <!-- 推荐商品区域 -->
        <div v-if="recommendedProducts.length > 0" class="w-full max-w-4xl px-4">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-bold text-gray-800 flex items-center gap-2">
              <span>🎁</span>
              <span>为您推荐</span>
            </h3>
            <button 
              @click="handleRefreshProducts"
              :disabled="refreshing"
              class="flex items-center gap-1 px-3 py-1.5 text-sm text-blue-600 hover:bg-blue-50 rounded-full transition-colors disabled:opacity-50"
            >
              <span :class="{ 'animate-spin': refreshing }">🔄</span>
              <span>换一换</span>
            </button>
          </div>
          
          <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
            <div 
              v-for="product in recommendedProducts" 
              :key="product.id || product.productId"
              class="bg-white rounded-xl shadow-sm hover:shadow-md transition-all hover:scale-105 overflow-hidden border border-gray-100"
            >
              <div class="bg-gradient-to-br from-blue-50 to-indigo-50 p-4 text-center border-b border-gray-100">
                <span class="text-4xl">📦</span>
              </div>
              <div class="p-3">
                <h4 class="text-sm font-medium text-gray-800 line-clamp-2 mb-1">{{ product.name }}</h4>
                <p class="text-red-500 font-bold text-base">¥{{ product.price }}</p>
                <button 
                  @click="handleShowOrderDialog(product)"
                  class="w-full mt-2 py-1.5 text-xs bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  立即下单
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <message-bubble
          v-for="(msg, idx) in messages"
          :key="idx"
          :message="msg.text"
          :role="msg.role"
          :products="msg.products"
          @showOrderDialog="handleShowOrderDialog"
      />

      <!-- Loading indicator -->
      <div v-if="loading" class="flex justify-start">
        <div class="bg-white rounded-lg rounded-bl-none shadow-md p-4 flex gap-2">
          <div class="w-3 h-3 bg-blue-400 rounded-full animate-bounce" />
          <div class="w-3 h-3 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0.15s" />
          <div class="w-3 h-3 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0.3s" />
        </div>
      </div>
    </div>

    <!-- 下单对话框 -->
    <order-dialog
        :visible="orderDialogVisible"
        :product="selectedProduct"
        @close="orderDialogVisible = false"
        @order-created="handleOrderCreated"
    />

    <!-- 支付对话框 -->
    <payment-dialog
        :visible="paymentDialogVisible"
        :order-info="paymentOrderInfo"
        @close="paymentDialogVisible = false"
        @success="handlePaymentSuccess"
        @cancel="handlePaymentCancel"
    />
    <div class="bg-white border-t border-gray-200 px-4 py-3 shadow-lg">
      <div class="flex gap-3 max-w-5xl mx-auto">
        <input
            v-model="inputMessage"
            @keydown.enter="handleSendMessage"
            type="text"
            :placeholder="T.PLACEHOLDER_INPUT"
            :disabled="loading"
            class="flex-1 px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
        />
        <button
            @click="handleSendMessage"
            :disabled="loading || !inputMessage.trim()"
            class="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-semibold"
        >
          {{ loading ? T.BTN_SENDING : T.BTN_SEND }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useChatLogic } from './useChatLogic.js'
import { CHAT_TEXT as T } from './Text.js'
import MessageBubble from '../MessageBubble/MessageBubble.vue'
import OrderDialog from '../Order/OrderDialog.vue'
import PaymentDialog from '../Payment/PaymentDialog.vue'

const {
  messages,
  inputMessage,
  loading,
  messagesContainer,
  orderDialogVisible,
  selectedProduct,
  recommendedProducts,
  refreshing,
  paymentDialogVisible,
  paymentOrderInfo,
  handleShowOrderDialog,
  handleOrderCreated,
  handlePaymentSuccess,
  handlePaymentCancel,
  handleSendMessage,
  handleRefreshProducts,
  startNewChat
} = useChatLogic()
</script>

<style scoped>
@import './ChatWindow.css';
</style>