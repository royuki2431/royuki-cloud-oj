<template>
  <div class="wrong-problems-page">
    <div class="page-header">
      <h2>我的错题本</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Refresh" @click="loadProblems" :loading="loading">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon total">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalCount || 0 }}</div>
            <div class="stat-label">总错题数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon error">
            <el-icon><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value error">{{ statistics.unresolvedCount || 0 }}</div>
            <div class="stat-label">未解决</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon success">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value success">{{ statistics.resolvedCount || 0 }}</div>
            <div class="stat-label">已解决</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-icon rate">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ resolveRate }}%</div>
            <div class="stat-label">解决率</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 错误类型分布 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>错误类型分布</span>
          </template>
          <div class="error-type-chart">
            <div v-for="(item, key) in errorTypeStats" :key="key" class="error-type-item">
              <div class="error-type-label">
                <el-tag :type="getErrorTypeTag(key)" size="small">{{ getErrorTypeText(key) }}</el-tag>
                <span class="count">{{ item.count }} 题</span>
              </div>
              <el-progress 
                :percentage="item.percentage" 
                :color="getErrorTypeColor(key)"
                :stroke-width="12"
              />
            </div>
            <el-empty v-if="Object.keys(errorTypeStats).length === 0" description="暂无错题数据" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span>难度分布</span>
          </template>
          <div class="difficulty-chart">
            <div v-for="(item, key) in difficultyStats" :key="key" class="difficulty-item">
              <div class="difficulty-label">
                <el-tag :type="getDifficultyType(key)" size="small">{{ getDifficultyText(key) }}</el-tag>
                <span class="count">{{ item.count }} 题</span>
              </div>
              <el-progress 
                :percentage="item.percentage" 
                :color="getDifficultyColor(key)"
                :stroke-width="12"
              />
            </div>
            <el-empty v-if="Object.keys(difficultyStats).length === 0" description="暂无错题数据" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选和错题列表 -->
    <el-card class="problem-list-card">
      <template #header>
        <div class="list-header">
          <span>错题列表</span>
          <div class="filter-group">
            <el-select v-model="filterDifficulty" placeholder="难度" clearable style="width: 100px; margin-right: 10px;">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
            <el-select v-model="filterErrorType" placeholder="错误类型" clearable style="width: 120px; margin-right: 10px;">
              <el-option label="编译错误" value="COMPILE_ERROR" />
              <el-option label="答案错误" value="WRONG_ANSWER" />
              <el-option label="超时" value="TIME_LIMIT_EXCEEDED" />
              <el-option label="内存超限" value="MEMORY_LIMIT_EXCEEDED" />
              <el-option label="运行错误" value="RUNTIME_ERROR" />
            </el-select>
            <el-radio-group v-model="filterType">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="unresolved">未解决</el-radio-button>
              <el-radio-button value="resolved">已解决</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      
      <el-table :data="paginatedProblems" v-loading="loading" style="width: 100%" empty-text="暂无错题记录" size="small">
        
        
        <el-table-column label="题目" min-width="120" max-width="400" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="goToProblem(row.problemId)">
              {{ getProblemTitle(row.problemId) }}
            </el-link>
          </template>
        </el-table-column>


        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(getProblemDifficulty(row.problemId))" size="small">
              {{ getDifficultyText(getProblemDifficulty(row.problemId)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getErrorTypeTag(row.errorType)" size="small">
              {{ getErrorTypeText(row.errorType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="错误次数" width="80" align="center">
          <template #default="{ row }">
            <span class="wrong-count">{{ row.wrongCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isResolved ? 'success' : 'danger'" size="small">
              {{ row.isResolved ? '已解决' : '未解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="添加时间" width="240" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="showAnalysis(row.problemId)">
              解析
            </el-button>
            <el-button type="primary" size="small" @click="goToProblem(row.problemId)">
              重做
            </el-button>
            <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="filteredProblems.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredProblems.length"
          layout="total, sizes, prev, pager, next"
          background
        />
      </div>
    </el-card>

    <!-- 解析对话框 -->
    <el-dialog
      v-model="analysisDialogVisible"
      :title="'题目解析 - ' + currentProblemTitle"
      width="70%"
      top="5vh"
      destroy-on-close
    >
      <div class="analysis-content" v-loading="analysisLoading">
        <div v-if="currentProblem">
          <!-- 题目信息 -->
          <div class="problem-info">
            <h3>{{ currentProblem.title }}</h3>
            <div class="problem-meta">
              <el-tag :type="getDifficultyType(currentProblem.difficulty)" size="small">
                {{ getDifficultyText(currentProblem.difficulty) }}
              </el-tag>
              <span class="category" v-if="currentProblem.category">{{ currentProblem.category }}</span>
            </div>
          </div>

          <!-- 题目描述 -->
          <el-divider content-position="left">题目描述</el-divider>
          <div class="description" v-html="currentProblem.description"></div>

          <!-- 解题思路 -->
          <el-divider content-position="left">解题思路</el-divider>
          <div class="solution-hint" v-if="currentProblem.hint">
            <div v-html="currentProblem.hint"></div>
          </div>
          <el-empty v-else description="暂无解题思路" :image-size="60" />

          <!-- 参考答案 -->
          <el-divider content-position="left">参考答案</el-divider>
          <div class="solution-code" v-if="referenceCode">
            <div class="code-source" v-if="referenceCodeSource">
              <el-tag type="info" size="small">{{ referenceCodeSource }}</el-tag>
            </div>
            <pre><code>{{ referenceCode }}</code></pre>
          </div>
          <div v-else-if="referenceLoading" class="loading-hint">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在加载参考答案...</span>
          </div>
          <el-empty v-else description="暂无参考答案" :image-size="60" />

          <!-- 知识点 -->
          <el-divider content-position="left">相关知识点</el-divider>
          <div class="knowledge-points" v-if="parsedTags.length > 0">
            <el-tag 
              v-for="tag in parsedTags" 
              :key="tag" 
              type="info" 
              size="small"
              style="margin-right: 8px; margin-bottom: 8px;"
            >
              {{ tag }}
            </el-tag>
          </div>
          <el-empty v-else description="暂无知识点标签" :image-size="60" />
        </div>
        <el-empty v-else-if="!analysisLoading" description="无法加载题目信息" />
      </div>
      <template #footer>
        <el-button @click="analysisDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="goToProblem(currentProblemId)">去做题</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Document, CircleClose, CircleCheck, TrendCharts, Loading } from '@element-plus/icons-vue'
import { 
  getWrongProblems, 
  resolveWrongProblem, 
  deleteWrongProblem,
  getWrongProblemStatistics,
  type WrongProblem 
} from '@/api/learning'
import { getProblemById } from '@/api/problem'
import { getProblemSubmissions } from '@/api/judge'
import type { Problem } from '@/types'

const router = useRouter()
const loading = ref(false)
const problems = ref<WrongProblem[]>([])
const filterType = ref('all')
const filterDifficulty = ref('')
const filterErrorType = ref('')
const statistics = ref<any>({})
const problemMap = ref<Map<number, Problem>>(new Map())
const currentPage = ref(1)
const pageSize = ref(10)

// 解析对话框相关
const analysisDialogVisible = ref(false)
const analysisLoading = ref(false)
const currentProblem = ref<Problem | null>(null)
const currentProblemId = ref(0)
const currentProblemTitle = computed(() => currentProblem.value?.title || '')

// 参考答案相关
const referenceCode = ref('')
const referenceCodeSource = ref('')
const referenceLoading = ref(false)

// 解析tags（后端可能返回JSON字符串）
const parsedTags = computed(() => {
  if (!currentProblem.value?.tags) return []
  const tags = currentProblem.value.tags
  if (Array.isArray(tags)) return tags
  if (typeof tags === 'string') {
    try {
      const parsed = JSON.parse(tags)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      // 如果不是JSON，可能是逗号分隔的字符串
      return tags.split(',').map(t => t.trim()).filter(t => t)
    }
  }
  return []
})

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 解决率
const resolveRate = computed(() => {
  const total = statistics.value.totalCount || 0
  const resolved = statistics.value.resolvedCount || 0
  if (total === 0) return '0.0'
  return ((resolved / total) * 100).toFixed(1)
})

// 错误类型统计
const errorTypeStats = computed(() => {
  const stats: Record<string, { count: number; percentage: number }> = {}
  const total = problems.value.length
  if (total === 0) return stats
  
  problems.value.forEach(p => {
    if (!stats[p.errorType]) {
      stats[p.errorType] = { count: 0, percentage: 0 }
    }
    stats[p.errorType].count++
  })
  
  Object.keys(stats).forEach(key => {
    stats[key].percentage = Math.round((stats[key].count / total) * 100)
  })
  
  return stats
})

// 难度统计
const difficultyStats = computed(() => {
  const stats: Record<string, { count: number; percentage: number }> = {}
  const total = problems.value.length
  if (total === 0) return stats
  
  problems.value.forEach(p => {
    const difficulty = getProblemDifficulty(p.problemId)
    if (!stats[difficulty]) {
      stats[difficulty] = { count: 0, percentage: 0 }
    }
    stats[difficulty].count++
  })
  
  Object.keys(stats).forEach(key => {
    stats[key].percentage = Math.round((stats[key].count / total) * 100)
  })
  
  return stats
})

// 过滤后的错题列表
const filteredProblems = computed(() => {
  let result = problems.value
  
  // 按状态筛选
  if (filterType.value === 'unresolved') {
    result = result.filter(p => !p.isResolved)
  } else if (filterType.value === 'resolved') {
    result = result.filter(p => p.isResolved)
  }
  
  // 按难度筛选
  if (filterDifficulty.value) {
    result = result.filter(p => getProblemDifficulty(p.problemId) === filterDifficulty.value)
  }
  
  // 按错误类型筛选
  if (filterErrorType.value) {
    result = result.filter(p => p.errorType === filterErrorType.value)
  }
  
  return result
})

// 分页后的数据
const paginatedProblems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredProblems.value.slice(start, end)
})

// 加载错题列表
const loadProblems = async () => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  
  loading.value = true
  try {
    const [problemsRes, statsRes] = await Promise.all([
      getWrongProblems(userId),
      getWrongProblemStatistics(userId)
    ])
    problems.value = problemsRes || []
    statistics.value = statsRes || {}
    
    // 加载题目信息
    await loadProblemInfo()
  } catch (error) {
    console.error('加载错题失败:', error)
    ElMessage.error('加载错题失败')
  } finally {
    loading.value = false
  }
}

// 加载题目信息
const loadProblemInfo = async () => {
  const problemIds = problems.value.map(p => p.problemId)
  const uniqueIds = [...new Set(problemIds)]
  
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

// 跳转到题目详情
const goToProblem = (problemId: number) => {
  analysisDialogVisible.value = false
  router.push(`/problem/${problemId}`)
}

// 显示题目解析
const showAnalysis = async (problemId: number) => {
  currentProblemId.value = problemId
  analysisDialogVisible.value = true
  analysisLoading.value = true
  referenceCode.value = ''
  referenceCodeSource.value = ''
  
  try {
    // 先从缓存中获取
    let problem = problemMap.value.get(problemId)
    if (!problem) {
      problem = await getProblemById(problemId)
      if (problem) {
        problemMap.value.set(problemId, problem)
      }
    }
    currentProblem.value = problem || null
    
    // 加载参考答案
    await loadReferenceCode(problemId)
  } catch (error) {
    console.error('获取题目解析失败:', error)
    ElMessage.error('获取题目解析失败')
    currentProblem.value = null
  } finally {
    analysisLoading.value = false
  }
}

// 加载参考答案（从通过的提交中获取）
const loadReferenceCode = async (problemId: number) => {
  referenceLoading.value = true
  try {
    // 获取该题目的提交记录，找一个通过的
    const submissions = await getProblemSubmissions(problemId, { page: 1, size: 50 })
    if (submissions && submissions.length > 0) {
      // 找到第一个通过的提交
      const acceptedSubmission = submissions.find((s: any) => s.status === 'ACCEPTED')
      if (acceptedSubmission && acceptedSubmission.code) {
        referenceCode.value = acceptedSubmission.code
        referenceCodeSource.value = '来自通过的提交'
      }
    }
  } catch (error) {
    console.error('获取参考答案失败:', error)
  } finally {
    referenceLoading.value = false
  }
}

// 获取错误类型标签颜色
const getErrorTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'COMPILE_ERROR': 'danger',
    'WRONG_ANSWER': 'warning',
    'TIME_LIMIT_EXCEEDED': 'info',
    'MEMORY_LIMIT_EXCEEDED': 'info',
    'RUNTIME_ERROR': 'danger'
  }
  return map[type] || 'info'
}

// 获取错误类型文本
const getErrorTypeText = (type: string) => {
  const map: Record<string, string> = {
    'COMPILE_ERROR': '编译错误',
    'WRONG_ANSWER': '答案错误',
    'TIME_LIMIT_EXCEEDED': '超时',
    'MEMORY_LIMIT_EXCEEDED': '内存超限',
    'RUNTIME_ERROR': '运行错误'
  }
  return map[type] || type
}

// 获取错误类型颜色（用于进度条）
const getErrorTypeColor = (type: string) => {
  const map: Record<string, string> = {
    'COMPILE_ERROR': '#f56c6c',
    'WRONG_ANSWER': '#e6a23c',
    'TIME_LIMIT_EXCEEDED': '#909399',
    'MEMORY_LIMIT_EXCEEDED': '#909399',
    'RUNTIME_ERROR': '#f56c6c'
  }
  return map[type] || '#909399'
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

// 获取难度颜色（用于进度条）
const getDifficultyColor = (difficulty: string) => {
  const map: Record<string, string> = {
    'EASY': '#67c23a',
    'MEDIUM': '#e6a23c',
    'HARD': '#f56c6c'
  }
  return map[difficulty] || '#909399'
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 格式化日期（简短格式）
const formatDate = (time: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 标记为已解决
const handleResolve = async (row: WrongProblem) => {
  try {
    await resolveWrongProblem(row.id)
    ElMessage.success('标记成功')
    loadProblems()
  } catch (error: any) {
    ElMessage.error('操作失败')
  }
}

// 删除错题（由el-popconfirm触发）
const handleDelete = async (row: WrongProblem) => {
  try {
    await deleteWrongProblem(row.id)
    ElMessage.success('删除成功')
    loadProblems()
  } catch (error: any) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadProblems()
})
</script>

<style scoped lang="scss">
.wrong-problems-page {
  padding: 20px;
  max-width: 1400px;
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    padding: 20px;
  }
  
  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;
    
    .el-icon {
      font-size: 28px;
      color: #fff;
    }
    
    &.total {
      background: linear-gradient(135deg, #409eff, #66b1ff);
    }
    
    &.error {
      background: linear-gradient(135deg, #f56c6c, #f89898);
    }
    
    &.success {
      background: linear-gradient(135deg, #67c23a, #85ce61);
    }
    
    &.rate {
      background: linear-gradient(135deg, #e6a23c, #ebb563);
    }
  }
  
  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      
      &.error {
        color: #f56c6c;
      }
      
      &.success {
        color: #67c23a;
      }
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  height: 100%;
  
  .error-type-chart,
  .difficulty-chart {
    .error-type-item,
    .difficulty-item {
      margin-bottom: 16px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .error-type-label,
      .difficulty-label {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        .count {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }
}

.problem-list-card {
  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .filter-group {
      display: flex;
      align-items: center;
    }
  }
  
  .problem-link {
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
    display: inline-block;
  }
  
  .wrong-count {
    color: #f56c6c;
    font-weight: bold;
  }
  
  .pagination-wrapper {
    padding: 12px 16px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #ebeef5;
  }
  
  :deep(.el-table) {
    .el-table__cell {
      padding: 8px 0;
    }
  }
}

// 解析对话框样式
.analysis-content {
  max-height: 70vh;
  overflow-y: auto;
  
  .problem-info {
    margin-bottom: 20px;
    
    h3 {
      margin: 0 0 10px 0;
      font-size: 20px;
      color: #303133;
    }
    
    .problem-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .category {
        color: #909399;
        font-size: 14px;
      }
    }
  }
  
  .description {
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    line-height: 1.8;
    
    :deep(pre) {
      background: #282c34;
      color: #abb2bf;
      padding: 12px;
      border-radius: 4px;
      overflow-x: auto;
    }
  }
  
  .solution-hint {
    padding: 16px;
    background: #f0f9eb;
    border-radius: 8px;
    border-left: 4px solid #67c23a;
    line-height: 1.8;
  }
  
  .solution-code {
    .code-source {
      margin-bottom: 8px;
    }
    
    pre {
      background: #282c34;
      color: #abb2bf;
      padding: 16px;
      border-radius: 8px;
      overflow-x: auto;
      margin: 0;
      
      code {
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        font-size: 14px;
        line-height: 1.6;
      }
    }
  }
  
  .loading-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #909399;
    padding: 20px;
    justify-content: center;
  }
  
  .knowledge-points {
    padding: 12px 0;
  }
}
</style>
