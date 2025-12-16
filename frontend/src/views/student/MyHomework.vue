<template>
    <div class="my-homework">
        <div class="page-header">
            <h2>我的作业</h2>
        </div>
        
        <!-- 筛选区域 -->
        <el-card class="filter-card" shadow="never">
            <el-form :inline="true">
                <el-form-item label="班级">
                    <el-select v-model="filterClassId" placeholder="全部班级" clearable @change="loadHomeworks" style="width: 200px">
                        <el-option v-for="cls in classes" :key="cls.classId" :label="cls.className" :value="cls.classId" />
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="filterStatus" @change="loadHomeworks">
                        <el-radio-button label="all">全部</el-radio-button>
                        <el-radio-button label="pending">待完成</el-radio-button>
                        <el-radio-button label="completed">已完成</el-radio-button>
                        <el-radio-button label="overdue">已截止</el-radio-button>
                    </el-radio-group>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 作业列表 -->
        <div class="homework-cards" v-loading="loading">
            <el-empty v-if="filteredHomework.length === 0 && !loading" description="暂无作业" />
            
            <el-card
                v-for="item in filteredHomework"
                :key="item.id"
                class="homework-card"
                shadow="hover"
            >
                <div class="homework-header">
                    <h3>{{ item.title }}</h3>
                    <div class="tags">
                        <el-tag :type="getStatusType(item)" size="small">
                            {{ getStatusText(item) }}
                        </el-tag>
                        <el-tag v-if="isOverdue(item.endTime) && !item.isCompleted" type="danger" size="small">
                            已截止
                        </el-tag>
                    </div>
                </div>
                
                <p class="homework-desc">{{ item.description || '暂无描述' }}</p>
                
                <div class="homework-meta">
                    <div class="meta-item">
                        <el-icon><Reading /></el-icon>
                        <span>{{ item.courseName }} - {{ item.className }}</span>
                    </div>
                    <div class="meta-item">
                        <el-icon><Calendar /></el-icon>
                        <span :class="{ 'text-danger': isOverdue(item.endTime) && !item.isCompleted }">
                            截止：{{ formatTime(item.endTime) }}
                        </span>
                    </div>
                    <div class="meta-item">
                        <el-icon><Document /></el-icon>
                        <span>题目数：{{ item.problemCount || 0 }}</span>
                    </div>
                </div>

                <el-progress 
                    v-if="item.problemCount > 0"
                    :percentage="Math.round((item.completedCount || 0) / item.problemCount * 100)" 
                    :status="item.isCompleted ? 'success' : ''"
                    :stroke-width="8"
                    class="progress-bar"
                />
                
                <div class="homework-footer">
                    <div class="score-info" v-if="item.earnedScore !== undefined && item.earnedScore !== null">
                        <span class="score">{{ item.earnedScore }}</span>
                        <span class="total">/ {{ item.totalScore }} 分</span>
                    </div>
                    <div class="score-info" v-else>
                        <span class="total">总分：{{ item.totalScore }} 分</span>
                    </div>
                    <el-button type="primary" @click="viewHomework(item)">
                        {{ item.isCompleted ? '查看详情' : '开始做题' }}
                    </el-button>
                </div>
            </el-card>
        </div>

        <!-- 作业详情对话框 -->
        <el-dialog v-model="detailDialogVisible" :title="currentHomework?.title" width="900px" top="5vh">
            <div class="homework-detail" v-if="currentHomework" v-loading="loadingDetail">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="所属课程">{{ currentHomework.courseName }}</el-descriptions-item>
                    <el-descriptions-item label="所属班级">{{ currentHomework.className }}</el-descriptions-item>
                    <el-descriptions-item label="开始时间">{{ formatTime(currentHomework.startTime) }}</el-descriptions-item>
                    <el-descriptions-item label="截止时间">
                        <span :class="{ 'text-danger': isOverdue(currentHomework.endTime) }">
                            {{ formatTime(currentHomework.endTime) }}
                        </span>
                    </el-descriptions-item>
                    <el-descriptions-item label="总分">{{ currentHomework.totalScore }} 分</el-descriptions-item>
                    <el-descriptions-item label="我的得分">
                        <span v-if="currentHomework.earnedScore !== undefined && currentHomework.earnedScore !== null" class="score-text">
                            {{ currentHomework.earnedScore }} 分
                        </span>
                        <span v-else>-</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="作业描述" :span="2">{{ currentHomework.description || '暂无描述' }}</el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">题目列表</el-divider>
                
                <el-table :data="homeworkProblems" border>
                    <el-table-column type="index" label="序号" width="60" align="center" />
                    <el-table-column prop="problemTitle" label="题目" min-width="200">
                        <template #default="{ row }">
                            <el-link type="primary" @click="goToProblem(row)">
                                {{ row.problemTitle || `题目 #${row.problemId}` }}
                            </el-link>
                        </template>
                    </el-table-column>
                    <el-table-column prop="difficulty" label="难度" width="100" align="center">
                        <template #default="{ row }">
                            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
                                {{ getDifficultyText(row.difficulty) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="score" label="分值" width="80" align="center" />
                    <el-table-column label="状态" width="100" align="center">
                        <template #default="{ row }">
                            <el-tag :type="row.submitted ? 'success' : 'info'" size="small">
                                {{ row.submitted ? '已提交' : '未提交' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="得分" width="80" align="center">
                        <template #default="{ row }">
                            {{ row.earnedScore !== undefined && row.earnedScore !== null ? row.earnedScore : '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="120" align="center">
                        <template #default="{ row }">
                            <el-button 
                                type="primary" 
                                size="small" 
                                @click="goToProblem(row)"
                                :disabled="isOverdue(currentHomework?.endTime) && !row.submitted"
                            >
                                {{ row.submitted ? '查看' : '做题' }}
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Document, Reading } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface StudentClass {
    classId: number
    className: string
    courseId: number
    courseName: string
}

interface HomeworkItem {
    id: number
    title: string
    description: string
    startTime: string
    endTime: string
    totalScore: number
    problemCount: number
    completedCount: number
    earnedScore: number | null
    isCompleted: boolean
    classId: number
    className: string
    courseId: number
    courseName: string
}

interface HomeworkProblem {
    problemId: number
    problemTitle: string
    difficulty: string
    score: number
    submitted: boolean
    earnedScore: number | null
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const loadingDetail = ref(false)
const classes = ref<StudentClass[]>([])
const homeworkList = ref<HomeworkItem[]>([])
const filterClassId = ref<number | null>(null)
const filterStatus = ref('all')
const detailDialogVisible = ref(false)
const currentHomework = ref<HomeworkItem | null>(null)
const homeworkProblems = ref<HomeworkProblem[]>([])

// 获取当前用户ID
const getUserId = () => {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
        return JSON.parse(userInfo).id
    }
    return null
}

const filteredHomework = computed(() => {
    let list = homeworkList.value
    
    if (filterClassId.value) {
        list = list.filter(item => item.classId === filterClassId.value)
    }
    
    if (filterStatus.value === 'pending') {
        list = list.filter(item => !item.isCompleted && !isOverdue(item.endTime))
    } else if (filterStatus.value === 'completed') {
        list = list.filter(item => item.isCompleted)
    } else if (filterStatus.value === 'overdue') {
        list = list.filter(item => isOverdue(item.endTime) && !item.isCompleted)
    }
    
    return list
})

const loadClasses = async () => {
    const userId = getUserId()
    if (!userId) return
    
    try {
        const res = await request.get(`/course/student/classes/${userId}`)
        classes.value = res || []
    } catch (error) {
        console.error('加载班级失败:', error)
    }
}

const loadHomeworks = async () => {
    const userId = getUserId()
    if (!userId) {
        ElMessage.warning('请先登录')
        return
    }
    
    loading.value = true
    try {
        const res = await request.get(`/course/homework/student/${userId}`)
        homeworkList.value = res || []
    } catch (error) {
        console.error('加载作业失败:', error)
        ElMessage.error('加载作业失败')
    } finally {
        loading.value = false
    }
}

const viewHomework = async (homework: HomeworkItem) => {
    currentHomework.value = homework
    detailDialogVisible.value = true
    
    loadingDetail.value = true
    try {
        const userId = getUserId()
        const res = await request.get(`/course/homework/student/detail/${homework.id}?studentId=${userId}`)
        homeworkProblems.value = res?.problems || []
    } catch (error) {
        console.error('加载作业详情失败:', error)
        homeworkProblems.value = []
    } finally {
        loadingDetail.value = false
    }
}

const goToProblem = (problem: HomeworkProblem) => {
    // 跳转到题目页面，带上作业ID参数
    router.push(`/problem/${problem.problemId}?homeworkId=${currentHomework.value?.id}`)
    detailDialogVisible.value = false
}

const isOverdue = (endTime: string) => {
    if (!endTime) return false
    return new Date(endTime) < new Date()
}

const formatTime = (time: string) => {
    if (!time) return '-'
    return new Date(time).toLocaleString('zh-CN')
}

const getStatusType = (item: HomeworkItem) => {
    if (item.isCompleted) return 'success'
    if (isOverdue(item.endTime)) return 'info'
    return 'warning'
}

const getStatusText = (item: HomeworkItem) => {
    if (item.isCompleted) return '已完成'
    if (isOverdue(item.endTime)) return '未完成'
    return '进行中'
}

const getDifficultyType = (difficulty: string) => {
    const map: Record<string, string> = {
        'EASY': 'success',
        'MEDIUM': 'warning',
        'HARD': 'danger'
    }
    return map[difficulty] || 'info'
}

const getDifficultyText = (difficulty: string) => {
    const map: Record<string, string> = {
        'EASY': '简单',
        'MEDIUM': '中等',
        'HARD': '困难'
    }
    return map[difficulty] || difficulty
}

onMounted(async () => {
    await loadClasses()
    await loadHomeworks()
    
    // 如果URL带有homeworkId参数，自动打开对应作业
    const homeworkId = route.query.homeworkId
    if (homeworkId) {
        const homework = homeworkList.value.find(h => h.id === Number(homeworkId))
        if (homework) {
            viewHomework(homework)
        }
    }
})
</script>

<style scoped lang="scss">
.my-homework {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    margin-bottom: 20px;
    
    h2 {
        margin: 0;
        font-size: 24px;
        color: #303133;
    }
}

.filter-card {
    margin-bottom: 20px;
    
    :deep(.el-card__body) {
        padding: 16px 20px;
    }
    
    :deep(.el-form-item) {
        margin-bottom: 0;
    }
}

.homework-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 20px;
}

.homework-card {
    transition: all 0.3s;
    
    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }
    
    .homework-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 12px;
        
        h3 {
            margin: 0;
            font-size: 16px;
            font-weight: 600;
            color: #303133;
            flex: 1;
            margin-right: 12px;
        }
        
        .tags {
            display: flex;
            gap: 6px;
        }
    }
    
    .homework-desc {
        color: #606266;
        font-size: 14px;
        margin: 0 0 12px;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }
    
    .homework-meta {
        display: flex;
        flex-direction: column;
        gap: 8px;
        margin-bottom: 12px;
        
        .meta-item {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            color: #909399;
        }
    }
    
    .progress-bar {
        margin-bottom: 16px;
    }
    
    .homework-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .score-info {
            .score {
                font-size: 24px;
                font-weight: 600;
                color: #409eff;
            }
            
            .total {
                font-size: 14px;
                color: #909399;
            }
        }
    }
}

.homework-detail {
    .score-text {
        font-weight: 600;
        color: #409eff;
    }
}

.text-danger {
    color: #f56c6c !important;
}
</style>
