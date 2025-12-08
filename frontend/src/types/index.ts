// 评测状态枚举
export enum JudgeStatus {
    PENDING = 'PENDING',
    JUDGING = 'JUDGING',
    ACCEPTED = 'ACCEPTED',
    WRONG_ANSWER = 'WRONG_ANSWER',
    TIME_LIMIT_EXCEEDED = 'TIME_LIMIT_EXCEEDED',
    MEMORY_LIMIT_EXCEEDED = 'MEMORY_LIMIT_EXCEEDED',
    RUNTIME_ERROR = 'RUNTIME_ERROR',
    COMPILE_ERROR = 'COMPILE_ERROR',
    SYSTEM_ERROR = 'SYSTEM_ERROR',
}

// 状态文本映射
export const JudgeStatusText: Record<JudgeStatus, string> = {
    [JudgeStatus.PENDING]: '等待评测',
    [JudgeStatus.JUDGING]: '评测中',
    [JudgeStatus.ACCEPTED]: '通过',
    [JudgeStatus.WRONG_ANSWER]: '答案错误',
    [JudgeStatus.TIME_LIMIT_EXCEEDED]: '超时',
    [JudgeStatus.MEMORY_LIMIT_EXCEEDED]: '内存超限',
    [JudgeStatus.RUNTIME_ERROR]: '运行错误',
    [JudgeStatus.COMPILE_ERROR]: '编译错误',
    [JudgeStatus.SYSTEM_ERROR]: '系统错误',
}

// 状态标签类型
export const JudgeStatusType: Record<JudgeStatus, 'info' | 'warning' | 'success' | 'danger'> = {
    [JudgeStatus.PENDING]: 'info',
    [JudgeStatus.JUDGING]: 'warning',
    [JudgeStatus.ACCEPTED]: 'success',
    [JudgeStatus.WRONG_ANSWER]: 'danger',
    [JudgeStatus.TIME_LIMIT_EXCEEDED]: 'warning',
    [JudgeStatus.MEMORY_LIMIT_EXCEEDED]: 'warning',
    [JudgeStatus.RUNTIME_ERROR]: 'danger',
    [JudgeStatus.COMPILE_ERROR]: 'warning',
    [JudgeStatus.SYSTEM_ERROR]: 'danger',
}

// 编程语言
export enum Language {
    JAVA = 'JAVA',
    C = 'C',
    CPP = 'CPP',
    PYTHON = 'PYTHON',
}

export const LanguageText: Record<Language, string> = {
    [Language.JAVA]: 'Java',
    [Language.C]: 'C',
    [Language.CPP]: 'C++',
    [Language.PYTHON]: 'Python',
}

// Monaco Editor语言映射
export const LanguageMonaco: Record<Language, string> = {
    [Language.JAVA]: 'java',
    [Language.C]: 'c',
    [Language.CPP]: 'cpp',
    [Language.PYTHON]: 'python',
}

// 提交代码请求
export interface SubmitCodeRequest {
    problemId: number
    userId: number
    language: Language
    code: string
}

// 提交记录
export interface Submission {
    id: number
    problemId: number
    userId: number
    language: Language
    code: string
    status: JudgeStatus
    score: number
    timeUsed?: number
    memoryUsed?: number
    errorMessage?: string
    passRate: number
    createTime: string
    judgedTime?: string
}

// 评测结果
export interface JudgeResult {
    submissionId: number
    status: JudgeStatus
    statusDesc: string
    score: number
    timeUsed?: number
    memoryUsed?: number
    passRate: string
    errorMessage?: string
}

// API响应
export interface ApiResponse<T = any> {
    code: number
    message: string
    data: T
}

// ==================== 用户相关 ====================

// 用户信息
export interface User {
    id: number
    username: string
    email: string
    role: 'STUDENT' | 'TEACHER' | 'ADMIN'
    createdTime: string
}

// 登录请求
export interface LoginRequest {
    username: string
    password: string
}

// 注册请求
export interface RegisterRequest {
    username: string
    password: string
    email: string
}

// 登录响应
export interface LoginResponse {
    token: string
    userId: number
    username: string
    role: string
}

// ==================== 题目相关 ====================

// 题目
export interface Problem {
    id: number
    title: string
    description: string
    difficulty: 'EASY' | 'MEDIUM' | 'HARD'
    tags: string[]
    timeLimit: number
    memoryLimit: number
    inputFormat?: string
    outputFormat?: string
    sampleInput?: string
    sampleOutput?: string
    acceptCount: number
    submitCount: number
    createdTime: string
}

// 难度文本映射
export const DifficultyText: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难',
}

// 难度颜色映射
export const DifficultyColor: Record<string, string> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger',
}

// ==================== 排行榜相关 ====================

// 排行榜条目
export interface RankingItem {
    rank: number
    userId: number
    username: string
    solvedCount: number
    totalSubmissions: number
    acceptRate: number
    score: number
}
