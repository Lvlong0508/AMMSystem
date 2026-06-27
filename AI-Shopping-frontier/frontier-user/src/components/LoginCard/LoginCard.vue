<template>
  <div class="login-overlay">
    <div class="login-card">
      <div class="login-card__header">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="login-card__logo">
          <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
        </svg>
        <h2 class="login-card__title">{{ isRegister ? T.REGISTER_TITLE : T.LOGIN_TITLE }}</h2>
        <p class="login-card__subtitle">{{ isRegister ? T.REGISTER_SUB : T.LOGIN_SUB }}</p>
      </div>

      <div class="login-card__form">
        <div class="login-card__field">
          <label class="login-card__label">{{ T.USERNAME }}</label>
          <input v-model="form.username" class="login-card__input" :class="{ 'login-card__input--error': errors.username }" :placeholder="T.USERNAME_PLACEHOLDER" @blur="validateUsername" @keydown.enter="handleSubmit" />
          <span v-if="errors.username" class="login-card__error">{{ errors.username }}</span>
        </div>

        <div class="login-card__field">
          <label class="login-card__label">{{ T.PASSWORD }}</label>
          <input v-model="form.password" class="login-card__input" :class="{ 'login-card__input--error': errors.password }" type="password" :placeholder="T.PASSWORD_PLACEHOLDER" @blur="validatePassword" @keydown.enter="handleSubmit" />
          <span v-if="errors.password" class="login-card__error">{{ errors.password }}</span>
        </div>

        <div v-if="isRegister" class="login-card__field">
          <label class="login-card__label">{{ T.CONFIRM_PASSWORD }}</label>
          <input v-model="form.confirmPassword" class="login-card__input" :class="{ 'login-card__input--error': errors.confirmPassword }" type="password" :placeholder="T.CONFIRM_PASSWORD_PLACEHOLDER" @blur="validateConfirmPassword" @keydown.enter="handleSubmit" />
          <span v-if="errors.confirmPassword" class="login-card__error">{{ errors.confirmPassword }}</span>
        </div>

        <div v-if="isRegister" class="login-card__field">
          <label class="login-card__label">{{ T.PHONE }}</label>
          <input v-model="form.phone" class="login-card__input" :class="{ 'login-card__input--error': errors.phone }" type="tel" :placeholder="T.PHONE_PLACEHOLDER" @blur="validatePhone" @keydown.enter="handleSubmit" />
          <span v-if="errors.phone" class="login-card__error">{{ errors.phone }}</span>
        </div>

        <div v-if="isRegister" class="login-card__field">
          <label class="login-card__label">{{ T.EMAIL }}</label>
          <input v-model="form.email" class="login-card__input" type="email" :placeholder="T.EMAIL_PLACEHOLDER" @keydown.enter="handleSubmit" />
        </div>

        <button class="login-card__submit" :disabled="loading || !isFormValid" @click="handleSubmit">
          {{ loading ? T.SUBMITTING : (isRegister ? T.REGISTER_BTN : T.LOGIN_BTN) }}
        </button>

        <p class="login-card__toggle">
          {{ isRegister ? T.HAS_ACCOUNT : T.NO_ACCOUNT }}
          <span class="login-card__toggle-link" @click="toggleMode">{{ isRegister ? T.GO_LOGIN : T.GO_REGISTER }}</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { T } from './Text'
import { useLoginCard } from './useLoginCard'

const emit = defineEmits(['logged-in'])
const { form, errors, isRegister, loading, isFormValid, validateUsername, validatePassword, validateConfirmPassword, validatePhone, handleSubmit, toggleMode } = useLoginCard(emit)
</script>

<style scoped>
@import './LoginCard.css';
</style>
