import { ref, reactive } from "vue"
import { useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { merchantRegister } from "@/api/auth"
import { createShop } from "@/api/shop"
import { useAuthStore } from "@/store/auth"
import { useShopStore } from "@/store/shop"
import * as T from "./Text.js"
import { buildAddressString } from "@/utils/region"

export function useRegister() {
  const router = useRouter()
  const authStore = useAuthStore()
  const shopStore = useShopStore()

  const isAlreadyLoggedIn = !!authStore.token
  const currentStep = ref(isAlreadyLoggedIn ? 1 : 0)
  const registeredUsername = ref(
    isAlreadyLoggedIn
      ? (authStore.merchantInfo?.username || "")
      : ""
  )
  const accountFormRef = ref(null)
  const shopFormRef = ref(null)
  const logoInputRef = ref(null)
  const submittingAccount = ref(false)
  const submittingShop = ref(false)
  const logoFile = ref(null)

  const accountForm = reactive({ username: "", phone: "", email: "", password: "", confirmPassword: "" })

  const validateConfirm = (_rule, value, callback) => {
    if (value !== accountForm.password) {
      callback(new Error(T.CONFIRM_MISMATCH))
    } else {
      callback()
    }
  }

  const accountRules = {
    username: [
      { required: true, message: T.USERNAME_REQUIRED, trigger: "blur" },
      { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: T.USERNAME_INVALID, trigger: "blur" }
    ],
    phone: [
      { required: true, message: T.PHONE_REQUIRED, trigger: "blur" },
      { pattern: /^1\d{10}$/, message: T.PHONE_INVALID, trigger: "blur" }
    ],
    password: [{ required: true, message: T.PASSWORD_REQUIRED, trigger: "blur" }],
    confirmPassword: [
      { required: true, message: T.CONFIRM_REQUIRED, trigger: "blur" },
      { validator: validateConfirm, trigger: "blur" }
    ]
  }

  const shopForm = reactive({ name: "", description: "", region: [], addressDetail: "", phone: "" })

  const shopRules = {
    name: [
      { required: true, message: T.SHOP_NAME_REQUIRED, trigger: "blur" },
      { max: 50, message: T.SHOP_NAME_MAX, trigger: "blur" }
    ],
    description: [{ max: 200, message: T.SHOP_DESC_MAX, trigger: "blur" }],
    region: [{ required: true, message: T.REGION_REQUIRED, trigger: "change" }],
    addressDetail: [{ max: 200, message: T.SHOP_ADDRESS_MAX, trigger: "blur" }],
    phone: [
      { max: 20, message: T.SHOP_PHONE_MAX, trigger: "blur" },
      { pattern: /^$|^(1\d{10}|0\d{2,3}-?\d{7,8})$/, message: T.SHOP_PHONE_INVALID, trigger: "blur" }
    ]
  }

  async function handleNext() {
    if (!accountFormRef.value) return
    const valid = await accountFormRef.value.validate().catch(() => false)
    if (!valid) return

    submittingAccount.value = true
    try {
      const res = await merchantRegister({
        username: accountForm.username,
        phone: accountForm.phone,
        email: accountForm.email || undefined,
        password: accountForm.password
      })
      if (res.data?.token) {
        authStore.token = res.data.token
        authStore.merchantInfo = res.data.merchantInfo
        authStore.merchantId = res.data?.merchantInfo?.id
        localStorage.setItem("satoken", res.data.token)
        localStorage.setItem("merchantInfo", JSON.stringify(res.data.merchantInfo))
        localStorage.setItem("merchantId", authStore.merchantId)
        registeredUsername.value = accountForm.username
        ElMessage.success(T.SUCCESS_ACCOUNT)
        currentStep.value = 1
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || T.ERROR_ACCOUNT
      ElMessage.error(msg)
    } finally {
      submittingAccount.value = false
    }
  }

  function handlePrev() {
    if (isAlreadyLoggedIn) return
    currentStep.value = 0
  }

  function handleLogoChange(event) {
    const file = event.target.files?.[0]
    if (!file) {
      logoFile.value = null
      return
    }
    if (!["image/jpeg", "image/png"].includes(file.type)) {
      ElMessage.warning(T.SHOP_LOGO_TYPE_INVALID)
      clearLogo()
      return
    }
    logoFile.value = file
  }

  function clearLogo() {
    logoFile.value = null
    if (logoInputRef.value) {
      logoInputRef.value.value = ""
    }
  }

  function buildShopFormData() {
    const formData = new FormData()
    const shopData = {
      name: shopForm.name,
      description: shopForm.description || undefined,
      address: buildAddressString(shopForm.region, shopForm.addressDetail) || undefined,
      phone: shopForm.phone || undefined
    }
    formData.append("shop", new Blob([JSON.stringify(shopData)], { type: "application/json" }))
    if (logoFile.value) {
      formData.append("logo", logoFile.value)
    }
    return formData
  }

  async function handleSubmitShop() {
    if (!shopFormRef.value) return
    const valid = await shopFormRef.value.validate().catch(() => false)
    if (!valid) return

    submittingShop.value = true
    try {
      await createShop(buildShopFormData())
      await shopStore.initShop(authStore.merchantId)
      ElMessage.success(T.SUCCESS_SHOP)
      currentStep.value = 2
      setTimeout(() => router.push(`/shop/${shopStore.currentShopId}`), 1500)
    } catch (err) {
      const msg = err.response?.data?.message || err.message || T.ERROR_SHOP
      ElMessage.error(msg)
    } finally {
      submittingShop.value = false
    }
  }

  return {
    T, currentStep,
    accountFormRef, accountForm, accountRules,
    shopFormRef, shopForm, shopRules,
    logoInputRef, logoFile,
    submittingAccount, submittingShop, registeredUsername,
    handleNext, handlePrev, handleLogoChange, clearLogo, handleSubmitShop
  }
}
