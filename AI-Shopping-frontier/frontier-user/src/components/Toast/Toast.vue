<template>
  <Transition name="toast-slide">
    <div v-if="visible" :class="['toast', `toast-${type}`]">
      <div class="toast-content">
        <span class="toast-icon">
          <svg v-if="type === 'success'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5" />
          </svg>
          <svg v-else-if="type === 'warning'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <svg v-else-if="type === 'error'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
        </span>
        <span class="toast-message">{{ message }}</span>
      </div>
      <button class="toast-close" @click="close">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
      <div class="toast-progress" :style="{ animationDuration: duration + 'ms' }" />
    </div>
  </Transition>
</template>

<script setup>
import { useToast } from './useToast.js'

const props = defineProps({
  type: {
    type: String,
    default: 'success',
    validator: (v) => ['success', 'warning', 'error'].includes(v),
  },
  message: { type: String, required: true },
  duration: { type: Number, default: 3000 },
})

const emit = defineEmits(['close'])
const { visible, close } = useToast(props, emit)
</script>

<style scoped>
@import './Toast.css';
</style>
