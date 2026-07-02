<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="rl-overlay" @click="$emit('close')">
        <Transition name="scale" appear>
          <div class="rl-modal" @click.stop>
            <div class="rl-modal__header">
              <span class="rl-modal__title">{{ T.TITLE }}</span>
              <button class="rl-modal__close" @click="$emit('close')">✕</button>
            </div>

            <div class="rl-modal__body">
              <div class="rl-shop-address">
                <div class="rl-shop-address__title">{{ T.SHOP_RETURN_TITLE }}</div>
                <div v-if="shopReturnName" class="rl-shop-address__name">{{ shopReturnName }}</div>
                <div class="rl-shop-address__row">
                  <span class="rl-shop-address__text">{{ shopReturnAddress }} {{ shopReturnPhone }}</span>
                  <button class="rl-shop-address__copy" @click="copyAddress">{{ copied ? '已复制' : T.COPY }}</button>
                </div>
              </div>
              <div class="rl-field">
                <label class="rl-field__label">{{ T.TRACKING_LABEL }}</label>
                <input
                  v-model="trackingNumber"
                  class="rl-input"
                  :placeholder="T.TRACKING_PLACEHOLDER"
                  type="text"
                />
              </div>
              <div class="rl-field">
                <label class="rl-field__label">{{ T.CONTACTS_LABEL }}</label>
                <div v-if="loadingAddress" class="rl-contacts-loading">{{ T.LOADING_ADDRESS }}</div>
                <div v-else-if="contacts.length === 0" class="rl-contacts-empty">{{ T.NO_CONTACTS }}</div>
                <div v-else class="rl-contacts-list">
                  <label
                    v-for="c in contacts"
                    :key="c.id"
                    class="rl-contact-item"
                    :class="{ 'rl-contact-item--active': selectedContactId === c.id }"
                  >
                    <input
                      type="radio"
                      :value="c.id"
                      v-model="selectedContactId"
                      class="rl-contact-item__radio"
                    />
                    <div class="rl-contact-item__info">
                      <div class="rl-contact-item__name">{{ c.name }} <span class="rl-contact-item__phone">{{ c.phone }}</span></div>
                      <div class="rl-contact-item__address">{{ c.address }}</div>
                    </div>
                  </label>
                </div>
              </div>
            </div>

            <div class="rl-modal__footer">
              <button class="rl-btn rl-btn--secondary" @click="$emit('close')">{{ T.BTN_CANCEL }}</button>
              <button class="rl-btn rl-btn--primary" :disabled="!canSubmit" @click="handleSubmit">
                {{ T.BTN_SUBMIT }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import * as T from './Text.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  shopReturnName: { type: String, default: '' },
  shopReturnAddress: { type: String, default: '' },
  shopReturnPhone: { type: String, default: '' },
  contacts: { type: Array, default: () => [] },
  loadingAddress: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'submit'])

const trackingNumber = ref('')
const copied = ref(false)
const selectedContactId = ref(null)

const canSubmit = computed(() => {
  return trackingNumber.value.trim().length > 0 && selectedContactId.value !== null
})

async function copyAddress() {
  const parts = [props.shopReturnName, props.shopReturnAddress, props.shopReturnPhone].filter(Boolean)
  const text = parts.join(' ')
  try {
    await navigator.clipboard.writeText(text)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {}
}

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', { trackingNumber: trackingNumber.value.trim(), contactId: Number(selectedContactId.value) })
  trackingNumber.value = ''
  selectedContactId.value = null
}

watch(() => props.visible, (v) => {
  if (!v) {
    trackingNumber.value = ''
    selectedContactId.value = null
  }
})
</script>

<style scoped>
@import './ReturnLogisticsModal.css';
</style>
