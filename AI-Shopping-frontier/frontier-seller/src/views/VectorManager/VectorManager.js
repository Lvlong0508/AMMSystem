import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getVectorCollections,
  getVectorDocuments,
  searchVector,
  deleteVectorDocuments
} from '@/api/knowledge'
import * as T from './Text.js'

function formatTime(iso) {
  if (!iso) return ''
  // 2026-06-30T17:27:36.191924800 → 2026-06-30 17:27:36
  return iso.replace('T', ' ').replace(/\.\d+/, '')
}

export function useVectorManager() {
  const loading = ref(false)
  const activeTab = ref('overview')

  const stats = ref({
    totalDocs: 0,
    totalChunks: 0,
    collectionName: '',
    dimension: 0
  })
  const recentDocs = ref([])
  const documents = ref([])
  const batchMode = ref(false)
  const selectedFiles = ref([])

  const isAllSelected = computed(() => {
    return documents.value.length > 0 && selectedFiles.value.length === documents.value.length
  })

  const searchQuery = ref('')
  const topK = ref(5)
  const searchResults = ref(null)
  const isSearching = ref(false)

  async function loadStats() {
    try {
      const res = await getVectorCollections()
      if (res?.code === 200) {
        stats.value = res.data
      }
    } catch {
      ElMessage.error(T.LOAD_FAILED)
    }
  }

  async function loadDocuments() {
    try {
      const res = await getVectorDocuments()
      if (res?.code === 200) {
        documents.value = res.data || []
        stats.value.totalDocs = documents.value.length
        recentDocs.value = documents.value.slice(0, 5)
      }
    } catch {
      ElMessage.error(T.LOAD_FAILED)
    }
  }

  async function refresh() {
    loading.value = true
    await Promise.all([loadStats(), loadDocuments()])
    loading.value = false
  }

  async function handleSearch() {
    if (!searchQuery.value.trim()) return
    isSearching.value = true
    try {
      const res = await searchVector(searchQuery.value.trim(), topK.value)
      if (res?.code === 200) {
        searchResults.value = res.data || []
      }
    } catch {
      ElMessage.error(T.SEARCH_FAILED)
    } finally {
      isSearching.value = false
    }
  }

  function clearSearch() {
    searchQuery.value = ''
    searchResults.value = null
  }

  async function deleteDocument(fileName) {
    try {
      await ElMessageBox.confirm(T.CONFIRM_DELETE_TEXT, T.CONFIRM_DELETE_TITLE)
      const res = await deleteVectorDocuments([fileName])
      if (res?.code === 200) {
        ElMessage.success(T.DELETE_SUCCESS)
        await refresh()
      }
    } catch {
      // cancelled or error
    }
  }

  function toggleBatch() {
    batchMode.value = !batchMode.value
    if (!batchMode.value) {
      selectedFiles.value = []
    }
  }

  function toggleSelectAll() {
    if (isAllSelected.value) {
      selectedFiles.value = []
    } else {
      selectedFiles.value = documents.value.map(d => d.fileName)
    }
  }

  async function batchDeleteSelected() {
    if (selectedFiles.value.length === 0) return
    try {
      await ElMessageBox.confirm(T.BATCH_CONFIRM_TEXT, T.BATCH_CONFIRM_TITLE)
      const res = await deleteVectorDocuments(selectedFiles.value)
      if (res?.code === 200) {
        ElMessage.success(T.BATCH_DELETE_SUCCESS)
        selectedFiles.value = []
        batchMode.value = false
        await refresh()
      }
    } catch {
      // cancelled or error
    }
  }

  function goToLibrary() {
    activeTab.value = 'library'
  }

  onMounted(() => {
    refresh()
  })

  return {
    loading, activeTab, stats, recentDocs,
    documents, searchQuery, topK, searchResults, isSearching,
    refresh, handleSearch, clearSearch, deleteDocument, goToLibrary,
    batchMode, selectedFiles, isAllSelected,
    toggleBatch, toggleSelectAll, batchDeleteSelected,
    formatTime
  }
}
