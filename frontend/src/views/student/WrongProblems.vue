<template>
  <div class="wrong-problems-page">
    <div class="page-header">
      <h2>我的错题本</h2>
      <div class="header-actions">
        <el-radio-group v-model="filterType" @change="handleFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unresolved">未解决</el-radio-button>
          <el-radio-button value="resolved">已解决</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-value">{{ statistics.total || 0 }}</div>
        <div class="stat-label">总错题数</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value error">{{ statistics.unresolved || 0 }}</div>
        <div class="stat-label">未解决</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-value success">{{ statistics.resolved || 0 }}</div>
        <div class="stat-label">已解决</div>
      </el-card>
    </div>

    <!-- 错题列表 -->
    <el-card class="problem-list-card">
      <el-table :data="filteredProblems" v-loading="loading" style="width: 100%">
        <el-table-column prop="problemId" label="题目ID" width="100" />
        <el-table-column label="题目" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="goToProblem(row.problemId)">
              查看题目 #{{ row.problemId }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="errorType" label="错误类型" width="150">
          <template #default="{ row }">
            <el-tag :type="getErrorTypeTag(row.errorType)" size="small">
              {{ getErrorTypeText(row.errorType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="wrongCount" label="错误次数" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isResolved ? 'success' : 'danger'" size="small">
              {{ row.isResolved ? '已解决' : '未解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="添加时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button 
              v-if="!row.isResolved"
              type="success" 
              size="small" 
              @click="handleResolve(row)"
            >
              标记已解决
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getWrongProblems, 
  resolveWrongProblem, 
  deleteWrongProblem,
  getWrongProblemStatistics,
  type WrongProblem 
} from '@/api/learning'

const router = useRouter()
const loading = ref(false)
const problems = ref<WrongProblem[]>([])
const filterType = ref('all')
const statistics = ref<any>({})

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 过滤后的错题列表
const filteredProblems = computed(() => {
  if (filterType.value === 'all') {
    return problems.value
  } else if (filterType.value === 'unresolved') {
    return problems.value.filter(p => !p.isResolved)
  } else {
    return problems.value.filter(p => p.isResolved)
  }
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
    problems.value = problemsRes
    statistics.value = statsRes
  } catch (error) {
    console.error('加载错题失败:', error)
    ElMessage.error('加载错题失败')
  } finally {
    loading.value = false
  }
}

// 处理筛选变化
const handleFilterChange = () => {
  // 筛选由 computed 自动处理
}

// 跳转到题目详情
const goToProblem = (problemId: number) => {
  router.push(`/problem/${problemId}`)
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

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 标记为已解决
const handleResolve = async (row: WrongProblem) => {
  try {
    await ElMessageBox.confirm('确定将此题标记为已解决吗？', '提示', {
      type: 'info'
    })
    await resolveWrongProblem(row.id)
    ElMessage.success('标记成功')
    loadProblems()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除错题
const handleDelete = async (row: WrongProblem) => {
  try {
    await ElMessageBox.confirm('确定删除此错题记录吗？', '警告', {
      type: 'warning'
    })
    await deleteWrongProblem(row.id)
    ElMessage.success('删除成功')
    loadProblems()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadProblems()
})
</script>

<style scoped lang="scss">
.wrong-problems-page {
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
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  
  .stat-value {
    font-size: 36px;
    font-weight: bold;
    color: #409eff;
    
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
    margin-top: 8px;
  }
}

.problem-list-card {
  :deep(.el-card__body) {
    padding: 0;
  }
}
</style>
