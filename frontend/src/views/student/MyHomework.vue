<template>
    <div class="my-homework">
        <el-page-header @back="$router.back()" content="我的作业" />
        
        <el-card class="homework-list" shadow="hover">
            <template #header>
                <div class="card-header">
                    <span>作业列表</span>
                    <el-radio-group v-model="filterStatus">
                        <el-radio-button label="all">全部</el-radio-button>
                        <el-radio-button label="pending">待完成</el-radio-button>
                        <el-radio-button label="completed">已完成</el-radio-button>
                    </el-radio-group>
                </div>
            </template>

            <el-empty v-if="homeworkList.length === 0" description="暂无作业" />
            
            <div v-else class="homework-cards">
                <el-card
                    v-for="item in filteredHomework"
                    :key="item.homework.id"
                    class="homework-card"
                    shadow="hover"
                >
                    <div class="homework-header">
                        <h3>{{ item.homework.title }}</h3>
                        <el-tag :type="item.isCompleted ? 'success' : 'warning'">
                            {{ item.isCompleted ? '已完成' : '未完成' }}
                        </el-tag>
                    </div>
                    
                    <p class="homework-desc">{{ item.homework.description }}</p>
                    
                    <div class="homework-info">
                        <div class="info-item">
                            <el-icon><Calendar /></el-icon>
                            <span>截止时间：{{ formatDate(item.homework.endTime) }}</span>
                        </div>
                        <div class="info-item">
                            <el-icon><Document /></el-icon>
                            <span>总分：{{ item.homework.totalScore }}</span>
                        </div>
                        <div class="info-item" v-if="item.earnedScore !== undefined">
                            <el-icon><TrophyBase /></el-icon>
                            <span>得分：{{ item.earnedScore }}</span>
                        </div>
                    </div>

                    <el-button type="primary" @click="viewHomework(item.homework)" class="view-btn">
                        查看作业
                    </el-button>
                </el-card>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Document, TrophyBase } from '@element-plus/icons-vue'
import type { StudentHomework, Homework } from '@/types'

const homeworkList = ref<StudentHomework[]>([])
const filterStatus = ref('all')

const filteredHomework = computed(() => {
    if (filterStatus.value === 'all') {
        return homeworkList.value
    } else if (filterStatus.value === 'pending') {
        return homeworkList.value.filter(item => !item.isCompleted)
    } else {
        return homeworkList.value.filter(item => item.isCompleted)
    }
})

const formatDate = (dateStr?: string) => {
    if (!dateStr) return '无'
    return new Date(dateStr).toLocaleString('zh-CN')
}

const loadHomework = async () => {
    try {
        // TODO: 调用API加载作业列表
        ElMessage.info('功能开发中')
    } catch (error) {
        ElMessage.error('加载作业失败')
    }
}

const viewHomework = (homework: Homework) => {
    ElMessage.info(`查看作业：${homework.title}`)
}

onMounted(() => {
    loadHomework()
})
</script>

<style scoped>
.my-homework {
    padding: 20px;
}

.homework-list {
    margin-top: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.homework-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 20px;
}

.homework-card {
    position: relative;
}

.homework-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.homework-header h3 {
    margin: 0;
    font-size: 18px;
}

.homework-desc {
    color: #606266;
    margin: 8px 0;
    min-height: 40px;
}

.homework-info {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin: 12px 0;
}

.info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #909399;
    font-size: 14px;
}

.view-btn {
    width: 100%;
    margin-top: 12px;
}
</style>
