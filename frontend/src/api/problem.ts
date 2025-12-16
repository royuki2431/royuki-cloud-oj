import request from '@/utils/request'
import type { Problem, TestCase } from '@/types'

// 获取题目列表
export interface ProblemListParams {
    page?: number
    pageNum?: number
    pageSize?: number
    difficulty?: string
    keyword?: string
    tagId?: number
}

export interface ProblemListResponse {
    records: Problem[]
    list?: Problem[]
    total: number
    pageNum?: number
    pageSize?: number
}

export function getProblemList(params: ProblemListParams) {
    return request.get<any, ProblemListResponse>('/problem/list', { params })
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

// 获取题目的样例测试用例
export function getSampleTestCases(id: number) {
    return request.get<any, TestCase[]>(`/problem/${id}/samples`)
}
