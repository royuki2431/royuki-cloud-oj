import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

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
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
    // 设置页面标题
    document.title = (to.meta.title as string) || 'Royuki Cloud OJ'

    // 检查是否需要登录
    const userStore = useUserStore()
    const requiresAuth = to.meta.requiresAuth

    if (requiresAuth && !userStore.isLoggedIn) {
        ElMessage.warning('请先登录')
        next('/login')
    } else if (to.path === '/login' && userStore.isLoggedIn) {
        // 已登录用户访问登录页，重定向到题库
        next('/problems')
    } else {
        next()
    }
})

export default router
