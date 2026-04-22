import axios from 'axios'
import Swal from 'sweetalert2'

export const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000
})

// 请求拦截器：自动添加 Sa-Token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('satoken')
        if (token) {
            config.headers['satoken'] = token
        }
        return config
    },
    error => Promise.reject(error)
)

// 响应拦截器：处理 401 未授权
request.interceptors.response.use(
    response => {
        // 返回数据结构: { message, token, userInfo, ... }
        return response.data
    },
    error => {
        if (error.response?.status === 401) {
            // Token 无效或过期，显示弹窗后跳转到登录页
            localStorage.removeItem('satoken')
            localStorage.removeItem('userInfo')
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
        // 包装错误，保留响应数据
        if (error.response?.data) {
            error.message = error.response.data.message || error.message
        }
        return Promise.reject(error)
    }
)
