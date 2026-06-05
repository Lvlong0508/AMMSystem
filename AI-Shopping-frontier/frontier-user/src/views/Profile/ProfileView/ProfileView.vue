<template>
  <div class="profile-view">
    <div class="profile-view__header">
      <button class="profile-view__back" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        <span>{{ T.BACK }}</span>
      </button>
      <h1 class="profile-view__title">{{ T.TITLE }}</h1>
    </div>

    <div class="profile-view__body">
      <div class="profile-view__avatar-section">
        <div class="profile-view__avatar">
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="8" r="4" />
            <path d="M20 21a8 8 0 1 0-16 0" />
          </svg>
        </div>
      </div>

      <div v-if="editing" class="profile-view__card">
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.NICKNAME }}</span>
          <input v-model="formNickname" class="profile-view__input" maxlength="20" />
        </div>
        <div class="profile-view__divider"></div>
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.PHONE }}</span>
          <input v-model="formPhone" class="profile-view__input" maxlength="11" type="tel" />
        </div>
        <div class="profile-view__divider"></div>
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.EMAIL }}</span>
          <input v-model="formEmail" class="profile-view__input" maxlength="50" />
        </div>
      </div>

      <div v-else class="profile-view__card">
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.USERNAME }}</span>
          <span class="profile-view__value">{{ profile.username || '-' }}</span>
        </div>
        <div class="profile-view__divider"></div>
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.NICKNAME }}</span>
          <span class="profile-view__value">{{ profile.nickname || '-' }}</span>
        </div>
        <div class="profile-view__divider"></div>
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.PHONE }}</span>
          <span class="profile-view__value">{{ profile.phone || '-' }}</span>
        </div>
        <div class="profile-view__divider"></div>
        <div class="profile-view__row">
          <span class="profile-view__label">{{ T.EMAIL }}</span>
          <span class="profile-view__value">{{ profile.email || '-' }}</span>
        </div>
      </div>

      <button v-if="!editing" class="profile-view__edit-btn" @click="startEdit">{{ T.EDIT }}</button>
      <div v-else class="profile-view__edit-actions">
        <button class="profile-view__btn profile-view__btn--cancel" @click="cancelEdit">{{ T.CANCEL }}</button>
        <button class="profile-view__btn profile-view__btn--save" :disabled="saving" @click="saveProfile">{{ saving ? T.SAVING : T.SAVE }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { PROFILE_VIEW_TEXT as T } from './Text'
import { useProfileView } from './useProfileView'

const {
  profile,
  editing,
  saving,
  formNickname,
  formPhone,
  formEmail,
  startEdit,
  cancelEdit,
  saveProfile,
  goBack
} = useProfileView()
</script>

<style scoped>
@import './ProfileView.css';
</style>
