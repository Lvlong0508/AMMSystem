import { ref } from 'vue'

export function useProductCardLogic(props, emit) {
  const parseTags = (tags) => {
    if (!tags) return []
    if (Array.isArray(tags)) return tags
    return []
  }

  const handleOrderClick = () => {
    emit('showOrderDialog', props.product)
  }

  return {
    parseTags,
    handleOrderClick
  }
}
