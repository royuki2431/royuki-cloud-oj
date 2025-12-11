import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, RegisterRequest } from '@/types'
import { UserRole, UserRoleLevel } from '@/types'
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

    // 计算属性：用户角色
    const userRole = computed<UserRole | null>(() => userInfo.value?.role || null)

    // 计算属性：是否为学生
    const isStudent = computed(() => userRole.value === UserRole.STUDENT)

    // 计算属性：是否为教师
    const isTeacher = computed(() => userRole.value === UserRole.TEACHER)

    // 计算属性：是否为管理员
    const isAdmin = computed(() => userRole.value === UserRole.ADMIN)

    // 检查是否有指定角色
    const hasRole = (role: UserRole): boolean => {
        return userRole.value === role
    }

    // 检查是否有任一角色
    const hasAnyRole = (roles: UserRole[]): boolean => {
        return roles.includes(userRole.value as UserRole)
    }

    // 检查是否至少拥有指定权限等级
    const hasMinRole = (role: UserRole): boolean => {
        if (!userRole.value) return false
        return UserRoleLevel[userRole.value] >= UserRoleLevel[role]
    }

    // 检查是否可以访问某个功能
    const canAccess = (requiredRole?: UserRole): boolean => {
        if (!requiredRole) return true // 无权限要求
        if (!isLoggedIn.value) return false // 未登录
        return hasMinRole(requiredRole) // 检查权限等级
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        userRole,
        isStudent,
        isTeacher,
        isAdmin,
        login,
        register,
        logout,
        loadUserInfo,
        initUser,
        hasRole,
        hasAnyRole,
        hasMinRole,
        canAccess,
    }
})
