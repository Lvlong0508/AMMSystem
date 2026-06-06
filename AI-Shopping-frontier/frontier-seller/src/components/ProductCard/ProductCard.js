import { computed } from 'vue'
import * as T from './Text.js'

export function useProductCard(props) {
  const parsedTags = computed(() => {
    if (!props.product?.tags) return []
    if (Array.isArray(props.product.tags)) return props.product.tags
    return String(props.product.tags).split(',').map(t => t.trim()).filter(Boolean)
  })

  function formatDate(dateStr) {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleString('zh-CN')
  }

  return { T, parsedTags, formatDate }
}
