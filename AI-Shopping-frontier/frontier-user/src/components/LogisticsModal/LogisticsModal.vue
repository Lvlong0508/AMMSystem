<template>
  <Teleport to="body">
    <div v-if="visible" class="logistics-modal-overlay" @click="$emit('close')">
      <div class="logistics-modal" @click.stop>
        <div class="logistics-modal__header">
          <span class="logistics-modal__title">{{ T.TITLE }}</span>
          <button class="logistics-modal__close" @click="$emit('close')">✕</button>
        </div>

        <div class="logistics-modal__body">
          <div v-if="loading" class="logistics-modal__loading">{{ T.LOADING }}</div>

          <template v-else-if="logisticsList.length > 0">
            <div v-for="item in logisticsList" :key="item.id" class="logistics-modal__item">
              <div class="logistics-modal__tag" :class="item.type === 'DELIVERY' ? 'logistics-modal__tag--delivery' : 'logistics-modal__tag--return'">
                {{ item.type === 'DELIVERY' ? T.TYPE_DELIVERY : T.TYPE_RETURN }}
              </div>
              <div class="logistics-modal__fields">
                <div class="logistics-modal__field">
                  <span class="logistics-modal__label">{{ T.ORDER_ID }}</span>
                  <span class="logistics-modal__value">{{ item.orderId }}</span>
                </div>
                <div class="logistics-modal__field">
                  <span class="logistics-modal__label">{{ T.TRACKING_NUMBER }}</span>
                  <span class="logistics-modal__value logistics-modal__value--tracking">{{ item.trackingNumber }}</span>
                </div>
                <div class="logistics-modal__field">
                  <span class="logistics-modal__label">{{ T.CREATED_AT }}</span>
                  <span class="logistics-modal__value">{{ formatTime(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </template>

          <div v-else class="logistics-modal__empty">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" class="logistics-modal__empty-icon"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
            <p class="logistics-modal__empty-text">{{ T.NO_INFO }}</p>
          </div>
        </div>

        <div class="logistics-modal__footer">
          <button class="logistics-modal__close-btn" @click="$emit('close')">{{ T.CLOSE }}</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { LOGISTICS_MODAL_TEXT as T } from './Text'

const props = defineProps({
  visible: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  logisticsList: { type: Array, default: () => [] }
})

defineEmits(['close'])

function formatTime(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString('zh-CN')
}
</script>

<style scoped>
@import './LogisticsModal.css';
</style>