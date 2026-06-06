import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShopDetail, getShopEmployees, registerEmployee, removeEmployee } from '@/api/shop'
import * as T from './Text.js'

export function useShopEmployees() {
  const route = useRoute()
  const shopId = ref(route.params.shopId)
  const shopInfo = ref(null)
  const employees = ref([])
  const loading = ref(false)
  const dialogVisible = ref(false)
  const submitting = ref(false)

  const form = ref({ name: '', phone: '', username: '', password: '' })

  const roleMap = { CLERK: '店员', MANAGER: '店长', ADMIN: '管理员' }

  async function loadShopInfo() {
    try { const res = await getShopDetail(shopId.value); if (res?.data) shopInfo.value = res.data } catch (e) { console.error(e) }
  }

  async function loadEmployees() {
    loading.value = true
    try {
      const res = await getShopEmployees(shopId.value)
      employees.value = res?.data?.employees || res?.employees || []
    } catch (e) {
      ElMessage.error('加载失败')
      employees.value = []
    } finally { loading.value = false }
  }

  function getAvatarText(name) { return name ? name.charAt(0).toUpperCase() : '?' }

  function getRoleText(role) { return roleMap[role] || role || '店员' }

  function showAddDialog() {
    form.value = { name: '', phone: '', username: '', password: '' }
    dialogVisible.value = true
  }

  function closeDialog() { dialogVisible.value = false }

  function validate() {
    if (!form.value.name.trim()) { ElMessage.warning(T.NAME_REQUIRED); return false }
    if (!form.value.phone.trim()) { ElMessage.warning(T.PHONE_REQUIRED); return false }
    if (!form.value.username.trim()) { ElMessage.warning(T.USERNAME_REQUIRED); return false }
    if (!form.value.password.trim()) { ElMessage.warning(T.PASSWORD_REQUIRED); return false }
    return true
  }

  async function handleSubmit() {
    if (!validate()) return
    submitting.value = true
    try {
      const res = await registerEmployee(shopId.value, { ...form.value })
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_ADD)
        closeDialog()
        await loadEmployees()
      } else {
        ElMessage.error(res?.message || '添加失败')
      }
    } catch (e) {
      ElMessage.error('添加失败')
    } finally { submitting.value = false }
  }

  async function handleRemove(emp) {
    try { await ElMessageBox.confirm(T.CONFIRM_REMOVE, { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }) } catch { return }
    try {
      const res = await removeEmployee(shopId.value, emp.merchantId)
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_REMOVE)
        await loadEmployees()
      } else {
        ElMessage.error(res?.message || '移除失败')
      }
    } catch (e) {
      ElMessage.error('移除失败')
    }
  }

  onMounted(() => { loadShopInfo(); loadEmployees() })

  return { T, shopInfo, employees, loading, dialogVisible, submitting, form, getAvatarText, getRoleText, showAddDialog, closeDialog, handleSubmit, handleRemove, loadEmployees }
}
