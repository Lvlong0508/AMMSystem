<template>
  <div class="shop-products">
    <div class="shop-products__toolbar">
      <h2 class="shop-products__title">{{ T.PAGE_TITLE }} <span v-if="shopInfo">- {{ shopInfo.name }}</span></h2>
      <div>
        <el-button @click="loadProducts">{{ T.BTN_REFRESH }}</el-button>
        <el-button type="primary" @click="showAddDialog">{{ T.BTN_ADD }}</el-button>
      </div>
    </div>

    <div class="shop-products__filters">
      <el-input v-model="searchKeyword" :placeholder="T.SEARCH_PLACEHOLDER" style="width: 280px" clearable />
    </div>

    <div v-loading="loading" class="product-grid">
      <ProductCard
        v-for="product in filteredProducts"
        :key="product.id || product.productId"
        :product="product"
        variant="abstract"
        @click="showDetail"
      />
    </div>

    <el-empty v-if="!loading && filteredProducts.length === 0" :description="T.EMPTY_TEXT" />

    <el-dialog v-model="detailVisible" :title="T.DIALOG_DETAIL" width="680px" destroy-on-close>
      <ProductCard
        v-if="selectedProduct"
        :product="selectedProduct"
        variant="detail"
        @edit="handleEditFromDetail"
        @toggle-sale="handleToggleSaleFromDetail"
        @delete="handleDeleteFromDetail"
      />
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="isEdit ? T.DIALOG_EDIT : T.DIALOG_ADD" width="600px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item :label="T.LABEL_NAME">
          <el-input v-model="form.name" :maxlength="100" />
        </el-form-item>
        <el-form-item :label="T.LABEL_DESC">
          <el-input v-model="form.description" type="textarea" :rows="3" :maxlength="500" />
        </el-form-item>
        <el-form-item :label="T.LABEL_TAGS">
          <el-cascader
            v-model="form.tags"
            :options="TAG_LIBRARY"
            :props="{ expandTrigger: 'hover', emitPath: false }"
            placeholder="选择商品分类标签"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
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
          <input type="file" accept=".jpg,.png" @change="handleFileChange" />
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
import ProductCard from '@/components/ProductCard/ProductCard.vue'
import { useShopProducts } from './ShopProducts.js'
const { TAG_LIBRARY, T, shopInfo, products, loading, searchKeyword, filteredProducts, detailVisible, selectedProduct, dialogVisible, isEdit, submitting, form, showAddDialog, showEditDialog, closeDialog, handleFileChange, handleSubmit, handleToggleSale, handleDelete, loadProducts, showDetail, closeDetail, handleEditFromDetail, handleToggleSaleFromDetail, handleDeleteFromDetail } = useShopProducts()
</script>

<style scoped src="./ShopProducts.css"></style>
