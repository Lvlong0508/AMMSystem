<!-- src/components/Contact/ContactManager.vue -->
<template>
  <div class="contact-manager">
    <!-- 头部工具栏 -->
    <div class="toolbar">
      <h2 class="title">{{ T.PAGE_TITLE }}</h2>
      <div class="actions">
        <div class="search-box">
          <input
            type="text"
            v-model="searchKeyword"
            :placeholder="T.SEARCH_PLACEHOLDER"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="btn-search" @click="handleSearch">{{ T.BTN_SEARCH }}</button>
          <button class="btn-clear" v-if="searchKeyword" @click="clearSearch">{{ T.BTN_CLEAR }}</button>
        </div>
        <button class="btn-add" @click="openAddDialog">{{ T.BTN_ADD }}</button>
      </div>
    </div>

    <!-- 地址信息列表 -->
    <div class="contact-list" v-if="contacts.length > 0">
      <div
        v-for="contact in contacts"
        :key="contact.id"
        class="contact-card"
        :class="{ 'selected': selectedContact?.id === contact.id }"
        @click="selectContact(contact)"
      >
        <div class="contact-header">
          <span class="contact-icon">{{ T.ICON_USER }}</span>
          <span class="contact-name">{{ contact.name }}</span>
        </div>
        <div class="contact-info">
          <div class="info-item">
            <span class="label">{{ T.LABEL_PHONE }}</span>
            <span class="value">{{ contact.phone }}</span>
          </div>
          <div class="info-item">
            <span class="label">{{ T.LABEL_ADDRESS }}</span>
            <span class="value">{{ contact.address }}</span>
          </div>
          <div class="info-item time" v-if="contact.createdAt">
            <span class="label">{{ T.LABEL_CREATE_TIME }}</span>
            <span class="value">{{ formatTime(contact.createdAt) }}</span>
          </div>
        </div>
        <div class="contact-actions">
          <button class="btn-edit" @click.stop="openEditDialog(contact)">{{ T.BTN_EDIT }}</button>
          <button class="btn-delete" @click.stop="handleDelete(contact)">{{ T.BTN_DELETE }}</button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div class="empty-state" v-else>
      <div class="empty-icon">{{ T.EMPTY_ICON }}</div>
      <p class="empty-text">{{ T.EMPTY_TEXT }}</p>
      <button class="btn-add-empty" @click="openAddDialog">{{ T.BTN_ADD_FIRST }}</button>
    </div>

    <!-- 编辑/新增弹窗 -->
    <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <div class="dialog-header">
          <h3 class="dialog-title">{{ isEdit ? T.DIALOG_TITLE_EDIT : T.DIALOG_TITLE_ADD }}</h3>
          <button class="close-btn" @click="closeDialog">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>{{ T.LABEL_NAME }} <span class="required">{{ T.REQUIRED_MARK }}</span></label>
            <input
              type="text"
              v-model="form.name"
              :placeholder="T.PLACEHOLDER_NAME"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>{{ T.LABEL_PHONE_INPUT }} <span class="required">{{ T.REQUIRED_MARK }}</span></label>
            <input
              type="tel"
              v-model="form.phone"
              :placeholder="T.PLACEHOLDER_PHONE"
              class="form-input"
            />
          </div>
          <div class="form-group">
            <label>{{ T.LABEL_ADDRESS_INPUT }} <span class="required">{{ T.REQUIRED_MARK }}</span></label>
            <textarea
              v-model="form.address"
              :placeholder="T.PLACEHOLDER_ADDRESS"
              class="form-input textarea"
              rows="3"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="closeDialog">{{ T.BTN_CANCEL }}</button>
          <button
            class="btn-submit"
            :disabled="!isFormValid || submitting"
            @click="handleSubmit"
          >
            {{ submitting ? T.BTN_SAVING : (isEdit ? T.BTN_SAVE : T.BTN_CREATE) }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="deleteDialogVisible" class="dialog-overlay" @click="closeDeleteDialog">
      <div class="dialog confirm-dialog" @click.stop>
        <div class="dialog-header warning">
          <h3 class="dialog-title">{{ T.DIALOG_TITLE_DELETE }}</h3>
        </div>
        <div class="dialog-body">
          <p class="confirm-text">
            {{ T.DELETE_CONFIRM_TEXT }} <strong>{{ contactToDelete?.name }}</strong> {{ T.DELETE_CONFIRM_SUFFIX }}<br/>
            {{ T.DELETE_WARNING }}
          </p>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="closeDeleteDialog">{{ T.BTN_CANCEL }}</button>
          <button
            class="btn-delete-confirm"
            :disabled="deleting"
            @click="confirmDelete"
          >
            {{ deleting ? T.BTN_DELETING : T.BTN_CONFIRM_DELETE }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useContactLogic } from './useContactLogic.js'
import { CONTACT_TEXT as T } from './Text.js'

const {
  contacts,
  selectedContact,
  searchKeyword,
  dialogVisible,
  deleteDialogVisible,
  isEdit,
  submitting,
  deleting,
  contactToDelete,
  form,
  isFormValid,
  formatTime,
  handleSearch,
  clearSearch,
  selectContact,
  openAddDialog,
  openEditDialog,
  closeDialog,
  handleSubmit,
  handleDelete,
  closeDeleteDialog,
  confirmDelete
} = useContactLogic()
</script>

<style scoped>
@import './ContactManager.css';
</style>
