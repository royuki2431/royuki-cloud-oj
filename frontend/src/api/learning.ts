import request from '@/utils/request'

// ==================== 学习进度 ====================

export interface LearningProgress {
  id: number
  userId: number
  problemId: number
  status: string
  attemptCount: number
  lastAttemptTime: string
  solvedTime: string | null
}

export function getLearningProgress(userId: number, problemId: number) {
  return request.get<any, LearningProgress>('/learning/progress', {
    params: { userId, problemId }
  })
}

export function getUserProgressList(userId: number) {
  return request.get<any, LearningProgress[]>('/learning/progress/list', {
    params: { userId }
  })
}

export function getLearningSummary(userId: number) {
  return request.get<any, any>('/learning/summary', {
    params: { userId }
  })
}

// ==================== 错题本 ====================

export interface WrongProblem {
  id: number
  userId: number
  problemId: number
  submissionId: number
  errorType: string
  wrongCount: number
  isResolved: number
  resolvedTime: string | null
  createdTime: string
  updatedTime: string
}

export function getWrongProblems(userId: number) {
  return request.get<any, WrongProblem[]>('/learning/wrong', {
    params: { userId }
  })
}

export function getUnresolvedWrongProblems(userId: number) {
  return request.get<any, WrongProblem[]>('/learning/wrong/unresolved', {
    params: { userId }
  })
}

export function resolveWrongProblem(id: number) {
  return request.post<any, void>('/learning/wrong/resolve', null, {
    params: { id }
  })
}

export function deleteWrongProblem(id: number) {
  return request.delete<any, void>(`/learning/wrong/${id}`)
}

export function getWrongProblemStatistics(userId: number) {
  return request.get<any, any>('/learning/wrong/statistics', {
    params: { userId }
  })
}

// ==================== 学习笔记 ====================

export interface LearningNote {
  id: number
  userId: number
  problemId: number
  title: string
  content: string
  isPublic: number
  createdTime: string
  updatedTime: string
}

export interface CreateNoteRequest {
  userId: number
  problemId: number
  title: string
  content: string
  isPublic?: number
}

export interface UpdateNoteRequest {
  id: number
  title?: string
  content?: string
  isPublic?: number
}

export function createNote(data: CreateNoteRequest) {
  return request.post<any, number>('/learning/note', data)
}

export function updateNote(data: UpdateNoteRequest) {
  return request.put<any, void>('/learning/note', data)
}

export function deleteNote(id: number) {
  return request.delete<any, void>(`/learning/note/${id}`)
}

export function getNoteById(id: number) {
  return request.get<any, LearningNote>(`/learning/note/${id}`)
}

export function getUserNotes(userId: number) {
  return request.get<any, LearningNote[]>('/learning/note/user', {
    params: { userId }
  })
}

export function getProblemNotes(userId: number, problemId: number) {
  return request.get<any, LearningNote[]>('/learning/note/problem', {
    params: { userId, problemId }
  })
}

export function getPublicNotes(problemId: number) {
  return request.get<any, LearningNote[]>('/learning/note/public', {
    params: { problemId }
  })
}

export function searchNotes(userId: number, keyword: string) {
  return request.get<any, LearningNote[]>('/learning/note/search', {
    params: { userId, keyword }
  })
}

// ==================== 学习统计 ====================

export interface LearningStatistics {
  id: number
  userId: number
  statDate: string
  submitCount: number
  acceptCount: number
  problemSolved: number
  codeLines: number
  studyDuration: number
}

export function getLearningHeatmap(userId: number, startDate: string, endDate: string) {
  return request.get<any, LearningStatistics[]>('/learning/statistics/heatmap', {
    params: { userId, startDate, endDate }
  })
}

export function getLearningOverview(userId: number) {
  return request.get<any, any>('/learning/statistics/overview', {
    params: { userId }
  })
}

export function getLeaderboard(limit: number = 10) {
  return request.get<any, any[]>('/learning/statistics/leaderboard', {
    params: { limit }
  })
}
