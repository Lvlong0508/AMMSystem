import { ref } from 'vue'
import * as T from './Text.js'

export function useProductCard(props, emit) {
  const expanded = ref(false)

  function toggle() {
    expanded.value = !expanded.value
  }

  function handleEdit() {
    emit('edit', props.product)
  }

  function handleToggleSale() {
    emit('toggle-sale', props.product)
  }

  function handleDelete() {
    emit('delete', props.product)
  }

  return {
    T,
    expanded,
    toggle,
    handleEdit,
    handleToggleSale,
    handleDelete
  }
}
