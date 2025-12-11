import request from '@/utils/request'
import type { User, Problem } from '@/types'

// ==================== 用户管理API ====================

export interface UserListParams {
    keyword?: string
    role?: string
    status?: number | string
    page: number
    size: number
}

export interface UserListResponse {
    list: User[]
    total: number
}

export interface CreateUserRequest {
    username: string
    email: string
    password: string
    realName?: string
    role: string
    school?: string
    studentId?: string
    major?: string
    status: number
}

export interface UpdateUserRequest {
    email: string
    realName?: string
    role: string
    school?: string
    studentId?: string
    major?: string
    status: number
}

// 获取用户列表
export function getUserList(params: UserListParams) {
    return request.get<any, UserListResponse>('/user/admin/list', { params })
}

// 创建用户
export function createUser(data: CreateUserRequest) {
    return request.post<any, number>('/user/admin/create', data)
}

// 更新用户
export function updateUser(userId: number, data: UpdateUserRequest) {
    return request.put<any, boolean>(`/user/admin/update/${userId}`, data)
}

// 更新用户状态
export function updateUserStatus(userId: number, status: number) {
    return request.put<any, boolean>(`/user/admin/status/${userId}`, { status })
}

// 重置用户密码
export function resetUserPassword(userId: number) {
    return request.put<any, boolean>(`/user/admin/reset-password/${userId}`)
}

// 删除用户
export function deleteUser(userId: number) {
    return request.delete<any, boolean>(`/user/admin/delete/${userId}`)
}

// ==================== 题目管理API ====================

export interface ProblemListParams {
    keyword?: string
    difficulty?: string
    category?: string
    status?: number | string
    page: number
    size: number
}

export interface ProblemListResponse {
    list: Problem[]
    total: number
}

export interface CreateProblemRequest {
    title: string
    difficulty: string
    category?: string
    tags?: string[]
    description: string
    inputFormat?: string
    outputFormat?: string
    sampleInput?: string
    sampleOutput?: string
    hint?: string
    timeLimit: number
    memoryLimit: number
    status: number
}

export interface UpdateProblemRequest {
    title: string
    difficulty: string
    category?: string
    tags?: string[]
    description: string
    inputFormat?: string
    outputFormat?: string
    sampleInput?: string
    sampleOutput?: string
    hint?: string
    timeLimit: number
    memoryLimit: number
    status: number
}

// 获取题目列表（管理员）
export function getProblemListAdmin(params: ProblemListParams) {
    return request.get<any, ProblemListResponse>('/problem/admin/list', { params })
}

// 创建题目
export function createProblem(data: CreateProblemRequest) {
    return request.post<any, number>('/problem/admin/create', data)
}

// 更新题目
export function updateProblem(problemId: number, data: UpdateProblemRequest) {
    return request.put<any, boolean>(`/problem/admin/update/${problemId}`, data)
}

// 更新题目状态
export function updateProblemStatus(problemId: number, status: number) {
    return request.put<any, boolean>(`/problem/admin/status/${problemId}`, { status })
}

// 删除题目
export function deleteProblem(problemId: number) {
    return request.delete<any, boolean>(`/problem/admin/delete/${problemId}`)
}

// ==================== 测试用例管理API ====================

export interface TestCaseItem {
    id?: number
    problemId?: number
    input: string
    output: string
    isSample: number  // 1-样例 0-非样例
    score: number
    orderNum: number
}

// 获取题目的所有测试用例
export function getTestCases(problemId: number) {
    return request.get<any, TestCaseItem[]>(`/problem/${problemId}/testcases`)
}

// 批量保存测试用例
export function saveTestCases(problemId: number, testCases: TestCaseItem[]) {
    return request.post<any, void>(`/problem/${problemId}/testcases`, testCases)
}

// 获取测试用例数量
export function getTestCaseCount(problemId: number) {
    return request.get<any, number>(`/problem/${problemId}/testcases/count`)
}
