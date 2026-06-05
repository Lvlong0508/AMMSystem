import { computed } from 'vue'

export function useSkeleton(props) {
  const countArray = computed(() => {
    return Array.from({ length: props.count }, (_, i) => i)
  })

  const skeletonStyle = computed(() => {
    const style = {}
    if (props.width) style.width = typeof props.width === 'number' ? `${props.width}px` : props.width
    if (props.height) style.height = typeof props.height === 'number' ? `${props.height}px` : props.height
    return style
  })

  function getLineStyle(index) {
    const width = 100 - (index % 3) * 15
    return { width: `${width}%` }
  }

  return {
    countArray,
    skeletonStyle,
    getLineStyle,
  }
}
