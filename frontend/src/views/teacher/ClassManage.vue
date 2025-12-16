<template>
    <div class="class-manage">
        <el-page-header @back="$router.back()" content="班级管理" />
        
        <el-card class="toolbar" shadow="never">
            <div class="toolbar-content">
                <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadClasses" style="width: 200px">
                    <el-option 
                        v-for="course in courses" 
                        :key="course.id" 
                        :label="course.name" 
                        :value="course.id" 
                    />
                </el-select>
                <el-button type="primary" @click="showCreateDialog = true" :disabled="!selectedCourseId">
                    <el-icon><Plus /></el-icon>
                    创建班级
                </el-button>
            </div>
        </el-card>

        <el-card class="class-list" shadow="hover" v-loading="loading">
            <el-table :data="classes" stripe>
                <el-table-column prop="name" label="班级名称" />
                <el-table-column prop="code" label="邀请码" width="120">
                    <template #default="{ row }">
                        <el-tag type="success" @click="copyCode(row.code)" style="cursor: pointer">
                            {{ row.code }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="studentCount" label="学生人数" width="100" align="center" />
                <el-table-column prop="maxStudents" label="最大人数" width="100" align="center" />
                <el-table-column label="操作" width="360">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewStudents(row)">
                            学生
                        </el-button>
                        <el-dropdown trigger="click" style="margin: 0 8px">
                            <el-button type="success" size="small">
                                统计 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                            </el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item @click="viewStatistics(row)">快速查看</el-dropdown-item>
                                    <el-dropdown-item @click="goToDetailStats(row)">详细统计</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                        <el-button type="warning" size="small" @click="editClass(row)">
                            编辑
                        </el-button>
                        <el-button type="danger" size="small" @click="handleDeleteClass(row)">
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 创建班级对话框 -->
        <el-dialog v-model="showCreateDialog" title="创建班级" width="500px">
            <el-form :model="classForm" label-width="100px">
                <el-form-item label="班级名称" required>
                    <el-input v-model="classForm.name" placeholder="请输入班级名称" />
                </el-form-item>
                <el-form-item label="最大人数">
                    <el-input-number v-model="classForm.maxStudents" :min="10" :max="500" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showCreateDialog = false">取消</el-button>
                <el-button type="primary" @click="handleCreate" :loading="submitting">创建</el-button>
            </template>
        </el-dialog>

        <!-- 学生管理对话框 -->
        <el-dialog v-model="showStudentsDialog" :title="`学生管理 - ${currentClass?.name}`" width="800px">
            <div class="students-header">
                <span>共 {{ students.length }} 名学生</span>
                <el-input 
                    v-model="studentSearch" 
                    placeholder="搜索学生" 
                    style="width: 200px"
                    clearable
                />
            </div>
            <el-table :data="filteredStudents" v-loading="studentsLoading" max-height="400">
                <el-table-column prop="studentId" label="学生ID" width="100" />
                <el-table-column prop="studentName" label="姓名" width="120" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="joinTime" label="加入时间" width="180">
                    <template #default="{ row }">
                        {{ formatTime(row.joinTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="作业完成" width="120" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.completedCount === row.totalHomework ? 'success' : 'warning'" size="small">
                            {{ row.completedCount || 0 }}/{{ row.totalHomework || 0 }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                    <template #default="{ row }">
                        <el-button type="danger" size="small" @click="handleRemoveStudent(row)">
                            移除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>
        
        <!-- 编辑班级对话框 -->
        <el-dialog v-model="showEditDialog" title="编辑班级" width="500px">
            <el-form :model="editForm" label-width="100px">
                <el-form-item label="班级名称" required>
                    <el-input v-model="editForm.name" placeholder="请输入班级名称" />
                </el-form-item>
                <el-form-item label="最大人数">
                    <el-input-number v-model="editForm.maxStudents" :min="10" :max="500" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch 
                        v-model="editForm.status" 
                        :active-value="1" 
                        :inactive-value="0"
                        active-text="开放"
                        inactive-text="关闭"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showEditDialog = false">取消</el-button>
                <el-button type="primary" @click="handleUpdateClass" :loading="submitting">保存</el-button>
            </template>
        </el-dialog>

        <!-- 统计对话框 -->
        <el-dialog v-model="showStatsDialog" :title="`班级统计 - ${currentClass?.name}`" width="900px">
            <div class="stats-content" v-loading="statsLoading">
                <div class="stats-overview">
                    <div class="stat-item">
                        <div class="stat-value">{{ classStats.studentCount || 0 }}</div>
                        <div class="stat-label">学生人数</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">{{ displayStats.countLabel }}</div>
                        <div class="stat-label">{{ displayStats.countTitle }}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">{{ displayStats.rateLabel }}</div>
                        <div class="stat-label">{{ displayStats.rateTitle }}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">{{ displayStats.avgLabel }}</div>
                        <div class="stat-label">{{ displayStats.avgTitle }}</div>
                    </div>
                </div>
                
                <div class="ranking-toolbar">
                    <span class="ranking-title">学生排名</span>
                    <el-select v-model="rankingType" style="width: 150px" @change="onRankingTypeChange">
                        <el-option label="作业总分排名" value="homework_total" />
                        <el-option label="单个作业排名" value="homework_single" />
                        <el-option label="题库训练排名" value="practice" />
                    </el-select>
                    <el-select 
                        v-if="rankingType === 'homework_single'" 
                        v-model="selectedHomeworkId" 
                        placeholder="选择作业" 
                        style="width: 200px"
                        @change="loadHomeworkRanking"
                    >
                        <el-option 
                            v-for="hw in homeworkList" 
                            :key="hw.id" 
                            :label="hw.title" 
                            :value="hw.id" 
                        />
                    </el-select>
                </div>
                
                <!-- 作业总分排名 -->
                <el-table v-if="rankingType === 'homework_total'" :data="studentRanking" max-height="350">
                    <el-table-column label="排名" width="80" align="center">
                        <template #default="{ row }">
                            <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="studentName" label="学生姓名" min-width="100" />
                    <el-table-column prop="studentId" label="学号" width="100" />
                    <el-table-column prop="totalScore" label="总分" width="100" align="center" />
                    <el-table-column prop="completedHomeworks" label="已完成作业" width="120" align="center" />
                    <el-table-column prop="averageScore" label="平均分" width="100" align="center" />
                </el-table>
                
                <!-- 单个作业排名 -->
                <el-table v-else-if="rankingType === 'homework_single'" :data="homeworkRanking" max-height="350">
                    <el-table-column label="排名" width="80" align="center">
                        <template #default="{ row }">
                            <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="studentName" label="学生姓名" min-width="100" />
                    <el-table-column prop="studentId" label="学号" width="100" />
                    <el-table-column prop="score" label="得分" width="100" align="center" />
                    <el-table-column prop="submitTime" label="提交时间" width="160">
                        <template #default="{ row }">
                            {{ formatTime(row.submitTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100" align="center">
                        <template #default="{ row }">
                            <el-tag :type="row.completed ? 'success' : 'info'" size="small">
                                {{ row.completed ? '已完成' : '未完成' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                </el-table>
                
                <!-- 题库训练排名 -->
                <el-table v-else-if="rankingType === 'practice'" :data="practiceRanking" max-height="350">
                    <el-table-column label="排名" width="80" align="center">
                        <template #default="{ row }">
                            <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="studentName" label="学生姓名" min-width="100" />
                    <el-table-column prop="studentId" label="学号" width="100" />
                    <el-table-column prop="totalScore" label="总分" width="100" align="center" />
                    <el-table-column prop="solvedCount" label="通过题数" width="100" align="center" />
                    <el-table-column prop="submitCount" label="提交次数" width="100" align="center" />
                    <el-table-column prop="acceptRate" label="通过率" width="100" align="center">
                        <template #default="{ row }">
                            {{ row.acceptRate }}%
                        </template>
                    </el-table-column>
                </el-table>
                
                <el-empty v-if="getCurrentRankingData().length === 0" description="暂无排名数据" />
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowDown } from '@element-plus/icons-vue'
import { 
    getTeacherCourses,
    getCourseClasses,
    createClass,
    updateClass,
    deleteClass,
    getClassStudents,
    removeStudent,
    getClassStatistics,
    getStudentRanking,
    type Course,
    type CourseClass
} from '@/api/course'
import request from '@/utils/request'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const courses = ref<Course[]>([])
const classes = ref<CourseClass[]>([])
const selectedCourseId = ref<number | null>(null)
const showCreateDialog = ref(false)
const showStudentsDialog = ref(false)
const showStatsDialog = ref(false)
const showEditDialog = ref(false)
const currentClass = ref<CourseClass | null>(null)
const studentSearch = ref('')

// 学生管理
const students = ref<any[]>([])
const studentsLoading = ref(false)

// 统计
const classStats = ref<any>({})
const studentRanking = ref<any[]>([])
const homeworkRanking = ref<any[]>([])
const practiceRanking = ref<any[]>([])
const practiceStats = ref<any>({})
const homeworkSingleStats = ref<any>({})
const statsLoading = ref(false)
const rankingType = ref('homework_total')
const selectedHomeworkId = ref<number | null>(null)
const homeworkList = ref<any[]>([])

// 动态显示的统计数据
const displayStats = computed(() => {
    if (rankingType.value === 'homework_total') {
        return {
            countLabel: classStats.value.homeworkCount || 0,
            countTitle: '作业数量',
            rateLabel: classStats.value.homeworkCompletionRate || '0%',
            rateTitle: '作业完成率',
            avgLabel: classStats.value.averageScore || '0',
            avgTitle: '平均分'
        }
    } else if (rankingType.value === 'homework_single') {
        const hw = homeworkList.value.find(h => h.id === selectedHomeworkId.value)
        return {
            countLabel: hw?.problemCount || homeworkSingleStats.value.problemCount || 0,
            countTitle: '题目数量',
            rateLabel: homeworkSingleStats.value.completionRate || '0%',
            rateTitle: '完成率',
            avgLabel: homeworkSingleStats.value.averageScore || '0',
            avgTitle: '平均分'
        }
    } else {
        return {
            countLabel: practiceStats.value.maxSolvedCount || 0,
            countTitle: '最高通过题数',
            rateLabel: practiceStats.value.totalAcceptRate || '0%',
            rateTitle: '总通过率',
            avgLabel: practiceStats.value.averageScore || '0',
            avgTitle: '平均分'
        }
    }
})

const classForm = reactive({
    name: '',
    maxStudents: 100,
})

const editForm = reactive({
    id: 0,
    name: '',
    maxStudents: 100,
    status: 1
})

// 筛选学生
const filteredStudents = computed(() => {
    if (!studentSearch.value) return students.value
    const search = studentSearch.value.toLowerCase()
    return students.value.filter(s => 
        s.studentId?.toString().includes(search) ||
        s.studentName?.toLowerCase().includes(search) ||
        s.username?.toLowerCase().includes(search)
    )
})

// 获取当前用户ID
const getUserId = () => {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
        return JSON.parse(userInfo).id
    }
    return null
}

// 加载教师课程
const loadCourses = async () => {
    const userId = getUserId()
    if (!userId) return
    
    try {
        courses.value = await getTeacherCourses(userId)
        if (courses.value.length > 0) {
            // 检查URL参数中是否有courseId
            const courseIdFromQuery = route.query.courseId
            if (courseIdFromQuery) {
                selectedCourseId.value = Number(courseIdFromQuery)
            } else {
                selectedCourseId.value = courses.value[0].id
            }
            loadClasses()
        }
    } catch (error) {
        console.error('加载课程失败:', error)
    }
}

// 加载班级列表
const loadClasses = async () => {
    if (!selectedCourseId.value) return
    
    loading.value = true
    try {
        classes.value = await getCourseClasses(selectedCourseId.value)
    } catch (error) {
        console.error('加载班级失败:', error)
        ElMessage.error('加载班级失败')
    } finally {
        loading.value = false
    }
}

// 创建班级
const handleCreate = async () => {
    if (!classForm.name.trim()) {
        ElMessage.warning('请输入班级名称')
        return
    }
    
    const userId = getUserId()
    if (!userId || !selectedCourseId.value) return
    
    submitting.value = true
    try {
        await createClass({
            courseId: selectedCourseId.value,
            name: classForm.name,
            teacherId: userId,
            maxStudents: classForm.maxStudents
        })
        ElMessage.success('创建成功')
        showCreateDialog.value = false
        classForm.name = ''
        classForm.maxStudents = 100
        loadClasses()
    } catch (error) {
        ElMessage.error('创建失败')
    } finally {
        submitting.value = false
    }
}

// 复制邀请码
const copyCode = (code: string) => {
    navigator.clipboard.writeText(code)
    ElMessage.success('邀请码已复制')
}

// 查看学生列表
const viewStudents = async (classData: CourseClass) => {
    currentClass.value = classData
    showStudentsDialog.value = true
    studentsLoading.value = true
    
    try {
        students.value = await getClassStudents(classData.id)
    } catch (error) {
        ElMessage.error('加载学生列表失败')
    } finally {
        studentsLoading.value = false
    }
}

// 移除学生
const handleRemoveStudent = async (student: any) => {
    if (!currentClass.value) return
    
    try {
        await ElMessageBox.confirm('确定移除该学生吗？', '警告', { type: 'warning' })
        await removeStudent(currentClass.value.id, student.studentId)
        ElMessage.success('移除成功')
        viewStudents(currentClass.value)
        loadClasses()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error('移除失败')
        }
    }
}

// 查看统计
const viewStatistics = async (classData: CourseClass) => {
    currentClass.value = classData
    showStatsDialog.value = true
    statsLoading.value = true
    rankingType.value = 'homework_total'
    selectedHomeworkId.value = null
    homeworkRanking.value = []
    practiceRanking.value = []
    
    try {
        const [stats, ranking] = await Promise.all([
            getClassStatistics(classData.id),
            getStudentRanking(classData.id)
        ])
        classStats.value = stats
        studentRanking.value = ranking
        
        // 加载作业列表
        await loadHomeworkList()
    } catch (error) {
        ElMessage.error('加载统计失败')
    } finally {
        statsLoading.value = false
    }
}

// 加载班级作业列表
const loadHomeworkList = async () => {
    if (!currentClass.value) return
    try {
        const response = await request.get(`/course/homework/class/${currentClass.value.id}`)
        homeworkList.value = response || []
    } catch (error) {
        homeworkList.value = []
    }
}

// 排名类型变化
const onRankingTypeChange = async () => {
    if (rankingType.value === 'homework_total') {
        // 已经加载过了
    } else if (rankingType.value === 'homework_single') {
        homeworkSingleStats.value = {}
        // 默认选择第一个作业
        if (homeworkList.value.length > 0) {
            selectedHomeworkId.value = homeworkList.value[0].id
            await loadHomeworkRanking()
        } else {
            selectedHomeworkId.value = null
            homeworkRanking.value = []
        }
    } else if (rankingType.value === 'practice') {
        await loadPracticeRanking()
    }
}

// 加载单个作业排名
const loadHomeworkRanking = async () => {
    if (!currentClass.value || !selectedHomeworkId.value) return
    statsLoading.value = true
    try {
        const response = await request.get(`/course/statistics/homework/${selectedHomeworkId.value}/ranking`, {
            params: { classId: currentClass.value.id }
        })
        homeworkRanking.value = response || []
        
        // 计算单个作业的统计数据
        const ranking = homeworkRanking.value
        if (ranking.length > 0) {
            const completedCount = ranking.filter((r: any) => r.completed).length
            const totalScore = ranking.reduce((sum: number, r: any) => sum + (r.score || 0), 0)
            const hw = homeworkList.value.find(h => h.id === selectedHomeworkId.value)
            homeworkSingleStats.value = {
                problemCount: hw?.problemCount || 0,
                completionRate: `${Math.round(completedCount / ranking.length * 100)}%`,
                averageScore: ranking.length > 0 ? (totalScore / ranking.length).toFixed(1) : '0'
            }
        } else {
            homeworkSingleStats.value = {}
        }
    } catch (error) {
        homeworkRanking.value = []
        homeworkSingleStats.value = {}
    } finally {
        statsLoading.value = false
    }
}

// 加载题库训练排名
const loadPracticeRanking = async () => {
    if (!currentClass.value) return
    statsLoading.value = true
    try {
        const response = await request.get(`/course/statistics/practice/${currentClass.value.id}`)
        practiceRanking.value = response || []
        
        // 计算题库训练的统计数据
        const ranking = practiceRanking.value
        if (ranking.length > 0) {
            const maxSolvedCount = Math.max(...ranking.map((r: any) => r.solvedCount || 0))
            const totalSubmit = ranking.reduce((sum: number, r: any) => sum + (r.submitCount || 0), 0)
            const totalSolved = ranking.reduce((sum: number, r: any) => sum + (r.solvedCount || 0), 0)
            const totalScore = ranking.reduce((sum: number, r: any) => sum + (r.totalScore || 0), 0)
            practiceStats.value = {
                maxSolvedCount,
                totalAcceptRate: totalSubmit > 0 ? `${Math.round(totalSolved / totalSubmit * 100)}%` : '0%',
                averageScore: ranking.length > 0 ? (totalScore / ranking.length).toFixed(1) : '0'
            }
        } else {
            practiceStats.value = {}
        }
    } catch (error) {
        practiceRanking.value = []
        practiceStats.value = {}
    } finally {
        statsLoading.value = false
    }
}

// 获取当前排名数据
const getCurrentRankingData = () => {
    if (rankingType.value === 'homework_total') return studentRanking.value
    if (rankingType.value === 'homework_single') return homeworkRanking.value
    if (rankingType.value === 'practice') return practiceRanking.value
    return []
}

// 格式化时间
const formatTime = (time: string) => {
    if (!time) return '-'
    return new Date(time).toLocaleString('zh-CN')
}

// 编辑班级
const editClass = (classData: CourseClass) => {
    currentClass.value = classData
    Object.assign(editForm, {
        id: classData.id,
        name: classData.name,
        maxStudents: classData.maxStudents || 100,
        status: classData.status ?? 1
    })
    showEditDialog.value = true
}

// 更新班级
const handleUpdateClass = async () => {
    if (!editForm.name.trim()) {
        ElMessage.warning('请输入班级名称')
        return
    }
    
    submitting.value = true
    try {
        await updateClass({
            id: editForm.id,
            name: editForm.name,
            maxStudents: editForm.maxStudents,
            status: editForm.status
        })
        ElMessage.success('更新成功')
        showEditDialog.value = false
        loadClasses()
    } catch (error) {
        ElMessage.error('更新失败')
    } finally {
        submitting.value = false
    }
}

// 跳转到详细统计页面
const goToDetailStats = (classData: CourseClass) => {
    router.push({
        path: '/teacher/statistics',
        query: { courseId: selectedCourseId.value, classId: classData.id }
    })
}

// 删除班级
const handleDeleteClass = async (classData: CourseClass) => {
    try {
        await ElMessageBox.confirm('确定删除该班级吗？此操作不可恢复', '警告', { type: 'warning' })
        await deleteClass(classData.id)
        ElMessage.success('删除成功')
        loadClasses()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error('删除失败')
        }
    }
}

// 获取排名样式
const getRankClass = (rank: number) => {
    if (rank === 1) return 'rank-gold'
    if (rank === 2) return 'rank-silver'
    if (rank === 3) return 'rank-bronze'
    return ''
}

onMounted(() => {
    loadCourses()
})
</script>

<style scoped lang="scss">
.class-manage {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.toolbar {
    margin: 20px 0;
    
    .toolbar-content {
        display: flex;
        gap: 12px;
        align-items: center;
    }
}

.class-list {
    margin-top: 20px;
}

.stats-content {
    .stats-overview {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 20px;
        margin-bottom: 20px;
    }
    
    .stat-item {
        text-align: center;
        padding: 20px;
        background: #f5f7fa;
        border-radius: 8px;
        
        .stat-value {
            font-size: 28px;
            font-weight: bold;
            color: #409eff;
        }
        
        .stat-label {
            font-size: 14px;
            color: #909399;
            margin-top: 8px;
        }
    }
}

.ranking-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;
    
    .ranking-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
    }
}

.rank-gold {
    color: #ffd700;
    font-weight: bold;
    font-size: 18px;
}

.rank-silver {
    color: #c0c0c0;
    font-weight: bold;
    font-size: 16px;
}

.rank-bronze {
    color: #cd7f32;
    font-weight: bold;
}

.students-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    span {
        font-size: 14px;
        color: #606266;
    }
}
</style>
