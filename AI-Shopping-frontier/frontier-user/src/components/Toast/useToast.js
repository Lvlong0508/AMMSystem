import { ref, onMounted, onUnmounted } from 'vue'

export function useToast(props, emit) {
  const visible = ref(true)
  let timer = null

  function close() {
    visible.value = false
    emit('close')
  }

  onMounted(() => {
    timer = setTimeout(() => {
      close()
    }, props.duration)
  })

  onUnmounted(() => {
    if (timer) clearTimeout(timer)
  })

  return {
    visible,
    close,
  }
}
