import { ref, computed } from 'vue'

export function usePagination(fetchFn, defaultPageSize = 20) {
  const list = ref([])
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  const loading = ref(false)

  const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

  async function load(params = {}) {
    loading.value = true
    try {
      const res = await fetchFn({ page: currentPage.value, pageSize: pageSize.value, ...params })
      list.value = res.records || res.list || res.data || []
      total.value = res.total || 0
    } finally {
      loading.value = false
    }
  }

  function goPage(page) {
    currentPage.value = page
    load()
  }

  function reset() {
    currentPage.value = 1
    list.value = []
    total.value = 0
  }

  return { list, currentPage, pageSize, total, totalPages, loading, load, goPage, reset }
}
