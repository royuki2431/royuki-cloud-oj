<template>
  <div class="problem-detail">
    <!-- 作业模式提示 -->
    <el-alert
      v-if="isHomeworkMode"
      title="作业模式"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    >
      <template #default>
        您正在完成作业题目，提交后将自动记录到作业成绩中。
        <el-button type="primary" link @click="$router.push(`/my-homework?homeworkId=${homeworkId}`)">
          返回作业
        </el-button>
      </template>
    </el-alert>
    
    <el-row :gutter="20">
      <!-- 左侧：题目描述 -->
      <el-col :span="10">
        <el-card v-loading="loading">
          <template #header>
            <div class="problem-header">
              <h2>{{ problem?.id }}. {{ problem?.title }}</h2>
              <el-tag :type="DifficultyColor[problem?.difficulty || 'EASY']">
                {{ DifficultyText[problem?.difficulty || 'EASY'] }}
              </el-tag>
            </div>
          </template>

          <div v-if="problem" class="problem-content">
            <el-descriptions :column="2" border style="margin-bottom: 20px">
              <el-descriptions-item label="时间限制">
                {{ problem.timeLimit }}ms
              </el-descriptions-item>
              <el-descriptions-item label="内存限制">
                {{ problem.memoryLimit }}MB
              </el-descriptions-item>
              <el-descriptions-item label="通过率" :span="2">
                {{
                  problem.submitCount > 0
                    ? ((problem.acceptCount / problem.submitCount) * 100).toFixed(1) + '%'
                    : '0%'
                }}
                ({{ problem.acceptCount }}/{{ problem.submitCount }})
              </el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">题目描述</el-divider>
            <div class="section-content" v-html="problem.description"></div>

            <el-divider v-if="problem.inputFormat" content-position="left">输入格式</el-divider>
            <div v-if="problem.inputFormat" class="section-content">{{ problem.inputFormat }}</div>

            <el-divider v-if="problem.outputFormat" content-position="left">
              输出格式
            </el-divider>
            <div v-if="problem.outputFormat" class="section-content">
              {{ problem.outputFormat }}
            </div>

            <!-- 测试样例 -->
            <el-divider content-position="left">测试样例</el-divider>
            <div v-if="sampleTestCases.length > 0">
              <div v-for="(sample, index) in sampleTestCases" :key="sample.id" class="sample-case">
                <h4>样例 {{ index + 1 }}</h4>
                <div class="sample-item">
                  <div class="sample-label">输入：</div>
                  <pre class="sample-code">{{ sample.input }}</pre>
                </div>
                <div class="sample-item">
                  <div class="sample-label">输出：</div>
                  <pre class="sample-code">{{ sample.output }}</pre>
                </div>
              </div>
            </div>
            <!-- 如果没有样例测试用例，显示旧的样例数据作为fallback -->
            <div v-else-if="problem.sampleInput || problem.sampleOutput">
              <div class="sample-case">
                <h4>样例</h4>
                <div v-if="problem.sampleInput" class="sample-item">
                  <div class="sample-label">输入：</div>
                  <pre class="sample-code">{{ problem.sampleInput }}</pre>
                </div>
                <div v-if="problem.sampleOutput" class="sample-item">
                  <div class="sample-label">输出：</div>
                  <pre class="sample-code">{{ problem.sampleOutput }}</pre>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无测试样例" />

            <el-divider v-if="problem.hint" content-position="left">提示</el-divider>
            <div v-if="problem.hint" class="section-content">{{ problem.hint }}</div>

            <el-divider content-position="left">标签</el-divider>
            <el-tag v-for="tag in parseTags(problem.tags)" :key="tag" style="margin-right: 8px">
              {{ tag }}
            </el-tag>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：代码编辑器和提交历史 -->
      <el-col :span="14">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="代码编辑" name="editor">
              <template #label>
                <span><el-icon><Edit /></el-icon> 代码编辑</span>
              </template>
              <div class="editor-container">
                <!-- 编辑器控制栏 -->
                <div class="editor-controls">
                  <div class="controls-left">
                    <span class="auto-save-hint">
                      <el-icon><Clock /></el-icon>
                      代码自动保存到本地
                    </span>
                  </div>
                  <div class="controls-right">
                    <el-select
                      v-model="selectedLanguage"
                      placeholder="选择语言"
                      style="width: 120px"
                    >
                      <el-option
                        v-for="(text, lang) in LanguageText"
                        :key="lang"
                        :label="text"
                        :value="lang"
                      />
                    </el-select>
                    <el-button @click="resetCode" :disabled="submitting">
                      重置代码
                    </el-button>
                    <el-button
                      type="primary"
                      :loading="submitting"
                      :disabled="!code.trim()"
                      @click="handleSubmit"
                    >
                      <el-icon><Upload /></el-icon>
                      提交代码
                    </el-button>
                  </div>
                </div>

                <MonacoEditor
                  v-model="code"
                  :language="LanguageMonaco[selectedLanguage]"
                  height="calc(100vh - 340px)"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="提交历史" name="history">
              <template #label>
                <span><el-icon><Document /></el-icon> 提交历史</span>
              </template>
              <SubmissionHistory
                v-if="activeTab === 'history' && problem"
                :problem-id="problem.id"
                ref="submissionHistoryRef"
              />
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <!-- 评测结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="评测结果"
      width="800px"
      :close-on-click-modal="false"
    >
      <div v-if="judgeResult" class="judge-result">
        <!-- 总体结果 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">
            <el-tag :type="JudgeStatusType[judgeResult.status]" size="large">
              {{ JudgeStatusText[judgeResult.status] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="得分">
            <span class="score" :class="{ 'full-score': judgeResult.score === 100 }">
              {{ judgeResult.score }}分
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="运行时间">
            {{ judgeResult.timeUsed ? `${judgeResult.timeUsed}ms` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="内存使用">
            {{
              judgeResult.memoryUsed ? `${(judgeResult.memoryUsed / 1024).toFixed(2)}MB` : '-'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="通过率" :span="2">
            <el-progress
              :percentage="parseFloat(judgeResult.passRate)"
              :color="judgeResult.score === 100 ? '#67c23a' : '#e6a23c'"
            />
          </el-descriptions-item>
        </el-descriptions>

        <!-- 错误信息 -->
        <el-alert
          v-if="judgeResult.errorMessage"
          :title="judgeResult.errorMessage"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 16px"
        />

        <!-- 测试用例详细结果 -->
        <div v-if="judgeResult.testCaseResults && judgeResult.testCaseResults.length > 0" class="testcase-results">
          <el-divider content-position="left">
            <el-icon><Document /></el-icon>
            测试用例详情
          </el-divider>
          
          <el-collapse v-model="expandedTestCases">
            <el-collapse-item 
              v-for="(tc, index) in judgeResult.testCaseResults" 
              :key="tc.testCaseId"
              :name="index"
            >
              <template #title>
                <div class="testcase-header">
                  <el-tag 
                    :type="JudgeStatusType[tc.status]" 
                    size="small"
                    effect="dark"
                  >
                    {{ JudgeStatusText[tc.status] }}
                  </el-tag>
                  <span class="testcase-title">测试用例 #{{ index + 1 }}</span>
                  <span class="testcase-stats">
                    <el-icon><Timer /></el-icon> {{ tc.timeUsed }}ms
                    <el-icon style="margin-left: 12px;"><Cpu /></el-icon> {{ (tc.memoryUsed / 1024).toFixed(2) }}MB
                  </span>
                </div>
              </template>
              
              <div class="testcase-detail">
                <!-- 输入 -->
                <div class="detail-section" v-if="tc.input">
                  <div class="detail-label">输入：</div>
                  <pre class="detail-content">{{ tc.input }}</pre>
                </div>
                
                <!-- 期望输出 -->
                <div class="detail-section" v-if="tc.expectedOutput">
                  <div class="detail-label">期望输出：</div>
                  <pre class="detail-content expected">{{ tc.expectedOutput }}</pre>
                </div>
                
                <!-- 实际输出 -->
                <div class="detail-section" v-if="tc.actualOutput">
                  <div class="detail-label">实际输出：</div>
                  <pre class="detail-content" :class="{ 'correct': tc.status === JudgeStatus.ACCEPTED, 'wrong': tc.status !== JudgeStatus.ACCEPTED }">{{ tc.actualOutput }}</pre>
                </div>
                
                <!-- 错误信息 -->
                <el-alert
                  v-if="tc.errorMessage"
                  :title="tc.errorMessage"
                  type="error"
                  :closable="false"
                  show-icon
                  style="margin-top: 8px"
                />
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <div v-else class="judging">
        <el-icon class="is-loading" :size="50">
          <Loading />
        </el-icon>
        <p>{{ judgeStatus || '评测中，请稍候...' }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Edit, Document, Upload, Timer, Cpu, Clock } from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import SubmissionHistory from '@/components/SubmissionHistory.vue'
import { getProblemById, getSampleTestCases } from '@/api/problem'
import { submitCode, getJudgeResult } from '@/api/judge'
import { useUserStore } from '@/stores/user'
import { judgeWebSocket, type JudgeStatusMessage, type JudgeResultMessage } from '@/utils/websocket'
import {
  Language,
  LanguageText,
  LanguageMonaco,
  DifficultyText,
  DifficultyColor,
  JudgeStatusText,
  JudgeStatusType,
  JudgeStatus,
  type Problem,
  type JudgeResult,
  type TestCase,
  type TestCaseResult,
} from '@/types'

const route = useRoute()
const userStore = useUserStore()

const problemId = Number(route.params.id)
const homeworkId = route.query.homeworkId ? Number(route.query.homeworkId) : undefined

// 是否从作业页面进入
const isHomeworkMode = !!homeworkId

// 题目数据
const loading = ref(false)
const problem = ref<Problem | null>(null)
const sampleTestCases = ref<TestCase[]>([])

// 代码编辑
const code = ref('')
const selectedLanguage = ref<Language>(Language.JAVA)

// 提交状态
const submitting = ref(false)
const resultDialogVisible = ref(false)
const judgeResult = ref<JudgeResult | null>(null)
const judgeStatus = ref<string>('')  // 评测状态文本
const currentSubmissionId = ref<number | null>(null)
const expandedTestCases = ref<number[]>([])  // 展开的测试用例

// 标签页
const activeTab = ref('editor')
const submissionHistoryRef = ref<InstanceType<typeof SubmissionHistory> | null>(null)

// 默认代码模板
const defaultCode: Record<Language, string> = {
  [Language.JAVA]: `public class Solution {
    public static void main(String[] args) {
        // Write your code here
    }
}`,
  [Language.C]: `#include <stdio.h>

int main() {
    // Write your code here
    return 0;
}`,
  [Language.CPP]: `#include <iostream>
using namespace std;

int main() {
    // Write your code here
    return 0;
}`,
  [Language.PYTHON]: `# Write your code here
`,
}

// 解析标签
const parseTags = (tags: string | string[] | undefined): string[] => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    return JSON.parse(tags)
  } catch (e) {
    return []
  }
}

// 加载题目详情
const loadProblem = async () => {
  loading.value = true
  try {
    problem.value = await getProblemById(problemId)
    loadSavedCode()
    
    // 加载样例测试用例
    try {
      sampleTestCases.value = await getSampleTestCases(problemId)
      console.log('样例测试用例：', sampleTestCases.value)
    } catch (error) {
      console.warn('加载样例测试用例失败，使用默认样例', error)
      sampleTestCases.value = []
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载题目失败')
  } finally {
    loading.value = false
  }
}

// 本地存储的key
const getStorageKey = () => `code_${problemId}_${selectedLanguage.value}`

// 从本地存储加载代码
const loadSavedCode = () => {
  const savedCode = localStorage.getItem(getStorageKey())
  if (savedCode) {
    code.value = savedCode
  } else {
    code.value = defaultCode[selectedLanguage.value]
  }
}

// 保存代码到本地存储
const saveCodeToLocal = () => {
  if (code.value.trim()) {
    localStorage.setItem(getStorageKey(), code.value)
  }
}

// 重置代码
const resetCode = () => {
  ElMessageBox.confirm('确定要重置代码吗？此操作将清空当前代码。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      code.value = defaultCode[selectedLanguage.value]
      localStorage.removeItem(getStorageKey())
      ElMessage.success('代码已重置')
    })
    .catch(() => {
      // 用户取消
    })
}

// 监听语言切换
watch(selectedLanguage, (newLang, oldLang) => {
  // 保存旧语言的代码
  if (oldLang) {
    const oldKey = `code_${problemId}_${oldLang}`
    localStorage.setItem(oldKey, code.value)
  }
  
  // 加载新语言的代码
  loadSavedCode()
})

// 监听代码变化，自动保存（防抖）
let saveTimer: NodeJS.Timeout | null = null
watch(code, () => {
  if (saveTimer) {
    clearTimeout(saveTimer)
  }
  saveTimer = setTimeout(() => {
    saveCodeToLocal()
  }, 500)
})

// 连接 WebSocket
const connectWebSocket = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return
  
  try {
    await judgeWebSocket.connect(userStore.userInfo.id)
  } catch (error) {
    console.warn('WebSocket 连接失败，将使用轮询方式', error)
  }
}

// 提交代码
const handleSubmit = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  submitting.value = true
  resultDialogVisible.value = true
  judgeResult.value = null
  judgeStatus.value = '提交中...'

  try {
    const submitRequest: any = {
      problemId: problemId,
      userId: userStore.userInfo!.id,
      language: selectedLanguage.value,
      code: code.value,
    }
    // 如果是作业模式，传递homeworkId
    if (homeworkId) {
      submitRequest.homeworkId = homeworkId
    }
    const submissionId = await submitCode(submitRequest)

    currentSubmissionId.value = submissionId
    ElMessage.success('代码已提交，开始评测...')
    judgeStatus.value = '等待评测...'

    // 优先使用 WebSocket 接收结果
    if (judgeWebSocket.isConnected()) {
      judgeWebSocket.onSubmissionResult(
        submissionId,
        // 状态回调
        (statusMsg: JudgeStatusMessage) => {
          judgeStatus.value = statusMsg.message || `状态: ${statusMsg.status}`
        },
        // 结果回调
        (resultMsg: JudgeResultMessage) => {
          judgeResult.value = {
            submissionId: resultMsg.submissionId,
            status: resultMsg.status as JudgeStatus,
            statusDesc: resultMsg.statusDesc,
            score: resultMsg.score,
            timeUsed: resultMsg.timeUsed,
            memoryUsed: resultMsg.memoryUsed,
            passRate: resultMsg.passRate,
            errorMessage: resultMsg.errorMessage || undefined,
            testCaseResults: resultMsg.testCaseResults?.map(tc => ({
              testCaseId: tc.testCaseId,
              status: tc.status as JudgeStatus,
              timeUsed: tc.timeUsed,
              memoryUsed: tc.memoryUsed,
              input: tc.input,
              expectedOutput: tc.expectedOutput,
              actualOutput: tc.actualOutput,
              errorMessage: tc.errorMessage || undefined,
            })),
          }
          // 默认展开第一个失败的测试用例
          if (resultMsg.testCaseResults) {
            const failedIndex = resultMsg.testCaseResults.findIndex(tc => tc.status !== 'ACCEPTED')
            expandedTestCases.value = failedIndex >= 0 ? [failedIndex] : []
          }
          submitting.value = false
          // 刷新提交历史
          submissionHistoryRef.value?.refresh()
        }
      )
      
      // 设置超时，如果 30 秒没收到结果，降级到轮询
      setTimeout(() => {
        if (!judgeResult.value && currentSubmissionId.value === submissionId) {
          console.log('WebSocket 超时，降级到轮询')
          judgeWebSocket.removeSubmissionListener(submissionId)
          pollJudgeResult(submissionId)
        }
      }, 30000)
    } else {
      // WebSocket 未连接，使用轮询
      await pollJudgeResult(submissionId)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
    resultDialogVisible.value = false
    submitting.value = false
  }
}

// 轮询评测结果（降级方案）
const pollJudgeResult = async (submissionId: number) => {
  const maxAttempts = 30
  let attempts = 0

  const poll = async (): Promise<void> => {
    try {
      const result = await getJudgeResult(submissionId)

      if (result.status !== JudgeStatus.PENDING && result.status !== JudgeStatus.JUDGING) {
        judgeResult.value = result
        submitting.value = false
        // 刷新提交历史
        submissionHistoryRef.value?.refresh()
        return
      }

      judgeStatus.value = `评测中... (${attempts + 1}/${maxAttempts})`
      attempts++
      if (attempts < maxAttempts) {
        setTimeout(poll, 1000)
      } else {
        throw new Error('评测超时，请稍后查看提交历史')
      }
    } catch (error: any) {
      ElMessage.error(error.message || '查询评测结果失败')
      resultDialogVisible.value = false
      submitting.value = false
    }
  }

  await poll()
}

onMounted(() => {
  loadProblem()
  connectWebSocket()
})

onUnmounted(() => {
  // 清理当前提交的监听
  if (currentSubmissionId.value) {
    judgeWebSocket.removeSubmissionListener(currentSubmissionId.value)
  }
})
</script>

<style scoped lang="scss">
.problem-detail {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.problem-header {
  display: flex;
  align-items: center;
  gap: 16px;

  h2 {
    margin: 0;
    flex: 1;
  }
}

.problem-content {
  .section-content {
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    line-height: 1.8;
  }

  .sample-case {
    margin-bottom: 20px;
    
    h4 {
      margin: 0 0 12px 0;
      color: #409eff;
      font-size: 14px;
    }
    
    .sample-item {
      margin-bottom: 12px;
      
      .sample-label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }
    }
  }

  .sample-code {
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    overflow-x: auto;
    margin: 0;
    font-family: 'Courier New', Courier, monospace;
    font-size: 13px;
  }
}

.editor-container {
  .editor-controls {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 10px 12px;
    background: #f5f7fa;
    border-radius: 4px;

    .controls-left {
      display: flex;
      align-items: center;
    }

    .controls-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .auto-save-hint {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: #909399;
      
      .el-icon {
        font-size: 14px;
      }
    }
  }
}

.judge-result {
  .score {
    font-size: 24px;
    font-weight: bold;
    color: #409eff;
    
    &.full-score {
      color: #67c23a;
    }
  }
  
  .testcase-results {
    margin-top: 16px;
    
    .testcase-header {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;
      
      .testcase-title {
        font-weight: 500;
        color: #303133;
      }
      
      .testcase-stats {
        margin-left: auto;
        font-size: 12px;
        color: #909399;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
    
    .testcase-detail {
      padding: 12px;
      background: #fafafa;
      border-radius: 4px;
      
      .detail-section {
        margin-bottom: 12px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .detail-label {
          font-size: 12px;
          color: #909399;
          margin-bottom: 4px;
          font-weight: 500;
        }
        
        .detail-content {
          margin: 0;
          padding: 10px 12px;
          background: #f5f7fa;
          border-radius: 4px;
          font-family: 'Courier New', Courier, monospace;
          font-size: 13px;
          line-height: 1.5;
          white-space: pre-wrap;
          word-break: break-all;
          border-left: 3px solid #dcdfe6;
          
          &.expected {
            border-left-color: #409eff;
            background: #ecf5ff;
          }
          
          &.correct {
            border-left-color: #67c23a;
            background: #f0f9eb;
          }
          
          &.wrong {
            border-left-color: #f56c6c;
            background: #fef0f0;
          }
        }
      }
    }
  }
}

.judging {
  text-align: center;
  padding: 40px 0;

  p {
    margin-top: 16px;
    font-size: 16px;
    color: #606266;
  }
}
</style>
