<template>
    <div class="student-manage">
        <el-page-header @back="$router.back()" content="学生管理" />
        
        <el-card class="student-list" shadow="hover">
            <el-table :data="students" stripe>
                <el-table-column prop="username" label="用户名" />
                <el-table-column prop="email" label="邮箱" />
                <el-table-column prop="className" label="班级" />
                <el-table-column prop="createdTime" label="加入时间" width="180" />
                <el-table-column label="操作" width="150">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewProgress(row)">学习进度</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const students = ref([])

const loadStudents = async () => {
    ElMessage.info('功能开发中')
}

const viewProgress = (student: any) => {
    ElMessage.info(`查看学习进度：${student.username}`)
}

onMounted(() => {
    loadStudents()
})
</script>

<style scoped>
.student-manage {
    padding: 20px;
}

.student-list {
    margin-top: 20px;
}
</style>
