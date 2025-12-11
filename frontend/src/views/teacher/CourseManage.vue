<template>
    <div class="course-manage">
        <el-page-header content="课程管理" />
        
        <el-card class="toolbar" shadow="never">
            <el-button type="primary" @click="showCreateDialog = true">
                <el-icon><Plus /></el-icon>
                创建课程
            </el-button>
        </el-card>

        <el-card class="course-list" shadow="hover">
            <el-table :data="courses" stripe>
                <el-table-column prop="courseName" label="课程名称" />
                <el-table-column prop="description" label="描述" show-overflow-tooltip />
                <el-table-column prop="createdTime" label="创建时间" width="180" />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'">
                            {{ row.status === 1 ? '进行中' : '已结束' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="250">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="editCourse(row)">编辑</el-button>
                        <el-button type="success" size="small" @click="manageClasses(row)">班级管理</el-button>
                        <el-button type="danger" size="small" @click="deleteCourse(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="showCreateDialog" title="创建课程" width="600px">
            <el-form :model="courseForm" label-width="100px">
                <el-form-item label="课程名称">
                    <el-input v-model="courseForm.courseName" placeholder="请输入课程名称" />
                </el-form-item>
                <el-form-item label="课程描述">
                    <el-input v-model="courseForm.description" type="textarea" :rows="3" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showCreateDialog = false">取消</el-button>
                <el-button type="primary" @click="handleCreate">创建</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Course } from '@/types'
import { useRouter } from 'vue-router'

const router = useRouter()
const courses = ref<Course[]>([])
const showCreateDialog = ref(false)
const courseForm = reactive({
    courseName: '',
    description: '',
})

const loadCourses = async () => {
    ElMessage.info('功能开发中')
}

const handleCreate = async () => {
    ElMessage.success('创建课程成功')
    showCreateDialog.value = false
}

const editCourse = (course: Course) => {
    ElMessage.info(`编辑课程：${course.courseName}`)
}

const manageClasses = (course: Course) => {
    router.push('/teacher/classes')
}

const deleteCourse = (course: Course) => {
    ElMessage.warning(`删除课程：${course.courseName}`)
}

onMounted(() => {
    loadCourses()
})
</script>

<style scoped>
.course-manage {
    padding: 20px;
}

.toolbar {
    margin: 20px 0;
}

.course-list {
    margin-top: 20px;
}
</style>
