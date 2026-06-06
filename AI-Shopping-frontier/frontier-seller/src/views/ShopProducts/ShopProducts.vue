<template>
  <div class="shop-products">
    <div class="shop-products__toolbar">
      <h2 class="shop-products__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <div>
        <el-button size="small" @click="loadProducts">{{ T.BTN_REFRESH }}</el-button>
        <el-button size="small" type="primary" @click="showAddDialog">{{ T.BTN_ADD }}</el-button>
      </div>
    </div>

    <div class="shop-products__filters">
      <el-input v-model="searchKeyword" size="small" :placeholder="T.SEARCH_PLACEHOLDER" style="width: 200px" clearable />
    </div>

    <el-card shadow="never">
      <el-table :data="filteredProducts" v-loading="loading" stripe border size="small" style="width: 100%">
        <el-table-column prop="productId" label="ID" width="80" />
        <el-table-column prop="name" :label="T.LABEL_NAME" min-width="150" />
        <el-table-column :label="T.LABEL_PRICE" width="120">
          <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stock" :label="T.LABEL_STOCK" width="100" />
        <el-table-column :label="T.LABEL_DESC" min-width="200">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column :label="T.LABEL_IMAGE" width="100">
          <template #default="{ row }">
            <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width: 50px; height: 50px" fit="cover" />
            <span v-else style="color: #ccc">无</span>
          </template>
        </el-table-column>
        <el-table-column :label="'操作'" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showEditDialog(row)">{{ T.BTN_EDIT }}</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">{{ T.BTN_DELETE }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredProducts.length === 0" :description="T.EMPTY_TEXT" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? T.DIALOG_EDIT : T.DIALOG_ADD" width="520px">
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="500" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="T.LABEL_PRICE">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="T.LABEL_STOCK">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="T.LABEL_IMAGE">
          <input type="file" accept="image/jpeg,image/png" @change="handleFileChange" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ T.BTN_CANCEL }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? (isEdit ? T.BTN_SAVING : T.BTN_ADD_SUBMITTING) : (isEdit ? T.BTN_SAVE : T.BTN_ADD_SUBMIT) }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useShopProducts } from './ShopProducts.js'
const props = useShopProducts()
const { T, shopInfo, products, loading, searchKeyword, filteredProducts, dialogVisible, isEdit, submitting, form, showAddDialog, showEditDialog, closeDialog, handleFileChange, handleSubmit, handleDelete, loadProducts } = props
</script>

<style scoped src="./ShopProducts.css"></style>
