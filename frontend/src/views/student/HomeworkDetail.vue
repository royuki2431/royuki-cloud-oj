<template>
  <div class="homework-detail-page">
    <el-page-header @back="$router.back()">
      <template #content>
        <span class="page-title">{{ homework?.title || '作业详情' }}</span>
      </template>
    </el-page-header>

    <div class="homework-content" v-loading="loading">
      <!-- 作业信息卡片 -->
      <el-card class="homework-info-card">
        <div class="homework-header">
          <div class="homework-meta">
            <h1>{{ homework?.title }}</h1>
            <p class="description">{{ homework?.description || '暂无描述' }}</p>
            <div class="time-info">
              <div class="time-item">
                <span class="label">开始时间:</span>
                <span>{{ formatTime(homework?.startTime) }}</span>
              </div>
              <div class="time-item">
                <span class="label">截止时间:</span>
                <span :class="{ 'text-danger': isOverdue }">
                  {{ formatTime(homework?.endTime) }}
                </span>
              </div>
              <div class="time-item" v-if="!isOverdue && homework?.endTime">
                <span class="label">剩余时间:</span>
                <span class="countdown">{{ remainingTime }}</span>
              </div>
            </div>
          </div>
          <div class="score-summary">
            <div class="score-circle">
              <el-progress 
                type="circle" 
                :percentage="scorePercentage" 
                :width="120"
                :stroke-width="10"
              >
                <template #default>
                  <div class="score-text">
                    <span class="current">{{ myTotalScore }}</span>
                    <span class="divider">/</span>
                    <span class="total">{{ homework?.totalScore || 0 }}</span>
                  </div>
                </template>
              </el-progress>
            </div>
            <p class="score-label">我的得分</p>
          </div>
        </div>
      </el-card>

      <!-- 题目列表 -->
      <el-card class="problems-card">
        <template #header>
          <div class="card-header">
            <span>题目列表</span>
            <el-tag type="info">共 {{ problems.length }} 题</el-tag>
          </div>
        </template>
        
        <el-table :data="problems" stripe>
          <el-table-column label="序号" width="80" align="center">
            <template #default="{ $index }">
              {{ $index + 1 }}
            </template>
          </el-table-column>
          <el-table-column label="题目" min-width="200">
            <template #default="{ row }">
              <el-link type="primary" @click="goToProblem(row)">
                {{ row.problemTitle || `题目 #${row.problemId}` }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="分值" width="100" align="center">
            <template #default="{ row }">
              <span class="problem-score">{{ row.score }} 分</span>
            </template>
          </el-table-column>
          <el-table-column label="我的得分" width="120" align="center">
            <template #default="{ row }">
              <span :class="getScoreClass(row)">
                {{ row.myScore || 0 }} / {{ row.score }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getProblemStatusType(row)" size="small">
                {{ getProblemStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small" 
                @click="goToProblem(row)"
              >
                {{ row.myScore > 0 ? '继续做题' : '开始做题' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 提交统计 -->
      <el-card class="stats-card">
        <template #header>
          <span>提交统计</span>
        </template>
        <div class="stats-content">
          <div class="stat-item">
            <div class="stat-value">{{ completedCount }}</div>
            <div class="stat-label">已完成题目</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ problems.length - completedCount }}</div>
            <div class="stat-label">未完成题目</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ myTotalScore }}</div>
            <div class="stat-label">当前总分</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ scorePercentage }}%</div>
            <div class="stat-label">完成进度</div>
          </div>
        </div>
        <el-progress 
          :percentage="scorePercentage" 
          :stroke-width="12"
          :show-text="false"
          class="progress-bar"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHomeworkDetail } from '@/api/course'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const homework = ref<any>(null)
const problems = ref<any[]>([])
const remainingTime = ref('')

const homeworkId = Number(route.params.id)
let timer: number | null = null

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 计算是否已截止
const isOverdue = computed(() => {
  if (!homework.value?.endTime) return false
  return new Date(homework.value.endTime) < new Date()
})

// 计算我的总分
const myTotalScore = computed(() => {
  return problems.value.reduce((sum, p) => sum + (p.myScore || 0), 0)
})

// 计算得分百分比
const scorePercentage = computed(() => {
  if (!homework.value?.totalScore) return 0
  return Math.round((myTotalScore.value / homework.value.totalScore) * 100)
})

// 计算已完成题目数
const completedCount = computed(() => {
  return problems.value.filter(p => p.myScore >= p.score).length
})

// 加载作业数据
const loadHomeworkData = async () => {
  loading.value = true
  try {
    const res = await getHomeworkDetail(homeworkId)
    homework.value = res.homework
    problems.value = res.problems || []
    
    // 开始倒计时
    startCountdown()
  } catch (error) {
    console.error('加载作业数据失败:', error)
    ElMessage.error('加载作业数据失败')
  } finally {
    loading.value = false
  }
}

// 开始倒计时
const startCountdown = () => {
  if (timer) clearInterval(timer)
  
  const updateCountdown = () => {
    if (!homework.value?.endTime) {
      remainingTime.value = '-'
      return
    }
    
    const end = new Date(homework.value.endTime).getTime()
    const now = Date.now()
    const diff = end - now
    
    if (diff <= 0) {
      remainingTime.value = '已截止'
      if (timer) clearInterval(timer)
      return
    }
    
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)
    
    if (days > 0) {
      remainingTime.value = `${days}天 ${hours}小时`
    } else if (hours > 0) {
      remainingTime.value = `${hours}小时 ${minutes}分钟`
    } else {
      remainingTime.value = `${minutes}分 ${seconds}秒`
    }
  }
  
  updateCountdown()
  timer = window.setInterval(updateCountdown, 1000)
}

// 跳转到题目
const goToProblem = (problem: any) => {
  // 带上作业ID参数，方便记录作业提交
  router.push(`/problem/${problem.problemId}?homeworkId=${homeworkId}`)
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取得分样式
const getScoreClass = (problem: any) => {
  if (problem.myScore >= problem.score) return 'score-full'
  if (problem.myScore > 0) return 'score-partial'
  return 'score-zero'
}

// 获取题目状态类型
const getProblemStatusType = (problem: any) => {
  if (problem.myScore >= problem.score) return 'success'
  if (problem.myScore > 0) return 'warning'
  return 'info'
}

// 获取题目状态文本
const getProblemStatusText = (problem: any) => {
  if (problem.myScore >= problem.score) return '已完成'
  if (problem.myScore > 0) return '部分完成'
  return '未开始'
}

onMounted(() => {
  loadHomeworkData()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.homework-detail-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.homework-content {
  margin-top: 20px;
}

.homework-info-card {
  margin-bottom: 20px;
  
  .homework-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }
  
  .homework-meta {
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
    
    .time-info {
      display: flex;
      gap: 32px;
      
      .time-item {
        .label {
          color: #909399;
          margin-right: 8px;
        }
        
        .countdown {
          color: #e6a23c;
          font-weight: 600;
        }
      }
    }
  }
  
  .score-summary {
    text-align: center;
    
    .score-circle {
      .score-text {
        .current {
          font-size: 24px;
          font-weight: bold;
          color: #409eff;
        }
        
        .divider {
          margin: 0 4px;
          color: #c0c4cc;
        }
        
        .total {
          font-size: 16px;
          color: #909399;
        }
      }
    }
    
    .score-label {
      margin-top: 8px;
      color: #909399;
      font-size: 14px;
    }
  }
}

.problems-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .problem-score {
    color: #409eff;
    font-weight: 500;
  }
  
  .score-full {
    color: #67c23a;
    font-weight: 600;
  }
  
  .score-partial {
    color: #e6a23c;
    font-weight: 600;
  }
  
  .score-zero {
    color: #909399;
  }
}

.stats-card {
  .stats-content {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 20px;
  }
  
  .stat-item {
    text-align: center;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #409eff;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 8px;
    }
  }
  
  .progress-bar {
    margin-top: 16px;
  }
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
