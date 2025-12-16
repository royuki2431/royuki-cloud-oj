<template>
    <div class="my-classes">
        <div class="page-header">
            <h2>我的班级</h2>
            <el-button type="primary" @click="showJoinDialog">
                <el-icon><Plus /></el-icon>
                加入班级
            </el-button>
        </div>

        <!-- 班级卡片列表 -->
        <div class="class-grid" v-loading="loading">
            <el-empty v-if="classes.length === 0 && !loading" description="还没有加入任何班级">
                <el-button type="primary" @click="showJoinDialog">
                    立即加入
                </el-button>
            </el-empty>
            
            <el-card 
                v-for="cls in classes" 
                :key="cls.classId" 
                class="class-card"
                @click="viewClass(cls)"
            >
                <div class="class-cover">
                    <div class="cover-placeholder">
                        <el-icon :size="48"><School /></el-icon>
                    </div>
                </div>
                <div class="class-info">
                    <h3 class="class-name">{{ cls.className }}</h3>
                    <p class="class-course">
                        <el-icon><Reading /></el-icon>
                        {{ cls.courseName || '课程' }}
                    </p>
                    <p class="class-teacher">
                        <el-icon><User /></el-icon>
                        {{ cls.teacherName || '教师' }}
                    </p>
                    <p class="class-meta">
                        <span>班级码: {{ cls.classCode }}</span>
                        <span>{{ cls.studentCount || 0 }} 名学生</span>
                    </p>
                    <el-tag type="success" size="small">学习中</el-tag>
                </div>
            </el-card>
        </div>

        <!-- 班级详情对话框 -->
        <el-dialog v-model="classDialogVisible" :title="currentClass?.className" width="800px">
            <div class="class-detail" v-if="currentClass">
                <el-descriptions :column="2" border>
                    <el-descriptions-item label="班级名称">{{ currentClass.className }}</el-descriptions-item>
                    <el-descriptions-item label="班级码">{{ currentClass.classCode }}</el-descriptions-item>
                    <el-descriptions-item label="所属课程">{{ currentClass.courseName }}</el-descriptions-item>
                    <el-descriptions-item label="授课教师">{{ currentClass.teacherName }}</el-descriptions-item>
                    <el-descriptions-item label="学生人数">{{ currentClass.studentCount || 0 }} 人</el-descriptions-item>
                    <el-descriptions-item label="加入时间">{{ formatTime(currentClass.joinTime) }}</el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">课程信息</el-divider>
                <div class="course-section" v-if="courseDetail">
                    <el-descriptions :column="2" border>
                        <el-descriptions-item label="课程名称">{{ courseDetail.name }}</el-descriptions-item>
                        <el-descriptions-item label="学期">{{ courseDetail.semester || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="课程描述" :span="2">{{ courseDetail.description || '暂无描述' }}</el-descriptions-item>
                    </el-descriptions>
                </div>

                <el-collapse v-model="activeCollapse">

                
                    <el-collapse-item name="homeworks">
                        <template #title>
                            <span class="collapse-title">班级作业 ({{ classHomeworks.length }}个)</span>
                        </template>
                        <el-table :data="classHomeworks" max-height="300" v-loading="loadingHomeworks" size="small">
                            <el-table-column prop="title" label="作业标题" min-width="180">
                                <template #default="{ row }">
                                    <el-link type="primary" @click="goToHomework(row)">{{ row.title }}</el-link>
                                </template>
                            </el-table-column>
                            <el-table-column label="截止时间" width="180">
                                <template #default="{ row }">
                                    <span :class="{ 'text-danger': isOverdue(row.endTime) }">
                                        {{ formatTime(row.endTime) }}
                                    </span>
                                </template>
                            </el-table-column>
                            <el-table-column prop="totalScore" label="总分" width="80" align="center" />
                            <el-table-column label="状态" width="100" align="center">
                                <template #default="{ row }">
                                    <el-tag :type="isOverdue(row.endTime) ? 'info' : 'success'" size="small">
                                        {{ isOverdue(row.endTime) ? '已截止' : '进行中' }}
                                    </el-tag>
                                </template>
                            </el-table-column>
                        </el-table>
                        <el-empty v-if="classHomeworks.length === 0 && !loadingHomeworks" description="暂无作业" :image-size="60" />
                    </el-collapse-item>

                    <el-collapse-item name="students">
                        <template #title>
                            <span class="collapse-title">班级学生 ({{ classStudents.length }}人)</span>
                        </template>
                        <el-table :data="classStudents" max-height="300" v-loading="loadingStudents" size="small">
                            <el-table-column type="index" label="序号" width="60" align="center" />
                            <el-table-column prop="studentName" label="姓名" min-width="100" />
                            <el-table-column prop="username" label="用户名" min-width="100" />
                            <el-table-column prop="studentNo" label="学号" min-width="120" />
                            <el-table-column prop="joinTime" label="加入时间" width="160">
                                <template #default="{ row }">
                                    {{ formatTime(row.joinTime) }}
                                </template>
                            </el-table-column>
                        </el-table>
                        <el-empty v-if="classStudents.length === 0 && !loadingStudents" description="暂无学生" :image-size="60" />
                    </el-collapse-item>
                    

                </el-collapse>
            </div>
        </el-dialog>

        <!-- 加入班级对话框 -->
        <el-dialog v-model="joinDialogVisible" title="加入班级" width="450px" :close-on-click-modal="false">
            <div class="join-dialog-content">
                <div class="join-icon-wrapper">
                    <el-icon class="join-icon"><Postcard /></el-icon>
                </div>
                <p class="join-tip">请输入教师提供的班级邀请码</p>
                
                <el-form :model="joinForm" ref="joinFormRef" :rules="joinRules" label-width="80px">
                    <el-form-item label="邀请码" prop="inviteCode">
                        <el-input
                            v-model="joinForm.inviteCode"
                            placeholder="请输入邀请码"
                            maxlength="8"
                            show-word-limit
                            clearable
                            @keyup.enter="handleJoinClass"
                        />
                    </el-form-item>
                </el-form>
                
                <el-alert type="info" :closable="false" show-icon style="margin-top: 16px">
                    <template #title>提示</template>
                    <template #default>
                        <ul class="tips-list">
                            <li>邀请码由教师创建班级时生成</li>
                            <li>邀请码为6-8位字符，不区分大小写</li>
                        </ul>
                    </template>
                </el-alert>
            </div>
            <template #footer>
                <el-button @click="joinDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleJoinClass" :loading="joinLoading">
                    加入班级
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Reading, User, School, Postcard } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

interface StudentClass {
    classId: number
    className: string
    classCode: string
    courseId: number
    courseName: string
    teacherName: string
    studentCount: number
    joinTime: string
}

interface Homework {
    id: number
    title: string
    endTime: string
    totalScore: number
}

interface Course {
    id: number
    name: string
    description: string
    semester: string
}

const router = useRouter()
const loading = ref(false)
const loadingHomeworks = ref(false)
const loadingStudents = ref(false)
const classes = ref<StudentClass[]>([])
const classDialogVisible = ref(false)
const currentClass = ref<StudentClass | null>(null)
const classHomeworks = ref<Homework[]>([])
const classStudents = ref<any[]>([])
const courseDetail = ref<Course | null>(null)
const activeCollapse = ref<string[]>(['students', 'homeworks'])

// 加入班级相关
const joinDialogVisible = ref(false)
const joinLoading = ref(false)
const joinFormRef = ref<FormInstance>()
const joinForm = reactive({
    inviteCode: ''
})
const joinRules: FormRules = {
    inviteCode: [
        { required: true, message: '请输入邀请码', trigger: 'blur' },
        { min: 6, max: 8, message: '邀请码为6-8位字符', trigger: 'blur' }
    ]
}

// 获取当前用户ID
const getUserId = () => {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
        return JSON.parse(userInfo).id
    }
    return null
}

const loadClasses = async () => {
    const userId = getUserId()
    if (!userId) {
        ElMessage.warning('请先登录')
        return
    }
    
    loading.value = true
    try {
        const res = await request.get(`/course/student/classes/${userId}`)
        classes.value = res || []
    } catch (error) {
        console.error('加载班级失败:', error)
        ElMessage.error('加载班级失败')
    } finally {
        loading.value = false
    }
}

const viewClass = async (cls: StudentClass) => {
    currentClass.value = cls
    classDialogVisible.value = true
    classStudents.value = []
    
    // 加载课程详情
    try {
        const res = await request.get(`/course/${cls.courseId}`)
        courseDetail.value = res
    } catch (error) {
        console.error('加载课程详情失败:', error)
    }
    
    // 加载班级学生
    loadingStudents.value = true
    try {
        const res = await request.get(`/course/class/students/${cls.classId}`)
        classStudents.value = res || []
    } catch (error) {
        console.error('加载班级学生失败:', error)
    } finally {
        loadingStudents.value = false
    }
    
    // 加载班级作业
    loadingHomeworks.value = true
    try {
        const res = await request.get(`/course/homework/class/${cls.classId}`)
        classHomeworks.value = res || []
    } catch (error) {
        console.error('加载作业失败:', error)
    } finally {
        loadingHomeworks.value = false
    }
}

const goToHomework = (homework: Homework) => {
    router.push(`/my-homework?homeworkId=${homework.id}`)
    classDialogVisible.value = false
}

const isOverdue = (endTime: string) => {
    if (!endTime) return false
    return new Date(endTime) < new Date()
}

const formatTime = (time: string) => {
    if (!time) return '-'
    return new Date(time).toLocaleString('zh-CN')
}

// 显示加入班级对话框
const showJoinDialog = () => {
    joinForm.inviteCode = ''
    joinDialogVisible.value = true
}

// 加入班级
const handleJoinClass = async () => {
    if (!joinFormRef.value) return
    
    await joinFormRef.value.validate(async (valid) => {
        if (!valid) return
        
        const userId = getUserId()
        if (!userId) {
            ElMessage.warning('请先登录')
            return
        }
        
        joinLoading.value = true
        try {
            await request.post(`/course/class/join?studentId=${userId}&inviteCode=${joinForm.inviteCode.toUpperCase()}`)
            ElMessage.success('加入班级成功！')
            joinDialogVisible.value = false
            loadClasses() // 刷新班级列表
        } catch (error: any) {
            ElMessage.error(error.response?.data?.message || error.message || '加入班级失败')
        } finally {
            joinLoading.value = false
        }
    })
}

onMounted(() => {
    loadClasses()
})
</script>

<style scoped lang="scss">
.my-classes {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h2 {
        margin: 0;
        font-size: 24px;
        color: #303133;
    }
}

.class-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
}

.class-card {
    cursor: pointer;
    transition: all 0.3s;
    overflow: hidden;
    
    &:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }
    
    :deep(.el-card__body) {
        padding: 0;
    }
    
    .class-cover {
        height: 120px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        
        .cover-placeholder {
            color: rgba(255, 255, 255, 0.8);
        }
    }
    
    .class-info {
        padding: 16px;
        
        .class-name {
            margin: 0 0 8px;
            font-size: 16px;
            font-weight: 600;
            color: #303133;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        
        .class-course, .class-teacher {
            display: flex;
            align-items: center;
            gap: 4px;
            margin: 0 0 6px;
            font-size: 14px;
            color: #606266;
        }
        
        .class-meta {
            display: flex;
            justify-content: space-between;
            margin: 0 0 12px;
            font-size: 12px;
            color: #909399;
        }
    }
}

.class-detail {
    .course-section {
        margin-bottom: 20px;
    }
    
    .el-collapse {
        border: none;
        margin-top: 16px;
        
        .el-collapse-item__header {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
        }
        
        .el-collapse-item__content {
            padding-bottom: 0;
        }
    }
    
    .collapse-title {
        font-weight: 500;
        color: #303133;
    }
}

.text-danger {
    color: #f56c6c;
}

// 加入班级对话框样式
.join-dialog-content {
    text-align: center;
    
    .join-icon-wrapper {
        margin-bottom: 16px;
    }
    
    .join-icon {
        font-size: 64px;
        color: #409eff;
    }
    
    .join-tip {
        color: #606266;
        margin-bottom: 24px;
    }
    
    .tips-list {
        margin: 0;
        padding-left: 20px;
        text-align: left;
        
        li {
            margin: 4px 0;
        }
    }
}
</style>
