<!-- src/components/MessageBubble/MessageBubble.vue -->
<template>
  <div :class="['message-bubble', `role-${role}`]">
    <div :class="['bubble-content', `bg-${role}`]">
      <!-- Text content -->
      <p class="text-sm leading-relaxed whitespace-pre-wrap">{{ message }}</p>

      <!-- Product cards -->
      <div v-if="products && products.length > 0" class="mt-4 grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3">
        <product-card
            v-for="product in products"
            :key="product.id"
            :product="product"
            @showOrderDialog="$emit('showOrderDialog', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import ProductCard from '../ProductCard/ProductCard.vue'
import { defineProps, defineEmits } from 'vue'

defineProps({
  message: {
    type: String,
    required: true
  },
  role: {
    type: String,
    enum: ['user', 'ai'],
    required: true
  },
  products: {
    type: Array,
    default: () => []
  }
})

defineEmits(['showOrderDialog'])
</script>

<style scoped>
@import './MessageBubble.css';
</style>