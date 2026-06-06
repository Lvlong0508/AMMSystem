<template>
  <div class="shop-list">
    <div class="shop-list__toolbar">
      <h2 class="shop-list__title">
        {{ T.PAGE_TITLE }}
        <el-tag v-if="shops.length > 0" type="primary" size="small">{{ shops.length }}</el-tag>
      </h2>
      <div>
        <el-button size="small" @click="loadShops">{{ T.BTN_REFRESH }}</el-button>
        <el-button size="small" type="primary" @click="$router.push('/shop/register')">
          {{ T.BTN_CREATE }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="shop-list__grid">
      <el-card v-for="shop in shops" :key="shop.id" shadow="hover" class="shop-card">
        <div class="shop-card__header">
          <div>
            <h3 class="shop-card__name">{{ shop.name || `店铺 ${shop.id}` }}</h3>
            <p v-if="shop.description" class="shop-card__desc">{{ shop.description }}</p>
          </div>
          <el-tag :type="getStatusType(shop.status)" size="small">
            {{ getStatusText(shop.status) }}
          </el-tag>
        </div>

        <div class="shop-card__details">
          <span class="shop-card__label">{{ T.LABEL_ADDRESS }}:</span>
          <span>{{ shop.address || '-' }}</span>
          <span class="shop-card__label">{{ T.LABEL_PHONE }}:</span>
          <span>{{ shop.phone || '-' }}</span>
          <span class="shop-card__label">{{ T.LABEL_HOURS }}:</span>
          <span>{{ shop.businessHours || '-' }}</span>
          <span class="shop-card__label">{{ T.LABEL_CREATED }}:</span>
          <span>{{ formatDate(shop.createdAt) }}</span>
        </div>

        <div class="shop-card__actions">
          <el-button size="small" @click="goToProducts(shop.id)">{{ T.BTN_PRODUCTS }}</el-button>
          <el-button size="small" @click="goToOrders(shop.id)">{{ T.BTN_ORDERS }}</el-button>
          <el-button size="small" @click="goToEmployees(shop.id)">{{ T.BTN_EMPLOYEES }}</el-button>
          <el-button size="small" @click="goToAddresses(shop.id)">{{ T.BTN_ADDRESSES }}</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && shops.length === 0" :description="T.EMPTY_TEXT">
      <el-button type="primary" @click="$router.push('/shop/register')">
        {{ T.BTN_CREATE_NOW }}
      </el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { useShopList } from './ShopList.js'

const { shops, loading, T, getStatusText, getStatusType, formatDate, goToProducts, goToOrders, goToEmployees, goToAddresses, loadShops } = useShopList()
</script>

<style scoped src="./ShopList.css"></style>
