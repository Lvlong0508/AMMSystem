<template>
  <Transition name="action-sheet">
    <div v-if="show" class="action-sheet-overlay" @click.self="handleCancel">
      <div class="action-sheet-panel" role="dialog" aria-modal="true">
        <div class="action-sheet-header">
          <h3>{{ title }}</h3>
          <p v-if="description">{{ description }}</p>
        </div>
        <div class="action-sheet-actions">
          <button
            :class="['action-btn', danger ? 'action-btn-danger' : 'action-btn-confirm']"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </button>
          <button class="action-btn action-btn-cancel" @click="handleCancel">
            {{ cancelText }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { useActionSheet } from './useActionSheet.js'
import { ACTION_SHEET_TEXT as T } from './Text.js'

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  confirmText: { type: String, default: T.CONFIRM_DEFAULT },
  cancelText: { type: String, default: T.CANCEL_DEFAULT },
  danger: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'cancel'])
const { handleConfirm, handleCancel } = useActionSheet(props, emit)
</script>

<style scoped>
@import './ActionSheet.css';
</style>
