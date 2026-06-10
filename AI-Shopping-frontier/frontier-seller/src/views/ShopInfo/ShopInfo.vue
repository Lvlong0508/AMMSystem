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

    <el-card v-loading="loading" shadow="never">
      <div v-if="logoPreview" class="shop-info__logo">
        <el-avatar :size="100" shape="circle" class="shop-info__logo-avatar">
          <el-image :src="logoPreview" fit="cover" style="width: 100%; height: 100%" />
        </el-avatar>
      </div>

      <el-descriptions :column="1" border>
        <el-descriptions-item :label="T.LABEL_NAME">
          {{ form.name || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_DESC">
          {{ form.description || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_PHONE">
          {{ form.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_REGION">
          {{ form.region.length ? form.region.join(' / ') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="T.LABEL_ADDRESS">
          {{ form.addressDetail || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="T.DIALOG_TITLE" width="600px">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="editForm.name" :placeholder="T.PLACEHOLDER_NAME" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="editForm.description" type="textarea" :rows="3" :placeholder="T.PLACEHOLDER_DESC" :maxlength="500" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE">
          <el-input v-model="editForm.phone" :placeholder="T.PLACEHOLDER_PHONE" :maxlength="20" />
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
        <el-form-item :label="T.LABEL_ADDRESS">
          <el-input v-model="editForm.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS" :maxlength="200" />
        </el-form-item>
        <el-form-item :label="T.LABEL_LOGO">
          <div class="shop-info__edit-logo">
            <el-image
              v-if="logoPreview && !editLogoFile"
              :src="logoPreview"
              class="shop-info__edit-logo-preview"
              fit="cover"
            />
            <input ref="editLogoInputRef" type="file" accept=".jpg,.png" @change="handleEditLogoChange" />
            <span v-if="editLogoFile" class="shop-info__edit-logo-name">{{ editLogoFile.name }}</span>
            <el-button v-if="editLogoFile" type="danger" link @click="clearEditLogo">{{ T.BTN_CLEAR_LOGO }}</el-button>
          </div>
        </el-form-item>
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
  T, form, loading, saving, toggling, shopStatus, logoPreview,
  dialogVisible, editForm, editLogoFile, editLogoInputRef,
  openEditDialog, handleEditSave, handleEditLogoChange, clearEditLogo, handleToggleStatus
} = useShopInfo()
</script>

<style scoped src="./ShopInfo.css"></style>
