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

// 测试用例执行结果
export interface TestCaseResult {
    testCaseId: number
    status: JudgeStatus
    timeUsed: number
    memoryUsed: number
    input?: string
    expectedOutput?: string
    actualOutput?: string
    errorMessage?: string
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
    testCaseResults?: TestCaseResult[]
}

// API响应
export interface ApiResponse<T = any> {
    code: number
    message: string
    data: T
}

// ==================== 用户相关 ====================

// 用户角色枚举
export enum UserRole {
    STUDENT = 'STUDENT',
    TEACHER = 'TEACHER',
    ADMIN = 'ADMIN',
}

// 角色文本映射
export const UserRoleText: Record<UserRole, string> = {
    [UserRole.STUDENT]: '学生',
    [UserRole.TEACHER]: '教师',
    [UserRole.ADMIN]: '管理员',
}

// 角色权限等级（数字越大权限越高）
export const UserRoleLevel: Record<UserRole, number> = {
    [UserRole.STUDENT]: 1,
    [UserRole.TEACHER]: 2,
    [UserRole.ADMIN]: 3,
}

// 用户信息
export interface User {
    id: number
    username: string
    email: string
    phone?: string
    realName?: string
    avatar?: string
    role: UserRole | string
    status?: number
    school?: string
    studentId?: string
    grade?: string
    major?: string
    createTime?: string
    createdTime?: string  // 兼容字段
    updateTime?: string
    lastLoginTime?: string
    lastLoginIp?: string
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
    category?: string
    tags: string | string[]  // 后端存储为JSON字符串，前端使用时为数组
    timeLimit: number
    memoryLimit: number
    inputFormat?: string
    outputFormat?: string
    sampleInput?: string
    sampleOutput?: string
    hint?: string
    status?: number
    acceptCount: number
    submitCount: number
    createdTime: string
}

// 测试用例
export interface TestCase {
    id: number
    problemId: number
    input: string
    output: string
    isSample: number  // 是否为样例：1-是 0-否
    score: number
    orderNum: number
    createTime?: string
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

// ==================== 课程相关 ====================

// 课程
export interface Course {
    id: number
    courseName: string
    description?: string
    teacherId: number
    teacherName?: string
    startTime?: string
    endTime?: string
    status: number
    createdTime: string
}

// 班级
export interface CourseClass {
    id: number
    courseId: number
    className: string
    description?: string
    teacherId: number
    inviteCode: string
    maxStudents?: number
    currentStudents: number
    createdTime: string
}

// 作业
export interface Homework {
    id: number
    courseId: number
    classId?: number
    title: string
    description?: string
    teacherId: number
    startTime?: string
    endTime?: string
    totalScore: number
    status: number
    createdTime: string
}

// 作业题目
export interface HomeworkProblem {
    id: number
    homeworkId: number
    problemId: number
    score: number
    orderNum: number
}

// 学生作业信息
export interface StudentHomework {
    homework: Homework
    submissionCount: number
    isCompleted: boolean
    earnedScore?: number
}
