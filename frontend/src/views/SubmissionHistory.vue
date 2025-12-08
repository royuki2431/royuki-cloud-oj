<template>
  <div class="submission-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">提交历史</span>
          <el-button :icon="Refresh" @click="loadSubmissions">刷新</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="submissions"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="提交ID" width="100" />
        
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="JudgeStatusType[row.status]">
              {{ JudgeStatusText[row.status] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="语言" width="100">
          <template #default="{ row }">
            {{ LanguageText[row.language] }}
          </template>
        </el-table-column>

        <el-table-column prop="score" label="得分" width="80">
          <template #default="{ row }">
            <span class="score">{{ row.score }}</span>
          </template>
        </el-table-column>

        <el-table-column label="运行时间" width="120">
          <template #default="{ row }">
            {{ row.timeUsed ? `${row.timeUsed}ms` : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="内存" width="120">
          <template #default="{ row }">
            {{ row.memoryUsed ? `${(row.memoryUsed / 1024).toFixed(2)}MB` : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="passRate" label="通过率" width="100">
          <template #default="{ row }">
            {{ row.passRate }}%
          </template>
        </el-table-column>

        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewCode(row)">
              查看代码
            </el-button>
            <el-button link type="warning" size="small" @click="handleRejudge(row.id)">
              重新评测
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
        @current-change="loadSubmissions"
        @size-change="loadSubmissions"
      />
    </el-card>

    <!-- 代码查看对话框 -->
    <el-dialog
      v-model="codeDialogVisible"
      :title="`提交 #${selectedSubmission?.id} - ${LanguageText[selectedSubmission?.language || Language.JAVA]}`"
      width="80%"
    >
      <MonacoEditor
        v-if="selectedSubmission"
        :model-value="selectedSubmission.code"
        :language="LanguageMonaco[selectedSubmission.language]"
        :readonly="true"
        height="600px"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { getUserSubmissions, rejudge } from '@/api/judge'
import { useUserStore } from '@/stores/user'
import {
  Language,
  LanguageText,
  LanguageMonaco,
  JudgeStatusText,
  JudgeStatusType,
  type Submission,
} from '@/types'

const userStore = useUserStore()

// 分页参数
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 数据
const loading = ref(false)
const submissions = ref<Submission[]>([])

// 代码查看
const codeDialogVisible = ref(false)
const selectedSubmission = ref<Submission | null>(null)

// 加载提交记录
const loadSubmissions = async () => {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true
  try {
    const data = await getUserSubmissions({
      userId: userStore.userInfo.id,
      page: currentPage.value,
      size: pageSize.value,
    })

    submissions.value = data
    total.value = data.length
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 查看代码
const viewCode = (submission: Submission) => {
  selectedSubmission.value = submission
  codeDialogVisible.value = true
}

// 重新评测
const handleRejudge = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要重新评测这个提交吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await rejudge(id)
    ElMessage.success('已提交重新评测请求')
    loadSubmissions()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '重新评测失败')
    }
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

onMounted(() => {
  loadSubmissions()
})
</script>

<style scoped lang="scss">
.submission-history {
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

.score {
  font-weight: bold;
  color: #409eff;
}
</style>
