import { computed } from 'vue'

export function useButton(props, emit) {
  const classes = computed(() => [
    'btn',
    `btn-${props.variant}`,
    `btn-${props.size}`,
    {
      'btn-loading': props.loading,
      'btn-disabled': props.disabled,
    },
  ])

  function handleClick(event) {
    if (props.disabled || props.loading) return
    emit('click', event)
  }

  return {
    classes,
    handleClick,
  }
}
