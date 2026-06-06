<template>
  <div class="shop-register">
    <el-card shadow="never" class="shop-register__card">
      <div class="shop-register__brand">
        <svg class="shop-register__brand-icon" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="oklch(0.9 0.02 240)"/>
          <path d="M16 30V20l8 6 8-6v10" stroke="oklch(0.5 0.1 240)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <h1 class="shop-register__title">{{ T.PAGE_TITLE }}</h1>
        <p class="shop-register__subtitle">{{ T.SUBTITLE }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item :label="T.LABEL_NAME" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="T.PLACEHOLDER_NAME"
            :maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            :placeholder="T.PLACEHOLDER_DESC"
            :maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="shop-register__submit" size="large" round>
            {{ submitting ? T.BTN_SUBMITTING : T.BTN_SUBMIT }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="shop-register__footer">
        <el-button text @click="handleLogout">{{ T.BTN_LOGOUT }}</el-button>
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { useShopRegister } from './ShopRegister.js'
const { T, formRef, form, submitting, rules, handleSubmit, handleLogout } = useShopRegister()
</script>
<style scoped src="./ShopRegister.css"></style>