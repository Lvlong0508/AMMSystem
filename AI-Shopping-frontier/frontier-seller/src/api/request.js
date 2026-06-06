import axios from 'axios'
import { ElMessageBox } from 'element-plus'

export const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000
})

// 请求拦截器：自动添加 Sa-Token、用户ID和角色
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('satoken')
        if (token) {
            config.headers['satoken'] = token
        }
        const merchantId = localStorage.getItem('merchantId')
        if (merchantId) {
            config.headers['X-User-Id'] = merchantId
        }
        // 添加角色信息
        const currentRole = localStorage.getItem('currentRole')
        if (currentRole) {
            try {
                const role = JSON.parse(currentRole)
                if (role.role) {
                    config.headers['X-Merchant-Role'] = role.role
                }
                if (role.shopId) {
                    config.headers['X-Shop-Id'] = role.shopId
                }
            } catch (e) {
                // ignore
            }
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器：处理 401 未授权
request.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response?.status === 401) {
            // Token 无效或过期，显示弹窗后跳转到登录页
            localStorage.removeItem('satoken')
            localStorage.removeItem('merchantInfo')
            ElMessageBox.alert('请先登录', '未登录', {
                confirmButtonText: '去登录',
                type: 'warning',
                closeOnClickModal: false,
                callback: () => { window.location.href = '/login' }
            })
        }
        return Promise.reject(error)
    }
)
