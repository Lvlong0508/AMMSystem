<template>
  <span class="status-tag" :class="statusClass">
    <span class="status-dot"></span>
    {{ displayText }}
  </span>
</template>

<script setup>
import { useStatusTag } from './useStatusTag'

const props = defineProps({
  status: { type: String, required: true }
})

const { statusClass, displayText } = useStatusTag(props)
</script>

<style scoped>
@import './StatusTag.css';
</style>
