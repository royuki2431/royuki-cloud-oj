<template>
  <div class="learning-progress-page" v-loading="loading">
    <div class="page-header">
      <h2>学习进度</h2>
      <el-button type="primary" text @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 总体统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-main">
            <span class="stat-value">{{ summary.solvedCount || 0 }}</span>
            <span class="stat-divider">/</span>
            <span class="stat-total">{{ summary.totalProblems || 0 }}</span>
          </div>
          <div class="stat-label">已解决题目</div>
        </div>
        <el-progress 
          :percentage="solvedPercentage" 
          :stroke-width="8"
          :show-text="false"
        />
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-main">
            <span class="stat-value">{{ summary.totalSubmissions || 0 }}</span>
          </div>
          <div class="stat-label">提交总数</div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-main">
            <span class="stat-value success">{{ acceptRate }}%</span>
          </div>
          <div class="stat-label">通过率</div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-main">
            <span class="stat-value warning">{{ summary.streak || 0 }}</span>
          </div>
          <div class="stat-label">连续学习天数</div>
        </div>
      </el-card>
    </div>

    <el-row :gutter="20">
      <!-- 难度分布 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>难度分布</span>
          </template>
          <div class="difficulty-chart-container">
            <div class="pie-chart" ref="pieChartRef"></div>
            <div class="difficulty-legend">
              <div class="legend-item">
                <div class="legend-header">
                  <span class="legend-dot easy"></span>
                  <span class="legend-label">简单</span>
                  <span class="legend-value">{{ difficultyStats.easy?.solved || 0 }} / {{ difficultyStats.easy?.total || 0 }}</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(difficultyStats.easy?.solved, difficultyStats.easy?.total)" 
                  :stroke-width="6"
                  status="success"
                  :show-text="false"
                />
              </div>
              <div class="legend-item">
                <div class="legend-header">
                  <span class="legend-dot medium"></span>
                  <span class="legend-label">中等</span>
                  <span class="legend-value">{{ difficultyStats.medium?.solved || 0 }} / {{ difficultyStats.medium?.total || 0 }}</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(difficultyStats.medium?.solved, difficultyStats.medium?.total)" 
                  :stroke-width="6"
                  status="warning"
                  :show-text="false"
                />
              </div>
              <div class="legend-item">
                <div class="legend-header">
                  <span class="legend-dot hard"></span>
                  <span class="legend-label">困难</span>
                  <span class="legend-value">{{ difficultyStats.hard?.solved || 0 }} / {{ difficultyStats.hard?.total || 0 }}</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(difficultyStats.hard?.solved, difficultyStats.hard?.total)" 
                  :stroke-width="6"
                  status="exception"
                  :show-text="false"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 最近提交 -->
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>最近提交</span>
          </template>
          <el-table :data="recentSubmissions" max-height="250" size="small">
            <el-table-column prop="problemId" label="题目" min-width="100">
              <template #default="{ row }">
                <el-link type="primary" @click="goToProblem(row.problemId)">
                  {{ getProblemTitle(row.problemId) }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="65">
              <template #default="{ row }">
                <el-tag :type="getDifficultyType(getProblemDifficulty(row.problemId))" size="small">
                  {{ getDifficultyText(getProblemDifficulty(row.problemId)) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="75">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executionTime" label="耗时" width="70" align="center">
              <template #default="{ row }">
                <span v-if="row.executionTime !== null && row.executionTime !== undefined">{{ row.executionTime }}ms</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastSubmitTime" label="时间" width="90">
              <template #default="{ row }">
                {{ formatTime(row.lastSubmitTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 推荐题目 -->
    <el-card class="recommend-card">
      <template #header>
        <div class="card-header">
          <span>推荐题目</span>
          <el-button type="primary" text @click="refreshRecommend">
            <el-icon><Refresh /></el-icon>
            换一批
          </el-button>
        </div>
      </template>
      <div class="recommend-list">
        <div 
          v-for="problem in recommendProblems" 
          :key="problem.id" 
          class="recommend-item"
          @click="goToProblem(problem.id)"
        >
          <div class="problem-info">
            <span class="problem-title">{{ problem.title }}</span>
            <el-tag :type="getDifficultyType(problem.difficulty)" size="small">
              {{ getDifficultyText(problem.difficulty) }}
            </el-tag>
          </div>
          <div class="problem-meta">
            <span>通过率: {{ problem.acceptRate || 0 }}%</span>
          </div>
        </div>
        <el-empty v-if="recommendProblems.length === 0" description="暂无推荐题目" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getLearningSummary, getUserProgressList, type LearningProgress, type LearningSummary } from '@/api/learning'
import { getProblemList, getProblemById } from '@/api/problem'
import type { Problem } from '@/types'
import * as echarts from 'echarts'

const router = useRouter()
const loading = ref(false)
const pieChartRef = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null

const summary = ref<LearningSummary>({
  solvedCount: 0,
  totalProblems: 0,
  totalSubmissions: 0,
  acceptedCount: 0,
  easySolved: 0,
  mediumSolved: 0,
  hardSolved: 0,
  easyTotal: 50,
  mediumTotal: 80,
  hardTotal: 30,
  streak: 0
})
const progressList = ref<LearningProgress[]>([])
const recommendProblems = ref<any[]>([])
const problemMap = ref<Map<number, Problem>>(new Map())
const allProblems = ref<any[]>([])

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 计算已解决百分比
const solvedPercentage = computed(() => {
  if (!summary.value.totalProblems) return 0
  return Math.round((summary.value.solvedCount / summary.value.totalProblems) * 100)
})

// 计算通过率
const acceptRate = computed(() => {
  if (!summary.value.totalSubmissions) return '0.0'
  return ((summary.value.acceptedCount / summary.value.totalSubmissions) * 100).toFixed(1)
})

// 难度统计
const difficultyStats = computed(() => ({
  easy: { total: summary.value.easyTotal, solved: summary.value.easySolved },
  medium: { total: summary.value.mediumTotal, solved: summary.value.mediumSolved },
  hard: { total: summary.value.hardTotal, solved: summary.value.hardSolved }
}))

// 最近提交
const recentSubmissions = computed(() => {
  return progressList.value
    .filter(p => p.lastSubmitTime)
    .sort((a, b) => new Date(b.lastSubmitTime!).getTime() - new Date(a.lastSubmitTime!).getTime())
    .slice(0, 10)
})

// 加载数据
const loadData = async () => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true
  try {
    const [summaryRes, progressRes] = await Promise.all([
      getLearningSummary(userId),
      getUserProgressList(userId),
      loadAllProblems()
    ])
    
    summary.value = summaryRes
    progressList.value = progressRes || []
    
    // 加载题目信息用于显示题目名称
    await loadProblemInfo()
    
    // 加载推荐题目
    loadRecommendProblems()
    
    // 初始化饼图
    nextTick(() => {
      initPieChart()
    })
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载学习进度失败')
  } finally {
    loading.value = false
  }
}

// 初始化饼图
const initPieChart = () => {
  if (!pieChartRef.value) return
  
  if (pieChart) {
    pieChart.dispose()
  }
  
  pieChart = echarts.init(pieChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    series: [
      {
        type: 'pie',
        radius: ['50%', '75%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'center',
          formatter: () => {
            const total = (summary.value.easySolved || 0) + (summary.value.mediumSolved || 0) + (summary.value.hardSolved || 0)
            return `{value|${total}}\n{label|已解决}`
          },
          rich: {
            value: {
              fontSize: 24,
              fontWeight: 'bold',
              color: '#303133'
            },
            label: {
              fontSize: 12,
              color: '#909399',
              padding: [5, 0, 0, 0]
            }
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: summary.value.easySolved || 0, name: '简单', itemStyle: { color: '#67c23a' } },
          { value: summary.value.mediumSolved || 0, name: '中等', itemStyle: { color: '#e6a23c' } },
          { value: summary.value.hardSolved || 0, name: '困难', itemStyle: { color: '#f56c6c' } }
        ]
      }
    ]
  }
  
  pieChart.setOption(option)
}

// 监听窗口大小变化
window.addEventListener('resize', () => {
  pieChart?.resize()
})

// 加载题目信息（用于显示题目名称）
const loadProblemInfo = async () => {
  const problemIds = progressList.value.map(p => p.problemId)
  const uniqueIds = [...new Set(problemIds)]
  
  // 批量获取题目信息
  const promises = uniqueIds.map(async (id) => {
    try {
      const problem = await getProblemById(id)
      if (problem) {
        problemMap.value.set(id, problem)
      }
    } catch (error) {
      console.error(`获取题目${id}信息失败:`, error)
    }
  })
  
  await Promise.all(promises)
}

// 获取题目名称
const getProblemTitle = (problemId: number) => {
  const problem = problemMap.value.get(problemId)
  return problem?.title || `题目 #${problemId}`
}

// 获取题目难度
const getProblemDifficulty = (problemId: number) => {
  const problem = problemMap.value.get(problemId)
  return problem?.difficulty || 'EASY'
}

// 加载所有题目用于推荐
const loadAllProblems = async () => {
  try {
    const res = await getProblemList({ pageNum: 1, pageSize: 200 })
    allProblems.value = res.list || res.records || []
  } catch (error) {
    console.error('加载题目列表失败:', error)
  }
}

// 加载推荐题目（未完成的简单题优先，简单题做完推荐中等，中等做完推荐困难）
const loadRecommendProblems = async () => {
  // 获取已完成的题目ID集合
  const completedIds = new Set(
    progressList.value
      .filter(p => p.status === 'COMPLETED')
      .map(p => p.problemId)
  )
  
  // 按难度分类未完成的题目
  const unsolvedEasy = allProblems.value.filter(p => p.difficulty === 'EASY' && !completedIds.has(p.id))
  const unsolvedMedium = allProblems.value.filter(p => p.difficulty === 'MEDIUM' && !completedIds.has(p.id))
  const unsolvedHard = allProblems.value.filter(p => p.difficulty === 'HARD' && !completedIds.has(p.id))
  
  let candidates: any[] = []
  
  // 优先推荐简单题
  if (unsolvedEasy.length > 0) {
    candidates = unsolvedEasy
  } else if (unsolvedMedium.length > 0) {
    // 简单题都做完了，推荐中等题
    candidates = unsolvedMedium
  } else if (unsolvedHard.length > 0) {
    // 中等题也做完了，推荐困难题
    candidates = unsolvedHard
  }
  
  // 随机选取5道题
  const shuffled = candidates.sort(() => Math.random() - 0.5)
  const selected = shuffled.slice(0, 5)
  
  // 计算通过率
  recommendProblems.value = selected.map((p: any) => ({
    ...p,
    acceptRate: p.submitCount > 0 
      ? ((p.acceptCount / p.submitCount) * 100).toFixed(1)
      : '0.0'
  }))
}

// 刷新推荐
const refreshRecommend = () => {
  loadRecommendProblems()
}

// 跳转到题目
const goToProblem = (problemId: number) => {
  router.push(`/problem/${problemId}`)
}

// 获取百分比
const getPercentage = (solved: number | undefined, total: number | undefined) => {
  if (!total) return 0
  return Math.round(((solved || 0) / total) * 100)
}

// 获取状态类型
const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    'COMPLETED': 'success',
    'IN_PROGRESS': 'warning',
    'NOT_STARTED': 'info',
    // 兼容旧状态
    'SOLVED': 'success',
    'ATTEMPTED': 'warning'
  }
  return map[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    'COMPLETED': '已通过',
    'IN_PROGRESS': '尝试中',
    'NOT_STARTED': '未开始',
    // 兼容旧状态
    'SOLVED': '已通过',
    'ATTEMPTED': '尝试中'
  }
  return map[status] || status
}

// 获取难度类型
const getDifficultyType = (difficulty: string) => {
  const map: Record<string, string> = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger'
  }
  return map[difficulty] || 'info'
}

// 获取难度文本
const getDifficultyText = (difficulty: string) => {
  const map: Record<string, string> = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难'
  }
  return map[difficulty] || difficulty
}

// 格式化时间
const formatTime = (time: string | null) => {
  if (!time) return '-'
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.learning-progress-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h2 {
    margin: 0;
    font-size: 24px;
    color: #303133;
  }
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  .stat-content {
    text-align: center;
    margin-bottom: 12px;
  }
  
  .stat-main {
    font-size: 32px;
    font-weight: bold;
    color: #409eff;
    
    .stat-divider {
      margin: 0 4px;
      color: #c0c4cc;
    }
    
    .stat-total {
      font-size: 20px;
      color: #909399;
    }
    
    &.success, .success {
      color: #67c23a;
    }
    
    &.warning, .warning {
      color: #e6a23c;
    }
  }
  
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 8px;
  }
}

.chart-card {
  margin-bottom: 20px;
  
  .difficulty-chart-container {
    display: flex;
    align-items: center;
    gap: 20px;
    
    .pie-chart {
      width: 160px;
      height: 160px;
      flex-shrink: 0;
    }
    
    .difficulty-legend {
      flex: 1;
      
      .legend-item {
        margin-bottom: 16px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .legend-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 6px;
        }
        
        .legend-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          flex-shrink: 0;
          
          &.easy { background-color: #67c23a; }
          &.medium { background-color: #e6a23c; }
          &.hard { background-color: #f56c6c; }
        }
        
        .legend-label {
          font-size: 14px;
          color: #606266;
          min-width: 36px;
        }
        
        .legend-value {
          font-size: 13px;
          color: #909399;
          margin-left: auto;
        }
      }
    }
  }
}

.recommend-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .recommend-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
  }
  
  .recommend-item {
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: #ecf5ff;
      transform: translateY(-2px);
    }
    
    .problem-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      
      .problem-title {
        font-weight: 500;
        color: #303133;
      }
    }
    
    .problem-meta {
      font-size: 12px;
      color: #909399;
    }
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
