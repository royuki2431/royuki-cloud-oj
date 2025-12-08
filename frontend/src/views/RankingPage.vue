<template>
  <div class="ranking-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">排行榜</span>
          <el-button :icon="Refresh" @click="loadRanking">刷新</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="rankings"
        stripe
        style="width: 100%"
        :row-class-name="tableRowClassName"
      >
        <el-table-column label="排名" width="100">
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

        <el-table-column prop="username" label="用户名" min-width="150" />

        <el-table-column prop="solvedCount" label="解决题目" width="120" sortable>
          <template #default="{ row }">
            <el-tag type="success">{{ row.solvedCount }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="totalSubmissions" label="总提交" width="120" sortable />

        <el-table-column label="通过率" width="150" sortable>
          <template #default="{ row }">
            <el-progress
              :percentage="row.acceptRate"
              :color="getProgressColor(row.acceptRate)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="score" label="总分" width="120" sortable>
          <template #default="{ row }">
            <span class="score">{{ row.score }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: center"
        @current-change="loadRanking"
        @size-change="loadRanking"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy, Refresh } from '@element-plus/icons-vue'
import type { RankingItem } from '@/types'

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 数据
const loading = ref(false)
const rankings = ref<RankingItem[]>([])

// 加载排行榜数据（模拟数据，后续接入真实API）
const loadRanking = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise((resolve) => setTimeout(resolve, 500))

    // 生成模拟数据
    const mockData: RankingItem[] = Array.from({ length: 50 }, (_, i) => ({
      rank: i + 1,
      userId: i + 1,
      username: `user${i + 1}`,
      solvedCount: Math.floor(Math.random() * 100),
      totalSubmissions: Math.floor(Math.random() * 500) + 100,
      acceptRate: Math.floor(Math.random() * 50) + 50,
      score: Math.floor(Math.random() * 5000) + 1000,
    })).sort((a, b) => b.score - a.score)

    // 更新排名
    mockData.forEach((item, index) => {
      item.rank = index + 1
    })

    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value

    rankings.value = mockData.slice(start, end)
    total.value = mockData.length
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
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
