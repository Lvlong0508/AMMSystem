import { ref, computed, watch } from "vue"
import { getContactList } from "@/api/contact"
import { placeOrder } from "@/api/order"
import { showError } from "@/utils/swal"
import { requireLogin } from "@/stores/authStore"

export function useOrderModal(props, { emit }) {
  const quantity = ref(1)
  const contacts = ref([])
  const selectedContactId = ref(null)
  const submitting = ref(false)
  const loadingAddress = ref(false)
  const maxQuantity = computed(() => props.product?.stock ?? 1)
  const totalPrice = computed(() => (props.product?.price || 0) * quantity.value)
  const canSubmit = computed(() => quantity.value > 0 && quantity.value <= maxQuantity.value && selectedContactId.value !== null && !submitting.value)

  const decrement = () => { if (quantity.value > 1) quantity.value-- }
  const increment = () => { if (quantity.value < maxQuantity.value) quantity.value++ }

  const loadContacts = async () => {
    loadingAddress.value = true
    try {
      const res = await getContactList()
      contacts.value = res.contacts || []
      const def = contacts.value.find(c => c.isDefault === 1 || c.isDefault === true)
      if (def) selectedContactId.value = def.id
      else if (contacts.value.length) selectedContactId.value = contacts.value[0].id
    } catch { showError("加载地址失败") }
    finally { loadingAddress.value = false }
  }

  const handleSubmit = async () => {
    if (!requireLogin() || !canSubmit.value) return
    submitting.value = true
    try {
      const oid = await placeOrder({ productId: props.product.id, quantity: quantity.value, contactId: selectedContactId.value })
      emit("order-placed", oid)
    } catch (e) { showError(e.message || "下单失败") }
    finally { submitting.value = false }
  }

  watch(() => props.visible, (v) => { if (v) { quantity.value = 1; selectedContactId.value = null; loadContacts() } })

  return { quantity, contacts, selectedContactId, totalPrice, maxQuantity, canSubmit, submitting, loadingAddress, decrement, increment, handleSubmit }
}
