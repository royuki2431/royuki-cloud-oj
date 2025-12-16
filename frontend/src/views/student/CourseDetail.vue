<template>
  <div class="course-detail-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">{{ course?.name || '课程详情' }}</span>
      </template>
    </el-page-header>

    <div class="course-content" v-loading="loading">
      <!-- 课程信息卡片 -->
      <el-card class="course-info-card">
        <div class="course-header">
          <div class="course-cover">
            <img v-if="course?.coverImage" :src="course.coverImage" alt="课程封面" />
            <div v-else class="cover-placeholder">
              <el-icon :size="64"><Reading /></el-icon>
            </div>
          </div>
          <div class="course-meta">
            <h1>{{ course?.name }}</h1>
            <p class="description">{{ course?.description || '暂无描述' }}</p>
            <div class="meta-items">
              <span><el-icon><User /></el-icon> {{ course?.teacherName || '教师' }}</span>
              <span><el-icon><Calendar /></el-icon> {{ course?.semester }}</span>
              <span><el-icon><UserFilled /></el-icon> {{ course?.studentCount || 0 }} 名学生</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab" class="course-tabs">
        <!-- 班级列表 -->
        <el-tab-pane label="班级" name="classes">
          <div class="class-list">
            <el-card v-for="cls in classes" :key="cls.id" class="class-card">
              <div class="class-info">
                <h3>{{ cls.name }}</h3>
                <p>邀请码: <el-tag type="success">{{ cls.code }}</el-tag></p>
                <p>学生人数: {{ cls.studentCount }} / {{ cls.maxStudents }}</p>
              </div>
              <div class="class-actions">
                <el-button 
                  v-if="!isInClass(cls.id)" 
                  type="primary" 
                  size="small"
                  @click="handleJoinClass(cls.code)"
                >
                  加入班级
                </el-button>
                <el-tag v-else type="success" size="small">已加入</el-tag>
              </div>
            </el-card>
            <el-empty v-if="classes.length === 0" description="暂无班级" />
          </div>
        </el-tab-pane>

        <!-- 作业列表 -->
        <el-tab-pane label="作业" name="homework">
          <el-table :data="homeworks" stripe>
            <el-table-column prop="title" label="作业标题" min-width="200">
              <template #default="{ row }">
                <el-link type="primary" @click="viewHomework(row)">
                  {{ row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="截止时间" width="180">
              <template #default="{ row }">
                <span :class="{ 'text-danger': isOverdue(row.endTime) }">
                  {{ formatTime(row.endTime) }}
                </span>
                <el-tag v-if="isOverdue(row.endTime)" type="danger" size="small" class="ml-2">
                  已截止
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="总分" width="80" align="center" />
            <el-table-column label="我的得分" width="100" align="center">
              <template #default="{ row }">
                <span class="score">{{ row.myScore || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getHomeworkStatusType(row)" size="small">
                  {{ getHomeworkStatusText(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewHomework(row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="homeworks.length === 0" description="暂无作业" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Reading, User, Calendar, UserFilled } from '@element-plus/icons-vue'
import { 
  getCourseById, 
  getCourseClasses, 
  joinClass,
  type Course, 
  type CourseClass 
} from '@/api/course'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('classes')
const course = ref<Course | null>(null)
const classes = ref<CourseClass[]>([])
const homeworks = ref<any[]>([])
const joinedClassIds = ref<number[]>([])

const courseId = Number(route.params.id)

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 加载课程数据
const loadCourseData = async () => {
  loading.value = true
  try {
    const [courseRes, classesRes] = await Promise.all([
      getCourseById(courseId),
      getCourseClasses(courseId)
    ])
    course.value = courseRes
    classes.value = classesRes
    
    // TODO: 加载作业列表和已加入的班级
  } catch (error) {
    console.error('加载课程数据失败:', error)
    ElMessage.error('加载课程数据失败')
  } finally {
    loading.value = false
  }
}

// 检查是否已加入班级
const isInClass = (classId: number) => {
  return joinedClassIds.value.includes(classId)
}

// 加入班级
const handleJoinClass = async (inviteCode: string) => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  
  try {
    await joinClass(userId, inviteCode)
    ElMessage.success('加入成功')
    loadCourseData()
  } catch (error: any) {
    ElMessage.error(error.message || '加入失败')
  }
}

// 查看作业详情
const viewHomework = (homework: any) => {
  router.push(`/homework/${homework.id}`)
}

// 判断是否已截止
const isOverdue = (endTime: string) => {
  if (!endTime) return false
  return new Date(endTime) < new Date()
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取作业状态类型
const getHomeworkStatusType = (homework: any) => {
  if (homework.myScore >= homework.totalScore) return 'success'
  if (homework.myScore > 0) return 'warning'
  if (isOverdue(homework.endTime)) return 'danger'
  return 'info'
}

// 获取作业状态文本
const getHomeworkStatusText = (homework: any) => {
  if (homework.myScore >= homework.totalScore) return '已完成'
  if (homework.myScore > 0) return '进行中'
  if (isOverdue(homework.endTime)) return '未完成'
  return '未开始'
}

onMounted(() => {
  loadCourseData()
})
</script>

<style scoped lang="scss">
.course-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.course-content {
  margin-top: 20px;
}

.course-info-card {
  margin-bottom: 20px;
  
  .course-header {
    display: flex;
    gap: 24px;
  }
  
  .course-cover {
    width: 200px;
    height: 150px;
    border-radius: 8px;
    overflow: hidden;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .cover-placeholder {
      color: rgba(255, 255, 255, 0.8);
    }
  }
  
  .course-meta {
    flex: 1;
    
    h1 {
      margin: 0 0 12px;
      font-size: 24px;
      color: #303133;
    }
    
    .description {
      margin: 0 0 16px;
      color: #606266;
      line-height: 1.6;
    }
    
    .meta-items {
      display: flex;
      gap: 24px;
      color: #909399;
      font-size: 14px;
      
      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

.course-tabs {
  :deep(.el-tabs__content) {
    padding: 20px 0;
  }
}

.class-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.class-card {
  .class-info {
    h3 {
      margin: 0 0 12px;
      font-size: 16px;
      color: #303133;
    }
    
    p {
      margin: 0 0 8px;
      font-size: 14px;
      color: #606266;
    }
  }
  
  .class-actions {
    margin-top: 16px;
    text-align: right;
  }
}

.text-danger {
  color: #f56c6c;
}

.ml-2 {
  margin-left: 8px;
}

.score {
  font-weight: 600;
  color: #409eff;
}
</style>
