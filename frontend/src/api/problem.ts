import request from '@/utils/request'
import type { Problem } from '@/types'

// 获取题目列表
export function getProblemList(params: { pageNum: number; pageSize: number }) {
    return request.get<any, { list: Problem[]; total: number; pageNum: number; pageSize: number }>('/problem/list', { params })
}

// 获取题目详情
export function getProblemById(id: number) {
    return request.get<any, Problem>(`/problem/${id}`)
}

// 根据难度查询题目
export function getProblemsByDifficulty(
    difficulty: string,
    params: { pageNum: number; pageSize: number }
) {
    return request.get<any, Problem[]>(`/problem/difficulty/${difficulty}`, { params })
}

// 搜索题目
export function searchProblems(params: { title: string; pageNum: number; pageSize: number }) {
    return request.get<any, Problem[]>('/problem/search', { params })
}

// 获取题目总数
export function getProblemCount() {
    return request.get<any, number>('/problem/count')
}
