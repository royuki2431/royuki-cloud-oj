<template>
  <div class="problem-manage-page">
    <div class="page-header">
      <h2>题库管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建题目
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="题目标题" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="filterForm.difficulty" placeholder="全部" clearable style="width: 120px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filterForm.category" placeholder="全部" clearable style="width: 120px">
            <el-option label="算法" value="算法" />
            <el-option label="数据结构" value="数据结构" />
            <el-option label="数学" value="数学" />
            <el-option label="字符串" value="字符串" />
            <el-option label="动态规划" value="动态规划" />
            <el-option label="图论" value="图论" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadProblems">搜索</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 题目列表 -->
    <el-card class="table-card">
      <el-table :data="problems" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="题目标题" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="handleEdit(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" align="center" show-overflow-tooltip />
        <el-table-column label="标签" width="180">
          <template #default="{ row }">
            <el-tag 
              v-for="tag in parseTags(row.tags)?.slice(0, 3)" 
              :key="tag" 
              size="small" 
              style="margin-right: 4px; margin-bottom: 2px"
            >
              {{ tag }}
            </el-tag>
            <span v-if="parseTags(row.tags)?.length > 3" style="color: #909399; font-size: 12px">
              +{{ parseTags(row.tags).length - 3 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="80" align="center">
          <template #default="{ row }">
            {{ getAcceptRate(row) }}%
          </template>
        </el-table-column>
        <el-table-column prop="submitCount" label="提交数" width="80" align="center" />
        <el-table-column label="测试用例" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.testCaseCount > 0 ? 'success' : 'danger'" size="small">
              {{ row.testCaseCount || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" size="small" @click="handleTestCases(row)">测试用例</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadProblems"
        @current-change="loadProblems"
        class="pagination"
      />
    </el-card>

    <!-- 题目编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑题目' : '新建题目'" 
      width="900px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="problemForm" :rules="rules" ref="formRef" label-width="100px">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form-item label="题目标题" prop="title">
              <el-input v-model="problemForm.title" placeholder="请输入题目标题" />
            </el-form-item>
            
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="problemForm.difficulty" style="width: 100%">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
            </el-form-item>

            <el-form-item label="分类">
              <el-select v-model="problemForm.category" style="width: 100%" allow-create filterable placeholder="选择或输入分类">
                <el-option label="算法" value="算法" />
                <el-option label="数据结构" value="数据结构" />
                <el-option label="数学" value="数学" />
                <el-option label="字符串" value="字符串" />
                <el-option label="动态规划" value="动态规划" />
                <el-option label="图论" value="图论" />
              </el-select>
            </el-form-item>

            <el-form-item label="标签">
              <el-select 
                v-model="tagList" 
                multiple 
                filterable 
                allow-create 
                placeholder="选择或输入标签"
                style="width: 100%"
              >
                <el-option label="数组" value="数组" />
                <el-option label="链表" value="链表" />
                <el-option label="栈" value="栈" />
                <el-option label="队列" value="队列" />
                <el-option label="树" value="树" />
                <el-option label="图" value="图" />
                <el-option label="哈希表" value="哈希表" />
                <el-option label="排序" value="排序" />
                <el-option label="搜索" value="搜索" />
                <el-option label="贪心" value="贪心" />
                <el-option label="分治" value="分治" />
                <el-option label="回溯" value="回溯" />
              </el-select>
            </el-form-item>

            <el-form-item label="来源">
              <el-input v-model="problemForm.source" placeholder="请输入题目来源，如：LeetCode、牛客网、原创等" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="题目描述" name="description">
            <el-form-item label="题目描述" prop="description">
              <el-input 
                v-model="problemForm.description" 
                type="textarea" 
                :rows="8" 
                placeholder="请输入题目描述，支持Markdown格式"
              />
            </el-form-item>
            
            <el-form-item label="输入格式">
              <el-input 
                v-model="problemForm.inputFormat" 
                type="textarea" 
                :rows="3" 
                placeholder="描述输入数据的格式"
              />
            </el-form-item>
            
            <el-form-item label="输出格式">
              <el-input 
                v-model="problemForm.outputFormat" 
                type="textarea" 
                :rows="3" 
                placeholder="描述输出数据的格式"
              />
            </el-form-item>

            <el-form-item label="样例输入">
              <el-input 
                v-model="problemForm.sampleInput" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入样例输入"
              />
            </el-form-item>

            <el-form-item label="样例输出">
              <el-input 
                v-model="problemForm.sampleOutput" 
                type="textarea" 
                :rows="3" 
                placeholder="请输入样例输出"
              />
            </el-form-item>
            
            <el-form-item label="提示">
              <el-input 
                v-model="problemForm.hint" 
                type="textarea" 
                :rows="2" 
                placeholder="可选的解题提示"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="限制条件" name="limits">
            <el-form-item label="时间限制" prop="timeLimit">
              <el-input-number v-model="problemForm.timeLimit" :min="100" :max="10000" :step="100" />
              <span class="unit">毫秒</span>
            </el-form-item>

            <el-form-item label="内存限制" prop="memoryLimit">
              <el-input-number v-model="problemForm.memoryLimit" :min="64" :max="512" :step="64" />
              <span class="unit">MB</span>
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 测试用例对话框 -->
    <el-dialog 
      v-model="testCaseDialogVisible" 
      title="测试用例管理" 
      width="900px"
      destroy-on-close
    >
      <div class="testcase-header">
        <span>题目: {{ currentProblem?.title }}</span>
        <el-button type="primary" size="small" @click="addTestCase">
          <el-icon><Plus /></el-icon>
          添加用例
        </el-button>
      </div>
      
      <el-table :data="testCases" border>
        <el-table-column label="序号" width="60" align="center">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="输入" min-width="200">
          <template #default="{ row }">
            <el-input 
              v-model="row.input" 
              type="textarea" 
              :rows="3" 
              placeholder="测试输入"
            />
          </template>
        </el-table-column>
        <el-table-column label="期望输出" min-width="200">
          <template #default="{ row }">
            <el-input 
              v-model="row.expectedOutput" 
              type="textarea" 
              :rows="3" 
              placeholder="期望输出"
            />
          </template>
        </el-table-column>
        <el-table-column label="样例" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.isSample" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ $index }">
            <el-button type="danger" size="small" @click="removeTestCase($index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <el-button @click="testCaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTestCases" :loading="savingTestCases">
          保存测试用例
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface Problem {
  id?: number
  title: string
  description: string
  difficulty: string
  timeLimit: number
  memoryLimit: number
  inputFormat?: string
  outputFormat?: string
  sampleInput?: string
  sampleOutput?: string
  hint?: string
  category?: string
  tags?: string
  source?: string
  submitCount?: number
  acceptCount?: number
  testCaseCount?: number
  authorId?: number
}

// 获取当前用户ID
const getCurrentUserId = (): number | null => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      const user = JSON.parse(userInfo)
      return user.id || null
    } catch {
      return null
    }
  }
  return null
}

interface TestCase {
  id?: number
  problemId?: number
  input: string
  output?: string
  expectedOutput?: string
  isSample: number
  score?: number
  orderNum?: number
}

const loading = ref(false)
const problems = ref<Problem[]>([])
const dialogVisible = ref(false)
const testCaseDialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const savingTestCases = ref(false)
const formRef = ref<FormInstance>()
const currentProblem = ref<Problem | null>(null)
const testCases = ref<TestCase[]>([])

const filterForm = reactive({
  keyword: '',
  difficulty: '',
  category: ''
})

const activeTab = ref('basic')

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const problemForm = reactive({
  title: '',
  description: '',
  difficulty: 'EASY',
  timeLimit: 1000,
  memoryLimit: 256,
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  hint: '',
  category: '',
  tags: '',
  source: ''
})

// 标签列表（用于编辑）
const tagList = ref<string[]>([])

const rules: FormRules = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  timeLimit: [{ required: true, message: '请设置时间限制', trigger: 'blur' }],
  memoryLimit: [{ required: true, message: '请设置内存限制', trigger: 'blur' }]
}

// 加载题目列表（只加载当前教师创建的题目）
const loadProblems = async () => {
  loading.value = true
  try {
    const userId = getCurrentUserId()
    console.log('当前用户ID:', userId)
    
    // 使用管理员接口，传入authorId只查询自己的题目
    const params: any = {
      page: pagination.page,
      size: pagination.pageSize
    }
    
    // 只有userId有效时才添加authorId参数
    if (userId) {
      params.authorId = userId
    }
    
    if (filterForm.keyword) {
      params.keyword = filterForm.keyword
    }
    if (filterForm.difficulty) {
      params.difficulty = filterForm.difficulty
    }
    if (filterForm.category) {
      params.category = filterForm.category
    }
    
    console.log('请求参数:', params)
    const res = await request.get('/problem/admin/list', { params })
    problems.value = res.list || []
    pagination.total = res.total || 0
    
    // 加载每个题目的测试用例数量
    for (const problem of problems.value) {
      try {
        const countRes = await request.get(`/problem/${problem.id}/testcases/count`)
        problem.testCaseCount = countRes || 0
      } catch {
        problem.testCaseCount = 0
      }
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

// 重置筛选
const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.difficulty = ''
  filterForm.category = ''
  pagination.page = 1
  loadProblems()
}

// 新建题目
const handleCreate = () => {
  isEdit.value = false
  Object.assign(problemForm, {
    id: undefined,
    title: '',
    description: '',
    difficulty: 'EASY',
    timeLimit: 1000,
    memoryLimit: 256,
    inputFormat: '',
    outputFormat: '',
    sampleInput: '',
    sampleOutput: '',
    hint: '',
    category: '',
    tags: '',
    source: ''
  })
  tagList.value = []
  activeTab.value = 'basic'
  dialogVisible.value = true
}

// 编辑题目
const handleEdit = (row: Problem) => {
  isEdit.value = true
  Object.assign(problemForm, {
    ...row,
    category: row.category || '',
    sampleInput: row.sampleInput || '',
    sampleOutput: row.sampleOutput || '',
    source: row.source || ''
  })
  // 解析标签
  try {
    tagList.value = row.tags ? JSON.parse(row.tags) : []
  } catch {
    tagList.value = row.tags ? [row.tags] : []
  }
  activeTab.value = 'basic'
  dialogVisible.value = true
}

// 解析标签JSON
const parseTags = (tags: string | string[] | undefined): string[] => {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    return JSON.parse(tags)
  } catch {
    return []
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      const userId = getCurrentUserId()
      // 将标签列表转换为JSON字符串
      problemForm.tags = JSON.stringify(tagList.value)
      
      if (isEdit.value) {
        // 更新题目，传入authorId进行权限校验
        const url = userId 
          ? `/problem/admin/update/${(problemForm as any).id}?authorId=${userId}`
          : `/problem/admin/update/${(problemForm as any).id}`
        await request.put(url, problemForm)
        ElMessage.success('更新成功')
      } else {
        // 创建题目，设置authorId
        const createData = { ...problemForm, authorId: userId }
        await request.post('/problem/admin/create', createData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadProblems()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 删除题目
const handleDelete = async (row: Problem) => {
  try {
    await ElMessageBox.confirm(`确定要删除题目“${row.title}”吗？`, '确认删除', {
      type: 'warning'
    })
    
    const userId = getCurrentUserId()
    // 传入authorId进行权限校验
    const url = userId 
      ? `/problem/admin/delete/${row.id}?authorId=${userId}`
      : `/problem/admin/delete/${row.id}`
    await request.delete(url)
    ElMessage.success('删除成功')
    loadProblems()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 管理测试用例
const handleTestCases = async (row: Problem) => {
  currentProblem.value = row
  
  try {
    const res = await request.get(`/problem/${row.id}/testcases`)
    // 将后端的 output 字段映射到前端的 expectedOutput
    testCases.value = (res || []).map((tc: any) => ({
      ...tc,
      expectedOutput: tc.output || tc.expectedOutput || ''
    }))
  } catch {
    testCases.value = []
  }
  
  testCaseDialogVisible.value = true
}

// 添加测试用例
const addTestCase = () => {
  testCases.value.push({
    input: '',
    expectedOutput: '',
    isSample: 0,
    score: 10,
    orderNum: testCases.value.length + 1
  })
}

// 删除测试用例
const removeTestCase = (index: number) => {
  testCases.value.splice(index, 1)
}

// 保存测试用例
const saveTestCases = async () => {
  if (!currentProblem.value?.id) return
  
  // 验证测试用例
  for (let i = 0; i < testCases.value.length; i++) {
    const tc = testCases.value[i]
    if (!tc.expectedOutput?.trim()) {
      ElMessage.warning(`第${i + 1}个测试用例的期望输出不能为空`)
      return
    }
  }
  
  savingTestCases.value = true
  try {
    // 将前端的 expectedOutput 转换为后端的 output
    const dataToSave = testCases.value.map((tc, index) => ({
      id: tc.id,
      problemId: tc.problemId,
      input: tc.input,
      output: tc.expectedOutput,
      isSample: tc.isSample,
      score: tc.score || 10,
      orderNum: tc.orderNum || index + 1
    }))
    await request.post(`/problem/${currentProblem.value.id}/testcases`, dataToSave)
    ElMessage.success('保存成功')
    testCaseDialogVisible.value = false
    loadProblems()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    savingTestCases.value = false
  }
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

// 获取通过率
const getAcceptRate = (row: Problem) => {
  if (!row.submitCount || row.submitCount === 0) return 0
  return ((row.acceptCount || 0) / row.submitCount * 100).toFixed(1)
}

onMounted(() => {
  loadProblems()
})
</script>

<style scoped lang="scss">
.problem-manage-page {
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

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  .pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}

.unit {
  margin-left: 8px;
  color: #909399;
}

.testcase-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}
</style>
