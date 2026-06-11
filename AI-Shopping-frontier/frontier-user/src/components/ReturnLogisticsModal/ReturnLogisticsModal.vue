<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="rl-overlay" @click="$emit('close')">
        <div class="rl-modal" @click.stop>
          <div class="rl-modal__header">
            <h2 class="rl-modal__title">{{ T.TITLE }}</h2>
            <button class="rl-modal__close" @click="$emit('close')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <div class="rl-modal__body">
            <div class="rl-field">
              <label class="rl-field__label">{{ T.TRACKING_LABEL }}</label>
              <IInput
                v-model="trackingNumber"
                :placeholder="T.TRACKING_PLACEHOLDER"
                type="text"
              />
            </div>

            <div class="rl-field">
              <label class="rl-field__label">{{ T.ADDRESS_LABEL }}</label>
              <div v-if="loadingAddress" class="rl-field__loading">{{ T.LOADING_ADDRESS }}</div>
              <select v-else v-model="selectedContactId" class="rl-select">
                <option v-if="contacts.length === 0" value="" disabled>{{ T.NO_ADDRESS }}</option>
                <option
                  v-for="c in contacts"
                  :key="c.id"
                  :value="c.id"
                >
                  {{ c.name }} {{ c.phone }} - {{ c.address }}
                </option>
              </select>
            </div>
          </div>

          <div class="rl-modal__footer">
            <button class="rl-btn rl-btn--secondary" @click="$emit('close')">{{ T.BTN_CANCEL }}</button>
            <button
              class="rl-btn rl-btn--primary"
              :disabled="!canSubmit"
              @click="handleSubmit"
            >
              {{ T.BTN_SUBMIT }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import * as T from './Text.js'
import IInput from '@/components/IInput/IInput.vue'

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

watch(() => props.visible, (v) => {
  if (v && props.contacts.length > 0 && !selectedContactId.value) {
    selectedContactId.value = props.contacts[0].id
  }
  if (!v) reset()
})
</script>

<style scoped>
@import './ReturnLogisticsModal.css';
</style>
