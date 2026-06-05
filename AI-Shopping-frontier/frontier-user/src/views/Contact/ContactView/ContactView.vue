<template>
  <div class="contact-view">
    <div v-if="loading" class="contact-view__loading">
      <div v-for="i in 2" :key="i" class="contact-view__skeleton"></div>
    </div>

    <div v-else-if="contacts.length === 0" class="contact-view__empty">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="contact-view__empty-icon"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
      <p class="contact-view__empty-text">{{ T.EMPTY_TEXT }}</p>
    </div>

    <div v-else class="contact-view__list">
      <div
        v-for="contact in contacts"
        :key="contact.id"
        class="contact-view__card"
      >
        <div class="contact-view__card-header">
          <div class="contact-view__name-row">
            <span class="contact-view__name">{{ contact.name }}</span>
            <span class="contact-view__phone">{{ contact.phone }}</span>
          </div>
          <span v-if="contact.isDefault === 1 || contact.isDefault === true" class="contact-view__default-badge">{{ T.DEFAULT }}</span>
        </div>
        <p class="contact-view__address">{{ contact.address }}</p>
        <div class="contact-view__divider"></div>
        <div class="contact-view__actions">
          <button class="contact-view__action-btn contact-view__action-btn--edit" @click="handleEdit(contact)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
            {{ T.EDIT }}
          </button>
          <button v-if="contact.isDefault !== 1 && contact.isDefault !== true" class="contact-view__action-btn contact-view__action-btn--default" @click="handleSetDefault(contact)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            {{ T.SET_DEFAULT }}
          </button>
          <button class="contact-view__action-btn contact-view__action-btn--delete" @click="handleDeleteClick(contact)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            {{ T.DELETE }}
          </button>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showDeleteSheet" class="action-sheet-overlay" @click="showDeleteSheet = false">
        <div class="action-sheet" @click.stop>
          <div class="action-sheet__content">
            <h3 class="action-sheet__title">{{ T.DELETE_TITLE }}</h3>
            <p class="action-sheet__desc">{{ T.DELETE_DESC }}</p>
          </div>
          <div class="action-sheet__actions">
            <button class="action-sheet__btn action-sheet__btn--danger" @click="confirmDelete">{{ T.DELETE_CONFIRM }}</button>
            <button class="action-sheet__btn action-sheet__btn--cancel" @click="showDeleteSheet = false">{{ T.RETHINK }}</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { CONTACT_VIEW_TEXT as T } from './Text'
import { useContactView } from './useContactView'

const {
  contacts,
  loading,
  showDeleteSheet,
  handleEdit,
  handleSetDefault,
  handleDeleteClick,
  confirmDelete
} = useContactView()
</script>

<style scoped>
@import './ContactView.css';
</style>
