import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { User, LoginRequest, RegisterRequest } from '@/types'
import { login as loginApi, register as registerApi, getUserInfo, logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
    const token = ref<string>(localStorage.getItem('token') || '')
    const userInfo = ref<User | null>(null)
    const isLoggedIn = ref<boolean>(!!token.value)

    // 登录
    const login = async (data: LoginRequest) => {
        const response = await loginApi(data)
        token.value = response.token
        localStorage.setItem('token', response.token)
        localStorage.setItem('userId', response.userId.toString())
        isLoggedIn.value = true

        // 获取用户详细信息
        await loadUserInfo(response.userId)

        return response
    }

    // 注册
    const register = async (data: RegisterRequest) => {
        const userId = await registerApi(data)
        return userId
    }

    // 加载用户信息
    const loadUserInfo = async (userId?: number) => {
        const uid = userId || Number(localStorage.getItem('userId'))
        if (uid) {
            userInfo.value = await getUserInfo(uid)
            localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        }
    }

    // 登出
    const logout = () => {
        token.value = ''
        userInfo.value = null
        isLoggedIn.value = false
        logoutApi()
    }

    // 初始化时从localStorage恢复用户信息
    const initUser = () => {
        const storedUserInfo = localStorage.getItem('userInfo')
        if (storedUserInfo) {
            try {
                userInfo.value = JSON.parse(storedUserInfo)
                isLoggedIn.value = true
            } catch (e) {
                console.error('Failed to parse userInfo', e)
            }
        }
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        login,
        register,
        logout,
        loadUserInfo,
        initUser,
    }
})
