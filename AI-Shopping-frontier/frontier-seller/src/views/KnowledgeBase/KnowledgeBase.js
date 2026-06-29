import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  uploadKnowledgeFiles,
  listUploadFiles,
  listFinishFiles,
  deleteUploadFiles,
  deleteFinishFiles,
  ingestFiles
} from '@/api/knowledge'
import * as T from './Text.js'

export function useKnowledgeBase() {
  const loading = ref(false)
  const activeTab = ref('upload')
  const uploadFiles = ref([])
  const finishFiles = ref([])
  const batchMode = ref(false)
  const selectedFiles = ref([])
  const uploadDialogVisible = ref(false)
  const uploadingFiles = ref([])
  const uploading = ref(false)
  const uploadRef = ref(null)

  const currentList = computed(() =>
    activeTab.value === 'upload' ? uploadFiles : finishFiles
  )

  async function loadUploadFiles() {
    try {
      const res = await listUploadFiles()
      uploadFiles.value = res?.data || []
    } catch {
      ElMessage.error(T.LOAD_FAILED)
    }
  }

  async function loadFinishFiles() {
    try {
      const res = await listFinishFiles()
      finishFiles.value = res?.data || []
    } catch {
      ElMessage.error(T.LOAD_FAILED)
    }
  }

  async function refresh() {
    loading.value = true
    await Promise.all([loadUploadFiles(), loadFinishFiles()])
    loading.value = false
  }

  function toggleBatch() {
    batchMode.value = !batchMode.value
    if (!batchMode.value) {
      selectedFiles.value = []
    }
  }

  function onUploadFileChange(uploadFile, uploadFiles) {
    uploadingFiles.value = uploadFiles.map(f => f.raw || f)
  }

  function removeFile(index) {
    uploadingFiles.value.splice(index, 1)
    uploadRef.value?.handleRemove(uploadRef.value.uploadFiles[index])
  }

  function removeAll() {
    uploadingFiles.value = []
    uploadRef.value?.clearFiles()
  }

  function onDialogClose() {
    uploadingFiles.value = []
    uploadRef.value?.clearFiles()
  }

  async function confirmUpload() {
    if (uploadingFiles.value.length === 0) {
      ElMessage.warning(T.SELECT_FILES_PROMPT)
      return
    }
    uploading.value = true
    try {
      const res = await uploadKnowledgeFiles(uploadingFiles.value)
      if (res?.code === 200) {
        ElMessage.success(T.UPLOAD_SUCCESS)
        uploadDialogVisible.value = false
        uploadingFiles.value = []
        await loadUploadFiles()
      } else {
        ElMessage.error(res?.message || T.UPLOAD_FAILED)
      }
    } catch {
      ElMessage.error(T.UPLOAD_FAILED)
    } finally {
      uploading.value = false
    }
  }

  async function importSingle(fileName) {
    try {
      await ingestFiles([fileName])
      ElMessage.success(T.IMPORT_SUCCESS)
      await refresh()
    } catch {
      ElMessage.error(T.IMPORT_FAILED)
    }
  }

  async function importSelected() {
    if (selectedFiles.value.length === 0) {
      ElMessage.warning(T.SELECT_IMPORT_PROMPT)
      return
    }
    try {
      await ingestFiles(selectedFiles.value)
      ElMessage.success(T.IMPORT_SUCCESS)
      selectedFiles.value = []
      batchMode.value = false
      await refresh()
    } catch {
      ElMessage.error(T.IMPORT_FAILED)
    }
  }

  async function deleteSingle(fileName) {
    try {
      await ElMessageBox.confirm(T.CONFIRM_DELETE_SINGLE(fileName), T.CONFIRM_DELETE_TITLE)
      if (activeTab.value === 'upload') {
        await deleteUploadFiles([fileName])
      } else {
        await deleteFinishFiles([fileName])
      }
      ElMessage.success(T.DELETE_SUCCESS)
      await refresh()
    } catch {
      // cancelled or error - ElMessageBox cancel throws
    }
  }

  async function deleteSelected() {
    if (selectedFiles.value.length === 0) {
      ElMessage.warning(T.SELECT_DELETE_PROMPT)
      return
    }
    try {
      await ElMessageBox.confirm(T.CONFIRM_DELETE_TEXT, T.CONFIRM_DELETE_TITLE)
      if (activeTab.value === 'upload') {
        await deleteUploadFiles(selectedFiles.value)
      } else {
        await deleteFinishFiles(selectedFiles.value)
      }
      ElMessage.success(T.DELETE_SUCCESS)
      selectedFiles.value = []
      batchMode.value = false
      await refresh()
    } catch {
      // cancelled or error
    }
  }

  function handleSelectionChange(val) {
    selectedFiles.value = val
  }

  onMounted(() => {
    refresh()
  })

  return {
    loading,
    activeTab,
    uploadFiles,
    finishFiles,
    currentList,
    batchMode,
    selectedFiles,
    uploadDialogVisible,
    uploadingFiles,
    uploading,
    refresh,
    toggleBatch,
    uploadRef,
    onUploadFileChange,
    removeFile,
    removeAll,
    onDialogClose,
    confirmUpload,
    importSingle,
    importSelected,
    deleteSingle,
    deleteSelected,
    handleSelectionChange
  }
}
