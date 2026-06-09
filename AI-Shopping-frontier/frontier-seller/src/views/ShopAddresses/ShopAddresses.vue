<template>
  <div class="shop-addresses">    <div class="shop-addresses__toolbar">      <h2 class="shop-addresses__title">{{ T.PAGE_TITLE }}</h2>      <div>        <el-button @click="loadAddresses">{{ T.BTN_REFRESH }}</el-button>        <el-button type="primary" @click="showAddDialog">{{ T.BTN_ADD }}</el-button>      </div>    </div>    <el-tabs v-model="activeTab" class="shop-addresses__tabs">      <el-tab-pane :label="`${T.TAB_SHIPPING} (${shippingAddresses.length})`" :name="1" />      <el-tab-pane :label="`${T.TAB_RETURN} (${returnAddresses.length})`" :name="2" />    </el-tabs>    <el-card shadow="never">      <el-table :data="currentAddresses" v-loading="loading" stripe border style="width: 100%">        <el-table-column :label="T.LABEL_TYPE" width="80">          <template #default="{ row }">            <el-tag :type="row.addressType === 1 ? 'primary' : 'warning'">              {{ row.addressType === 1 ? T.TYPE_SHIPPING : T.TYPE_RETURN }}            </el-tag>          </template>        </el-table-column>        <el-table-column prop="name" :label="T.LABEL_NAME" min-width="120" />        <el-table-column prop="phone" :label="T.LABEL_PHONE" min-width="130" />        <el-table-column prop="address" :label="T.LABEL_ADDRESS" min-width="250" />        <el-table-column :label="T.BADGE_DEFAULT" width="80">          <template #default="{ row }">            <el-tag v-if="row.isDefault === 1" type="success">{{ T.BADGE_DEFAULT }}</el-tag>          </template>        </el-table-column>        <el-table-column :label="T.LABEL_ACTION" width="180" fixed="right">          <template #default="{ row }">            <el-button v-if="row.isDefault !== 1" text @click="handleSetDefault(row)">{{ T.BTN_SET_DEFAULT }}</el-button>            <el-button text type="primary" @click="showEditDialog(row)">{{ T.BTN_EDIT }}</el-button>            <el-button text type="danger" @click="handleDelete(row)">{{ T.BTN_DELETE }}</el-button>          </template>        </el-table-column>      </el-table>      <el-empty v-if="!loading && currentAddresses.length === 0" :description="activeTab === 1 ? T.EMPTY_SHIPPING : T.EMPTY_RETURN" />    </el-card>    <el-dialog v-model="dialogVisible" :title="isEdit ? T.DIALOG_EDIT : T.DIALOG_ADD" width="520px">      <el-form label-position="top">        <el-form-item :label="T.LABEL_TYPE">          <el-select v-model="form.addressType" style="width: 100%">            <el-option :label="T.TYPE_SHIPPING" :value="1" />            <el-option :label="T.TYPE_RETURN" :value="2" />          </el-select>        </el-form-item>        <el-form-item :label="T.LABEL_NAME">          <el-input v-model="form.name" :maxlength="50" />        </el-form-item>        <el-form-item :label="T.LABEL_PHONE">          <el-input v-model="form.phone" :maxlength="20" />        </el-form-item>                <el-form-item :label="T.LABEL_REGION">
          <el-cascader
            v-model="form.region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover' }"
            placeholder="请选择省/市/区"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="T.LABEL_ADDRESS">
          <el-input v-model="form.addressDetail" :placeholder="T.PLACEHOLDER_ADDRESS_DETAIL" :maxlength="200" show-word-limit />
        </el-form-item>        <el-form-item>          <el-checkbox v-model="form.isDefault" :true-value="1" :false-value="0">            {{ T.LABEL_DEFAULT }}          </el-checkbox>        </el-form-item>      </el-form>      <template #footer>        <el-button @click="closeDialog">{{ T.BTN_CANCEL }}</el-button>        <el-button type="primary" :loading="submitting" @click="handleSubmit">          {{ submitting ? T.BTN_SAVING : T.BTN_SAVE }}        </el-button>      </template>    </el-dialog>  </div>
</template>
<script setup>
import { useShopAddresses } from './ShopAddresses.js'
import { regionData } from 'element-china-area-data'

const regionOptions = regionData

const props = useShopAddresses()
const { T, addresses, loading, activeTab, dialogVisible, isEdit, submitting, form, shippingAddresses, returnAddresses, currentAddresses, loadAddresses, formatDate, showAddDialog, showEditDialog, closeDialog, handleSubmit, handleDelete, handleSetDefault } = props
</script>
<style scoped src="./ShopAddresses.css"></style>