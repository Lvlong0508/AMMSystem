<!-- src/views/Login/Login.vue -->
<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <span class="logo">🛒</span>
        <h2>{{ isRegister ? '用户注册' : '用户登录' }}</h2>
        <p class="subtitle">AI-Mart 智能购物平台</p>
      </div>

      <div class="form-container">
        <!-- 用户名 -->
        <div class="form-item">
          <label>用户名 *</label>
          <input
            v-model="form.username"
            placeholder="3-20位字母数字下划线"
            @blur="validateUsername"
            :class="{ error: errors.username, success: validFields.username }"
          />
          <span class="field-hint" :class="{ error: errors.username }">
            {{ errors.username || (validFields.username ? '用户名可用' : '') }}
          </span>
        </div>

        <!-- 密码 -->
        <div class="form-item">
          <label>密码 *</label>
          <div class="password-input">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="6-20位，包含字母和数字"
              @input="checkPasswordStrength"
            />
            <span class="toggle-eye" @click="showPassword = !showPassword">
              {{ showPassword ? '🙈' : '🙉' }}
            </span>
          </div>
          <!-- 密码强度条 -->
          <div v-if="form.password" class="strength-bar">
            <div class="strength-track">
              <div class="strength-fill" :class="passwordStrengthClass" :style="{ width: passwordStrength + '%' }"></div>
            </div>
            <span class="strength-text">{{ passwordStrengthText }}</span>
          </div>
          <span v-if="errors.password" class="field-hint error">{{ errors.password }}</span>
        </div>

        <!-- 确认密码（仅注册） -->
        <div v-if="isRegister" class="form-item">
          <label>确认密码 *</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            @blur="validateConfirmPassword"
            :class="{ error: errors.confirmPassword }"
          />
          <span class="field-hint error">{{ errors.confirmPassword }}</span>
        </div>

        <!-- 昵称（仅注册） -->
        <div v-if="isRegister" class="form-item">
          <label>昵称</label>
          <input v-model="form.nickname" placeholder="选填，2-20个字符" />
        </div>

        <!-- 手机号（仅注册） -->
        <div v-if="isRegister" class="form-item">
          <label>手机号</label>
          <input
            v-model="form.phone"
            placeholder="选填，11位手机号"
            @blur="validatePhone"
            :class="{ error: errors.phone, success: validFields.phone }"
          />
          <span class="field-hint" :class="{ error: errors.phone }">
            {{ errors.phone || (validFields.phone ? '手机号可用' : '') }}
          </span>
        </div>

        <!-- 提交按钮 -->
        <button
          class="submit-btn"
          @click="handleSubmit"
          :disabled="loading || !isFormValid"
        >
          {{ loading ? '请稍候...' : (isRegister ? '注册' : '登录') }}
        </button>

        <!-- 切换模式 -->
        <p class="toggle-link">
          {{ isRegister ? '已有账号？' : '还没有账号？' }}
          <span @click="toggleMode">{{ isRegister ? '立即登录' : '立即注册' }}</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import { showSuccess, showError } from '../../utils/swal.js'
import { userLogin, userRegister, checkUsername, checkPhone } from '../../api/auth'

const router = useRouter()

// 表单状态
const isRegister = ref(false)
const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: ''
})

const errors = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

const validFields = reactive({
  username: false,
  phone: false
})

// 密码强度
const passwordStrength = ref(0)
const passwordStrengthClass = ref('')
const passwordStrengthText = ref('')

// 验证规则
const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{6,20}$/
const PHONE_PATTERN = /^1[3-9]\d{9}$/

// 计算表单是否有效
const isFormValid = computed(() => {
  if (!form.username || !form.password) return false
  if (isRegister.value) {
    if (!form.confirmPassword) return false
    if (form.password !== form.confirmPassword) return false
    if (!PASSWORD_PATTERN.test(form.password)) return false
  }
  return USERNAME_PATTERN.test(form.username)
})

// 检查密码强度
const checkPasswordStrength = () => {
  const pwd = form.password
  if (!pwd) {
    passwordStrength.value = 0
    return
  }

  let score = 0
  if (pwd.length >= 8) score += 25
  if (pwd.length >= 12) score += 25
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score += 25
  if (/\d/.test(pwd)) score += 25
  if (/[@$!%*#?&]/.test(pwd)) score += 10

  passwordStrength.value = Math.min(score, 100)

  if (score < 40) {
    passwordStrengthClass.value = 'weak'
    passwordStrengthText.value = '弱'
  } else if (score < 70) {
    passwordStrengthClass.value = 'medium'
    passwordStrengthText.value = '中'
  } else {
    passwordStrengthClass.value = 'strong'
    passwordStrengthText.value = '强'
  }
}

// 验证用户名
const validateUsername = async () => {
  errors.username = ''
  validFields.username = false

  if (!form.username) {
    errors.username = '用户名不能为空'
    return
  }

  if (!USERNAME_PATTERN.test(form.username)) {
    errors.username = '用户名需3-20位，只能包含字母、数字、下划线'
    return
  }

  // 检查唯一性（仅注册时）
  if (isRegister.value) {
    try {
      const res = await checkUsername(form.username)
      if (!res.available) {
        errors.username = res.message
        return
      }
      validFields.username = true
    } catch (e) {
      // 忽略检查失败
    }
  }
}

// 验证确认密码
const validateConfirmPassword = () => {
  errors.confirmPassword = ''
  if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次输入的密码不一致'
  }
}

// 验证手机号
const validatePhone = async () => {
  errors.phone = ''
  validFields.phone = false

  if (!form.phone) return

  if (!PHONE_PATTERN.test(form.phone)) {
    errors.phone = '请输入有效的11位手机号'
    return
  }

  // 检查唯一性（仅注册时）
  if (isRegister.value) {
    try {
      const res = await checkPhone(form.phone)
      if (!res.available) {
        errors.phone = res.message
        return
      }
      validFields.phone = true
    } catch (e) {
      // 忽略检查失败
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  // 验证
  await validateUsername()
  if (isRegister.value) {
    validateConfirmPassword()
    await validatePhone()
  }

  if (!isFormValid.value) return

  loading.value = true
  try {
    let res
    if (isRegister.value) {
      res = await userRegister({
        username: form.username,
        password: form.password,
        nickname: form.nickname,
        phone: form.phone
      })
    } else {
      res = await userLogin({
        username: form.username,
        password: form.password
      })
    }

    if (res.token) {
      // 保存 Sa-Token
      localStorage.setItem('satoken', res.token)
      localStorage.setItem('userInfo', JSON.stringify(res.userInfo))
      await showSuccess(res.message || '登录成功')
      sessionStorage.setItem('needReload', '1')
      router.push('/')
    } else {
      showError(res.message || '操作失败')
    }
  } catch (e) {
    // 显示详细错误信息
    let errorMsg = '网络错误，请稍后重试'
    if (e.response?.data?.message) {
      errorMsg = e.response.data.message
    } else if (e.message) {
      errorMsg = e.message
    }
    showError(errorMsg)
  } finally {
    loading.value = false
  }
}

// 切换登录/注册模式
const toggleMode = () => {
  isRegister.value = !isRegister.value
  // 重置表单
  Object.keys(form).forEach(key => form[key] = '')
  Object.keys(errors).forEach(key => errors[key] = '')
  Object.keys(validFields).forEach(key => validFields[key] = false)
  passwordStrength.value = 0
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 48px;
  display: block;
  margin-bottom: 10px;
}

.auth-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.subtitle {
  margin: 8px 0 0;
  color: #666;
  font-size: 14px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.form-item input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-item input:focus {
  outline: none;
  border-color: #667eea;
}

.form-item input.error {
  border-color: #f56c6c;
}

.form-item input.success {
  border-color: #67c23a;
}

.password-input {
  position: relative;
}

.password-input input {
  padding-right: 40px;
}

.toggle-eye {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  font-size: 16px;
  user-select: none;
}

.field-hint {
  font-size: 12px;
  color: #67c23a;
  margin-top: 4px;
  display: block;
}

.field-hint.error {
  color: #f56c6c;
}

.strength-bar {
  margin-top: 8px;
}

.strength-track {
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s;
}

.strength-fill.weak {
  background: #f56c6c;
}

.strength-fill.medium {
  background: #e6a23c;
}

.strength-fill.strong {
  background: #67c23a;
}

.strength-text {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  display: block;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
}

.toggle-link span {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}

.toggle-link span:hover {
  text-decoration: underline;
}
</style>
