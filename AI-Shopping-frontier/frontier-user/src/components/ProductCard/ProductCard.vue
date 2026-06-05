<template>
  <div v-if="variant === 'abstract'" class="product-card product-card--abstract" @click="$emit('viewDetail', product)">
    <div class="product-card__image-wrap">
      <img class="product-card__image" :src="product.imageUrl" :alt="product.name" />
      <span class="product-card__price-badge">¥{{ product.price.toFixed(2) }}</span>
    </div>
    <div class="product-card__body">
      <h3 class="product-card__name">{{ product.name }}</h3>
      <p class="product-card__desc">{{ product.description }}</p>
      <div class="product-card__tags">
        <span v-for="tag in parsedTags" :key="tag" class="product-card__tag">{{ tag }}</span>
      </div>
      <div class="product-card__divider"></div>
      <div class="product-card__footer">
        <span class="product-card__shop-stock">{{ product.shopName }} · {{ T.STOCK_LABEL }}{{ product.stock }}</span>
        <span class="product-card__view-link">{{ T.VIEW_DETAIL }}</span>
      </div>
    </div>
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
