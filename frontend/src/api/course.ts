import request from '@/utils/request'

// ==================== 课程管理 ====================

export interface Course {
    id: number
    name: string
    description: string
    teacherId: number
    teacherName?: string
    coverImage?: string
    semester: string
    status: number
    studentCount: number
    createdTime: string
}

export function createCourse(data: Partial<Course>) {
    return request.post<any, number>('/course/create', data)
}

export function getCourseById(id: number) {
    return request.get<any, Course>(`/course/detail/${id}`)
}

export function getTeacherCourses(teacherId: number) {
    return request.get<any, Course[]>(`/course/teacher/${teacherId}`)
}

export function getStudentCourses(studentId: number) {
    return request.get<any, Course[]>(`/course/student/${studentId}`)
}

export function updateCourse(data: Partial<Course>) {
    return request.put<any, void>('/course/update', data)
}

export function deleteCourse(id: number) {
    return request.delete<any, void>(`/course/delete/${id}`)
}

// ==================== 班级管理 ====================

export interface CourseClass {
    id: number
    courseId: number
    name: string
    code: string
    teacherId: number
    maxStudents: number
    studentCount: number
    status: number
    createdTime: string
}

export function createClass(data: Partial<CourseClass>) {
    return request.post<any, number>('/course/class/create', data)
}

export function getClassById(id: number) {
    return request.get<any, CourseClass>(`/course/class/${id}`)
}

export function getClassByCode(code: string) {
    return request.get<any, CourseClass>('/course/class/code', {
        params: { code }
    })
}

export function getCourseClasses(courseId: number) {
    return request.get<any, CourseClass[]>(`/course/class/list/${courseId}`)
}

export function joinClass(studentId: number, inviteCode: string) {
    return request.post<any, void>('/course/class/join', null, {
        params: { studentId, inviteCode }
    })
}

export function leaveClass(studentId: number, classId: number) {
    return request.post<any, void>('/course/class/leave', null, {
        params: { studentId, classId }
    })
}

export function removeStudent(classId: number, studentId: number) {
    return request.post<any, void>('/course/class/removeStudent', null, {
        params: { classId, studentId }
    })
}

export function getClassStudents(classId: number) {
    return request.get<any, any[]>(`/course/class/students/${classId}`)
}

export function getStudentClasses(studentId: number) {
    return request.get<any, CourseClass[]>(`/course/class/student/${studentId}`)
}

export function isStudentInClass(studentId: number, classId: number) {
    return request.get<any, boolean>('/course/class/check', {
        params: { studentId, classId }
    })
}

export function updateClass(data: Partial<CourseClass>) {
    return request.put<any, void>('/course/class/update', data)
}

export function deleteClass(id: number) {
    return request.delete<any, void>(`/course/class/delete/${id}`)
}

// ==================== 作业管理 ====================

export interface Homework {
    id: number
    classId: number
    title: string
    description: string
    startTime: string
    endTime: string
    totalScore: number
    status: number
    createdTime: string
}

export interface HomeworkProblem {
    problemId: number
    score: number
    orderNum: number
}

export function createHomework(data: {
    classId: number
    title: string
    description?: string
    startTime: string
    endTime: string
    totalScore?: number
    problems: HomeworkProblem[]
}) {
    return request.post<any, number>('/course/homework/create', data)
}

export function getHomeworkDetail(id: number) {
    return request.get<any, any>(`/course/homework/detail/${id}`)
}

export function getClassHomeworks(classId: number) {
    return request.get<any, Homework[]>(`/course/homework/class/${classId}`)
}

export function getStudentHomeworks(studentId: number) {
    return request.get<any, any[]>(`/course/homework/student/${studentId}`)
}

export function updateHomework(data: Partial<Homework>) {
    return request.put<any, void>('/course/homework/update', data)
}

export function deleteHomework(id: number) {
    return request.delete<any, void>(`/course/homework/delete/${id}`)
}

// ==================== 统计接口 ====================

export function getClassStatistics(classId: number) {
    return request.get<any, any>(`/course/statistics/class/${classId}`)
}

export function getHomeworkCompletion(classId: number, homeworkId: number) {
    return request.get<any, any>('/course/statistics/homework', {
        params: { classId, homeworkId }
    })
}

export function getStudentRanking(classId: number) {
    return request.get<any, any[]>(`/course/statistics/ranking/${classId}`)
}

export function getCourseStatistics(courseId: number) {
    return request.get<any, any>(`/course/statistics/course/${courseId}`)
}

// 获取学生作业提交记录
export function getStudentHomeworkSubmissions(classId: number, studentId: number) {
    return request.get<any, any[]>('/course/homework/submissions', {
        params: { classId, studentId }
    })
}
