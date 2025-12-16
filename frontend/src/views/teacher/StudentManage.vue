<template>
    <div class="student-manage">
        <div class="page-header">
            <h2>学生管理</h2>
        </div>
        
        <!-- 筛选区域 -->
        <el-card class="filter-card" shadow="never">
            <el-row :gutter="16">
                <el-col :span="6">
                    <el-select v-model="selectedCourseId" placeholder="选择课程" @change="onCourseChange" style="width: 100%">
                        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </el-col>
                <el-col :span="6">
                    <el-select v-model="selectedClassId" placeholder="选择班级" @change="loadStudents" style="width: 100%" clearable>
                        <el-option label="全部班级" :value="null" />
                        <el-option v-for="cls in classes" :key="cls.id" :label="cls.name" :value="cls.id" />
                    </el-select>
                </el-col>
                <el-col :span="6">
                    <el-input v-model="searchKeyword" placeholder="搜索学生姓名/用户名" clearable @input="filterStudents">
                        <template #prefix>
                            <el-icon><Search /></el-icon>
                        </template>
                    </el-input>
                </el-col>
                <el-col :span="6">
                    <el-button @click="resetFilter">重置</el-button>
                    <el-button type="primary" @click="exportStudents">
                        <el-icon><Download /></el-icon>
                        导出
                    </el-button>
                </el-col>
            </el-row>
        </el-card>

        <!-- 统计概览 -->
        <el-row :gutter="16" class="stats-row">
            <el-col :span="6">
                <el-card class="stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-value">{{ stats.totalStudents }}</div>
                        <div class="stat-label">学生总数</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card class="stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-value success">{{ stats.activeStudents }}</div>
                        <div class="stat-label">活跃学生</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card class="stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-value warning">{{ stats.avgCompletion }}%</div>
                        <div class="stat-label">平均完成率</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :span="6">
                <el-card class="stat-card" shadow="hover">
                    <div class="stat-content">
                        <div class="stat-value primary">{{ stats.avgScore }}</div>
                        <div class="stat-label">平均分</div>
                    </div>
                </el-card>
            </el-col>
        </el-row>
        
        <!-- 学生列表 -->
        <el-card class="student-list" shadow="hover" v-loading="loading">
            <el-table :data="filteredStudents" stripe @sort-change="handleSortChange">
                <el-table-column prop="studentId" label="ID" width="80" />
                <el-table-column prop="studentName" label="姓名" width="120" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="className" label="班级" width="150" />
                <el-table-column prop="joinTime" label="加入时间" width="180" sortable>
                    <template #default="{ row }">
                        {{ formatTime(row.joinTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="作业完成" width="120" align="center" sortable="custom" prop="completionRate">
                    <template #default="{ row }">
                        <el-progress 
                            :percentage="row.completionRate || 0" 
                            :stroke-width="8"
                            :color="getProgressColor(row.completionRate)"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="平均分" width="100" align="center" sortable="custom" prop="avgScore">
                    <template #default="{ row }">
                        <span :class="getScoreClass(row.avgScore)">{{ row.avgScore?.toFixed(1) || '-' }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewProgress(row)">进度</el-button>
                        <el-button type="success" size="small" @click="viewSubmissions(row)">提交</el-button>
                        <el-button type="danger" size="small" @click="removeFromClass(row)">移除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="filteredStudents.length === 0 && !loading" description="暂无学生数据" />
        </el-card>

        <!-- 学习进度对话框 -->
        <el-dialog v-model="progressDialogVisible" :title="`学习进度 - ${currentStudent?.studentName}`" width="800px">
            <div class="progress-content" v-loading="progressLoading">
                <el-descriptions :column="3" border>
                    <el-descriptions-item label="学生姓名">{{ currentStudent?.studentName }}</el-descriptions-item>
                    <el-descriptions-item label="用户名">{{ currentStudent?.username }}</el-descriptions-item>
                    <el-descriptions-item label="所属班级">{{ currentStudent?.className }}</el-descriptions-item>
                    <el-descriptions-item label="加入时间">{{ formatTime(currentStudent?.joinTime) }}</el-descriptions-item>
                    <el-descriptions-item label="作业完成率">{{ currentStudent?.completionRate || 0 }}%</el-descriptions-item>
                    <el-descriptions-item label="平均分">{{ currentStudent?.avgScore?.toFixed(1) || '-' }}</el-descriptions-item>
                </el-descriptions>

                <el-divider>作业完成情况</el-divider>
                
                <el-table :data="studentHomeworks" max-height="300">
                    <el-table-column prop="title" label="作业名称" />
                    <el-table-column label="状态" width="100" align="center">
                        <template #default="{ row }">
                            <el-tag :type="row.completed ? 'success' : 'warning'" size="small">
                                {{ row.completed ? '已完成' : '未完成' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="得分" width="100" align="center">
                        <template #default="{ row }">
                            {{ row.score !== null ? row.score : '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column label="提交时间" width="180">
                        <template #default="{ row }">
                            {{ row.submitTime ? formatTime(row.submitTime) : '-' }}
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </el-dialog>

        <!-- 提交记录对话框 -->
        <el-dialog v-model="submissionsDialogVisible" :title="`提交记录 - ${currentStudent?.studentName}`" width="900px">
            <el-table :data="studentSubmissions" max-height="400" v-loading="submissionsLoading">
                <el-table-column prop="problemId" label="题目ID" width="80" />
                <el-table-column prop="problemTitle" label="题目名称" min-width="150" />
                <el-table-column label="状态" width="120" align="center">
                    <template #default="{ row }">
                        <el-tag :type="getStatusType(row.status)" size="small">
                            {{ row.status }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="language" label="语言" width="80" />
                <el-table-column prop="score" label="得分" width="80" align="center" />
                <el-table-column prop="submitTime" label="提交时间" width="180">
                    <template #default="{ row }">
                        {{ formatTime(row.submitTime) }}
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { 
    getTeacherCourses, 
    getCourseClasses, 
    getClassStudents,
    removeStudent,
    getStudentHomeworks,
    type Course, 
    type CourseClass 
} from '@/api/course'
import { getUserSubmissions } from '@/api/judge'

const loading = ref(false)
const progressLoading = ref(false)
const submissionsLoading = ref(false)
const courses = ref<Course[]>([])
const classes = ref<CourseClass[]>([])
const students = ref<any[]>([])
const selectedCourseId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)
const searchKeyword = ref('')

const progressDialogVisible = ref(false)
const submissionsDialogVisible = ref(false)
const currentStudent = ref<any>(null)
const studentHomeworks = ref<any[]>([])
const studentSubmissions = ref<any[]>([])

const stats = ref({
    totalStudents: 0,
    activeStudents: 0,
    avgCompletion: 0,
    avgScore: 0
})

// 获取当前用户ID
const getUserId = () => {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
        return JSON.parse(userInfo).id
    }
    return null
}

// 筛选学生
const filteredStudents = computed(() => {
    if (!searchKeyword.value) return students.value
    const keyword = searchKeyword.value.toLowerCase()
    return students.value.filter(s => 
        s.studentName?.toLowerCase().includes(keyword) ||
        s.username?.toLowerCase().includes(keyword) ||
        s.studentId?.toString().includes(keyword)
    )
})

// 加载课程
const loadCourses = async () => {
    const userId = getUserId()
    if (!userId) return
    
    try {
        courses.value = await getTeacherCourses(userId)
        if (courses.value.length > 0) {
            selectedCourseId.value = courses.value[0].id
            await onCourseChange()
        }
    } catch (error) {
        console.error('加载课程失败:', error)
    }
}

// 课程变化
const onCourseChange = async () => {
    if (!selectedCourseId.value) return
    
    try {
        classes.value = await getCourseClasses(selectedCourseId.value)
        selectedClassId.value = null
        await loadAllStudents()
    } catch (error) {
        console.error('加载班级失败:', error)
    }
}

// 加载所有学生（按课程）
const loadAllStudents = async () => {
    if (!selectedCourseId.value) return
    
    loading.value = true
    students.value = []
    
    try {
        // 获取该课程下所有班级的学生
        for (const cls of classes.value) {
            const classStudents = await getClassStudents(cls.id)
            classStudents.forEach((s: any) => {
                s.className = cls.name
                s.classId = cls.id
            })
            students.value.push(...classStudents)
        }
        
        // 计算统计数据
        calculateStats()
    } catch (error) {
        console.error('加载学生失败:', error)
        ElMessage.error('加载学生列表失败')
    } finally {
        loading.value = false
    }
}

// 加载指定班级学生
const loadStudents = async () => {
    if (!selectedClassId.value) {
        await loadAllStudents()
        return
    }
    
    loading.value = true
    try {
        const classStudents = await getClassStudents(selectedClassId.value)
        const cls = classes.value.find(c => c.id === selectedClassId.value)
        classStudents.forEach((s: any) => {
            s.className = cls?.name || '-'
            s.classId = selectedClassId.value
        })
        students.value = classStudents
        calculateStats()
    } catch (error) {
        console.error('加载学生失败:', error)
        ElMessage.error('加载学生列表失败')
    } finally {
        loading.value = false
    }
}

// 计算统计数据
const calculateStats = () => {
    const total = students.value.length
    const active = students.value.filter(s => (s.completionRate || 0) > 0).length
    const avgCompletion = total > 0 
        ? students.value.reduce((sum, s) => sum + (s.completionRate || 0), 0) / total 
        : 0
    const avgScore = total > 0 
        ? students.value.reduce((sum, s) => sum + (s.avgScore || 0), 0) / total 
        : 0
    
    stats.value = {
        totalStudents: total,
        activeStudents: active,
        avgCompletion: Math.round(avgCompletion),
        avgScore: Math.round(avgScore * 10) / 10
    }
}

// 筛选学生
const filterStudents = () => {
    // 使用computed自动处理
}

// 重置筛选
const resetFilter = () => {
    searchKeyword.value = ''
    selectedClassId.value = null
    loadAllStudents()
}

// 导出学生数据
const exportStudents = () => {
    if (filteredStudents.value.length === 0) {
        ElMessage.warning('没有可导出的数据')
        return
    }
    
    // 准备CSV数据
    const headers = ['学生ID', '姓名', '用户名', '班级', '加入时间', '作业完成率(%)', '平均分']
    const rows = filteredStudents.value.map(s => [
        s.studentId,
        s.studentName || '',
        s.username || '',
        s.className || '',
        formatTime(s.joinTime),
        s.completionRate || 0,
        s.avgScore?.toFixed(1) || 0
    ])
    
    // 添加BOM以支持中文
    let csvContent = '\uFEFF' + headers.join(',') + '\n'
    rows.forEach(row => {
        csvContent += row.map(cell => `"${cell}"`).join(',') + '\n'
    })
    
    // 创建下载链接
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `学生数据_${new Date().toLocaleDateString()}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('导出成功')
}

// 查看学习进度
const viewProgress = async (student: any) => {
    currentStudent.value = student
    progressDialogVisible.value = true
    progressLoading.value = true
    
    try {
        // 获取学生的作业完成情况
        const homeworks = await getStudentHomeworks(student.studentId)
        
        // 过滤出当前课程的作业
        studentHomeworks.value = homeworks.filter((hw: any) => {
            // 如果作业属于当前选择的课程
            return hw.courseId === selectedCourseId.value
        }).map((hw: any) => ({
            id: hw.id,
            title: hw.title,
            completed: hw.isCompleted || false,
            score: hw.earnedScore ?? null,
            submitTime: hw.lastSubmitTime
        }))
        
        // 如果没有过滤到数据，显示所有作业
        if (studentHomeworks.value.length === 0 && homeworks.length > 0) {
            studentHomeworks.value = homeworks.map((hw: any) => ({
                id: hw.id,
                title: hw.title,
                completed: hw.isCompleted || false,
                score: hw.earnedScore ?? null,
                submitTime: hw.lastSubmitTime
            }))
        }
    } catch (error) {
        console.error('加载进度失败:', error)
        ElMessage.error('加载学习进度失败')
    } finally {
        progressLoading.value = false
    }
}

// 查看提交记录
const viewSubmissions = async (student: any) => {
    currentStudent.value = student
    submissionsDialogVisible.value = true
    submissionsLoading.value = true
    
    try {
        // 获取学生的提交记录
        const submissions = await getUserSubmissions({
            userId: student.studentId,
            page: 1,
            size: 50
        })
        
        studentSubmissions.value = submissions.map((sub: any) => ({
            id: sub.id,
            problemId: sub.problemId,
            problemTitle: sub.problemTitle || `题目${sub.problemId}`,
            status: sub.status,
            language: sub.language,
            score: sub.score,
            submitTime: sub.createTime
        }))
    } catch (error) {
        console.error('加载提交记录失败:', error)
        ElMessage.error('加载提交记录失败')
    } finally {
        submissionsLoading.value = false
    }
}

// 从班级移除学生
const removeFromClass = async (student: any) => {
    try {
        await ElMessageBox.confirm(
            `确定要将学生"${student.studentName}"从班级"${student.className}"中移除吗？`,
            '确认移除',
            { type: 'warning' }
        )
        await removeStudent(student.classId, student.studentId)
        ElMessage.success('移除成功')
        loadStudents()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '移除失败')
        }
    }
}

// 排序处理
const handleSortChange = ({ prop, order }: any) => {
    if (!prop || !order) return
    
    students.value.sort((a, b) => {
        const aVal = a[prop] || 0
        const bVal = b[prop] || 0
        return order === 'ascending' ? aVal - bVal : bVal - aVal
    })
}

// 格式化时间
const formatTime = (time: string) => {
    if (!time) return '-'
    return new Date(time).toLocaleString('zh-CN')
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
    if (percentage >= 80) return '#67c23a'
    if (percentage >= 60) return '#e6a23c'
    return '#f56c6c'
}

// 获取分数样式
const getScoreClass = (score: number) => {
    if (score >= 90) return 'score-excellent'
    if (score >= 60) return 'score-pass'
    return 'score-fail'
}

// 获取状态类型
const getStatusType = (status: string) => {
    if (status === 'ACCEPTED') return 'success'
    if (status === 'WRONG_ANSWER') return 'danger'
    if (status === 'PENDING') return 'info'
    return 'warning'
}

onMounted(() => {
    loadCourses()
})
</script>

<style scoped lang="scss">
.student-manage {
    padding: 20px;
    max-width: 1400px;
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
}

.stats-row {
    margin-bottom: 20px;
    
    .stat-card {
        .stat-content {
            text-align: center;
            padding: 10px 0;
            
            .stat-value {
                font-size: 28px;
                font-weight: bold;
                color: #303133;
                
                &.success { color: #67c23a; }
                &.warning { color: #e6a23c; }
                &.primary { color: #409eff; }
            }
            
            .stat-label {
                font-size: 14px;
                color: #909399;
                margin-top: 8px;
            }
        }
    }
}

.student-list {
    margin-top: 20px;
}

.progress-content {
    .el-descriptions {
        margin-bottom: 20px;
    }
}

.score-excellent { color: #67c23a; font-weight: 600; }
.score-pass { color: #e6a23c; font-weight: 600; }
.score-fail { color: #f56c6c; font-weight: 600; }
</style>
