<template>
  <div class="ranking-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">全站排行榜</span>
          <el-button :icon="Refresh" @click="loadRanking">刷新</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="rankings"
        stripe
        style="width: 100%"
        :row-class-name="tableRowClassName"
        @sort-change="handleSortChange"
      >
        <el-table-column label="排名" width="80">
          <template #default="{ row }">
            <div class="rank-cell">
              <el-icon v-if="row.rank === 1" class="rank-icon gold">
                <Trophy />
              </el-icon>
              <el-icon v-else-if="row.rank === 2" class="rank-icon silver">
                <Trophy />
              </el-icon>
              <el-icon v-else-if="row.rank === 3" class="rank-icon bronze">
                <Trophy />
              </el-icon>
              <span class="rank-number">{{ row.rank }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="realName" label="姓名" width="80" align="center" />

        <el-table-column prop="username" label="用户名" min-width="120" />

        <el-table-column prop="school" label="学校" min-width="150" />

        <el-table-column prop="totalProblemsSolved" label="解决题目" width="110" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag type="success">{{ row.totalProblemsSolved || 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalSubmissions" label="总提交" width="100" align="center" sortable="custom" />

        <el-table-column prop="totalAccepted" label="通过次数" width="120" align="center" sortable="custom" />

        <el-table-column prop="acceptRate" label="通过率" width="130" sortable="custom">
          <template #default="{ row }">
            <el-progress
              :percentage="row.acceptRate || 0"
              :color="getProgressColor(row.acceptRate || 0)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>

        <el-table-column prop="totalScore" label="总分" width="100" align="center" sortable="custom">
          <template #default="{ row }">
            <span class="score">{{ row.totalScore || 0 }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: center"
        @current-change="applySort"
        @size-change="applySort"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface RankingItem {
  rank: number
  userId: number
  username: string
  realName: string
  school: string
  totalProblemsSolved: number
  totalSubmissions: number
  totalAccepted: number
  totalScore: number
  acceptRate: number
}

// 分页参数
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 数据
const loading = ref(false)
const rankings = ref<RankingItem[]>([])
const allData = ref<RankingItem[]>([])

// 排序参数
const sortProp = ref('')
const sortOrder = ref('')

// 加载排行榜数据
const loadRanking = async () => {
  loading.value = true
  try {
    const res = await request.get('/learning/statistics/leaderboard', {
      params: { limit: 100 }
    })
    
    const data = res || []
    // 计算通过率并添加排名
    data.forEach((item: any, index: number) => {
      item.acceptRate = item.totalSubmissions > 0 
        ? Math.round((item.totalAccepted / item.totalSubmissions) * 100) 
        : 0
      item.rank = index + 1
    })
    
    allData.value = data
    applySort()
  } catch (error: any) {
    ElMessage.error(error.message || '加载排行榜失败')
  } finally {
    loading.value = false
  }
}

// 应用排序和分页
const applySort = () => {
  let data = [...allData.value]
  
  // 排序
  if (sortProp.value && sortOrder.value) {
    data.sort((a: any, b: any) => {
      const valA = a[sortProp.value] || 0
      const valB = b[sortProp.value] || 0
      if (sortOrder.value === 'ascending') {
        return valA - valB
      } else {
        return valB - valA
      }
    })
  }
  
  // 重新计算排名
  data.forEach((item, index) => {
    item.rank = index + 1
  })
  
  // 分页处理
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  rankings.value = data.slice(start, end)
  total.value = data.length
}

// 处理排序变化
const handleSortChange = ({ prop, order }: { prop: string, order: string }) => {
  sortProp.value = prop
  sortOrder.value = order
  currentPage.value = 1
  applySort()
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 表格行类名
const tableRowClassName = ({ row }: { row: RankingItem }) => {
  if (row.rank === 1) return 'first-place'
  if (row.rank === 2) return 'second-place'
  if (row.rank === 3) return 'third-place'
  return ''
}

onMounted(() => {
  loadRanking()
})
</script>

<style scoped lang="scss">
.ranking-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title {
    font-size: 18px;
    font-weight: bold;
  }
}

.rank-cell {
  display: flex;
  align-items: center;
  gap: 8px;

  .rank-icon {
    font-size: 20px;

    &.gold {
      color: #ffd700;
    }

    &.silver {
      color: #c0c0c0;
    }

    &.bronze {
      color: #cd7f32;
    }
  }

  .rank-number {
    font-weight: bold;
    font-size: 16px;
  }
}

.score {
  font-weight: bold;
  color: #409eff;
  font-size: 16px;
}

:deep(.first-place) {
  background-color: #fff7e6 !important;
}

:deep(.second-place) {
  background-color: #f5f5f5 !important;
}

:deep(.third-place) {
  background-color: #fff4f1 !important;
}
</style>
