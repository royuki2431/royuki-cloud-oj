<template>
    <div class="my-courses">
        <el-page-header @back="$router.back()" content="我的课程" />
        
        <el-card class="course-list" shadow="hover">
            <template #header>
                <div class="card-header">
                    <span>课程列表</span>
                    <el-button type="primary" @click="showJoinDialog = true">
                        <el-icon><Plus /></el-icon>
                        加入课程
                    </el-button>
                </div>
            </template>

            <el-empty v-if="courses.length === 0" description="还没有加入任何课程" />
            
            <el-table v-else :data="courses" stripe>
                <el-table-column prop="courseName" label="课程名称" />
                <el-table-column prop="teacherName" label="教师" />
                <el-table-column prop="description" label="描述" show-overflow-tooltip />
                <el-table-column prop="createdTime" label="创建时间" width="180" />
                <el-table-column label="操作" width="200">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="viewCourse(row)">
                            查看详情
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 加入课程对话框 -->
        <el-dialog v-model="showJoinDialog" title="加入课程" width="500px">
            <p>请使用班级邀请码加入课程</p>
            <el-button type="primary" @click="$router.push('/join-class')">
                前往加入班级
            </el-button>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Course } from '@/types'

const courses = ref<Course[]>([])
const showJoinDialog = ref(false)

const loadCourses = async () => {
    try {
        // TODO: 调用API加载课程列表
        ElMessage.info('功能开发中')
    } catch (error) {
        ElMessage.error('加载课程失败')
    }
}

const viewCourse = (course: Course) => {
    ElMessage.info(`查看课程：${course.courseName}`)
}

onMounted(() => {
    loadCourses()
})
</script>

<style scoped>
.my-courses {
    padding: 20px;
}

.course-list {
    margin-top: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
</style>
