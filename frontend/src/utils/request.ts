import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

// 创建axios实例
const service: AxiosInstance = axios.create({
    baseURL: '/api',
    timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // 从localStorage获取token
        const token = localStorage.getItem('token')
        if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        console.error('Request Error:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
        const res = response.data as any
        const { code, message, data } = res

        if (code === 200) {
            // 检查是否是分页响应（包含 total 字段）
            if ('total' in res && 'current' in res) {
                // 分页响应，返回完整对象
                return {
                    data: data,
                    total: res.total,
                    current: res.current,
                    size: res.size,
                    pages: res.pages
                }
            }
            return data
        } else {
            ElMessage.error(message || '请求失败')
            return Promise.reject(new Error(message || '请求失败'))
        }
    },
    (error) => {
        console.error('Response Error:', error)

        // 处理不同的HTTP状态码
        if (error.response) {
            const status = error.response.status
            const responseData = error.response.data
            const message = responseData?.message || error.message
            const errorInfo = responseData?.data // 错误详情

            switch (status) {
                case 400:
                    ElMessage.error(`请求错误: ${message}`)
                    break
                case 401:
                    ElMessage.error('未登录或登录已过期，请重新登录')
                    // 可以在这里添加跳转到登录页的逻辑
                    localStorage.removeItem('token')
                    break
                case 403:
                    ElMessage.error('无权限访问')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 429:
                    // 限流错误，显示详细提示
                    const retryAfter = errorInfo?.retryAfter || 3
                    const hint = errorInfo?.hint || '请求过于频繁'
                    ElMessage.warning({
                        message: `${hint}，请${retryAfter}秒后重试`,
                        duration: retryAfter * 1000
                    })
                    break
                case 503:
                    // 服务降级错误
                    const degradeHint = errorInfo?.hint || '服务暂时不可用'
                    const degradeRetry = errorInfo?.retryAfter || 10
                    ElMessage.error({
                        message: `${degradeHint}，请${degradeRetry}秒后重试`,
                        duration: 5000
                    })
                    break
                case 500:
                    ElMessage.error('服务器错误，请稍后重试')
                    break
                default:
                    ElMessage.error(message || '网络错误')
            }

            // 返回包含错误详情的错误对象
            const enhancedError = new Error(message) as any
            enhancedError.code = status
            enhancedError.errorInfo = errorInfo
            return Promise.reject(enhancedError)
        } else if (error.request) {
            // 请求已发送但没有收到响应
            ElMessage.error('网络连接失败，请检查网络设置')
            return Promise.reject(new Error('网络连接失败'))
        } else {
            // 其他错误
            ElMessage.error(error.message || '请求失败')
            return Promise.reject(error)
        }
    }
)

export default service
