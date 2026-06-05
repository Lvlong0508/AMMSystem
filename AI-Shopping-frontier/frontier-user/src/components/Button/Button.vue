<template>
  <button
    :class="classes"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <span v-if="loading" class="btn-spinner">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <circle cx="12" cy="12" r="10" stroke-dasharray="31.4 31.4" stroke-linecap="round" />
      </svg>
    </span>
    <span v-else-if="icon" class="btn-icon"><slot name="icon" /></span>
    <span class="btn-text"><slot /></span>
  </button>
</template>

<script setup>
import { useButton } from './useButton.js'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'secondary', 'text', 'danger'].includes(v),
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v),
  },
  disabled: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  icon: { type: Boolean, default: false },
})

const emit = defineEmits(['click'])
const { classes, handleClick } = useButton(props, emit)
</script>

<style scoped>
@import './Button.css';
</style>
