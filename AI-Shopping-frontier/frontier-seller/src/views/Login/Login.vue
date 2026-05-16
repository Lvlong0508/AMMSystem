<!-- src/views/Login/Login.vue -->
<template>
  <div class="auth-page merchant">
    <div class="auth-card">
      <div class="auth-header">
        <span class="logo">🏪</span>
        <h2>商家登录</h2>
        <p class="subtitle">AI-Mart 商家服务平台</p>
      </div>

      <div class="form-container">
        <!-- 用户名 -->
        <div class="form-item">
          <label>商家账号 *</label>
          <input
            v-model="form.username"
            placeholder="请输入商家账号"
            @blur="validateUsername"
            :class="{ error: errors.username }"
          />
          <span v-if="errors.username" class="field-hint error">{{ errors.username }}</span>
        </div>

        <!-- 密码 -->
        <div class="form-item">
          <label>密码 *</label>
          <div class="password-input">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
            />
            <span class="toggle-eye" @click="showPassword = !showPassword">
              {{ showPassword ? '🙈' : '🙉' }}
            </span>
          </div>
          <span v-if="errors.password" class="field-hint error">{{ errors.password }}</span>
        </div>

        <!-- 提交按钮 -->
        <button
          class="submit-btn"
          @click="handleLogin"
          :disabled="loading || !isFormValid"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <p class="hint-text">
          测试账号：merchant001 / 123456
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showError } from '../../utils/swal.js'
import { merchantLogin } from '../../api/auth'

const router = useRouter()

// 表单状态
const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const errors = reactive({
  username: '',
  password: ''
})

// 验证规则
const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,20}$/

// 计算表单是否有效
const isFormValid = computed(() => {
  return form.username && form.password &&
         USERNAME_PATTERN.test(form.username)
})

// 验证用户名
const validateUsername = () => {
  errors.username = ''

  if (!form.username) {
    errors.username = '账号不能为空'
    return
  }

  if (!USERNAME_PATTERN.test(form.username)) {
    errors.username = '账号格式不正确'
  }
}

// 提交表单
const handleLogin = async () => {
  validateUsername()

  if (!isFormValid.value) return

  loading.value = true
  try {
    const res = await merchantLogin({
      username: form.username,
      password: form.password
    })

    if (res.token) {
      // 保存 Sa-Token
      localStorage.setItem('satoken', res.token)
      localStorage.setItem('merchantInfo', JSON.stringify(res.merchantInfo))
      sessionStorage.setItem('needReload', '1')
      router.push('/')
    } else {
      showError(res.message || '登录失败')
    }
  } catch (e) {
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
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-page.merchant {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
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
  border-color: #f59e0b;
}

.form-item input.error {
  border-color: #f56c6c;
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

.submit-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
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
  box-shadow: 0 8px 20px rgba(245, 158, 11, 0.4);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hint-text {
  text-align: center;
  margin-top: 20px;
  color: #999;
  font-size: 12px;
}
</style>
