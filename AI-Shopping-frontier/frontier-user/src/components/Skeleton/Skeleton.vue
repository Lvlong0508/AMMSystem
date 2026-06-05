<template>
  <div :class="['skeleton', `skeleton-${variant}`]" :style="skeletonStyle">
    <template v-if="variant === 'line'">
      <div
        v-for="i in countArray"
        :key="i"
        class="skeleton-line"
        :style="getLineStyle(i)"
      />
    </template>
    <div v-else-if="variant === 'card'" class="skeleton-card">
      <div class="skeleton-card-image" />
      <div class="skeleton-card-title" />
      <div class="skeleton-card-price" />
    </div>
    <div v-else-if="variant === 'circle'" class="skeleton-circle" />
  </div>
</template>

<script setup>
import { useSkeleton } from './useSkeleton.js'

const props = defineProps({
  variant: {
    type: String,
    default: 'line',
    validator: (v) => ['line', 'card', 'circle'].includes(v),
  },
  width: { type: [String, Number], default: null },
  height: { type: [String, Number], default: null },
  count: { type: Number, default: 3 },
})

const { countArray, skeletonStyle, getLineStyle } = useSkeleton(props)
</script>

<style scoped>
@import './Skeleton.css';
</style>
