<template>
  <div class="register-page">
    <el-card shadow="always" class="register-card">
      <div class="register-brand">
        <svg class="register-brand__icon" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="oklch(0.9 0.02 240)"/>
          <path d="M16 30V20l8 6 8-6v10" stroke="oklch(0.5 0.1 240)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <h1 class="register-brand__title">{{ T.PAGE_TITLE }}</h1>
        <p class="register-brand__subtitle">{{ T.BRAND_NAME }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleRegister"
      >
        <el-form-item :label="T.LABEL_USERNAME" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE" prop="phone">
          <el-input v-model="form.phone" autocomplete="tel" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PASSWORD" prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item :label="T.LABEL_CONFIRM" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" class="register-btn" round size="large">
            {{ submitting ? T.BTN_LOADING : T.BTN_REGISTER }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <router-link to="/login" class="register-footer__link">{{ T.LINK_LOGIN }}</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRegister } from './Register.js'
const { T, formRef, form, submitting, rules, handleRegister } = useRegister()
</script>

<style scoped src="./Register.css"></style>
