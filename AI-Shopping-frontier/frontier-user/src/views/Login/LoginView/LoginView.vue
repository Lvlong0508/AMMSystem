<template>
  <div class="login-view">
    <div class="login-view__card">
      <div class="login-view__header">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="login-view__logo"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>
        <h2 class="login-view__title">{{ isRegister ? T.REGISTER_TITLE : T.LOGIN_TITLE }}</h2>
        <p class="login-view__subtitle">{{ isRegister ? T.REGISTER_SUB : T.LOGIN_SUB }}</p>
      </div>

      <div class="login-view__form">
        <div class="login-view__field">
          <label class="login-view__label">{{ T.USERNAME }}</label>
          <input
            v-model="form.username"
            class="login-view__input"
            :class="{ 'login-view__input--error': errors.username }"
            :placeholder="T.USERNAME_PLACEHOLDER"
            @blur="validateUsername"
          />
          <span v-if="errors.username" class="login-view__error">{{ errors.username }}</span>
        </div>

        <div class="login-view__field">
          <label class="login-view__label">{{ T.PASSWORD }}</label>
          <input
            v-model="form.password"
            class="login-view__input"
            :class="{ 'login-view__input--error': errors.password }"
            type="password"
            :placeholder="T.PASSWORD_PLACEHOLDER"
            @blur="validatePassword"
          />
          <span v-if="errors.password" class="login-view__error">{{ errors.password }}</span>
        </div>

        <div v-if="isRegister" class="login-view__field">
          <label class="login-view__label">{{ T.CONFIRM_PASSWORD }}</label>
          <input
            v-model="form.confirmPassword"
            class="login-view__input"
            :class="{ 'login-view__input--error': errors.confirmPassword }"
            type="password"
            :placeholder="T.CONFIRM_PASSWORD_PLACEHOLDER"
            @blur="validateConfirmPassword"
          />
          <span v-if="errors.confirmPassword" class="login-view__error">{{ errors.confirmPassword }}</span>
        </div>

        <div v-if="isRegister" class="login-view__field">
          <label class="login-view__label">{{ T.PHONE }}</label>
          <input
            v-model="form.phone"
            class="login-view__input"
            type="tel"
            :placeholder="T.PHONE_PLACEHOLDER"
          />
        </div>

        <button
          class="login-view__submit"
          :disabled="loading || !isFormValid"
          @click="handleSubmit"
        >
          {{ loading ? T.SUBMITTING : (isRegister ? T.REGISTER_BTN : T.LOGIN_BTN) }}
        </button>

        <p class="login-view__toggle">
          {{ isRegister ? T.HAS_ACCOUNT : T.NO_ACCOUNT }}
          <span class="login-view__toggle-link" @click="toggleMode">
            {{ isRegister ? T.GO_LOGIN : T.GO_REGISTER }}
          </span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { LOGIN_VIEW_TEXT as T } from './Text'
import { useLoginView } from './useLoginView'

const {
  form,
  errors,
  isRegister,
  loading,
  isFormValid,
  validateUsername,
  validatePassword,
  validateConfirmPassword,
  handleSubmit,
  toggleMode
} = useLoginView()
</script>

<style scoped>
@import './LoginView.css';
</style>
