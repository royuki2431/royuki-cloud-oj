<template>
  <div class="submission-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">提交历史</span>
          <el-button :icon="Refresh" @click="loadSubmissions">刷新</el-button>
        </div>
      </template>

      <!-- 版本对比操作栏 -->
      <div class="compare-toolbar" v-if="selectedRows.length > 0">
        <span class="selected-count">已选择 {{ selectedRows.length }} 个提交</span>
        <el-button 
          type="primary" 
          :disabled="selectedRows.length !== 2"
          @click="compareVersions"
        >
          对比选中的两个版本
        </el-button>
        <el-button @click="clearSelection">清除选择</el-button>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="submissions"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="提交ID" width="100" />
        
        <el-table-column label="题目" min-width="180">
          <template #default="{ row }">
            <router-link 
              v-if="row.problemId" 
              :to="`/problem/${row.problemId}`" 
              class="problem-link"
            >
              {{ row.problemTitle || `题目${row.problemId}` }}
            </router-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
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
              查看
            </el-button>
            <el-button link type="success" size="small" @click="compareWithPrevious(row)">
              对比
            </el-button>
            <el-button link type="warning" size="small" @click="handleRejudge(row.id)">
              重测
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

    <!-- 版本对比对话框 -->
    <el-dialog
      v-model="compareDialogVisible"
      title="代码版本对比"
      width="95%"
      top="2vh"
      destroy-on-close
    >
      <div class="compare-header">
        <div class="version-info left">
          <el-tag type="danger">旧版本</el-tag>
          <span>提交 #{{ compareLeft?.id }}</span>
          <span class="time">{{ formatTime(compareLeft?.createTime || '') }}</span>
          <el-tag :type="JudgeStatusType[compareLeft?.status]" size="small">
            {{ JudgeStatusText[compareLeft?.status] }}
          </el-tag>
        </div>
        <div class="version-info right">
          <el-tag type="success">新版本</el-tag>
          <span>提交 #{{ compareRight?.id }}</span>
          <span class="time">{{ formatTime(compareRight?.createTime || '') }}</span>
          <el-tag :type="JudgeStatusType[compareRight?.status]" size="small">
            {{ JudgeStatusText[compareRight?.status] }}
          </el-tag>
        </div>
      </div>
      <div class="diff-container">
        <MonacoDiffEditor
          v-if="compareLeft && compareRight"
          :original="compareLeft.code"
          :modified="compareRight.code"
          :language="LanguageMonaco[compareLeft.language]"
          height="70vh"
        />
      </div>
      <div class="compare-stats">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="得分变化">
            <span :class="getScoreChangeClass(compareLeft?.score, compareRight?.score)">
              {{ compareLeft?.score }} → {{ compareRight?.score }}
              ({{ getScoreChange(compareLeft?.score, compareRight?.score) }})
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="运行时间">
            {{ compareLeft?.timeUsed || '-' }}ms → {{ compareRight?.timeUsed || '-' }}ms
          </el-descriptions-item>
          <el-descriptions-item label="通过率">
            {{ compareLeft?.passRate }}% → {{ compareRight?.passRate }}%
          </el-descriptions-item>
          <el-descriptions-item label="代码行数">
            {{ getLineCount(compareLeft?.code) }} → {{ getLineCount(compareRight?.code) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import MonacoDiffEditor from '@/components/MonacoDiffEditor.vue'
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

// 版本对比
const tableRef = ref()
const selectedRows = ref<Submission[]>([])
const compareDialogVisible = ref(false)
const compareLeft = ref<Submission | null>(null)
const compareRight = ref<Submission | null>(null)

// 加载提交记录
const loadSubmissions = async () => {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录')
    return
  }

  loading.value = true
  try {
    const res = await getUserSubmissions({
      userId: userStore.userInfo.id,
      page: currentPage.value,
      size: pageSize.value,
    })

    submissions.value = res.data || []
    total.value = res.total || 0
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

// 处理选择变化
const handleSelectionChange = (rows: Submission[]) => {
  selectedRows.value = rows
}

// 清除选择
const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

// 对比选中的两个版本
const compareVersions = () => {
  if (selectedRows.value.length !== 2) {
    ElMessage.warning('请选择两个提交进行对比')
    return
  }
  
  // 按时间排序，旧的在左边，新的在右边
  const sorted = [...selectedRows.value].sort((a, b) => 
    new Date(a.createTime).getTime() - new Date(b.createTime).getTime()
  )
  
  compareLeft.value = sorted[0]
  compareRight.value = sorted[1]
  compareDialogVisible.value = true
}

// 与上一个版本对比
const compareWithPrevious = (submission: Submission) => {
  // 找到同一题目的上一个提交
  const sameProblems = submissions.value.filter(s => s.problemId === submission.problemId)
  const currentIndex = sameProblems.findIndex(s => s.id === submission.id)
  
  if (currentIndex === -1 || currentIndex === sameProblems.length - 1) {
    ElMessage.warning('没有找到该题目的上一个提交版本')
    return
  }
  
  // 提交记录通常是按时间倒序的，所以下一个索引是更早的提交
  compareLeft.value = sameProblems[currentIndex + 1]
  compareRight.value = submission
  compareDialogVisible.value = true
}

// 获取得分变化
const getScoreChange = (oldScore?: number, newScore?: number) => {
  const diff = (newScore || 0) - (oldScore || 0)
  if (diff > 0) return `+${diff}`
  return diff.toString()
}

// 获取得分变化样式
const getScoreChangeClass = (oldScore?: number, newScore?: number) => {
  const diff = (newScore || 0) - (oldScore || 0)
  if (diff > 0) return 'score-up'
  if (diff < 0) return 'score-down'
  return ''
}

// 获取代码行数
const getLineCount = (code?: string) => {
  if (!code) return 0
  return code.split('\n').length
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

.problem-link {
  color: #409eff;
  text-decoration: none;
  
  &:hover {
    text-decoration: underline;
  }
}

// 版本对比工具栏
.compare-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f9eb;
  border-radius: 4px;
  margin-bottom: 16px;
  
  .selected-count {
    color: #67c23a;
    font-weight: 500;
  }
}

// 对比对话框样式
.compare-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  
  .version-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 20px;
    border-radius: 8px;
    flex: 1;
    
    &.left {
      background: #fef0f0;
      margin-right: 8px;
    }
    
    &.right {
      background: #f0f9eb;
      margin-left: 8px;
    }
    
    .time {
      color: #909399;
      font-size: 13px;
    }
  }
}

.diff-container {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.compare-stats {
  margin-top: 16px;
  
  :deep(.score-up) {
    color: #67c23a;
    font-weight: bold;
  }
  
  :deep(.score-down) {
    color: #f56c6c;
    font-weight: bold;
  }
}
</style>
