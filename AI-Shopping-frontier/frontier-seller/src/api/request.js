import axios from 'axios'
import Swal from 'sweetalert2'

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
        // 优先使用 merchantInfo（商家端）
        const merchantInfo = localStorage.getItem('merchantInfo')
        if (merchantInfo) {
            try {
                const merchant = JSON.parse(merchantInfo)
                if (merchant.id) {
                    config.headers['X-User-Id'] = merchant.id
                }
            } catch (e) {
                // ignore
            }
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
            Swal.fire({
                title: '未登录',
                text: '请先登录',
                icon: 'warning',
                confirmButtonText: '去登录',
                allowOutsideClick: false
            }).then(() => {
                window.location.href = '/login'
            })
        }
        return Promise.reject(error)
    }
)
