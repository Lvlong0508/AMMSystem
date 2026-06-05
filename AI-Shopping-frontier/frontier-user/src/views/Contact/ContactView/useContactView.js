import { ref, onMounted } from 'vue'
import { getContactList, setDefaultContact, deleteContact } from '@/api/contact'
import { showSuccess, showError } from '@/utils/swal'

export function useContactView() {
  const contacts = ref([])
  const loading = ref(true)
  const showDeleteSheet = ref(false)
  const contactToDelete = ref(null)

  const loadContacts = async () => {
    loading.value = true
    try {
      const res = await getContactList()
      contacts.value = res.contacts || (Array.isArray(res) ? res : [])
    } catch {
      showError('加载地址失败')
    } finally {
      loading.value = false
    }
  }

  const handleEdit = (contact) => {}

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
    handleEdit,
    handleSetDefault,
    handleDeleteClick,
    confirmDelete
  }
}
