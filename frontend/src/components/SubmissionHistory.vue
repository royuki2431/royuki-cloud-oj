<template>
  <div class="submission-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>提交历史</h3>
          <el-button @click="refresh" :loading="loading" circle>
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </template>

      <el-table
        :data="submissions"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="提交ID" width="100" />
        
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="语言" width="100">
          <template #default="{ row }">
            {{ getLanguageText(row.language) }}
          </template>
        </el-table-column>

        <el-table-column prop="score" label="得分" width="80" />

        <el-table-column label="运行时间" width="110">
          <template #default="{ row }">
            {{ row.timeUsed ? `${row.timeUsed}ms` : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="内存使用" width="110">
          <template #default="{ row }">
            {{ row.memoryUsed ? `${(row.memoryUsed / 1024).toFixed(2)}MB` : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; justify-content: center"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 提交详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="提交详情"
      width="800px"
    >
      <div v-if="currentSubmission" class="submission-detail">
        <el-descriptions :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="提交ID">
            {{ currentSubmission.id }}
          </el-descriptions-item>
          <el-descriptions-item label="题目ID">
            {{ currentSubmission.problemId }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentSubmission.status)">
              {{ getStatusText(currentSubmission.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="得分">
            <span class="score">{{ currentSubmission.score }}分</span>
          </el-descriptions-item>
          <el-descriptions-item label="运行时间">
            {{ currentSubmission.timeUsed ? `${currentSubmission.timeUsed}ms` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="内存使用">
            {{ currentSubmission.memoryUsed ? `${(currentSubmission.memoryUsed / 1024).toFixed(2)}MB` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="语言">
            {{ getLanguageText(currentSubmission.language) }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatTime(currentSubmission.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="currentSubmission.errorMessage"
          :title="currentSubmission.errorMessage"
          type="error"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-divider content-position="left">提交代码</el-divider>
        <pre class="code-block">{{ currentSubmission.code }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getUserSubmissions, getProblemSubmissions } from '@/api/judge'
import {
  JudgeStatus,
  JudgeStatusText,
  JudgeStatusType,
  Language,
  LanguageText,
  type Submission,
} from '@/types'

interface Props {
  userId?: number
  problemId?: number
}

const props = defineProps<Props>()

const loading = ref(false)
const submissions = ref<Submission[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const detailDialogVisible = ref(false)
const currentSubmission = ref<Submission | null>(null)

// 加载提交历史
const loadSubmissions = async () => {
  loading.value = true
  try {
    if (props.userId) {
      // 加载用户的提交历史
      submissions.value = await getUserSubmissions({
        userId: props.userId,
        page: currentPage.value,
        size: pageSize.value,
      })
    } else if (props.problemId) {
      // 加载题目的提交历史
      submissions.value = await getProblemSubmissions(props.problemId, {
        page: currentPage.value,
        size: pageSize.value,
      })
    }
    total.value = submissions.value.length // 这里应该从API获取总数
  } catch (error: any) {
    ElMessage.error(error.message || '加载提交历史失败')
  } finally {
    loading.value = false
  }
}

// 刷新
const refresh = () => {
  loadSubmissions()
}

// 查看详情
const viewDetail = (submission: Submission) => {
  currentSubmission.value = submission
  detailDialogVisible.value = true
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  loadSubmissions()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadSubmissions()
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

// 获取状态类型
const getStatusType = (status: JudgeStatus) => {
  return JudgeStatusType[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: JudgeStatus) => {
  return JudgeStatusText[status] || '未知'
}

// 获取语言文本
const getLanguageText = (language: Language) => {
  return LanguageText[language] || language
}

onMounted(() => {
  loadSubmissions()
})

defineExpose({
  refresh,
})
</script>

<style scoped lang="scss">
.submission-history {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h3 {
      margin: 0;
    }
  }

  .score {
    font-size: 18px;
    font-weight: bold;
    color: #409eff;
  }
}

.submission-detail {
  .code-block {
    padding: 16px;
    background: #f5f7fa;
    border-radius: 4px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.6;
    max-height: 400px;
  }
}
</style>
