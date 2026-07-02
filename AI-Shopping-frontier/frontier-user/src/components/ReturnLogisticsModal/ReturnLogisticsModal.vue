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
  shopReturnPhone: { type: String, default: '' }
})

const emit = defineEmits(['close', 'submit'])

const trackingNumber = ref('')
const copied = ref(false)

const canSubmit = computed(() => trackingNumber.value.trim().length > 0)

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
  emit('submit', { trackingNumber: trackingNumber.value.trim() })
  trackingNumber.value = ''
}

watch(() => props.visible, (v) => {
  if (!v) trackingNumber.value = ''
})
</script>

<style scoped>
@import './ReturnLogisticsModal.css';
</style>
