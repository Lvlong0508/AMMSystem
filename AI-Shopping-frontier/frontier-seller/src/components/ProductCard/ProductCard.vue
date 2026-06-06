<template>
  <el-card shadow="hover" class="product-card">
    <div class="product-card__abstract" @click="toggle">
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
        <el-tag v-if="product.isSale" type="success">{{ T.STATUS_ON }}</el-tag>
        <el-tag v-else type="info">{{ T.STATUS_OFF }}</el-tag>
      </div>
      <el-button text class="product-card__toggle">
        <svg :class="{ 'is-expanded': expanded }" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
      </el-button>
    </div>

    <div v-if="expanded" class="product-card__concrete">
      <el-divider />
      <p v-if="product.description" class="product-card__desc">{{ product.description }}</p>
      <div class="product-card__actions">
        <el-button type="primary" @click.stop="handleEdit">{{ T.BTN_EDIT }}</el-button>
        <el-button :type="product.isSale ? 'warning' : 'success'" @click.stop="handleToggleSale">
          {{ product.isSale ? T.BTN_UNLIST : T.BTN_LIST }}
        </el-button>
        <el-button type="danger" @click.stop="handleDelete">{{ T.BTN_DELETE }}</el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { useProductCard } from './ProductCard.js'

const props = defineProps({ product: { type: Object, required: true } })
const emit = defineEmits(['edit', 'delete', 'toggle-sale'])

const { T, expanded, toggle, handleEdit, handleToggleSale, handleDelete } = useProductCard(props, emit)
</script>

<style scoped src="./ProductCard.css"></style>
