import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { UserRole } from '@/types'

// 扩展路由元信息类型
declare module 'vue-router' {
    interface RouteMeta {
        title?: string
        requiresAuth?: boolean
        requiredRole?: UserRole
        requiredRoles?: UserRole[]
    }
}

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        redirect: '/problems',
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/LoginPage.vue'),
        meta: { title: '登录 / 注册', requiresAuth: false },
    },
    {
        path: '/problems',
        name: 'ProblemList',
        component: () => import('@/views/ProblemList.vue'),
        meta: { title: '题目列表', requiresAuth: false },
    },
    {
        path: '/problem/:id',
        name: 'ProblemDetail',
        component: () => import('@/views/ProblemDetail.vue'),
        meta: { title: '题目详情', requiresAuth: false },
    },
    {
        path: '/judge',
        name: 'Judge',
        component: () => import('@/views/JudgePage.vue'),
        meta: { title: '代码评测', requiresAuth: false },
    },
    {
        path: '/submissions',
        name: 'Submissions',
        component: () => import('@/views/SubmissionHistory.vue'),
        meta: { title: '提交历史', requiresAuth: true },
    },
    {
        path: '/ranking',
        name: 'Ranking',
        component: () => import('@/views/RankingPage.vue'),
        meta: { title: '排行榜', requiresAuth: false },
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/UserProfile.vue'),
        meta: { title: '个人中心', requiresAuth: true },
    },
    {
        path: '/user-debug',
        name: 'UserDebug',
        component: () => import('@/views/UserDebug.vue'),
        meta: { title: '用户信息调试', requiresAuth: false },
    },
    // ==================== 学生功能 ====================
    {
        path: '/my-classes',
        name: 'MyClasses',
        component: () => import('@/views/student/MyCourses.vue'),
        meta: { title: '我的班级', requiresAuth: true },
    },
    {
        path: '/my-homework',
        name: 'MyHomework',
        component: () => import('@/views/student/MyHomework.vue'),
        meta: { title: '我的作业', requiresAuth: true },
    },
    // 加入班级功能已融入"我的班级"页面，保留路由以兼容旧链接
    {
        path: '/join-class',
        redirect: '/my-classes',
    },
    {
        path: '/wrong-problems',
        name: 'WrongProblems',
        component: () => import('@/views/student/WrongProblems.vue'),
        meta: { title: '错题本', requiresAuth: true },
    },
    {
        path: '/learning-notes',
        name: 'LearningNotes',
        component: () => import('@/views/student/LearningNotes.vue'),
        meta: { title: '学习笔记', requiresAuth: true },
    },
    {
        path: '/learning-stats',
        name: 'LearningStats',
        component: () => import('@/views/student/LearningStats.vue'),
        meta: { title: '学习统计', requiresAuth: true },
    },
    {
        path: '/learning-progress',
        name: 'LearningProgress',
        component: () => import('@/views/student/LearningProgress.vue'),
        meta: { title: '学习进度', requiresAuth: true },
    },
    {
        path: '/training-path',
        name: 'TrainingPath',
        component: () => import('@/views/student/TrainingPath.vue'),
        meta: { title: '训练路径', requiresAuth: true },
    },
    {
        path: '/course/:id',
        name: 'CourseDetail',
        component: () => import('@/views/student/CourseDetail.vue'),
        meta: { title: '课程详情', requiresAuth: true },
    },
    {
        path: '/homework/:id',
        name: 'HomeworkDetail',
        component: () => import('@/views/student/HomeworkDetail.vue'),
        meta: { title: '作业详情', requiresAuth: true },
    },
    // ==================== 教师功能 ====================
    {
        path: '/teacher/courses',
        name: 'TeacherCourses',
        component: () => import('@/views/teacher/CourseManage.vue'),
        meta: { title: '课程管理', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    {
        path: '/teacher/classes',
        name: 'TeacherClasses',
        component: () => import('@/views/teacher/ClassManage.vue'),
        meta: { title: '班级管理', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    {
        path: '/teacher/homework',
        name: 'TeacherHomework',
        component: () => import('@/views/teacher/HomeworkManage.vue'),
        meta: { title: '作业管理', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    {
        path: '/teacher/students',
        name: 'TeacherStudents',
        component: () => import('@/views/teacher/StudentManage.vue'),
        meta: { title: '学生管理', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    {
        path: '/teacher/problems',
        name: 'TeacherProblems',
        component: () => import('@/views/teacher/ProblemManage.vue'),
        meta: { title: '题库管理', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    {
        path: '/teacher/analysis',
        name: 'TeacherAnalysis',
        component: () => import('@/views/teacher/SubmissionAnalysis.vue'),
        meta: { title: '提交分析', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    {
        path: '/teacher/statistics',
        name: 'TeacherStatistics',
        component: () => import('@/views/teacher/ClassStatistics.vue'),
        meta: { title: '班级统计', requiresAuth: true, requiredRole: UserRole.TEACHER },
    },
    // ==================== 管理员功能 ====================
    {
        path: '/admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiredRole: UserRole.ADMIN },
    },
    {
        path: '/admin/problems',
        name: 'AdminProblems',
        component: () => import('@/views/admin/ProblemManage.vue'),
        meta: { title: '题目管理', requiresAuth: true, requiredRole: UserRole.ADMIN },
    },
    {
        path: '/admin/notes',
        name: 'AdminNotes',
        component: () => import('@/views/admin/NoteManage.vue'),
        meta: { title: '笔记管理', requiresAuth: true, requiredRole: UserRole.ADMIN },
    },
    {
        path: '/admin/system',
        name: 'AdminSystem',
        component: () => import('@/views/admin/SystemSettings.vue'),
        meta: { title: '系统设置', requiresAuth: true, requiredRole: UserRole.ADMIN },
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
    // 设置页面标题
    document.title = (to.meta.title as string) || 'Royuki Cloud OJ'

    const userStore = useUserStore()
    const requiresAuth = to.meta.requiresAuth
    const requiredRole = to.meta.requiredRole
    const requiredRoles = to.meta.requiredRoles

    // 1. 检查是否需要登录
    if (requiresAuth && !userStore.isLoggedIn) {
        ElMessage.warning('请先登录')
        next('/login')
        return
    }

    // 2. 已登录用户访问登录页，重定向到题库
    if (to.path === '/login' && userStore.isLoggedIn) {
        next('/problems')
        return
    }

    // 3. 检查单一角色权限
    if (requiredRole && !userStore.canAccess(requiredRole)) {
        ElMessage.error('您没有权限访问此页面')
        next('/problems')
        return
    }

    // 4. 检查多角色权限（满足任一即可）
    if (requiredRoles && !userStore.hasAnyRole(requiredRoles)) {
        ElMessage.error('您没有权限访问此页面')
        next('/problems')
        return
    }

    next()
})

export default router
