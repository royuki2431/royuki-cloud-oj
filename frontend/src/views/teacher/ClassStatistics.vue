<template>
  <div class="class-statistics">
    <div class="page-header">
      <el-page-header @back="$router.back()">
        <template #content>
          <span>班级统计</span>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-select v-model="selectedCourseId" placeholder="选择课程" @change="loadClasses" style="width: 200px">
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="selectedClassId" placeholder="选择班级" @change="loadStatistics" style="width: 200px; margin-left: 12px">
          <el-option v-for="cls in classes" :key="cls.id" :label="cls.name" :value="cls.id" />
        </el-select>
      </div>
    </div>

    <div class="stats-content" v-loading="loading">
      <!-- 概览卡片 -->
      <el-row :gutter="20" class="overview-cards">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #409eff">
              <el-icon size="24"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.studentCount || 0 }}</div>
              <div class="stat-label">学生人数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #67c23a">
              <el-icon size="24"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.homeworkCount || 0 }}</div>
              <div class="stat-label">作业数量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #e6a23c">
              <el-icon size="24"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.homeworkCompletionRate || '0%' }}</div>
              <div class="stat-label">作业完成率</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: #f56c6c">
              <el-icon size="24"><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.averageScore || '0' }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="charts-row">
        <!-- 作业完成情况 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>作业完成情况</span>
            </template>
            <el-table :data="homeworkStats" max-height="300">
              <el-table-column prop="title" label="作业名称" min-width="120" show-overflow-tooltip />
              <el-table-column prop="problemCount" label="题目数" width="80" align="center" />
              <el-table-column label="完成率" width="140">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="row.completionRate || 0" 
                    :stroke-width="10"
                    :color="getProgressColor(row.completionRate)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="平均分" width="80" align="center">
                <template #default="{ row }">
                  <span :class="getScoreClass(row.avgScore)">{{ row.avgScore || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="isHomeworkEnded(row) ? 'info' : 'success'" size="small">
                    {{ isHomeworkEnded(row) ? '已结束' : '进行中' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <!-- 学生排名 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <span>学生排名 TOP 10</span>
            </template>
            <el-table :data="studentRanking.slice(0, 10)" max-height="300">
              <el-table-column label="排名" width="70" align="center">
                <template #default="{ row }">
                  <span :class="getRankClass(row.rank)">
                    <template v-if="row.rank <= 3">🏆</template>
                    {{ row.rank }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="studentName" label="学生" min-width="100" />
              <el-table-column prop="totalScore" label="总分" width="80" align="center" />
              <el-table-column prop="completedHomeworks" label="完成作业" width="100" align="center" />
              <el-table-column prop="averageScore" label="平均分" width="80" align="center" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 全部学生成绩 -->
      <el-card class="full-ranking">
        <template #header>
          <div class="card-header">
            <span>全部学生成绩</span>
            <div class="header-right">
              <el-select v-model="rankingType" style="width: 150px; margin-right: 12px" @change="onRankingTypeChange">
                <el-option label="作业总分排名" value="homework_total" />
                <el-option label="单个作业排名" value="homework_single" />
                <el-option label="题库训练排名" value="practice" />
              </el-select>
              <el-select 
                v-if="rankingType === 'homework_single'" 
                v-model="selectedHomeworkId" 
                placeholder="选择作业" 
                style="width: 180px; margin-right: 12px"
                @change="loadHomeworkRanking"
              >
                <el-option v-for="hw in homeworkList" :key="hw.id" :label="hw.title" :value="hw.id" />
              </el-select>
              <el-button type="primary" size="small" @click="exportData" :disabled="currentRankingData.length === 0">
                <el-icon><Download /></el-icon>
                导出数据
              </el-button>
            </div>
          </div>
        </template>
        
        <!-- 作业总分排名 -->
        <el-table v-if="rankingType === 'homework_total'" :data="paginatedData" stripe style="width: 100%">
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ row }">
              <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="studentName" label="姓名" min-width="140" />
          <el-table-column prop="totalScore" label="总分" min-width="120" align="center">
            <template #default="{ row }">
              <span :class="getScoreClass(row.totalScore)">{{ row.totalScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="completedHomeworks" label="已完成作业" min-width="140" align="center" />
          <el-table-column prop="averageScore" label="平均分" min-width="120" align="center" />
        </el-table>
        
        <!-- 单个作业排名 -->
        <el-table v-else-if="rankingType === 'homework_single'" :data="paginatedData" stripe style="width: 100%">
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ row }">
              <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="studentName" label="姓名" min-width="140" />
          <el-table-column prop="score" label="得分" min-width="120" align="center" />
          <el-table-column prop="submitTime" label="提交时间" min-width="180">
            <template #default="{ row }">
              {{ formatTime(row.submitTime) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.completed ? 'success' : 'info'" size="small">
                {{ row.completed ? '已完成' : '未完成' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 题库训练排名 -->
        <el-table v-else-if="rankingType === 'practice'" :data="paginatedData" stripe style="width: 100%">
          <el-table-column label="排名" width="80" align="center">
            <template #default="{ row }">
              <span :class="getRankClass(row.rank)">{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="studentId" label="学号" min-width="120" />
          <el-table-column prop="studentName" label="姓名" min-width="140" />
          <el-table-column prop="totalScore" label="总分" min-width="120" align="center" />
          <el-table-column prop="solvedCount" label="通过题数" min-width="120" align="center" />
          <el-table-column prop="submitCount" label="提交次数" min-width="120" align="center" />
          <el-table-column label="通过率" min-width="100" align="center">
            <template #default="{ row }">
              {{ row.acceptRate }}%
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="currentRankingData.length === 0 && !loading" description="暂无学生数据" />
        
        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="currentRankingData.length > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="currentRankingData.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
      
      <!-- 未选择班级提示 -->
      <el-empty v-if="!selectedClassId && !loading" description="请选择课程和班级查看统计数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Document, TrendCharts, Trophy, Download } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { 
  getTeacherCourses, 
  getCourseClasses, 
  getClassStatistics, 
  getStudentRanking,
  type Course,
  type CourseClass
} from '@/api/course'
import request from '@/utils/request'

const route = useRoute()
const loading = ref(false)
const courses = ref<Course[]>([])
const classes = ref<CourseClass[]>([])
const selectedCourseId = ref<number | null>(null)
const selectedClassId = ref<number | null>(null)

const stats = ref<any>({})
const homeworkStats = ref<any[]>([])
const studentRanking = ref<any[]>([])
const homeworkRanking = ref<any[]>([])
const practiceRanking = ref<any[]>([])
const homeworkList = ref<any[]>([])

// 排名类型和分页
const rankingType = ref('homework_total')
const selectedHomeworkId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(20)

// 当前排名数据
const currentRankingData = computed(() => {
  if (rankingType.value === 'homework_total') return studentRanking.value
  if (rankingType.value === 'homework_single') return homeworkRanking.value
  if (rankingType.value === 'practice') return practiceRanking.value
  return []
})

// 分页后的数据
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return currentRankingData.value.slice(start, end)
})

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 加载课程
const loadCourses = async () => {
  const userId = getUserId()
  if (!userId) return
  
  try {
    courses.value = await getTeacherCourses(userId)
    if (courses.value.length > 0) {
      const courseIdFromQuery = route.query.courseId
      if (courseIdFromQuery) {
        selectedCourseId.value = Number(courseIdFromQuery)
      } else {
        selectedCourseId.value = courses.value[0].id
      }
      await loadClasses()
    }
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

// 加载班级
const loadClasses = async () => {
  if (!selectedCourseId.value) return
  
  try {
    classes.value = await getCourseClasses(selectedCourseId.value)
    if (classes.value.length > 0) {
      const classIdFromQuery = route.query.classId
      if (classIdFromQuery) {
        selectedClassId.value = Number(classIdFromQuery)
      } else {
        selectedClassId.value = classes.value[0].id
      }
      loadStatistics()
    }
  } catch (error) {
    console.error('加载班级失败:', error)
  }
}

// 加载统计数据
const loadStatistics = async () => {
  if (!selectedClassId.value) return
  
  loading.value = true
  rankingType.value = 'homework_total'
  currentPage.value = 1
  homeworkRanking.value = []
  practiceRanking.value = []
  selectedHomeworkId.value = null
  
  try {
    const [statsData, ranking] = await Promise.all([
      getClassStatistics(selectedClassId.value),
      getStudentRanking(selectedClassId.value)
    ])
    
    stats.value = statsData || {}
    studentRanking.value = ranking || []
    
    // 加载作业统计（包含完成率和平均分）
    await loadHomeworkStats()
    
    // 加载作业列表（用于单个作业排名选择）
    await loadHomeworkList()
  } catch (error) {
    console.error('加载统计失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载作业列表
const loadHomeworkList = async () => {
  if (!selectedClassId.value) return
  try {
    const response = await request.get(`/course/homework/class/${selectedClassId.value}`)
    homeworkList.value = response || []
  } catch {
    homeworkList.value = []
  }
}

// 排名类型变化
const onRankingTypeChange = async () => {
  currentPage.value = 1
  if (rankingType.value === 'homework_total') {
    // 已经加载过了
  } else if (rankingType.value === 'homework_single') {
    // 默认选择第一个作业
    if (homeworkList.value.length > 0) {
      selectedHomeworkId.value = homeworkList.value[0].id
      await loadHomeworkRanking()
    } else {
      selectedHomeworkId.value = null
      homeworkRanking.value = []
    }
  } else if (rankingType.value === 'practice') {
    await loadPracticeRanking()
  }
}

// 加载单个作业排名
const loadHomeworkRanking = async () => {
  if (!selectedClassId.value || !selectedHomeworkId.value) return
  loading.value = true
  currentPage.value = 1
  try {
    const response = await request.get(`/course/statistics/homework/${selectedHomeworkId.value}/ranking`, {
      params: { classId: selectedClassId.value }
    })
    homeworkRanking.value = response || []
  } catch {
    homeworkRanking.value = []
  } finally {
    loading.value = false
  }
}

// 加载题库训练排名
const loadPracticeRanking = async () => {
  if (!selectedClassId.value) return
  loading.value = true
  try {
    const response = await request.get(`/course/statistics/practice/${selectedClassId.value}`)
    practiceRanking.value = response || []
  } catch {
    practiceRanking.value = []
  } finally {
    loading.value = false
  }
}

// 分页处理
const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
}

// 加载作业统计
const loadHomeworkStats = async () => {
  if (!selectedClassId.value) return
  
  try {
    const homeworks = await request.get(`/course/homework/class/${selectedClassId.value}`)
    
    // 为每个作业获取统计数据
    const statsPromises = (homeworks || []).map(async (hw: any) => {
      try {
        const ranking = await request.get(`/course/statistics/homework/${hw.id}/ranking`, {
          params: { classId: selectedClassId.value }
        })
        
        const rankingData = ranking || []
        const completedCount = rankingData.filter((r: any) => r.completed).length
        const totalScore = rankingData.reduce((sum: number, r: any) => sum + (r.score || 0), 0)
        
        return {
          ...hw,
          completionRate: rankingData.length > 0 ? Math.round(completedCount / rankingData.length * 100) : 0,
          avgScore: rankingData.length > 0 ? (totalScore / rankingData.length).toFixed(1) : '-'
        }
      } catch {
        return { ...hw, completionRate: 0, avgScore: '-' }
      }
    })
    
    homeworkStats.value = await Promise.all(statsPromises)
  } catch {
    homeworkStats.value = []
  }
}

// 判断作业是否已结束
const isHomeworkEnded = (homework: any) => {
  if (!homework.endTime) return false
  return new Date(homework.endTime) < new Date()
}

// 导出数据
const exportData = () => {
  const data = currentRankingData.value
  if (data.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  let headers: string[]
  let rows: any[][]
  let filename: string
  
  if (rankingType.value === 'homework_total') {
    headers = ['排名', '学号', '姓名', '总分', '已完成作业', '平均分']
    rows = data.map((s: any) => [
      s.rank, s.studentId, s.studentName || '', s.totalScore || 0, s.completedHomeworks || '', s.averageScore || ''
    ])
    filename = '作业总分排名'
  } else if (rankingType.value === 'homework_single') {
    const hw = homeworkList.value.find(h => h.id === selectedHomeworkId.value)
    headers = ['排名', '学号', '姓名', '得分', '提交时间', '状态']
    rows = data.map((s: any) => [
      s.rank, s.studentId, s.studentName || '', s.score || 0, formatTime(s.submitTime), s.completed ? '已完成' : '未完成'
    ])
    filename = `作业排名_${hw?.title || ''}`
  } else {
    headers = ['排名', '学号', '姓名', '总分', '通过题数', '提交次数', '通过率']
    rows = data.map((s: any) => [
      s.rank, s.studentId, s.studentName || '', s.totalScore || 0, s.solvedCount || 0, s.submitCount || 0, `${s.acceptRate || 0}%`
    ])
    filename = '题库训练排名'
  }
  
  const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}_${new Date().toLocaleDateString()}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 获取分数样式
const getScoreClass = (score: number) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
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
.class-statistics {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  .header-actions {
    display: flex;
    align-items: center;
  }
}

.overview-cards {
  margin-bottom: 20px;
  
  .stat-card {
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      padding: 20px;
    }
    
    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      margin-right: 16px;
    }
    
    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }
      
      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

.charts-row {
  margin-bottom: 20px;
}

.full-ranking {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    
    .header-right {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.score-excellent { color: #67c23a; font-weight: 600; }
.score-pass { color: #e6a23c; font-weight: 600; }
.score-fail { color: #f56c6c; font-weight: 600; }

.rank-gold { color: #ffd700; font-weight: bold; font-size: 18px; }
.rank-silver { color: #c0c0c0; font-weight: bold; font-size: 16px; }
.rank-bronze { color: #cd7f32; font-weight: bold; }
</style>
