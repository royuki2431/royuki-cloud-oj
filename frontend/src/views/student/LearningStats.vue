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
    </div>

    <!-- 学习热力图 -->
    <el-card class="heatmap-card">
      <template #header>
        <div class="card-header">
          <span>学习热力图</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="loadHeatmap"
          />
        </div>
      </template>
      <div class="heatmap-container" v-loading="heatmapLoading">
        <div class="heatmap-grid">
          <div 
            v-for="(day, index) in heatmapData" 
            :key="index"
            class="heatmap-cell"
            :class="getHeatmapClass(day.submitCount)"
            :title="`${day.statDate}: ${day.submitCount} 次提交`"
          ></div>
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
      </div>
    </el-card>

    <!-- 排行榜 -->
    <el-card class="leaderboard-card">
      <template #header>
        <span>排行榜 Top 10</span>
      </template>
      <el-table :data="leaderboard" v-loading="leaderboardLoading">
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <span :class="getRankClass($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="totalProblemsSolved" label="解决题目" align="center" />
        <el-table-column prop="totalAccepted" label="通过次数" align="center" />
        <el-table-column prop="totalSubmissions" label="提交次数" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Select, Trophy, TrendCharts } from '@element-plus/icons-vue'
import { 
  getLearningOverview, 
  getLearningHeatmap, 
  getLeaderboard,
  type LearningStatistics
} from '@/api/learning'

const overview = ref<any>({})
const heatmapData = ref<LearningStatistics[]>([])
const leaderboard = ref<any[]>([])
const heatmapLoading = ref(false)
const leaderboardLoading = ref(false)

// 日期范围，默认最近3个月
const getDefaultDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 3)
  return [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]
}
const dateRange = ref(getDefaultDateRange())

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

// 加载热力图数据
const loadHeatmap = async () => {
  const userId = getUserId()
  if (!userId || !dateRange.value) return
  
  heatmapLoading.value = true
  try {
    heatmapData.value = await getLearningHeatmap(
      userId, 
      dateRange.value[0], 
      dateRange.value[1]
    )
  } catch (error) {
    console.error('加载热力图失败:', error)
  } finally {
    heatmapLoading.value = false
  }
}

// 加载排行榜
const loadLeaderboard = async () => {
  leaderboardLoading.value = true
  try {
    leaderboard.value = await getLeaderboard(10)
  } catch (error) {
    console.error('加载排行榜失败:', error)
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
  loadLeaderboard()
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
  grid-template-columns: repeat(4, 1fr);
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

.heatmap-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.heatmap-container {
  .heatmap-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 3px;
  }
  
  .heatmap-cell {
    width: 12px;
    height: 12px;
    border-radius: 2px;
    cursor: pointer;
  }
  
  .level-0 { background: #ebedf0; }
  .level-1 { background: #9be9a8; }
  .level-2 { background: #40c463; }
  .level-3 { background: #30a14e; }
  .level-4 { background: #216e39; }
  
  .heatmap-legend {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 12px;
    font-size: 12px;
    color: #909399;
    
    .legend-cell {
      width: 12px;
      height: 12px;
      border-radius: 2px;
    }
  }
}

.leaderboard-card {
  .rank-gold {
    color: #ffd700;
    font-weight: bold;
    font-size: 18px;
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
