import { ref, onMounted, nextTick } from 'vue'
import { getContactList, setDefaultContact, deleteContact, createContact } from '@/api/contact'
import { showSuccess, showError } from '@/utils/swal'

export function useContactView() {
  const contacts = ref([])
  const loading = ref(true)
  const showDeleteSheet = ref(false)
  const contactToDelete = ref(null)
  const showFormSheet = ref(false)
  const formName = ref('')
  const formPhone = ref('')
  const formAddr = ref('')
  const formError = ref('')
  const formSubmitting = ref(false)
  const nameInput = ref(null)

  const loadContacts = async () => {
    loading.value = true
    try {
      const res = await getContactList()
      const list = res.contacts || (Array.isArray(res) ? res : [])
      list.sort((a, b) => {
        if (a.isDefault === 1 || a.isDefault === true) return -1
        if (b.isDefault === 1 || b.isDefault === true) return 1
        return (a.createdAt || '').localeCompare(b.createdAt || '')
      })
      contacts.value = list
    } catch {
      showError('加载地址失败')
    } finally {
      loading.value = false
    }
  }

  const handleEdit = (contact) => {}

  const closeForm = () => {
    showFormSheet.value = false
    formName.value = ''
    formPhone.value = ''
    formAddr.value = ''
    formError.value = ''
  }

  const handleAdd = () => {
    formName.value = ''
    formPhone.value = ''
    formAddr.value = ''
    formError.value = ''
    showFormSheet.value = true
    nextTick(() => {
      nameInput.value?.focus()
    })
  }

  const submitForm = async () => {
    const name = formName.value.trim()
    const phone = formPhone.value.trim()
    const address = formAddr.value.trim()
    if (!name || !phone || !address) {
      formError.value = '请填写完整信息'
      return
    }
    formSubmitting.value = true
    formError.value = ''
    try {
      await createContact({ name, phone, address })
      showSuccess('地址已添加')
      closeForm()
      await loadContacts()
    } catch {
      showError('添加失败')
    } finally {
      formSubmitting.value = false
    }
  }

  const handleSetDefault = async (contact) => {
    try {
      await setDefaultContact(contact.id)
      showSuccess('已设为默认地址')
      await loadContacts()
    } catch {
      showError('设置失败')
    }
  }

  const handleDeleteClick = (contact) => {
    contactToDelete.value = contact
    showDeleteSheet.value = true
  }

  const confirmDelete = async () => {
    showDeleteSheet.value = false
    if (!contactToDelete.value) return
    try {
      await deleteContact(contactToDelete.value.id)
      showSuccess('地址已删除')
      await loadContacts()
    } catch {
      showError('删除失败')
    }
    contactToDelete.value = null
  }

  onMounted(loadContacts)

  return {
    contacts,
    loading,
    showDeleteSheet,
    showFormSheet,
    formName,
    formPhone,
    formAddr,
    formError,
    formSubmitting,
    nameInput,
    handleEdit,
    handleSetDefault,
    handleDeleteClick,
    confirmDelete,
    handleAdd,
    closeForm,
    submitForm
  }
}
