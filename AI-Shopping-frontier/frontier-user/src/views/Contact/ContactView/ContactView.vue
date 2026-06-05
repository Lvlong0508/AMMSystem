<template>
  <div class="contact-view">
    <div class="contact-view__header">
      <button class="contact-view__back" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        <span>返回</span>
      </button>
      <h1 class="contact-view__header-title">地址管理</h1>
      <button class="contact-view__add-btn" @click="openForm">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        <span>添加</span>
      </button>
    </div>

    <div class="contact-view__body">
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

    <Teleport to="body">
      <div v-if="showFormSheet" class="form-modal-overlay" @click="handleCloseForm">
        <div class="form-modal" @click.stop>
          <h2 class="form-modal__title">{{ T.ADD_TITLE }}</h2>
          <div class="form-modal__fields">
            <div class="form-modal__field">
              <label class="form-modal__label">{{ T.FIELD_NAME }}</label>
              <input ref="nameInput" v-model="formName" class="form-modal__input" :placeholder="T.PLACEHOLDER_NAME" maxlength="20" />
            </div>
            <div class="form-modal__field">
              <label class="form-modal__label">{{ T.FIELD_PHONE }}</label>
              <input v-model="formPhone" class="form-modal__input" :placeholder="T.PLACEHOLDER_PHONE" maxlength="11" type="tel" />
            </div>
            <div class="form-modal__field">
              <label class="form-modal__label">{{ T.REGION }}</label>
              <div class="form-modal__region-row">
                <div class="form-modal__picker" :class="{ 'form-modal__picker--open': activePicker === 'province', 'form-modal__picker--filled': selectedProvince }">
                  <div class="form-modal__picker-trigger" @click="togglePicker('province')">
                    <span class="form-modal__picker-text" :class="{ 'form-modal__picker-text--empty': !selectedProvince }">{{ selectedProvince ? getLabel(regionOptions, selectedProvince) : T.SELECT_PROVINCE }}</span>
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg>
                  </div>
                  <div v-if="activePicker === 'province'" class="form-modal__picker-dropdown">
                    <div class="form-modal__picker-search">
                      <input v-model="provinceSearch" class="form-modal__picker-search-input" placeholder="搜索省份" />
                    </div>
                    <div class="form-modal__picker-options">
                      <div v-for="p in filteredProvinces" :key="p.value" class="form-modal__picker-option" :class="{ 'form-modal__picker-option--active': selectedProvince === p.value }" @click="selectProvince(p.value)">{{ p.label }}</div>
                    </div>
                  </div>
                </div>
                <div class="form-modal__picker" :class="{ 'form-modal__picker--open': activePicker === 'city', 'form-modal__picker--filled': selectedCity, 'form-modal__picker--disabled': !selectedProvince }">
                  <div class="form-modal__picker-trigger" @click="selectedProvince && togglePicker('city')">
                    <span class="form-modal__picker-text" :class="{ 'form-modal__picker-text--empty': !selectedCity }">{{ selectedCity ? getLabel(cityOptions, selectedCity) : T.SELECT_CITY }}</span>
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg>
                  </div>
                  <div v-if="activePicker === 'city'" class="form-modal__picker-dropdown">
                    <div class="form-modal__picker-search">
                      <input v-model="citySearch" class="form-modal__picker-search-input" placeholder="搜索城市" />
                    </div>
                    <div class="form-modal__picker-options">
                      <div v-for="c in filteredCities" :key="c.value" class="form-modal__picker-option" :class="{ 'form-modal__picker-option--active': selectedCity === c.value }" @click="selectCity(c.value)">{{ c.label }}</div>
                    </div>
                  </div>
                </div>
                <div class="form-modal__picker" :class="{ 'form-modal__picker--open': activePicker === 'district', 'form-modal__picker--filled': selectedDistrict, 'form-modal__picker--disabled': !selectedCity }">
                  <div class="form-modal__picker-trigger" @click="selectedCity && togglePicker('district')">
                    <span class="form-modal__picker-text" :class="{ 'form-modal__picker-text--empty': !selectedDistrict }">{{ selectedDistrict ? getLabel(districtOptions, selectedDistrict) : T.SELECT_DISTRICT }}</span>
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg>
                  </div>
                  <div v-if="activePicker === 'district'" class="form-modal__picker-dropdown">
                    <div class="form-modal__picker-search">
                      <input v-model="districtSearch" class="form-modal__picker-search-input" placeholder="搜索区县" />
                    </div>
                    <div class="form-modal__picker-options">
                      <div v-for="d in filteredDistricts" :key="d.value" class="form-modal__picker-option" :class="{ 'form-modal__picker-option--active': selectedDistrict === d.value }" @click="selectDistrict(d.value)">{{ d.label }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="form-modal__field">
              <label class="form-modal__label">{{ T.FIELD_ADDR }}</label>
              <input v-model="formAddr" class="form-modal__input" :placeholder="T.PLACEHOLDER_ADDR" maxlength="100" :disabled="!selectedProvince || !selectedCity" />
            </div>
            <p v-if="formError" class="form-modal__error">{{ formError }}</p>
          </div>
          <div class="form-modal__actions">
            <button class="form-modal__btn form-modal__btn--cancel" @click="closeForm">{{ T.CANCEL }}</button>
            <button class="form-modal__btn form-modal__btn--submit" :disabled="formSubmitting" @click="handleRegionSubmit">
              {{ formSubmitting ? T.SUBMITTING : T.SUBMIT }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { CONTACT_VIEW_TEXT as T } from './Text'
import { useContactView } from './useContactView'
import { regionData } from 'element-china-area-data'
import { ref, computed, onMounted, onUnmounted } from 'vue'

import { useRouter } from 'vue-router'

const regionOptions = ref(regionData)

const selectedProvince = ref('')
const selectedCity = ref('')
const selectedDistrict = ref('')

const cityOptions = computed(() => {
  const p = regionOptions.value.find(r => r.value === selectedProvince.value)
  return p ? p.children : []
})

const districtOptions = computed(() => {
  const c = cityOptions.value.find(r => r.value === selectedCity.value)
  return c ? c.children : []
})

const onProvinceChange = () => {
  selectedCity.value = ''
  selectedDistrict.value = ''
}

const onCityChange = () => {
  selectedDistrict.value = ''
}

function getLabel(options, value) {
  const found = options.find(r => r.value === value)
  return found ? found.label : ''
}

const activePicker = ref(null)
const provinceSearch = ref('')
const citySearch = ref('')
const districtSearch = ref('')

const filteredProvinces = computed(() =>
  regionOptions.value.filter(p => p.label.includes(provinceSearch.value))
)

const filteredCities = computed(() =>
  cityOptions.value.filter(c => c.label.includes(citySearch.value))
)

const filteredDistricts = computed(() =>
  districtOptions.value.filter(d => d.label.includes(districtSearch.value))
)

function togglePicker(name) {
  if (activePicker.value === name) {
    activePicker.value = null
  } else {
    activePicker.value = name
    provinceSearch.value = ''
    citySearch.value = ''
    districtSearch.value = ''
  }
}

function selectProvince(value) {
  selectedProvince.value = value
  selectedCity.value = ''
  selectedDistrict.value = ''
  activePicker.value = null
  provinceSearch.value = ''
}

function selectCity(value) {
  selectedCity.value = value
  selectedDistrict.value = ''
  activePicker.value = null
  citySearch.value = ''
}

function selectDistrict(value) {
  selectedDistrict.value = value
  activePicker.value = null
  districtSearch.value = ''
}

function onPickerOverlayClick(e) {
  if (!e.target.closest('.form-modal__picker')) {
    activePicker.value = null
  }
}

onMounted(() => {
  document.addEventListener('click', onPickerOverlayClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onPickerOverlayClick)
})

const handleRegionSubmit = () => {
  if (!selectedProvince.value || !selectedCity.value) {
    formError.value = '请选择省市区'
    return
  }
  if (!formAddr.value.trim()) {
    formError.value = '请填写详细地址'
    return
  }
  const province = getLabel(regionOptions.value, selectedProvince.value)
  const city = getLabel(cityOptions.value, selectedCity.value)
  const district = getLabel(districtOptions.value, selectedDistrict.value)
  const regionStr = [province, city, district].filter(Boolean).join('')
  formAddr.value = regionStr + formAddr.value
  submitForm()
}

const openForm = () => {
  selectedProvince.value = ''
  selectedCity.value = ''
  selectedDistrict.value = ''
  activePicker.value = null
  provinceSearch.value = ''
  citySearch.value = ''
  districtSearch.value = ''
  handleAdd()
}

const handleCloseForm = () => {
  activePicker.value = null
  closeForm()
}

const router = useRouter()

const goBack = () => {
  router.back()
}

const {
  contacts,
  loading,
  showDeleteSheet,
  showFormSheet,
  formName,
  formPhone,
  formAddr,
  formError,
  formSubmitting,
  nameInput,
  handleEdit,
  handleSetDefault,
  handleDeleteClick,
  confirmDelete,
  handleAdd,
  closeForm,
  submitForm
} = useContactView()
</script>

<style scoped>
@import './ContactView.css';
</style>
