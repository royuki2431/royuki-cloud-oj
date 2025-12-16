import request from '@/utils/request'

// ==================== 系统设置 ====================

export interface SystemSettings {
    // 基础设置
    systemName: string
    systemDesc: string
    keywords: string
    icp: string
    allowRegister: boolean
    requireEmailVerify: boolean
    minPasswordLength: number
    maxLoginAttempts: number

    // 评测设置
    defaultTimeLimit: number
    defaultMemoryLimit: number
    maxConcurrentJudge: number
    judgeTimeout: number
    supportedLanguages: string
    submitInterval: number

    // 邮件设置
    smtpHost?: string
    smtpPort?: number
    emailFrom?: string
    emailFromName?: string
    smtpUsername?: string
    smtpPassword?: string
    smtpSsl?: boolean
}

export interface SystemInfo {
    version: string
    startTime: string
    uptime: string
    dbStatus: boolean
    redisStatus: boolean
    mqStatus: boolean
    userCount: number
    problemCount?: number
    submissionCount?: number
    cpuUsage: number
    memoryUsage: number
    diskUsage: number
}

export interface CacheInfo {
    redisKeys: number
    sessionCount: number
    problemCacheCount?: number
    userCacheCount?: number
}

/**
 * 获取系统设置
 */
export function getSystemSettings() {
    return request.get<any, SystemSettings>('/system/settings')
}

/**
 * 更新系统设置
 */
export function updateSystemSettings(settings: Partial<SystemSettings>) {
    return request.post<any, void>('/system/settings', settings)
}

/**
 * 获取系统信息
 */
export function getSystemInfo() {
    return request.get<any, SystemInfo>('/system/info')
}

/**
 * 获取缓存信息
 */
export function getCacheInfo() {
    return request.get<any, CacheInfo>('/system/cache/info')
}

/**
 * 清除缓存
 */
export function clearCache(type: 'redis' | 'session' | 'problem') {
    return request.post<any, void>('/system/cache/clear', null, {
        params: { type }
    })
}

// ==================== 统计数据 ====================

/**
 * 获取题目统计
 */
export function getProblemStats() {
    return request.get<any, number>('/problem/count').then(count => ({ total: count }))
}

/**
 * 获取提交统计
 */
export function getSubmissionStats() {
    return request.get<any, number>('/judge/submission/count').then(count => ({ total: count }))
}
