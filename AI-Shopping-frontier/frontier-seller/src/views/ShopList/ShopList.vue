<template>
  <div class="shop-list">
    <div class="shop-list__toolbar">
      <h2 class="shop-list__title">
        {{ T.PAGE_TITLE }}
        <el-tag v-if="shops.length > 0" type="primary">{{ shops.length }}</el-tag>
      </h2>
      <el-button type="primary" @click="goRegister">{{ T.BTN_CREATE }}</el-button>
    </div>

    <div v-loading="loading" class="shop-list__grid">
      <el-card
        v-for="shop in shops"
        :key="shop.id"
        shadow="hover"
        class="shop-card"
        @click="enterShop(shop.id)"
      >
        <div class="shop-card__header">
          <el-avatar :size="48" class="shop-card__avatar">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8"/><path d="M12 17v4"/></svg>
          </el-avatar>
          <div class="shop-card__info">
            <h3 class="shop-card__name">{{ shop.name || `店铺 ${shop.id}` }}</h3>
            <span class="shop-card__id">ID: {{ shop.id }}</span>
          </div>
        </div>
        <div class="shop-card__footer">
          <el-button type="primary" size="default" @click.stop="enterShop(shop.id)">
            {{ T.BTN_ENTER }}
          </el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && shops.length === 0" :description="T.EMPTY_TEXT">
      <el-button type="primary" @click="goRegister">{{ T.BTN_CREATE_NOW }}</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { useShopList } from './ShopList.js'
const { shops, loading, T, formatDate, enterShop, loadShops, goRegister } = useShopList()
</script>

<style scoped src="./ShopList.css"></script>
<style>
</style>
