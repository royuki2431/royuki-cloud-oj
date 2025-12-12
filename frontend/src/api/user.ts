import request from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest, User } from '@/types'

// 用户登录
export function login(data: LoginRequest) {
    return request.post<any, LoginResponse>('/user/login', data)
}

// 用户注册
export function register(data: RegisterRequest) {
    return request.post<any, number>('/user/register', data)
}

// 获取用户信息
export function getUserInfo(userId: number) {
    return request.get<any, User>(`/user/info/${userId}`)
}

// 登出（前端清除token）
export function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
}

// 更新用户信息
export interface UpdateUserRequest {
    id: number
    realName?: string
    email?: string
    phone?: string
    school?: string
    studentId?: string
    grade?: string
    major?: string
    avatar?: string
}

export function updateUserInfo(data: UpdateUserRequest) {
    return request.put<any, void>('/user/info', data)
}

// 修改密码
export interface ChangePasswordRequest {
    userId: number
    oldPassword: string
    newPassword: string
}

export function changeUserPassword(data: ChangePasswordRequest) {
    return request.put<any, void>('/user/password', data)
}
