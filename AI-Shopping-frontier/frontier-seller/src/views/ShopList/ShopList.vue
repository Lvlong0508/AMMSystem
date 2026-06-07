<template>
  <div class="shop-select-page">
    <div class="shop-select-page__header">
      <div class="shop-select-page__user">
        <el-avatar :size="36">{{ auth.merchantName?.charAt(0)?.toUpperCase() || 'M' }}</el-avatar>
        <span class="shop-select-page__username">{{ auth.merchantName }}</span>
      </div>
      <div class="shop-select-page__actions">
        <el-button size="default" @click="handleLogout">{{ T.BTN_LOGOUT }}</el-button>
      </div>
    </div>

    <div class="shop-select-page__body">
      <h1 class="shop-select-page__title">{{ T.SELECT_TITLE }}</h1>

      <div v-if="shopStore.shops.length > 0" class="shop-select-page__toolbar">
        <el-button type="primary" size="large" @click="goRegister">{{ T.BTN_CREATE }}</el-button>
      </div>

      <div v-loading="loading" class="shop-select-page__grid">
        <el-card
          v-for="shop in shopStore.shops"
          :key="shop.id"
          shadow="hover"
          class="shop-card"
          @click="showShopDetail(shop)"
        >
          <div class="shop-card__inner">
            <div class="shop-card__avatar">
              <el-avatar :size="64" shape="square">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="2" y="3" width="20" height="14" rx="2"/>
                  <path d="M8 21h8"/>
                  <path d="M12 17v4"/>
                </svg>
              </el-avatar>
            </div>
            <div class="shop-card__info">
              <h3 class="shop-card__name">{{ shop.name || `店铺 ${shop.id}` }}</h3>
              <span class="shop-card__id">ID: {{ shop.id }}</span>
            </div>
            <el-button type="primary" size="large" class="shop-card__btn" @click.stop="enterShop(shop.id)">
              {{ T.BTN_ENTER }}
            </el-button>
          </div>
        </el-card>
      </div>

      <el-empty v-if="!loading && shopStore.shops.length === 0" :description="T.EMPTY_TEXT">
        <el-button type="primary" size="large" @click="goRegister">{{ T.BTN_CREATE_NOW }}</el-button>
      </el-empty>
    </div>

    <el-dialog
      v-model="detailVisible"
      :title="selectedShop?.name || '店铺详情'"
      width="480px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="shop-detail">
        <div class="shop-detail__row">
          <span class="shop-detail__label">店铺名称</span>
          <span class="shop-detail__value">{{ shopDetail?.name || selectedShop?.name || '-' }}</span>
        </div>
        <div class="shop-detail__row">
          <span class="shop-detail__label">店铺ID</span>
          <span class="shop-detail__value">{{ selectedShop?.id || '-' }}</span>
        </div>
        <div class="shop-detail__row">
          <span class="shop-detail__label">店铺简介</span>
          <span class="shop-detail__value">{{ shopDetail?.description || '-' }}</span>
        </div>
        <div class="shop-detail__row">
          <span class="shop-detail__label">联系电话</span>
          <span class="shop-detail__value">{{ shopDetail?.phone || '-' }}</span>
        </div>
        <div class="shop-detail__row">
          <span class="shop-detail__label">店铺地址</span>
          <span class="shop-detail__value">{{ shopDetail?.address || '-' }}</span>
        </div>
        <div class="shop-detail__row">
          <span class="shop-detail__label">营业时间</span>
          <span class="shop-detail__value">{{ shopDetail?.businessHours || '-' }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeDetail">取消</el-button>
        <el-button type="primary" size="large" @click="enterShop(selectedShop?.id)">{{ T.BTN_ENTER }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShopList } from './ShopList.js'
const { loading, detailVisible, detailLoading, selectedShop, shopDetail, T, showShopDetail, closeDetail, enterShop, goRegister, handleLogout, auth, shopStore } = useShopList()
</script>

<style scoped src="./ShopList.css"></style>
