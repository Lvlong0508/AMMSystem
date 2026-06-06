<template>
  <div :class="['chat-bubble', `chat-bubble-${role}`]">
    <div class="chat-bubble-avatar" aria-hidden="true">
      <svg v-if="role === 'ai'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2" y="2" width="20" height="20" rx="4" />
        <path d="M12 8v8M8 12h8" />
      </svg>
      <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
    <div class="chat-bubble-content">
      <div class="chat-bubble-message">{{ message }}</div>
      <span v-if="time" class="chat-bubble-time">{{ time }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  role: {
    type: String,
    required: true,
    validator: (v) => ['ai', 'user'].includes(v),
  },
  message: { type: String, required: true },
  time: { type: String, default: '' },
})
</script>

<style scoped>
@import './ChatBubble.css';
</style>
