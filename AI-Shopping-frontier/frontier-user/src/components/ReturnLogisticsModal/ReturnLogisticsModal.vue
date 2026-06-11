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
                <label class="rl-field__label">{{ T.ADDRESS_LABEL }}</label>
                <div v-if="loadingAddress" class="rl-loading">{{ T.LOADING_ADDRESS }}</div>
                <div v-else-if="contacts.length === 0" class="rl-empty">{{ T.NO_ADDRESS }}</div>
                <div v-else class="rl-address-list">
                  <div
                    v-for="c in contacts"
                    :key="c.id"
                    class="rl-address-item"
                    :class="{ 'rl-address-item--selected': selectedContactId === c.id }"
                    @click="selectedContactId = c.id"
                  >
                    <div class="rl-address-item__radio">
                      <div v-if="selectedContactId === c.id" class="rl-address-item__radio-dot"></div>
                    </div>
                    <div class="rl-address-item__info">
                      <div class="rl-address-item__top">
                        <span class="rl-address-item__name">{{ c.name }}</span>
                        <span class="rl-address-item__phone">{{ c.phone }}</span>
                        <span v-if="c.isDefault" class="rl-address-item__badge">默认</span>
                      </div>
                      <div class="rl-address-item__addr">{{ c.address }}</div>
                    </div>
                  </div>
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
  contacts: { type: Array, default: () => [] },
  loadingAddress: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'submit'])

const trackingNumber = ref('')
const selectedContactId = ref('')

const canSubmit = computed(() =>
  trackingNumber.value.trim().length > 0 && selectedContactId.value
)

function handleSubmit() {
  if (!canSubmit.value) return
  emit('submit', {
    trackingNumber: trackingNumber.value.trim(),
    contactId: Number(selectedContactId.value)
  })
  reset()
}

function reset() {
  trackingNumber.value = ''
  selectedContactId.value = ''
}

watch([() => props.visible, () => props.contacts], () => {
  if (props.visible && props.contacts.length > 0 && !selectedContactId.value) {
    const defaultContact = props.contacts.find(c => c.isDefault)
    selectedContactId.value = defaultContact ? defaultContact.id : props.contacts[0].id
  }
  if (!props.visible) reset()
})
</script>

<style scoped>
@import './ReturnLogisticsModal.css';
</style>
