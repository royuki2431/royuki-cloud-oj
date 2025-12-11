<template>
    <div class="class-manage">
        <el-page-header @back="$router.back()" content="班级管理" />
        
        <el-card class="toolbar" shadow="never">
            <el-button type="primary" @click="showCreateDialog = true">
                <el-icon><Plus /></el-icon>
                创建班级
            </el-button>
        </el-card>

        <el-card class="class-list" shadow="hover">
            <el-table :data="classes" stripe>
                <el-table-column prop="className" label="班级名称" />
                <el-table-column prop="inviteCode" label="邀请码" width="120">
                    <template #default="{ row }">
                        <el-tag>{{ row.inviteCode }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="currentStudents" label="学生人数" width="100" />
                <el-table-column prop="createdTime" label="创建时间" width="180" />
                <el-table-column label="操作" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewStudents(row)">学生列表</el-button>
                        <el-button type="danger" size="small" @click="deleteClass(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="showCreateDialog" title="创建班级" width="600px">
            <el-form :model="classForm" label-width="100px">
                <el-form-item label="班级名称">
                    <el-input v-model="classForm.className" placeholder="请输入班级名称" />
                </el-form-item>
                <el-form-item label="选择课程">
                    <el-select v-model="classForm.courseId" placeholder="请选择课程">
                        <el-option label="数据结构" :value="1" />
                        <el-option label="算法设计" :value="2" />
                    </el-select>
                </el-form-item>
                <el-form-item label="班级描述">
                    <el-input v-model="classForm.description" type="textarea" :rows="3" />
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
import type { CourseClass } from '@/types'

const classes = ref<CourseClass[]>([])
const showCreateDialog = ref(false)
const classForm = reactive({
    className: '',
    courseId: null as number | null,
    description: '',
})

const loadClasses = async () => {
    ElMessage.info('功能开发中')
}

const handleCreate = async () => {
    ElMessage.success('创建班级成功，邀请码：ABC123')
    showCreateDialog.value = false
}

const viewStudents = (classData: CourseClass) => {
    ElMessage.info(`查看学生列表：${classData.className}`)
}

const deleteClass = (classData: CourseClass) => {
    ElMessage.warning(`删除班级：${classData.className}`)
}

onMounted(() => {
    loadClasses()
})
</script>

<style scoped>
.class-manage {
    padding: 20px;
}

.toolbar {
    margin: 20px 0;
}

.class-list {
    margin-top: 20px;
}
</style>
