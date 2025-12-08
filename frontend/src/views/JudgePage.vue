<template>
  <div class="judge-page">
    <el-card class="editor-card">
      <template #header>
        <div class="card-header">
          <span class="title">代码编辑器</span>
          <div class="controls">
            <el-select v-model="selectedLanguage" placeholder="选择语言" style="width: 120px">
              <el-option
                v-for="(text, lang) in LanguageText"
                :key="lang"
                :label="text"
                :value="lang"
              />
            </el-select>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!code.trim()"
              @click="handleSubmit"
            >
              提交代码
            </el-button>
          </div>
        </div>
      </template>

      <MonacoEditor
        v-model="code"
        :language="LanguageMonaco[selectedLanguage]"
        height="600px"
      />
    </el-card>

    <!-- 评测结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="评测结果"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="judgeResult" class="judge-result">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">
            <el-tag :type="JudgeStatusType[judgeResult.status]">
              {{ JudgeStatusText[judgeResult.status] }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="得分">
            <span class="score">{{ judgeResult.score }}分</span>
          </el-descriptions-item>
          <el-descriptions-item label="运行时间">
            {{ judgeResult.timeUsed ? `${judgeResult.timeUsed}ms` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="内存使用">
            {{ judgeResult.memoryUsed ? `${(judgeResult.memoryUsed / 1024).toFixed(2)}MB` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="通过率" :span="2">
            <el-progress
              :percentage="parseFloat(judgeResult.passRate)"
              :color="judgeResult.score === 100 ? '#67c23a' : '#e6a23c'"
            />
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="judgeResult.errorMessage"
          :title="judgeResult.errorMessage"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 16px"
        />
      </div>

      <div v-else class="judging">
        <el-icon class="is-loading" :size="50">
          <Loading />
        </el-icon>
        <p>评测中，请稍候...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import MonacoEditor from '@/components/MonacoEditor.vue'
import { submitCode, getJudgeResult } from '@/api/judge'
import {
  Language,
  LanguageText,
  LanguageMonaco,
  JudgeStatusText,
  JudgeStatusType,
  JudgeStatus,
  type JudgeResult,
} from '@/types'

// 代码和语言选择
const code = ref('')
const selectedLanguage = ref<Language>(Language.JAVA)

// 提交状态
const submitting = ref(false)
const resultDialogVisible = ref(false)
const judgeResult = ref<JudgeResult | null>(null)

// 默认代码模板
const defaultCode: Record<Language, string> = {
  [Language.JAVA]: `public class Solution {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}`,
  [Language.C]: `#include <stdio.h>

int main() {
    printf("Hello World!\\n");
    return 0;
}`,
  [Language.CPP]: `#include <iostream>
using namespace std;

int main() {
    cout << "Hello World!" << endl;
    return 0;
}`,
  [Language.PYTHON]: `print("Hello World!")`,
}

// 初始化代码
code.value = defaultCode[selectedLanguage.value]

// 监听语言切换，更新默认代码
watch(selectedLanguage, (newLang) => {
  if (!code.value.trim()) {
    code.value = defaultCode[newLang]
  }
})

// 提交代码
const handleSubmit = async () => {
  submitting.value = true
  resultDialogVisible.value = true
  judgeResult.value = null

  try {
    // 提交代码
    const submissionId = await submitCode({
      problemId: 1, // 这里暂时写死为1，实际应该从路由参数获取
      userId: 3, // 这里暂时写死为3，实际应该从用户登录信息获取
      language: selectedLanguage.value,
      code: code.value,
    })

    ElMessage.success('代码已提交，开始评测...')

    // 轮询查询评测结果
    await pollJudgeResult(submissionId)
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
    resultDialogVisible.value = false
  } finally {
    submitting.value = false
  }
}

// 轮询评测结果
const pollJudgeResult = async (submissionId: number) => {
  const maxAttempts = 30 // 最多轮询30次 (30秒)
  let attempts = 0

  const poll = async (): Promise<void> => {
    try {
      const result = await getJudgeResult(submissionId)

      // 如果评测完成，显示结果
      if (result.status !== JudgeStatus.PENDING && result.status !== JudgeStatus.JUDGING) {
        judgeResult.value = result
        return
      }

      // 如果还在评测中，继续轮询
      attempts++
      if (attempts < maxAttempts) {
        setTimeout(poll, 1000) // 1秒后再次查询
      } else {
        throw new Error('评测超时，请稍后查看提交历史')
      }
    } catch (error: any) {
      ElMessage.error(error.message || '查询评测结果失败')
      resultDialogVisible.value = false
    }
  }

  await poll()
}
</script>

<style scoped lang="scss">
.judge-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.editor-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 18px;
      font-weight: bold;
    }

    .controls {
      display: flex;
      gap: 12px;
    }
  }
}

.judge-result {
  .score {
    font-size: 24px;
    font-weight: bold;
    color: #409eff;
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
