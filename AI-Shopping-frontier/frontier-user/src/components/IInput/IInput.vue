<script setup>
import { useVModel } from "@vueuse/core"
import { computed, ref } from "vue"

defineOptions({
  inheritAttrs: false,
})

const props = defineProps({
  defaultValue: [String, Number],
  modelValue: [String, Number],
  class: String,
})

const emit = defineEmits(["update:modelValue"])

const modelValue = useVModel(props, "modelValue", emit, {
  passive: true,
  defaultValue: props.defaultValue,
})

const wrapperRef = ref(null)
const inputEl = ref(null)
const mouse = ref({ x: 0, y: 0 })
const hovering = ref(false)

const glowStyle = computed(() => {
  if (!hovering.value) return { background: "#F1F5FD" }
  return {
    background: `radial-gradient(180px circle at ${mouse.value.x}px ${mouse.value.y}px, rgba(37, 99, 235, 0.45), transparent 70%)`,
  }
})

function handleMouseMove(e) {
  if (!wrapperRef.value) return
  const rect = wrapperRef.value.getBoundingClientRect()
  mouse.value = { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

defineExpose({ inputEl })
</script>

<template>
  <div
    ref="wrapperRef"
    class="chat-input-wrapper"
    :class="{ 'chat-input-wrapper--hover': hovering }"
    :style="glowStyle"
    @mouseenter="hovering = true"
    @mousemove="handleMouseMove"
    @mouseleave="hovering = false"
  >
    <div class="chat-input-inner">
      <input
        ref="inputEl"
        v-bind="$attrs"
        v-model="modelValue"
        class="chat-input"
        :class="props.class"
      />
    </div>
  </div>
</template>

<style scoped>
.chat-input-wrapper {
  border-radius: 38px;
  padding: 2px;
  transition: all 200ms ease-out;
  overflow: hidden;
}

.chat-input-wrapper--hover {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
}

.chat-input-inner {
  border-radius: 36px;
  background: #F1F5FD;
  display: flex;
  align-items: center;
  padding: 0 26px;
  height: 72px;
  transition: all 200ms ease-out;
}

.chat-input-wrapper:focus-within .chat-input-inner {
  background: #fff;
}

.chat-input-wrapper:focus-within {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.15);
}

.chat-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 20px;
  font-family: var(--font-body);
  color: #0F172A;
  outline: none;
  height: 100%;
  width: 100%;
}

.chat-input::placeholder {
  color: #94a3b8;
}

.chat-input:disabled {
  opacity: 0.5;
}
</style>
