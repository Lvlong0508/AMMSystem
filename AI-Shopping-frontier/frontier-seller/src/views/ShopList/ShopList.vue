<template>
  <div class="shop-select-page">
    <div class="shop-select-page__header">
      <div class="shop-select-page__user">
        <el-avatar :size="36">{{ auth.merchantName?.charAt(0)?.toUpperCase() || 'M' }}</el-avatar>
        <span class="shop-select-page__username">{{ auth.merchantName }}</span>
      </div>
    </div>

    <div class="shop-select-page__body">
      <h1 class="shop-select-page__title">{{ T.SELECT_TITLE }}</h1>

      <div v-loading="loading" class="shop-select-page__grid">
        <el-card
          v-for="shop in shopStore.shops"
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

      <el-empty v-if="!loading && shopStore.shops.length === 0" :description="T.EMPTY_TEXT">
        <el-button type="primary" @click="goRegister">{{ T.BTN_CREATE_NOW }}</el-button>
      </el-empty>
    </div>

    <div class="shop-select-page__footer">
      <el-button type="primary" @click="goRegister">{{ T.BTN_CREATE }}</el-button>
      <el-button @click="handleLogout">{{ T.BTN_LOGOUT }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { useShopList } from './ShopList.js'
const { loading, T, enterShop, goRegister, handleLogout, auth, shopStore } = useShopList()
</script>

<style scoped src="./ShopList.css"></style>
