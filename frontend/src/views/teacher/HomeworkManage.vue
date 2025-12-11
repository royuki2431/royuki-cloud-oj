<template>
    <div class="homework-manage">
        <el-page-header @back="$router.back()" content="作业管理" />
        
        <el-card class="toolbar" shadow="never">
            <el-button type="primary" @click="showCreateDialog = true">
                <el-icon><Plus /></el-icon>
                布置作业
            </el-button>
        </el-card>

        <el-card class="homework-list" shadow="hover">
            <el-table :data="homeworkList" stripe>
                <el-table-column prop="title" label="作业标题" />
                <el-table-column prop="totalScore" label="总分" width="80" />
                <el-table-column prop="endTime" label="截止时间" width="180" />
                <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'">
                            {{ row.status === 1 ? '进行中' : '已结束' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewStatistics(row)">统计</el-button>
                        <el-button type="danger" size="small" @click="deleteHomework(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <el-dialog v-model="showCreateDialog" title="布置作业" width="800px">
            <el-form :model="homeworkForm" label-width="100px">
                <el-form-item label="作业标题">
                    <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
                </el-form-item>
                <el-form-item label="选择课程">
                    <el-select v-model="homeworkForm.courseId" placeholder="请选择课程">
                        <el-option label="数据结构" :value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item label="选择班级">
                    <el-select v-model="homeworkForm.classId" placeholder="请选择班级">
                        <el-option label="2024级1班" :value="1" />
                    </el-select>
                </el-form-item>
                <el-form-item label="截止时间">
                    <el-date-picker v-model="homeworkForm.endTime" type="datetime" placeholder="选择截止时间" />
                </el-form-item>
                <el-form-item label="作业描述">
                    <el-input v-model="homeworkForm.description" type="textarea" :rows="3" />
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
import type { Homework } from '@/types'

const homeworkList = ref<Homework[]>([])
const showCreateDialog = ref(false)
const homeworkForm = reactive({
    title: '',
    courseId: null as number | null,
    classId: null as number | null,
    endTime: '',
    description: '',
})

const loadHomework = async () => {
    ElMessage.info('功能开发中')
}

const handleCreate = async () => {
    ElMessage.success('布置作业成功')
    showCreateDialog.value = false
}

const viewStatistics = (homework: Homework) => {
    ElMessage.info(`查看统计：${homework.title}`)
}

const deleteHomework = (homework: Homework) => {
    ElMessage.warning(`删除作业：${homework.title}`)
}

onMounted(() => {
    loadHomework()
})
</script>

<style scoped>
.homework-manage {
    padding: 20px;
}

.toolbar {
    margin: 20px 0;
}

.homework-list {
    margin-top: 20px;
}
</style>
