<!-- src/components/ProductCard/ProductCard.vue -->
<template>
  <div class="product-card rounded-lg overflow-hidden shadow-md border border-gray-200 bg-white transition-transform hover:scale-105">
    <!-- Header with emoji -->
    <div class="bg-gradient-to-br from-blue-50 to-indigo-50 p-4 text-center border-b border-gray-100">
      <span class="text-4xl">{{ T.ICON_PRODUCT }}</span>
    </div>

    <!-- Content -->
    <div class="p-4">
      <!-- Product name and ID -->
      <div class="flex items-center justify-between mb-2">
        <h3 class="font-semibold text-gray-800 text-sm line-clamp-2">{{ product.name }}</h3>
        <span class="text-xs text-gray-400 flex-shrink-0 ml-2">{{ T.ID_PREFIX }}{{ product.id }}</span>
      </div>

      <!-- Description -->
      <p class="text-xs text-gray-600 mb-2 line-clamp-2">{{ product.description }}</p>

      <!-- Tags -->
      <div class="flex flex-wrap gap-1 mb-3">
        <span
            v-for="tag in parseTags(product.tags)"
            :key="tag"
            class="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-600 font-medium"
        >
          {{ tag }}
        </span>
      </div>

      <!-- Stock info -->
      <div class="text-xs text-gray-500 mb-2">
        {{ T.LABEL_STOCK }}<span :class="product.stock > 0 ? 'text-green-600' : 'text-red-600'">{{ product.stock > 0 ? T.STOCK_IN : T.STOCK_OUT }}</span>
      </div>

      <!-- Price and button -->
      <div class="flex items-center justify-between pt-2 border-t border-gray-100">
        <span class="text-lg font-bold text-red-500">¥{{ product.price.toFixed(2) }}</span>
        <button
            @click="handleOrderClick"
            :disabled="product.stock <= 0"
            class="px-3 py-1 bg-blue-600 text-white rounded-full text-sm hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-semibold"
        >
          {{ product.stock > 0 ? T.BTN_ORDER : T.BTN_OUT_OF_STOCK }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useProductCardLogic } from './useProductCard.js'
import { PRODUCT_CARD_TEXT as T } from './Text.js'

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['order', 'showOrderDialog'])

const { parseTags, handleOrderClick } = useProductCardLogic(props, emit)
</script>

<style scoped>
@import './ProductCard.css';
</style>