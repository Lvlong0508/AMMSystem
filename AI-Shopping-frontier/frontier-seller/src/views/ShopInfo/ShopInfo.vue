<template>
  <div class="shop-info">
    <div class="shop-info__toolbar">
      <h2 class="shop-info__title">{{ T.PAGE_TITLE }}</h2>
      <el-tag v-if="shopStatus === 1" type="success">{{ T.STATUS_OPENED }}</el-tag>
      <el-tag v-else-if="shopStatus === 0" type="danger">{{ T.STATUS_CLOSED }}</el-tag>
    </div>
    <el-card v-loading="loading" shadow="never">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :placeholder="T.PLACEHOLDER_NAME" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="T.PLACEHOLDER_DESC" :maxlength="500" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE">
          <el-input v-model="form.phone" :placeholder="T.PLACEHOLDER_PHONE" :maxlength="20" />
        </el-form-item>
        <el-form-item :label="T.LABEL_REGION">
          <el-cascader
            v-model="form.region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover' }"
            :placeholder="T.PLACEHOLDER_REGION"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_ADDRESS">
          <el-input v-model="form.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS" :maxlength="200" />
        </el-form-item>
        <el-form-item :label="T.LABEL_LOGO">
          <div class="shop-info__logo-upload">
            <el-image
              v-if="logoPreview"
              :src="logoPreview"
              class="shop-info__logo-preview"
              fit="cover"
            />
            <input ref="logoInputRef" type="file" accept=".jpg,.png" @change="handleLogoChange" />
            <span v-if="logoFile" class="shop-info__logo-name">{{ logoFile.name }}</span>
            <el-button v-if="logoFile" type="danger" link @click="clearLogo">{{ T.BTN_CLEAR_LOGO }}</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            {{ saving ? T.BTN_SAVING : T.BTN_SAVE }}
          </el-button>
          <el-button :type="shopStatus === 1 ? 'danger' : 'success'" :loading="toggling" @click="handleToggleStatus">
            {{ shopStatus === 1 ? T.BTN_CLOSE : T.BTN_OPEN }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
<script setup>
import { useShopInfo } from './ShopInfo.js'
import { regionData } from 'element-china-area-data'

const regionOptions = regionData

const { T, form, loading, saving, toggling, shopStatus, logoPreview, logoFile, logoInputRef, handleLogoChange, clearLogo, handleSave, handleToggleStatus } = useShopInfo()
</script>
<style scoped src="./ShopInfo.css"></style>