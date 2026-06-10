<template>
  <div class="shop-info">
    <div class="shop-info__header">
      <h2 class="shop-info__title">{{ T.PAGE_TITLE }}</h2>
      <el-switch
        :model-value="shopStatus === 1"
        :loading="toggling"
        :disabled="toggling"
        inline-prompt
        active-text="营业中"
        inactive-text="已关闭"
        @change="handleToggleStatus"
      />
      <el-button class="shop-info__edit-btn" type="primary" @click="openEditDialog">
        {{ T.BTN_EDIT }}
      </el-button>
    </div>

    <el-card v-loading="loading" class="shop-info__card" shadow="never">
      <div class="shop-info__hero">
        <div class="shop-info__logo-panel">
          <el-avatar :size="180" shape="circle" class="shop-info__logo-avatar">
            <el-image v-if="logoPreview" :src="logoPreview" fit="cover" style="width: 100%; height: 100%" />
            <span v-else>{{ (form.name || T.PAGE_TITLE).slice(0, 1) }}</span>
          </el-avatar>
        </div>
        <div class="shop-info__summary">
          <div class="shop-info__summary-head">
            <p class="shop-info__eyebrow">STORE PROFILE</p>
            <h3 class="shop-info__name">{{ form.name || '-' }}</h3>
          </div>
          <el-divider />
          <div class="shop-info__description-block">
            <span class="shop-info__field-label">{{ T.LABEL_DESC }}</span>
            <p class="shop-info__description">{{ form.description || '-' }}</p>
          </div>
        </div>
      </div>

      <el-divider class="shop-info__section-divider" />

      <div class="shop-info__details">
        <div class="shop-info__detail-row">
          <span class="shop-info__detail-label">{{ T.LABEL_PHONE }}</span>
          <span class="shop-info__field-value">{{ form.phone || '-' }}</span>
        </div>
        <div class="shop-info__detail-row">
          <span class="shop-info__detail-label">{{ T.LABEL_ADDRESS }}</span>
          <span class="shop-info__field-value shop-info__field-value--address">{{ displayAddress }}</span>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="T.DIALOG_TITLE" width="min(920px, calc(100vw - 32px))" class="shop-info__dialog">
      <el-form ref="editFormRef" class="shop-info__edit-form" :model="editForm" :rules="editRules" label-position="top">
        <div class="shop-info__edit-column shop-info__edit-column--primary">
          <el-form-item :label="T.LABEL_LOGO">
            <div class="shop-info__edit-logo">
              <el-image
                v-if="editLogoPreview || logoPreview"
                :src="editLogoPreview || logoPreview"
                class="shop-info__edit-logo-preview"
                fit="cover"
              />
              <input ref="editLogoInputRef" type="file" accept=".jpg,.png" @change="handleEditLogoChange" />
              <span v-if="editLogoFile" class="shop-info__edit-logo-name">{{ editLogoFile.name }}</span>
              <el-button v-if="editLogoFile" type="danger" link @click="clearEditLogo">{{ T.BTN_CLEAR_LOGO }}</el-button>
            </div>
          </el-form-item>
          <el-form-item :label="T.LABEL_NAME" prop="name">
            <el-input v-model="editForm.name" :placeholder="T.PLACEHOLDER_NAME" :maxlength="20" show-word-limit />
          </el-form-item>
          <el-form-item :label="T.LABEL_DESC" prop="description">
            <el-input v-model="editForm.description" type="textarea" :rows="6" :placeholder="T.PLACEHOLDER_DESC" :maxlength="500" show-word-limit />
          </el-form-item>
        </div>
        <div class="shop-info__edit-column">
          <el-form-item :label="T.LABEL_PHONE" prop="phone">
            <el-input v-model="editForm.phone" :placeholder="T.PLACEHOLDER_PHONE" :maxlength="11" />
          </el-form-item>
          <el-form-item :label="T.LABEL_REGION">
            <el-cascader
              v-model="editForm.region"
              :options="regionOptions"
              :props="{ expandTrigger: 'hover' }"
              :placeholder="T.PLACEHOLDER_REGION"
              clearable
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="T.LABEL_ADDRESS" prop="addressDetail">
            <el-input v-model="editForm.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS" :maxlength="200" show-word-limit />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleEditSave">
          {{ saving ? T.BTN_SAVING : T.BTN_SAVE }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShopInfo } from './ShopInfo.js'
import { regionData } from 'element-china-area-data'

const regionOptions = regionData

const {
  T, form, displayAddress, loading, saving, toggling, shopStatus, logoPreview,
  dialogVisible, editFormRef, editForm, editRules, editLogoFile, editLogoPreview, editLogoInputRef,
  openEditDialog, handleEditSave, handleEditLogoChange, clearEditLogo, handleToggleStatus
} = useShopInfo()
</script>

<style scoped src="./ShopInfo.css"></style>
