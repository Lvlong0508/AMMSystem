import ProductCard from '../ProductCard/ProductCard.vue'

export function useMessageBubbleLogic(props, emit) {
  const parseTags = (tags) => {
    if (!tags) return []
    if (Array.isArray(tags)) return tags
    return []
  }

  return {
    parseTags
  }
}
