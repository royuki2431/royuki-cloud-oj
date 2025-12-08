import request from '@/utils/request'
import type { SubmitCodeRequest, Submission, JudgeResult } from '@/types'

// 提交代码
export function submitCode(data: SubmitCodeRequest) {
    return request.post<any, number>('/judge/submit', data)
}

// 查询评测结果
export function getJudgeResult(submissionId: number) {
    return request.get<any, JudgeResult>(`/judge/result/${submissionId}`)
}

// 查询提交记录
export function getSubmission(submissionId: number) {
    return request.get<any, Submission>(`/judge/submission/${submissionId}`)
}

// 分页查询用户提交
export function getUserSubmissions(params: { userId: number; page: number; size: number }) {
    return request.get<any, Submission[]>(`/judge/submissions/user/${params.userId}`, {
        params: { page: params.page, size: params.size }
    })
}

// 分页查询题目提交
export function getProblemSubmissions(problemId: number, params: { page: number; size: number }) {
    return request.get<any, Submission[]>(`/judge/submissions/problem/${problemId}`, { params })
}

// 重新评测
export function rejudge(submissionId: number) {
    return request.post<any, void>(`/judge/rejudge/${submissionId}`)
}
