import axios from 'axios'

export const request = axios.create({
    baseURL: '/api',
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
    response => response.data,
    error => {
        if (error.response?.status === 401) {
            // Token 无效或过期，清除登录状态并跳转到登录页
            localStorage.removeItem('satoken')
            localStorage.removeItem('merchantInfo')
            window.location.href = '/login'
        }
        return Promise.reject(error)
    }
)
