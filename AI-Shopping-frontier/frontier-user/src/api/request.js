import axios from 'axios'
import Swal from 'sweetalert2'

export const request = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 30000
})

// 请求拦截器：自动添加 Sa-Token 和用户ID
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('satoken')
        if (token) {
            config.headers['satoken'] = token
        }
        const userInfo = localStorage.getItem('userInfo')
        if (userInfo) {
            try {
                const user = JSON.parse(userInfo)
                if (user.id) {
                    config.headers['X-User-Id'] = user.id
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
    response => {
        const body = response.data
        // 解包 ApiResponse: { code, message, data } → 直接返回 data 并带上 message
        if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
            if (body.code === 200 && body.data !== null && body.data !== undefined) {
                const data = body.data
                if (typeof data === 'object' && !Array.isArray(data) && data.message === undefined) {
                    data.message = body.message
                }
                return data
            }
            // 业务错误（code ≠ 200）
            return Promise.reject({ response: { data: { message: body.message || '请求失败' }, status: body.code } })
        }
        return body
    },
    error => {
        if (error.response?.status === 401) {
            localStorage.removeItem('satoken')
            localStorage.removeItem('userInfo')
            window.location.reload()
        }
        // 包装错误，保留响应数据
        if (error.response?.data) {
            error.message = error.response.data.message || error.message
        }
        return Promise.reject(error)
    }
)
