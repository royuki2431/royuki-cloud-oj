<template>
  <div class="problem-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">题目列表</span>
          <div class="filters">
            <el-input
              v-model="searchTitle"
              placeholder="搜索题目"
              style="width: 200px"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
            <el-select
              v-model="selectedDifficulty"
              placeholder="难度筛选"
              clearable
              style="width: 120px"
              @change="handleFilter"
            >
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="problems" stripe style="width: 100%">
        <el-table-column prop="id" label="题号" width="80" />

        <el-table-column label="题目" min-width="200">
          <template #default="{ row }">
            <el-link type="primary" @click="goToProblem(row.id)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="难度" width="100">
          <template #default="{ row }">
            <el-tag :type="DifficultyColor[row.difficulty]">
              {{ DifficultyText[row.difficulty] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="标签" min-width="200">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags"
              :key="tag"
              size="small"
              style="margin-right: 8px"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="通过率" width="120">
          <template #default="{ row }">
            {{
              row.submitCount > 0
                ? ((row.acceptCount / row.submitCount) * 100).toFixed(1) + '%'
                : '0%'
            }}
          </template>
        </el-table-column>

        <el-table-column label="提交/通过" width="120">
          <template #default="{ row }">
            {{ row.submitCount }} / {{ row.acceptCount }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goToProblem(row.id)">
              开始做题
            </el-button>
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
        @current-change="loadProblems"
        @size-change="loadProblems"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getProblemList, searchProblems, getProblemsByDifficulty } from '@/api/problem'
import { DifficultyText, DifficultyColor, type Problem } from '@/types'

const router = useRouter()

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选参数
const searchTitle = ref('')
const selectedDifficulty = ref('')

// 数据
const loading = ref(false)
const problems = ref<Problem[]>([])

// 加载题目列表
const loadProblems = async () => {
  loading.value = true
  try {
    const data = await getProblemList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    // 后端返回的是list字段，不是records
    problems.value = data.list || data.records || []
    total.value = data.total
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索题目
const handleSearch = async () => {
  if (!searchTitle.value.trim()) {
    loadProblems()
    return
  }

  loading.value = true
  try {
    const data = await searchProblems({
      title: searchTitle.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    problems.value = data
    total.value = data.length
  } catch (error: any) {
    ElMessage.error(error.message || '搜索失败')
  } finally {
    loading.value = false
  }
}

// 难度筛选
const handleFilter = async () => {
  if (!selectedDifficulty.value) {
    loadProblems()
    return
  }

  loading.value = true
  try {
    const data = await getProblemsByDifficulty(selectedDifficulty.value, {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })

    problems.value = data
    total.value = data.length
  } catch (error: any) {
    ElMessage.error(error.message || '筛选失败')
  } finally {
    loading.value = false
  }
}

// 跳转到题目详情
const goToProblem = (id: number) => {
  router.push(`/problem/${id}`)
}

onMounted(() => {
  loadProblems()
})
</script>

<style scoped lang="scss">
.problem-list {
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

  .filters {
    display: flex;
    gap: 12px;
  }
}
</style>
