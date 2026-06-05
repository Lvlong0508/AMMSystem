<template>
  <div class="profile-page">
    <div class="profile-page__header">
      <button class="profile-page__back" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        <span>返回</span>
      </button>
      <h1 class="profile-page__title">个人信息</h1>
    </div>

    <div class="profile-page__body">
      <div class="profile-page__avatar-section">
        <div class="profile-page__avatar">
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="8" r="4" />
            <path d="M20 21a8 8 0 1 0-16 0" />
          </svg>
        </div>
      </div>

      <div v-if="editing" class="profile-page__card">
        <div class="profile-page__row">
          <span class="profile-page__label">昵称</span>
          <input v-model="formNickname" class="profile-page__input" maxlength="20" />
        </div>
        <div class="profile-page__divider"></div>
        <div class="profile-page__row">
          <span class="profile-page__label">手机号</span>
          <input v-model="formPhone" class="profile-page__input" maxlength="11" type="tel" />
        </div>
        <div class="profile-page__divider"></div>
        <div class="profile-page__row">
          <span class="profile-page__label">邮箱</span>
          <input v-model="formEmail" class="profile-page__input" maxlength="50" />
        </div>
      </div>

      <div v-else class="profile-page__card">
        <div class="profile-page__row">
          <span class="profile-page__label">用户名</span>
          <span class="profile-page__value">{{ profile.username || '-' }}</span>
        </div>
        <div class="profile-page__divider"></div>
        <div class="profile-page__row">
          <span class="profile-page__label">昵称</span>
          <span class="profile-page__value">{{ profile.nickname || '-' }}</span>
        </div>
        <div class="profile-page__divider"></div>
        <div class="profile-page__row">
          <span class="profile-page__label">手机号</span>
          <span class="profile-page__value">{{ profile.phone || '-' }}</span>
        </div>
        <div class="profile-page__divider"></div>
        <div class="profile-page__row">
          <span class="profile-page__label">邮箱</span>
          <span class="profile-page__value">{{ profile.email || '-' }}</span>
        </div>
      </div>

      <button v-if="!editing" class="profile-page__edit-btn" @click="startEdit">编辑</button>
      <div v-else class="profile-page__edit-actions">
        <button class="profile-page__btn profile-page__btn--cancel" @click="cancelEdit">取消</button>
        <button class="profile-page__btn profile-page__btn--save" :disabled="saving" @click="saveProfile">{{ saving ? '保存中…' : '保存' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userGetProfile, userUpdateProfile } from '@/api/auth'
import { showSuccess, showError } from '@/utils/swal'

const router = useRouter()

const profile = ref({})
const editing = ref(false)
const saving = ref(false)
const formNickname = ref('')
const formPhone = ref('')
const formEmail = ref('')

const loadProfile = async () => {
  try {
    const res = await userGetProfile()
    profile.value = res
  } catch {
    // fallback to localStorage
    try {
      const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
      profile.value = local
    } catch {}
  }
}

const startEdit = () => {
  formNickname.value = profile.value.nickname || ''
  formPhone.value = profile.value.phone || ''
  formEmail.value = profile.value.email || ''
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
}

const saveProfile = async () => {
  const email = formEmail.value.trim()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    showError('邮箱格式不正确')
    return
  }
  const data = {
    nickname: formNickname.value,
    phone: formPhone.value,
    email
  }
  if (
    data.nickname === (profile.value.nickname || '') &&
    data.phone === (profile.value.phone || '') &&
    data.email === (profile.value.email || '')
  ) {
    editing.value = false
    return
  }
  saving.value = true
  try {
    const res = await userUpdateProfile(data)
    Object.assign(profile.value, res)
    showSuccess('个人信息已更新')
    editing.value = false
    const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (res.nickname) local.nickname = res.nickname
    if (res.phone) local.phone = res.phone
    if (res.email) local.email = res.email
    localStorage.setItem('userInfo', JSON.stringify(local))
  } catch {
    showError('更新失败')
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #F8FAFC;
  font-family: var(--font-body);
}

.profile-page__header {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #E4ECFC;
  flex-shrink: 0;
  position: relative;
}

.profile-page__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  background: none;
  font-size: 15px;
  color: #64748B;
  cursor: pointer;
  transition: color 200ms ease-out;
  font-family: var(--font-body);
}

.profile-page__back:hover {
  color: #2563EB;
}

.profile-page__title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  color: #0F172A;
  margin: 0;
}

.profile-page__body {
  flex: 1;
  padding: 32px 24px;
  overflow-y: auto;
}

.profile-page__avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.profile-page__avatar {
  width: 125px;
  height: 125px;
  border-radius: 50%;
  background: #F1F5FD;
  color: #2563EB;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}
.profile-page__avatar svg {
  width: 80px;
  height: 80px;
}

.profile-page__name {
  font-family: var(--font-heading);
  font-size: 22px;
  font-weight: 700;
  color: #0F172A;
  margin: 0;
}

.profile-page__card {
  background: #fff;
  border: 1px solid #E4ECFC;
  border-radius: 14px;
  overflow: hidden;
}

.profile-page__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
}

.profile-page__label {
  font-size: 15px;
  color: #64748B;
}

.profile-page__value {
  font-size: 15px;
  color: #0F172A;
  font-weight: 500;
}

.profile-page__divider {
  height: 1px;
  background: #E4ECFC;
  margin: 0 20px;
}

.profile-page__input {
  flex: 1;
  min-width: 0;
  text-align: right;
  padding: 6px 10px;
  border: 1.5px solid #E4ECFC;
  border-radius: 6px;
  font-size: 14px;
  color: #0F172A;
  background: #F8FAFC;
  outline: none;
  font-family: var(--font-body);
  transition: border-color 200ms ease-out, background 200ms ease-out;
  max-width: 200px;
}

.profile-page__input:focus {
  border-color: #2563EB;
  background: #fff;
}

.profile-page__edit-btn {
  display: block;
  width: 100%;
  margin-top: 20px;
  padding: 14px;
  border: none;
  border-radius: 8px;
  background: #2563EB;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  font-family: var(--font-body);
  transition: opacity 200ms ease-out;
}

.profile-page__edit-btn:hover {
  opacity: 0.9;
}

.profile-page__edit-btn:active {
  transform: scale(0.97);
}

.profile-page__edit-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.profile-page__btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  font-family: var(--font-body);
  transition: opacity 200ms ease-out;
}

.profile-page__btn:active {
  transform: scale(0.97);
}

.profile-page__btn--cancel {
  background: #F1F5FD;
  color: #64748B;
}

.profile-page__btn--cancel:hover {
  background: #E4ECFC;
}

.profile-page__btn--save {
  background: #2563EB;
  color: #fff;
}

.profile-page__btn--save:hover {
  opacity: 0.9;
}

.profile-page__btn--save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
</style>
