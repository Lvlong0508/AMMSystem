<template>
  <div v-if="variant === 'abstract'" class="product-card product-card--abstract" @click="$emit('viewDetail', product)">
    <div class="product-card__image-wrap">
      <img v-if="product.imageUrl" class="product-card__image" :src="product.imageUrl" :alt="product.name" />
      <div v-else class="product-card__image-placeholder">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
      </div>
    </div>
    <div class="product-card__name">{{ product.name }}</div>
    <div class="product-card__meta">
      <span class="product-card__stock">库存: {{ product.stock ?? '-' }}</span>
      <span class="product-card__price">¥{{ product.price?.toFixed(2) }}</span>
    </div>
    <button class="product-card__cta" @click.stop="$emit('buyNow', product)">去下单</button>
  </div>

  <div v-else class="product-card product-card--detail">
    <div class="product-card__image-col">
      <img class="product-card__detail-img" :src="product.imageUrl" :alt="product.name" />
    </div>
    <div class="product-card__info-col">
      <div class="product-card__tags">
        <span v-for="tag in parsedTags" :key="tag" class="product-card__tag">{{ tag }}</span>
      </div>
      <h3 class="product-card__detail-name">{{ product.name }}</h3>
      <div class="product-card__price-row">
        <span class="product-card__price">¥{{ product.price.toFixed(2) }}</span>
      </div>
      <p class="product-card__desc">{{ product.description }}</p>
      <div class="product-card__divider"></div>
      <div class="product-card__shop-row">
        <span>{{ product.shopName }}</span>
        <span>{{ T.STOCK_LABEL }}{{ product.stock }}</span>
      </div>
      <button class="product-card__cta" @click="$emit('buyNow', product)">{{ T.BUY_NOW }}</button>
    </div>
  </div>
</template>

<script setup>
import { PRODUCTION_CARD_TEXT as T } from './Text'
import { useProductCard } from './useProductCard'

const props = defineProps({
  variant: { type: String, default: 'abstract', validator: v => ['abstract', 'detail'].includes(v) },
  product: { type: Object, required: true }
})

defineEmits(['viewDetail', 'buyNow'])

const { parsedTags } = useProductCard(props)
</script>

<style scoped>
@import './ProductCard.css';
</style>
