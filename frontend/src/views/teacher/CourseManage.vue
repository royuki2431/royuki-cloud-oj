<template>
    <div class="course-manage">
        <div class="page-header">
            <h2>课程管理</h2>
            <el-button type="primary" @click="handleCreate">
                <el-icon><Plus /></el-icon>
                创建课程
            </el-button>
        </div>

        <el-card class="course-list" shadow="hover" v-loading="loading">
            <el-table :data="courses" stripe>
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="name" label="课程名称" min-width="180">
                    <template #default="{ row }">
                        <el-link type="primary" @click="viewCourseDetail(row)">{{ row.name }}</el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
                <el-table-column prop="semester" label="学期" width="120" />
                <el-table-column label="班级数" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag type="info">{{ row.classCount || 0 }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="学生数" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag type="success">{{ row.studentCount || 0 }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdTime" label="创建时间" width="180">
                    <template #default="{ row }">
                        {{ formatTime(row.createdTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'">
                            {{ row.status === 1 ? '进行中' : '已结束' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="280" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button type="success" size="small" @click="manageClasses(row)">班级</el-button>
                        <el-button type="warning" size="small" @click="viewStatistics(row)">统计</el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="courses.length === 0 && !loading" description="暂无课程，点击上方按钮创建" />
        </el-card>

        <!-- 创建/编辑课程对话框 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑课程' : '创建课程'" width="600px" destroy-on-close>
            <el-form :model="courseForm" :rules="rules" ref="formRef" label-width="100px">
                <el-form-item label="课程名称" prop="name">
                    <el-input v-model="courseForm.name" placeholder="请输入课程名称" />
                </el-form-item>
                <el-form-item label="学期" prop="semester">
                    <el-select v-model="courseForm.semester" placeholder="选择学期" style="width: 100%">
                        <el-option label="2024-2025学年第一学期" value="2024-2025-1" />
                        <el-option label="2024-2025学年第二学期" value="2024-2025-2" />
                        <el-option label="2025-2026学年第一学期" value="2025-2026-1" />
                        <el-option label="2025-2026学年第二学期" value="2025-2026-2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="课程描述">
                    <el-input v-model="courseForm.description" type="textarea" :rows="4" placeholder="请输入课程描述" />
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch 
                        v-model="courseForm.status" 
                        :active-value="1" 
                        :inactive-value="0"
                        active-text="进行中"
                        inactive-text="已结束"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">
                    {{ isEdit ? '保存' : '创建' }}
                </el-button>
            </template>
        </el-dialog>

        <!-- 课程统计对话框 -->
        <el-dialog v-model="statsDialogVisible" :title="`课程统计 - ${currentCourse?.name}`" width="900px">
            <div class="stats-overview" v-loading="statsLoading">
                <el-row :gutter="20">
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-value">{{ courseStats.classCount || 0 }}</div>
                            <div class="stat-label">班级数量</div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-value success">{{ courseStats.studentCount || 0 }}</div>
                            <div class="stat-label">学生总数</div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-value warning">{{ courseStats.homeworkCount || 0 }}</div>
                            <div class="stat-label">作业数量</div>
                        </div>
                    </el-col>
                    <el-col :span="6">
                        <div class="stat-card">
                            <div class="stat-value primary">{{ courseStats.avgScore?.toFixed(1) || 0 }}</div>
                            <div class="stat-label">平均分</div>
                        </div>
                    </el-col>
                </el-row>

                <el-divider>班级列表</el-divider>
                
                <el-table :data="courseClasses" max-height="300">
                    <el-table-column prop="name" label="班级名称" />
                    <el-table-column prop="code" label="邀请码" width="120">
                        <template #default="{ row }">
                            <el-tag type="success" @click="copyCode(row.code)" style="cursor: pointer">
                                {{ row.code }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column prop="studentCount" label="学生数" width="100" align="center" />
                    <el-table-column label="完成率" width="150">
                        <template #default="{ row }">
                            <el-progress :percentage="row.completionRate || 0" :stroke-width="8" />
                        </template>
                    </el-table-column>
                    <el-table-column label="平均分" width="100" align="center">
                        <template #default="{ row }">
                            {{ row.avgScore?.toFixed(1) || '-' }}
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { 
    getTeacherCourses, 
    createCourse, 
    updateCourse, 
    deleteCourse,
    getCourseClasses,
    getCourseStatistics,
    type Course, 
    type CourseClass 
} from '@/api/course'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const statsDialogVisible = ref(false)
const statsLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const courses = ref<Course[]>([])
const currentCourse = ref<Course | null>(null)
const courseStats = ref<any>({})
const courseClasses = ref<CourseClass[]>([])

const courseForm = reactive({
    id: undefined as number | undefined,
    name: '',
    description: '',
    semester: '2024-2025-2',
    status: 1
})

const rules: FormRules = {
    name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
    semester: [{ required: true, message: '请选择学期', trigger: 'change' }]
}

// 获取当前用户ID
const getUserId = () => {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
        return JSON.parse(userInfo).id
    }
    return null
}

// 加载课程列表
const loadCourses = async () => {
    const userId = getUserId()
    if (!userId) {
        ElMessage.warning('请先登录')
        return
    }
    
    loading.value = true
    try {
        const list = await getTeacherCourses(userId)
        // 为每个课程加载班级数量
        for (const course of list) {
            try {
                const classes = await getCourseClasses(course.id)
                ;(course as any).classCount = classes.length
                ;(course as any).studentCount = classes.reduce((sum, c) => sum + (c.studentCount || 0), 0)
            } catch {
                (course as any).classCount = 0
            }
        }
        courses.value = list
    } catch (error) {
        console.error('加载课程失败:', error)
        ElMessage.error('加载课程失败')
    } finally {
        loading.value = false
    }
}

// 创建课程
const handleCreate = () => {
    isEdit.value = false
    Object.assign(courseForm, {
        id: undefined,
        name: '',
        description: '',
        semester: '2024-2025-2',
        status: 1
    })
    dialogVisible.value = true
}

// 编辑课程
const handleEdit = (row: Course) => {
    isEdit.value = true
    Object.assign(courseForm, {
        id: row.id,
        name: row.name,
        description: row.description || '',
        semester: row.semester || '2024-2025-2',
        status: row.status
    })
    dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return
    
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        
        const userId = getUserId()
        if (!userId) return
        
        submitting.value = true
        try {
            if (isEdit.value) {
                await updateCourse({
                    id: courseForm.id,
                    name: courseForm.name,
                    description: courseForm.description,
                    semester: courseForm.semester,
                    status: courseForm.status
                })
                ElMessage.success('更新成功')
            } else {
                await createCourse({
                    name: courseForm.name,
                    description: courseForm.description,
                    semester: courseForm.semester,
                    teacherId: userId,
                    status: 1
                })
                ElMessage.success('创建成功')
            }
            dialogVisible.value = false
            loadCourses()
        } catch (error: any) {
            ElMessage.error(error.message || '操作失败')
        } finally {
            submitting.value = false
        }
    })
}

// 删除课程
const handleDelete = async (row: Course) => {
    try {
        await ElMessageBox.confirm(`确定要删除课程"${row.name}"吗？此操作不可恢复`, '确认删除', { type: 'warning' })
        await deleteCourse(row.id)
        ElMessage.success('删除成功')
        loadCourses()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '删除失败')
        }
    }
}

// 查看课程详情
const viewCourseDetail = (row: Course) => {
    viewStatistics(row)
}

// 班级管理
const manageClasses = (row: Course) => {
    router.push({ path: '/teacher/classes', query: { courseId: row.id } })
}

// 查看统计
const viewStatistics = async (row: Course) => {
    currentCourse.value = row
    statsDialogVisible.value = true
    statsLoading.value = true
    
    try {
        const [stats, classes] = await Promise.all([
            getCourseStatistics(row.id).catch(() => ({})),
            getCourseClasses(row.id)
        ])
        courseStats.value = {
            classCount: classes.length,
            studentCount: classes.reduce((sum, c) => sum + (c.studentCount || 0), 0),
            ...stats
        }
        courseClasses.value = classes
    } catch (error) {
        console.error('加载统计失败:', error)
    } finally {
        statsLoading.value = false
    }
}

// 复制邀请码
const copyCode = (code: string) => {
    navigator.clipboard.writeText(code)
    ElMessage.success('邀请码已复制')
}

// 格式化时间
const formatTime = (time: string) => {
    if (!time) return '-'
    return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
    loadCourses()
})
</script>

<style scoped lang="scss">
.course-manage {
    padding: 20px;
    max-width: 1400px;
    margin: 0 auto;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
        margin: 0;
        font-size: 24px;
        color: #303133;
    }
}

.course-list {
    margin-top: 20px;
}

.stats-overview {
    .stat-card {
        text-align: center;
        padding: 20px;
        background: #f5f7fa;
        border-radius: 8px;
        
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
</style>
