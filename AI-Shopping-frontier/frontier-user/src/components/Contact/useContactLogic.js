import { ref, computed, onMounted } from 'vue'
import {
  getAllContacts,
  createContact,
  updateContact,
  deleteContact,
  searchContactsByName,
  getContactByPhone
} from '../../api/contact.js'
import { showSuccess, showError, showConfirm } from '../../utils/swal.js'
import { CONTACT_MESSAGES } from '../../config/messages.js'

export function useContactLogic() {
  // 响应式数据
  const contacts = ref([])
  const selectedContact = ref(null)
  const searchKeyword = ref('')
  const dialogVisible = ref(false)
  const deleteDialogVisible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const deleting = ref(false)
  const contactToDelete = ref(null)

  const form = ref({
    id: null,
    name: '',
    phone: '',
    address: ''
  })

  // 计算属性
  const isFormValid = computed(() => {
    return form.value.name.trim() &&
      form.value.phone.trim() &&
      form.value.address.trim()
  })

  // 格式化时间
  const formatTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    return date.toLocaleString('zh-CN')
  }

  // 获取所有联系人
  const loadContacts = async () => {
    try {
      const res = await getAllContacts()
      contacts.value = res.data || []
    } catch (error) {
      console.error('获取联系人失败:', error)
      showError(CONTACT_MESSAGES.LOAD_FAILED)
    }
  }

  // 搜索
  const handleSearch = async () => {
    if (!searchKeyword.value.trim()) {
      loadContacts()
      return
    }
    try {
      // 先尝试按姓名搜索
      const res = await searchContactsByName(searchKeyword.value)
      if (res.data && res.data.length > 0) {
        contacts.value = res.data
      } else {
        // 如果没有结果，尝试按电话搜索
        const phoneRes = await getContactByPhone(searchKeyword.value)
        if (phoneRes.data) {
          contacts.value = [phoneRes.data]
        } else {
          contacts.value = []
        }
      }
    } catch (error) {
      console.error('搜索失败:', error)
      contacts.value = []
      showError(CONTACT_MESSAGES.SEARCH_FAILED)
    }
  }

  // 清空搜索
  const clearSearch = () => {
    searchKeyword.value = ''
    loadContacts()
  }

  // 选择联系人
  const selectContact = (contact) => {
    selectedContact.value = contact
  }

  // 打开新增弹窗
  const openAddDialog = () => {
    isEdit.value = false
    form.value = {
      id: null,
      name: '',
      phone: '',
      address: ''
    }
    dialogVisible.value = true
  }

  // 打开编辑弹窗
  const openEditDialog = (contact) => {
    isEdit.value = true
    form.value = {
      id: contact.id,
      name: contact.name,
      phone: contact.phone,
      address: contact.address
    }
    dialogVisible.value = true
  }

  // 关闭弹窗
  const closeDialog = () => {
    if (!submitting.value) {
      dialogVisible.value = false
    }
  }

  // 提交表单
  const handleSubmit = async () => {
    if (!isFormValid.value || submitting.value) return

    submitting.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updateContact(form.value)
      } else {
        res = await createContact(form.value)
      }

      if (res.data?.message?.includes('成功')) {
        showSuccess(isEdit.value ? CONTACT_MESSAGES.UPDATE_SUCCESS : CONTACT_MESSAGES.CREATE_SUCCESS)
        dialogVisible.value = false
        loadContacts()
      } else {
        showError(res.data?.message || CONTACT_MESSAGES.OPERATION_FAILED)
      }
    } catch (error) {
      console.error('保存联系人失败:', error)
      showError(error.response?.data?.message || CONTACT_MESSAGES.CREATE_FAILED)
    } finally {
      submitting.value = false
    }
  }

  // 打开删除确认弹窗
  const handleDelete = (contact) => {
    contactToDelete.value = contact
    deleteDialogVisible.value = true
  }

  // 关闭删除弹窗
  const closeDeleteDialog = () => {
    if (!deleting.value) {
      deleteDialogVisible.value = false
      contactToDelete.value = null
    }
  }

  // 确认删除
  const confirmDelete = async () => {
    if (!contactToDelete.value || deleting.value) return

    const result = await showConfirm(
      CONTACT_MESSAGES.DELETE_CONFIRM_TITLE,
      CONTACT_MESSAGES.DELETE_CONFIRM_TEXT,
      CONTACT_MESSAGES.CONFIRM_BUTTON,
      CONTACT_MESSAGES.CANCEL_BUTTON
    )
    if (!result.isConfirmed) return

    deleting.value = true
    try {
      const res = await deleteContact(contactToDelete.value.id)
      if (res.data?.message?.includes('成功')) {
        showSuccess(CONTACT_MESSAGES.DELETE_SUCCESS)
        closeDeleteDialog()
        // 如果删除的是当前选中的联系人，清空选中
        if (selectedContact.value?.id === contactToDelete.value.id) {
          selectedContact.value = null
        }
        loadContacts()
      } else {
        showError(res.data?.message || CONTACT_MESSAGES.OPERATION_FAILED)
      }
    } catch (error) {
      console.error('删除联系人失败:', error)
      showError(error.response?.data?.message || CONTACT_MESSAGES.DELETE_FAILED)
    } finally {
      deleting.value = false
    }
  }

  // 初始化
  onMounted(() => {
    loadContacts()
  })

  return {
    contacts,
    selectedContact,
    searchKeyword,
    dialogVisible,
    deleteDialogVisible,
    isEdit,
    submitting,
    deleting,
    contactToDelete,
    form,
    isFormValid,
    formatTime,
    loadContacts,
    handleSearch,
    clearSearch,
    selectContact,
    openAddDialog,
    openEditDialog,
    closeDialog,
    handleSubmit,
    handleDelete,
    closeDeleteDialog,
    confirmDelete
  }
}
