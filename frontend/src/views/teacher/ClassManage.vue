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
                <el-table-column label="操作" width="320">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewStudents(row)">
                            学生管理
                        </el-button>
                        <el-button type="success" size="small" @click="viewStatistics(row)">
                            统计
                        </el-button>
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
        <el-dialog v-model="showStudentsDialog" :title="`学生管理 - ${currentClass?.name}`" width="700px">
            <el-table :data="students" v-loading="studentsLoading">
                <el-table-column prop="studentId" label="学生ID" width="100" />
                <el-table-column prop="joinTime" label="加入时间">
                    <template #default="{ row }">
                        {{ formatTime(row.joinTime) }}
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

        <!-- 统计对话框 -->
        <el-dialog v-model="showStatsDialog" :title="`班级统计 - ${currentClass?.name}`" width="800px">
            <div class="stats-content" v-loading="statsLoading">
                <div class="stats-overview">
                    <div class="stat-item">
                        <div class="stat-value">{{ classStats.studentCount || 0 }}</div>
                        <div class="stat-label">学生人数</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">{{ classStats.homeworkCount || 0 }}</div>
                        <div class="stat-label">作业数量</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">{{ classStats.completionRate || '0%' }}</div>
                        <div class="stat-label">完成率</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value">{{ classStats.averageScore || '0' }}</div>
                        <div class="stat-label">平均分</div>
                    </div>
                </div>
                
                <el-divider>学生排名</el-divider>
                
                <el-table :data="studentRanking" max-height="300">
                    <el-table-column label="排名" width="80" align="center">
                        <template #default="{ row }">
                            <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="studentId" label="学生ID" />
                    <el-table-column prop="totalScore" label="总分" align="center" />
                    <el-table-column prop="submissionCount" label="提交次数" align="center" />
                    <el-table-column prop="averageScore" label="平均分" align="center" />
                </el-table>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { 
    getTeacherCourses,
    getCourseClasses,
    createClass,
    deleteClass,
    getClassStudents,
    removeStudent,
    getClassStatistics,
    getStudentRanking,
    type Course,
    type CourseClass
} from '@/api/course'

const loading = ref(false)
const submitting = ref(false)
const courses = ref<Course[]>([])
const classes = ref<CourseClass[]>([])
const selectedCourseId = ref<number | null>(null)
const showCreateDialog = ref(false)
const showStudentsDialog = ref(false)
const showStatsDialog = ref(false)
const currentClass = ref<CourseClass | null>(null)

// 学生管理
const students = ref<any[]>([])
const studentsLoading = ref(false)

// 统计
const classStats = ref<any>({})
const studentRanking = ref<any[]>([])
const statsLoading = ref(false)

const classForm = reactive({
    name: '',
    maxStudents: 100,
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
            selectedCourseId.value = courses.value[0].id
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
    
    try {
        const [stats, ranking] = await Promise.all([
            getClassStatistics(classData.id),
            getStudentRanking(classData.id)
        ])
        classStats.value = stats
        studentRanking.value = ranking
    } catch (error) {
        ElMessage.error('加载统计失败')
    } finally {
        statsLoading.value = false
    }
}

// 编辑班级
const editClass = (classData: CourseClass) => {
    ElMessage.info('编辑功能开发中')
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

// 格式化时间
const formatTime = (time: string) => {
    if (!time) return '-'
    return new Date(time).toLocaleString('zh-CN')
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
</style>
