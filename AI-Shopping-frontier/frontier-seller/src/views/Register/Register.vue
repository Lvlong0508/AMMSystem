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

      <!-- 步骤条 -->
      <el-steps :active="currentStep" align-center finish-status="success" class="register-steps">
        <el-step :title="T.STEP_ACCOUNT" />
        <el-step :title="T.STEP_SHOP" />
        <el-step :title="T.STEP_DONE" />
      </el-steps>

      <!-- 步骤 0: 账号信息 -->
      <el-form
        v-show="currentStep === 0"
        ref="accountFormRef"
        :model="accountForm"
        :rules="accountRules"
        label-position="top"
        size="large"
        @submit.prevent="handleNext"
      >
        <el-form-item :label="T.LABEL_USERNAME" prop="username">
          <el-input v-model="accountForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE" prop="phone">
          <el-input v-model="accountForm.phone" autocomplete="tel" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PASSWORD" prop="password">
          <el-input v-model="accountForm.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item :label="T.LABEL_CONFIRM" prop="confirmPassword">
          <el-input v-model="accountForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="submittingAccount" class="register-btn" round size="large">
            {{ submittingAccount ? T.BTN_LOADING : T.BTN_NEXT }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤 1: 店铺信息 -->
      <el-form
        v-show="currentStep === 1"
        ref="shopFormRef"
        :model="shopForm"
        :rules="shopRules"
        label-position="top"
        size="large"
        @submit.prevent="handleSubmitShop"
      >
        <el-alert
          :title="T.REGISTERED_ACCOUNT"
          :description="registeredUsername"
          type="success"
          :closable="false"
          show-icon
          class="register-account-alert"
        />

        <el-form-item :label="T.LABEL_SHOP_NAME" prop="name">
          <el-input
            v-model="shopForm.name"
            :placeholder="T.PLACEHOLDER_SHOP_NAME"
            :maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_SHOP_DESC" prop="description">
          <el-input
            v-model="shopForm.description"
            type="textarea"
            :rows="4"
            :placeholder="T.PLACEHOLDER_SHOP_DESC"
            :maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_SHOP_REGION" prop="region">
          <el-cascader
            v-model="shopForm.region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover' }"
            :placeholder="T.PLACEHOLDER_SHOP_REGION"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_SHOP_ADDRESS" prop="addressDetail">
          <el-input
            v-model="shopForm.addressDetail"
            :placeholder="T.PLACEHOLDER_SHOP_ADDRESS_DETAIL"
            :maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_SHOP_PHONE" prop="phone">
          <el-input
            v-model="shopForm.phone"
            :placeholder="T.PLACEHOLDER_SHOP_PHONE"
            :maxlength="20"
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_SHOP_LOGO">
          <div class="register-logo-upload">
            <input ref="logoInputRef" type="file" accept="image/jpeg,image/png" @change="handleLogoChange" />
            <span v-if="logoFile" class="register-logo-upload__name">{{ logoFile.name }}</span>
            <el-button v-if="logoFile" type="danger" link @click="clearLogo">{{ T.BTN_CLEAR_LOGO }}</el-button>
          </div>
        </el-form-item>
        <el-form-item class="register-form-actions">
          <el-button type="primary" native-type="submit" :loading="submittingShop" class="register-btn" round size="large">
            {{ submittingShop ? T.BTN_SUBMITTING : T.BTN_SUBMIT }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤 2: 完成 -->
      <div v-show="currentStep === 2" class="register-done">
        <el-result icon="success" :title="T.SUCCESS_SHOP" />
      </div>

      <div class="register-footer">
        <router-link to="/login" class="register-footer__link">{{ T.LINK_LOGIN }}</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRegister } from './Register.js'
import { regionData } from 'element-china-area-data'

const regionOptions = regionData

const {
  T, currentStep,
  accountFormRef, accountForm, accountRules,
  shopFormRef, shopForm, shopRules,
  logoInputRef, logoFile,
  submittingAccount, submittingShop, registeredUsername,
  handleNext, handleLogoChange, clearLogo, handleSubmitShop
} = useRegister()
</script>

<style scoped src="./Register.css"></style>
