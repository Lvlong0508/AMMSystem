<template>
  <div class="login-page">
    <transition name="el-fade-in-linear">
      <div v-if="verifying" class="login-verifying">
        <div class="login-verifying__spinner"></div>
        <h2 class="login-verifying__title">{{ T.BRAND_NAME }}</h2>
        <p class="login-verifying__text">{{ T.VERIFYING }}</p>
      </div>
    </transition>

    <el-card v-show="!verifying" class="login-card" shadow="always">
      <div class="login-brand">
        <svg class="login-brand__icon" viewBox="0 0 48 48" fill="none">
          <rect width="48" height="48" rx="12" fill="oklch(0.9 0.02 240)"/>
          <path d="M16 30V20l8 6 8-6v10" stroke="oklch(0.5 0.1 240)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <h1 class="login-brand__title">{{ T.PAGE_TITLE }}</h1>
        <p class="login-brand__subtitle">{{ T.BRAND_NAME }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item :label="T.USERNAME_LABEL" prop="username">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>

        <el-form-item :label="T.PASSWORD_LABEL" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            :loading="loading"
            class="login-btn"
            round
          >
            {{ loading ? T.LOGIN_LOADING : T.LOGIN_BUTTON }}
          </el-button>
        </el-form-item>
      </el-form>

    </el-card>

    <div class="login-footer">
      <router-link to="/register" class="login-footer__link">{{ T.LINK_REGISTER }}</router-link>
    </div>
  </div>
</template>

<script setup>
import { useLogin } from './Login.js'

const { T, formRef, form, loading, verifying, rules, handleLogin } = useLogin()
</script>

<style scoped src="./Login.css"></style>
