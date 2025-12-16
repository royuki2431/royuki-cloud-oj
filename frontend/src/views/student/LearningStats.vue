<template>
  <div class="learning-stats-page">
    <div class="page-header">
      <h2>学习统计</h2>
    </div>

    <!-- 概览卡片 -->
    <div class="overview-cards">
      <el-card class="overview-card">
        <div class="card-icon submissions">
          <el-icon><Document /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ overview.totalSubmissions || 0 }}</div>
          <div class="card-label">总提交次数</div>
        </div>
      </el-card>
      <el-card class="overview-card">
        <div class="card-icon accepted">
          <el-icon><Select /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ overview.totalAccepted || 0 }}</div>
          <div class="card-label">通过次数</div>
        </div>
      </el-card>
      <el-card class="overview-card">
        <div class="card-icon solved">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ overview.totalProblemsSolved || 0 }}</div>
          <div class="card-label">解决题目</div>
        </div>
      </el-card>
      <el-card class="overview-card">
        <div class="card-icon rate">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ acceptRate }}%</div>
          <div class="card-label">通过率</div>
        </div>
      </el-card>
      <el-card class="overview-card">
        <div class="card-icon score">
          <el-icon><Medal /></el-icon>
        </div>
        <div class="card-info">
          <div class="card-value">{{ overview.totalTrainingScore || 0 }}</div>
          <div class="card-label">总训练得分</div>
        </div>
      </el-card>
    </div>

    <!-- 班级排行榜 -->
    <el-card class="leaderboard-card">
      <template #header>
        <div class="card-header">
          <span>班级排行榜 Top 10</span>
          <el-select 
            v-model="selectedClassId" 
            placeholder="选择班级" 
            @change="loadClassLeaderboard"
            style="width: 200px"
            v-if="myClasses.length > 0"
          >
            <el-option 
              v-for="cls in myClasses" 
              :key="cls.classId" 
              :label="cls.className" 
              :value="cls.classId" 
            />
          </el-select>
        </div>
      </template>
      <el-empty v-if="myClasses.length === 0 && !classesLoading" description="暂未加入任何班级">
        <el-button type="primary" @click="$router.push('/my-classes')">加入班级</el-button>
      </el-empty>
      <el-table v-else :data="leaderboard" v-loading="leaderboardLoading">
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <span :class="getRankClass($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentName" label="学生" min-width="120" />
        <el-table-column prop="submitCount" label="提交次数" align="center" width="100" />
        <el-table-column prop="solvedCount" label="完成题目" align="center" width="100" />
        <el-table-column prop="totalScore" label="总分" align="center" width="100" />
      </el-table>
    </el-card>

    <!-- 学习日历 - 双月视图 -->
    <el-card class="calendar-card">
      <template #header>
        <div class="card-header">
          <span>学习日历</span>
          <div class="calendar-nav">
            <el-button text @click="prevMonth">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <span class="current-month">{{ twoMonthLabel }}</span>
            <el-button text @click="nextMonth" :disabled="isCurrentMonth">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <div class="dual-calendar-container" v-loading="heatmapLoading">
        <!-- 左侧月份（上个月） -->
        <div class="calendar-month">
          <div class="month-title">{{ prevMonthLabel }}</div>
          <div class="calendar-weekdays">
            <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
          </div>
          <div class="calendar-grid">
            <div 
              v-for="(day, index) in prevMonthDays" 
              :key="'prev-' + index"
              class="calendar-cell"
              :class="[
                getHeatmapClass(day.submitCount),
                { 'other-month': !day.isCurrentMonth, 'today': day.isToday }
              ]"
              :title="day.date ? `${day.date}: ${day.submitCount} 次提交` : ''"
            >
              <span class="day-number">{{ day.dayNumber }}</span>
              <span v-if="day.submitCount > 0" class="submit-count">{{ day.submitCount }}</span>
            </div>
          </div>
        </div>
        
        <!-- 右侧月份（当前月） -->
        <div class="calendar-month">
          <div class="month-title">{{ currentMonthLabel }}</div>
          <div class="calendar-weekdays">
            <div v-for="day in weekdays" :key="day" class="weekday">{{ day }}</div>
          </div>
          <div class="calendar-grid">
            <div 
              v-for="(day, index) in calendarDays" 
              :key="'curr-' + index"
              class="calendar-cell"
              :class="[
                getHeatmapClass(day.submitCount),
                { 'other-month': !day.isCurrentMonth, 'today': day.isToday }
              ]"
              :title="day.date ? `${day.date}: ${day.submitCount} 次提交` : ''"
            >
              <span class="day-number">{{ day.dayNumber }}</span>
              <span v-if="day.submitCount > 0" class="submit-count">{{ day.submitCount }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="heatmap-legend">
        <span>少</span>
        <div class="legend-cell level-0"></div>
        <div class="legend-cell level-1"></div>
        <div class="legend-cell level-2"></div>
        <div class="legend-cell level-3"></div>
        <div class="legend-cell level-4"></div>
        <span>多</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document, Select, Trophy, TrendCharts, ArrowLeft, ArrowRight, Medal } from '@element-plus/icons-vue'
import { 
  getLearningOverview, 
  getLearningHeatmap, 
  getClassLeaderboard,
  type LearningStatistics
} from '@/api/learning'
import request from '@/utils/request'

const router = useRouter()
const overview = ref<any>({})
const heatmapData = ref<any[]>([])
const leaderboard = ref<any[]>([])
const heatmapLoading = ref(false)
const leaderboardLoading = ref(false)
const classesLoading = ref(false)

// 班级相关
const myClasses = ref<any[]>([])
const selectedClassId = ref<number | null>(null)

// 日历相关
const currentDate = ref(new Date())
const weekdays = ['日', '一', '二', '三', '四', '五', '六']
const months = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']

// 当前月份标签
const currentMonthLabel = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  return `${year}年${months[month]}`
})

// 上个月标签
const prevMonthLabel = computed(() => {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() - 1)
  return `${date.getFullYear()}年${months[date.getMonth()]}`
})

// 双月标签
const twoMonthLabel = computed(() => {
  return `${prevMonthLabel.value} - ${currentMonthLabel.value}`
})

// 是否是当前月
const isCurrentMonth = computed(() => {
  const now = new Date()
  return currentDate.value.getFullYear() === now.getFullYear() && 
         currentDate.value.getMonth() === now.getMonth()
})

// 上个月
const prevMonth = () => {
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() - 1)
  currentDate.value = newDate
  loadHeatmap()
}

// 下个月
const nextMonth = () => {
  if (isCurrentMonth.value) return
  const newDate = new Date(currentDate.value)
  newDate.setMonth(newDate.getMonth() + 1)
  currentDate.value = newDate
  loadHeatmap()
}

// 生成指定月份的日历数据
const generateMonthDays = (year: number, month: number) => {
  const today = new Date()
  
  // 当月第一天
  const firstDay = new Date(year, month, 1)
  // 当月最后一天
  const lastDay = new Date(year, month + 1, 0)
  // 第一天是星期几
  const startWeekday = firstDay.getDay()
  // 当月天数
  const daysInMonth = lastDay.getDate()
  
  const days: any[] = []
  
  // 上个月的日期填充
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = startWeekday - 1; i >= 0; i--) {
    days.push({
      dayNumber: prevMonthLastDay - i,
      date: '',
      submitCount: 0,
      isCurrentMonth: false,
      isToday: false
    })
  }
  
  // 当月日期
  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    const heatmapDay = heatmapData.value.find(d => d.statDate === dateStr)
    const isToday = today.getFullYear() === year && today.getMonth() === month && today.getDate() === i
    
    days.push({
      dayNumber: i,
      date: dateStr,
      submitCount: heatmapDay?.submitCount || 0,
      isCurrentMonth: true,
      isToday
    })
  }
  
  // 下个月的日期填充（补满6行）
  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    days.push({
      dayNumber: i,
      date: '',
      submitCount: 0,
      isCurrentMonth: false,
      isToday: false
    })
  }
  
  return days
}

// 当前月日历数据
const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  return generateMonthDays(year, month)
})

// 上个月日历数据
const prevMonthDays = computed(() => {
  const date = new Date(currentDate.value)
  date.setMonth(date.getMonth() - 1)
  return generateMonthDays(date.getFullYear(), date.getMonth())
})

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 计算通过率
const acceptRate = computed(() => {
  if (!overview.value.totalSubmissions) return 0
  return ((overview.value.totalAccepted / overview.value.totalSubmissions) * 100).toFixed(1)
})

// 加载概览数据
const loadOverview = async () => {
  const userId = getUserId()
  if (!userId) return
  
  try {
    overview.value = await getLearningOverview(userId)
  } catch (error) {
    console.error('加载概览失败:', error)
  }
}

// 加载热力图数据（加载两个月的数据）
const loadHeatmap = async () => {
  const userId = getUserId()
  if (!userId) return
  
  // 计算上个月的起始日期
  const prevMonthDate = new Date(currentDate.value)
  prevMonthDate.setMonth(prevMonthDate.getMonth() - 1)
  const prevYear = prevMonthDate.getFullYear()
  const prevMonth = prevMonthDate.getMonth()
  const startDate = `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-01`
  
  // 当前月的结束日期
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const lastDay = new Date(year, month + 1, 0).getDate()
  const endDate = `${year}-${String(month + 1).padStart(2, '0')}-${lastDay}`
  
  heatmapLoading.value = true
  try {
    heatmapData.value = await getLearningHeatmap(userId, startDate, endDate)
    console.log('热力图数据:', heatmapData.value) // 调试日志
  } catch (error) {
    console.error('加载热力图失败:', error)
  } finally {
    heatmapLoading.value = false
  }
}

// 加载我的班级列表
const loadMyClasses = async () => {
  const userId = getUserId()
  if (!userId) return
  
  classesLoading.value = true
  try {
    const res = await request.get(`/course/student/classes/${userId}`)
    myClasses.value = res || []
    // 默认选择第一个班级
    if (myClasses.value.length > 0) {
      selectedClassId.value = myClasses.value[0].classId
      loadClassLeaderboard()
    }
  } catch (error) {
    console.error('加载班级列表失败:', error)
  } finally {
    classesLoading.value = false
  }
}

// 加载班级排行榜
const loadClassLeaderboard = async () => {
  if (!selectedClassId.value) return
  
  leaderboardLoading.value = true
  try {
    const data = await getClassLeaderboard(selectedClassId.value)
    // 只取前10名
    leaderboard.value = (data || []).slice(0, 10)
  } catch (error) {
    console.error('加载班级排行榜失败:', error)
  } finally {
    leaderboardLoading.value = false
  }
}

// 获取热力图单元格样式
const getHeatmapClass = (count: number) => {
  if (count === 0) return 'level-0'
  if (count <= 2) return 'level-1'
  if (count <= 5) return 'level-2'
  if (count <= 10) return 'level-3'
  return 'level-4'
}

// 获取排名样式
const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-gold'
  if (rank === 2) return 'rank-silver'
  if (rank === 3) return 'rank-bronze'
  return ''
}

onMounted(() => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  
  loadOverview()
  loadHeatmap()
  loadMyClasses()
})
</script>

<style scoped lang="scss">
.learning-stats-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
  
  h2 {
    margin: 0;
    font-size: 24px;
    color: #303133;
  }
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.overview-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  .card-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #fff;
    
    &.submissions { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
    &.accepted { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }
    &.solved { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
    &.rate { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
    &.score { background: linear-gradient(135deg, #f5af19 0%, #f12711 100%); }
  }
  
  .card-info {
    .card-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
    }
    
    .card-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.calendar-card {
  margin-bottom: 20px;
  
  .calendar-nav {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .current-month {
      font-size: 14px;
      font-weight: 500;
      min-width: 200px;
      text-align: center;
    }
  }
}

.dual-calendar-container {
  display: flex;
  gap: 24px;
  
  .calendar-month {
    flex: 1;
    
    .month-title {
      text-align: center;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
    }
  }
  
  .calendar-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 2px;
    margin-bottom: 4px;
    
    .weekday {
      text-align: center;
      font-size: 11px;
      font-weight: 500;
      color: #909399;
      padding: 4px 0;
    }
  }
  
  .calendar-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 2px;
  }
  
  .calendar-cell {
    aspect-ratio: 1;
    position: relative;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 32px;
    
    .day-number {
      font-size: 12px;
      font-weight: 500;
    }
    
    .submit-count {
      font-size: 9px;
      opacity: 0.8;
      margin-top: 1px;
    }
    
    &.other-month {
      opacity: 0.3;
    }
    
    &.today {
      border: 2px solid #409eff;
    }
    
    &:hover:not(.other-month) {
      transform: scale(1.08);
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
  }
  
  .level-0 { background: #ebedf0; color: #909399; }
  .level-1 { background: #9be9a8; color: #1a5928; }
  .level-2 { background: #40c463; color: #fff; }
  .level-3 { background: #30a14e; color: #fff; }
  .level-4 { background: #216e39; color: #fff; }
}

.heatmap-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 16px;
  font-size: 11px;
  color: #909399;
  
  .legend-cell {
    width: 12px;
    height: 12px;
    border-radius: 3px;
  }
  
  .level-0 { background: #ebedf0; }
  .level-1 { background: #9be9a8; }
  .level-2 { background: #40c463; }
  .level-3 { background: #30a14e; }
  .level-4 { background: #216e39; }
}

.leaderboard-card {
  .rank-gold {
    color: #ffd700;
    font-weight: bold;
    font-size: 18px;
    text-shadow: 0 0 2px rgba(0,0,0,0.3);
  }
  
  .rank-silver {
    color: #c0c0c0;
    font-weight: bold;
    font-size: 16px;
  }
  
  .rank-bronze {
    color: #cd7f32;
    font-weight: bold;
  }
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
