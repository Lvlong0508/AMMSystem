<template>
  <div class="shop-employees">
    <div class="shop-employees__toolbar">
      <h2 class="shop-employees__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <div>
        <el-button @click="loadEmployees">{{ T.BTN_REFRESH }}</el-button>
        <el-button type="primary" @click="showAddDialog">{{ T.BTN_ADD }}</el-button>
      </div>
    </div>

    <div v-loading="loading" class="employee-grid">
      <el-card v-for="emp in employees" :key="emp.merchantId" shadow="hover" class="employee-card">
        <div class="employee-card__header">
          <el-avatar :size="48">{{ getAvatarText(emp.name || emp.username) }}</el-avatar>
          <div class="employee-card__info">
            <span class="employee-card__name">{{ emp.name || emp.username }}</span>
            <el-tag size="small" class="employee-card__role">{{ getRoleText(emp.role) }}</el-tag>
          </div>
        </div>
        <div class="employee-card__body">
          <div class="employee-card__detail-row">
            <span class="employee-card__label">{{ T.LABEL_USERNAME }}</span>
            <span>{{ emp.username }}</span>
          </div>
          <div class="employee-card__detail-row">
            <span class="employee-card__label">{{ T.LABEL_PHONE }}</span>
            <span>{{ emp.phone || '-' }}</span>
          </div>
        </div>
        <div class="employee-card__footer">
          <el-button text type="danger" size="small" @click="handleRemove(emp)">{{ T.BTN_REMOVE }}</el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && employees.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="dialogVisible" :title="T.DIALOG_ADD" width="480px">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :maxlength="50" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PHONE">
          <el-input v-model="form.phone" :maxlength="20" />
        </el-form-item>
        <el-form-item :label="T.LABEL_USERNAME">
          <el-input v-model="form.username" :maxlength="50" />
        </el-form-item>
        <el-form-item :label="T.LABEL_PASSWORD">
          <el-input v-model="form.password" type="password" :maxlength="50" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? T.BTN_SUBMITTING : T.BTN_SUBMIT }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShopEmployees } from './ShopEmployees.js'
const props = useShopEmployees()
const { T, shopInfo, employees, loading, dialogVisible, submitting, form, getAvatarText, getRoleText, showAddDialog, closeDialog, handleSubmit, handleRemove, loadEmployees } = props
</script>

<style scoped src="./ShopEmployees.css"></style>
