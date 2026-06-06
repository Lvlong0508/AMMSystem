<template>
  <div class="shop-register">
    <div class="shop-register__header">
      <h2 class="shop-register__title">{{ T.PAGE_TITLE }}</h2>
      <el-button text @click="$router.push('/shop/list')">{{ T.BTN_BACK }}</el-button>
    </div>

    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
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
            :rows="4"
            :placeholder="T.PLACEHOLDER_DESC"
            :maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting">
            {{ submitting ? T.BTN_SUBMITTING : T.BTN_SUBMIT }}
          </el-button>
          <el-button @click="$router.push('/shop/list')">{{ T.BTN_CANCEL }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { useShopRegister } from './ShopRegister.js'

const { T, formRef, form, submitting, rules, handleSubmit } = useShopRegister()
</script>

<style scoped src="./ShopRegister.css"></style>
