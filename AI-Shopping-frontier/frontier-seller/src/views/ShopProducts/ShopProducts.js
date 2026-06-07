import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShopDetail } from '@/api/shop'
import { getProductsByShop, createProduct, updateProduct, deleteProduct, listProduct, unlistProduct, getProductById } from '@/api/product'
import * as T from './Text.js'

export function useShopProducts() {
  const route = useRoute()
  const shopId = computed(() => route.params.shopId)
  const shopInfo = ref(null)
  const products = ref([])
  const loading = ref(false)
  const searchKeyword = ref('')

  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const editingProductId = ref(null)

  const detailVisible = ref(false)
  const selectedProduct = ref(null)

  const form = ref({
    name: '', description: '', price: '', stock: '', image: null
  })

  const filteredProducts = computed(() => {
    if (!searchKeyword.value.trim()) return products.value
    const kw = searchKeyword.value.trim().toLowerCase()
    return products.value.filter(p => p.name?.toLowerCase().includes(kw))
  })

  async function loadShopInfo() {
    try {
      const res = await getShopDetail(shopId.value)
      if (res?.data) shopInfo.value = res.data
    } catch (e) { console.error(e) }
  }

  async function loadProducts() {
    loading.value = true
    try {
      const res = await getProductsByShop(shopId.value)
      products.value = (res?.data || []).map(p => ({
        ...p,
        id: p.id,
        productId: p.productId || p.id,
        shopId: p.shopId
      }))
    } catch (e) {
      ElMessage.error('加载失败')
      products.value = []
    } finally {
      loading.value = false
    }
  }

  async function showDetail(product) {
    try {
      const res = await getProductById(product.id || product.productId)
      const detail = res?.data
      if (detail) {
        
        
      }
      selectedProduct.value = detail || product
    } catch {
      selectedProduct.value = product
    }
    detailVisible.value = true
  }

  function closeDetail() {
    detailVisible.value = false
    selectedProduct.value = null
  }

  async function handleEditFromDetail(product) {
    closeDetail()
    showEditDialog(product)
  }

  async function handleToggleSaleFromDetail(product) {
    closeDetail()
    await handleToggleSale(product)
  }

  async function handleDeleteFromDetail(product) {
    closeDetail()
    await handleDelete(product)
  }

  function showAddDialog() {
    isEdit.value = false
    editingProductId.value = null
    form.value = { name: '', description: '', price: '', stock: '', image: null }
    dialogVisible.value = true
  }

  function showEditDialog(product) {
    isEdit.value = true
    editingProductId.value = product.id || product.productId
    form.value = {
      name: product.name || '',
      description: product.description || '',
      price: product.price || '',
      stock: product.stock || 0,
      image: null
    }
    dialogVisible.value = true
  }

  function closeDialog() { dialogVisible.value = false }

  function handleFileChange(e) {
    if (e.target.files.length > 0) form.value.image = e.target.files[0]
  }

  function validate() {
    if (!form.value.name.trim()) { ElMessage.warning(T.NAME_REQUIRED); return false }
    if (!form.value.price || form.value.price <= 0) { ElMessage.warning(T.PRICE_REQUIRED); return false }
    if (form.value.stock === '' || form.value.stock < 0) { ElMessage.warning(T.STOCK_REQUIRED); return false }
    return true
  }

  async function handleSubmit() {
    if (!validate()) return
    submitting.value = true
    try {
      let res
      if (isEdit.value) {
        const fd = new FormData()
        fd.append('product', new Blob([JSON.stringify({
          name: form.value.name.trim(),
          description: form.value.description.trim(),
          price: parseFloat(form.value.price),
          stock: parseInt(form.value.stock)
        })], { type: 'application/json' }))
        if (form.value.image) fd.append('image', form.value.image)
        res = await updateProduct(editingProductId.value, fd)
      } else {
        const fd = new FormData()
        fd.append('product', new Blob([JSON.stringify({
          name: form.value.name.trim(),
          description: form.value.description.trim(),
          price: parseFloat(form.value.price),
          stock: parseInt(form.value.stock),
          shopId: shopId.value
        })], { type: 'application/json' }))
        if (form.value.image) fd.append('image', form.value.image)
        res = await createProduct(fd)
      }
      if (res?.message?.includes('成功')) {
        ElMessage.success(isEdit.value ? T.SUCCESS_EDIT : T.SUCCESS_ADD)
        closeDialog()
        await loadProducts()
      } else {
        ElMessage.error(res?.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  }

  async function handleToggleSale(product) {
    try {
      if (product.isSale) {
        await unlistProduct(product.id || product.productId)
      } else {
        await listProduct(product.id || product.productId)
      }
      ElMessage.success(product.isSale ? T.UNLIST_SUCCESS : T.LIST_SUCCESS)
      await loadProducts()
    } catch (e) {
      ElMessage.error(T.OPERATION_FAILED)
    }
  }

  async function handleDelete(product) {
    try {
      await ElMessageBox.confirm(T.CONFIRM_DELETE, { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    } catch { return }
    try {
      const res = await deleteProduct(product.id || product.productId)
      if (res?.message?.includes('成功')) {
        ElMessage.success(T.SUCCESS_DELETE)
        await loadProducts()
      } else {
        ElMessage.error(res?.message || '删除失败')
      }
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }

  onMounted(() => { loadShopInfo(); loadProducts() })

  return {
    T, shopInfo, products, loading, searchKeyword, filteredProducts,
    detailVisible, selectedProduct,
    dialogVisible, isEdit, submitting, form,
    showAddDialog, showEditDialog, closeDialog, handleFileChange, handleSubmit,
    handleToggleSale, handleDelete, loadProducts,
    showDetail, closeDetail, handleEditFromDetail, handleToggleSaleFromDetail, handleDeleteFromDetail
  }
}
