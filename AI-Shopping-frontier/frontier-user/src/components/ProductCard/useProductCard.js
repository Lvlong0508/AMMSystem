import { computed } from 'vue'

export function useProductCard(props) {
  const parsedTags = computed(() => {
    if (!props.product.tags) return []
    if (Array.isArray(props.product.tags)) return props.product.tags
    return props.product.tags.split(',').map(t => t.trim()).filter(Boolean)
  })
  return { parsedTags }
}
