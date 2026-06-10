import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShopDetail, updateShop, closeShop, openShop } from '@/api/shop'
import { parseAddress, buildAddressString } from '@/utils/region'
import * as T from './Text.js'

export function useShopInfo() {
  const route = useRoute()
  const shopId = route.params.shopId
  const loading = ref(false)
  const saving = ref(false)
  const toggling = ref(false)
  const shopStatus = ref(null)
  const logoInputRef = ref(null)
  const logoFile = ref(null)
  const logoPreview = ref('')
  const dialogVisible = ref(false)
  const editFormRef = ref(null)
  const editForm = reactive({
    name: '',
    description: '',
    phone: '',
    region: [],
    addressDetail: ''
  })
  const editLogoFile = ref(null)
  const editLogoPreview = ref('')
  const editLogoInputRef = ref(null)

  const form = reactive({
    name: '',
    description: '',
    phone: '',
    region: [],
    addressDetail: ''
  })

  const displayAddress = computed(() => buildAddressString(form.region, form.addressDetail) || '-')

  const editRules = {
    name: [
      { required: true, message: '店铺名称不能为空', trigger: 'blur' },
      { max: 20, message: '店铺名称不能超过20个字', trigger: 'blur' }
    ],
    description: [
      { max: 500, message: '店铺简介不能超过500个字', trigger: 'blur' }
    ],
    phone: [
      { pattern: /^$|^1[3-9]\d{9}$/, message: '请输入11位标准手机号', trigger: 'blur' }
    ],
    addressDetail: [
      { max: 200, message: '店铺地址不能超过200个字', trigger: 'blur' }
    ]
  }

  const ALLOWED_EXTENSIONS = ['jpg', 'png']

  function getExtension(filename) {
    return filename.split('.').pop()?.toLowerCase()
  }

  function handleLogoChange(e) {
    const file = e.target.files[0]
    if (!file) {
      logoFile.value = null
      return
    }
    const ext = getExtension(file.name)
    if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
      ElMessage.warning(T.LOGO_TYPE_INVALID)
      clearLogo()
      return
    }
    logoFile.value = file
  }

  function clearLogo() {
    logoFile.value = null
    if (logoInputRef.value) {
      logoInputRef.value.value = ''
    }
  }

  function revokeEditLogoPreview() {
    if (editLogoPreview.value) {
      URL.revokeObjectURL(editLogoPreview.value)
      editLogoPreview.value = ''
    }
  }

  function openEditDialog() {
    editForm.name = form.name
    editForm.description = form.description
    editForm.phone = form.phone
    editForm.region = [...form.region]
    editForm.addressDetail = form.addressDetail
    editLogoFile.value = null
    revokeEditLogoPreview()
    dialogVisible.value = true
  }

  function handleEditLogoChange(e) {
    const file = e.target.files[0]
    if (!file) {
      clearEditLogo()
      return
    }
    const ext = getExtension(file.name)
    if (!ext || !ALLOWED_EXTENSIONS.includes(ext)) {
      ElMessage.warning(T.LOGO_TYPE_INVALID)
      clearEditLogo()
      return
    }
    revokeEditLogoPreview()
    editLogoFile.value = file
    editLogoPreview.value = URL.createObjectURL(file)
  }

  function clearEditLogo() {
    editLogoFile.value = null
    revokeEditLogoPreview()
    if (editLogoInputRef.value) {
      editLogoInputRef.value.value = ''
    }
  }

  async function loadShopInfo() {
    loading.value = true
    try {
      const res = await getShopDetail(shopId)
      const shopData = res?.data?.shop || res?.shop || {}
      const shopInfo = res?.data?.shopInfo || res?.shopInfo || {}
      const shop = { ...shopData, ...shopInfo }
      form.name = shop.name || ''
      form.description = shop.description || ''
      form.phone = shop.phone || ''
      const parsed = parseAddress(shop.address || '')
      form.region = parsed.region
      form.addressDetail = parsed.detail
      logoPreview.value = shop.logourl || ''
      shopStatus.value = shop.status
    } catch (e) {
      console.error('加载商店信息失败:', e)
      ElMessage.error(T.LOAD_FAILED)
    } finally {
      loading.value = false
    }
  }

  async function handleSave() {
    saving.value = true
    try {
      const fd = new FormData()
      const shopData = {
        name: form.name,
        description: form.description,
        phone: form.phone,
        address: buildAddressString(form.region, form.addressDetail)
      }
      fd.append('shop', new Blob([JSON.stringify(shopData)], { type: 'application/json' }))
      if (logoFile.value) {
        fd.append('logo', logoFile.value)
      }
      const res = await updateShop(shopId, fd)
      ElMessage.success(res?.message || T.SAVE_SUCCESS)
      if (logoFile.value) {
        logoPreview.value = ''
        logoFile.value = null
        if (logoInputRef.value) {
          logoInputRef.value.value = ''
        }
        loadShopInfo()
      }
    } catch (e) {
      ElMessage.error(T.SAVE_FAILED)
    } finally {
      saving.value = false
    }
  }

  async function handleEditSave() {
    if (editFormRef.value) {
      const valid = await editFormRef.value.validate().catch(() => false)
      if (!valid) return
    }
    saving.value = true
    try {
      const fd = new FormData()
      const shopData = {
        name: editForm.name,
        description: editForm.description,
        phone: editForm.phone,
        address: buildAddressString(editForm.region, editForm.addressDetail)
      }
      fd.append('shop', new Blob([JSON.stringify(shopData)], { type: 'application/json' }))
      if (editLogoFile.value) {
        fd.append('logo', editLogoFile.value)
      }
      const res = await updateShop(shopId, fd)
      ElMessage.success(res?.message || T.SAVE_SUCCESS)
      form.name = editForm.name
      form.description = editForm.description
      form.phone = editForm.phone
      form.region = [...editForm.region]
      form.addressDetail = editForm.addressDetail
      if (editLogoFile.value) {
        loadShopInfo()
      }
      clearEditLogo()
      dialogVisible.value = false
    } catch (e) {
      ElMessage.error(T.SAVE_FAILED)
    } finally {
      saving.value = false
    }
  }

  async function handleToggleStatus() {
    toggling.value = true
    try {
      if (shopStatus.value === 1) {
        await closeShop(shopId)
        shopStatus.value = 0
      } else {
        await openShop(shopId)
        shopStatus.value = 1
      }
      ElMessage.success(T.STATUS_TOGGLED)
    } catch (e) {
      ElMessage.error('操作失败')
    } finally {
      toggling.value = false
    }
  }

  onMounted(loadShopInfo)

  return {
    T, form, displayAddress, loading, saving, toggling, shopStatus, logoPreview,
    dialogVisible, editFormRef, editForm, editRules, editLogoFile, editLogoPreview, editLogoInputRef,
    openEditDialog, handleEditSave, handleEditLogoChange, clearEditLogo,
    handleToggleStatus
  }
}
