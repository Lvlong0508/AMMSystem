<template>
  <el-card
    v-if="variant === 'abstract'"
    shadow="hover"
    class="product-card product-card--abstract"
    @click="$emit('click', product)"
  >
    <div class="product-card__abstract">
      <div class="product-card__image">
        <el-image v-if="product.imageUrl" :src="product.imageUrl" style="width: 80px; height: 80px" fit="cover" />
        <div v-else class="product-card__image-placeholder">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
        </div>
      </div>
      <div class="product-card__info">
        <h3 class="product-card__name">{{ product.name }}</h3>
        <div class="product-card__meta">
          <span class="product-card__price">¥{{ product.price?.toFixed(2) }}</span>
          <span>{{ T.LABEL_STOCK }}: {{ product.stock }}</span>
        </div>
      </div>
      <div class="product-card__status">
        <el-tag v-if="product.isSale" type="success" size="small">{{ T.STATUS_ON }}</el-tag>
        <el-tag v-else type="info" size="small">{{ T.STATUS_OFF }}</el-tag>
      </div>
    </div>
  </el-card>

  <el-card v-else shadow="never" class="product-card product-card--detail">
    <div class="product-card__detail-layout">
      <div class="product-card__detail-image">
        <el-image v-if="product.imageUrl" :src="product.imageUrl" style="width: 240px; height: 240px" fit="cover" />
        <div v-else class="product-card__image-placeholder product-card__image-placeholder--lg">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
        </div>
      </div>
      <div class="product-card__detail-info">
        <div v-if="parsedTags.length" class="product-card__tags">
          <span v-for="tag in parsedTags" :key="tag" class="product-card__tag">{{ tag }}</span>
        </div>
        <h3 class="product-card__detail-name">{{ product.name }}</h3>
        <div class="product-card__price">¥{{ product.price?.toFixed(2) }}</div>
        <p class="product-card__desc">{{ product.description }}</p>
        <el-divider />
        <div class="product-card__detail-meta">
          <span>{{ T.LABEL_STOCK }}: {{ product.stock }}</span>
          <span>{{ T.LABEL_CREATED }}: {{ formatDate(product.createdAt) }}</span>
          <span>{{ T.LABEL_UPDATED }}: {{ formatDate(product.updatedAt) }}</span>
        </div>
        <div class="product-card__status">
          <el-tag v-if="product.isSale" type="success">{{ T.STATUS_ON }}</el-tag>
          <el-tag v-else type="info">{{ T.STATUS_OFF }}</el-tag>
        </div>
        <div class="product-card__actions">
          <el-button type="primary" @click="$emit('edit', product)">{{ T.BTN_EDIT }}</el-button>
          <el-button :type="product.isSale ? 'warning' : 'success'" @click="$emit('toggle-sale', product)">
            {{ product.isSale ? T.BTN_UNLIST : T.BTN_LIST }}
          </el-button>
          <el-button type="danger" @click="$emit('delete', product)">{{ T.BTN_DELETE }}</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { useProductCard } from './ProductCard.js'

const props = defineProps({
  product: { type: Object, required: true },
  variant: { type: String, default: 'abstract', validator: v => ['abstract', 'detail'].includes(v) }
})

defineEmits(['click', 'edit', 'toggle-sale', 'delete'])

const { T, parsedTags, formatDate } = useProductCard(props)
</script>

<style scoped src="./ProductCard.css"></style>
