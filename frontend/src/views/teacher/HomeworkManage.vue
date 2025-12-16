<template>
  <div class="homework-manage">
    <div class="page-header">
      <h2>作业管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        布置作业
      </el-button>
    </div>

    <!-- 筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="课程">
          <el-select v-model="filterCourseId" placeholder="全部课程" clearable @change="loadHomeworks" style="width: 200px">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable @change="loadHomeworks" style="width: 150px">
            <el-option label="进行中" value="active" />
            <el-option label="已结束" value="ended" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 作业列表 -->
    <el-card class="table-card" v-loading="loading">
      <el-table :data="homeworkList" stripe>
        <el-table-column prop="title" label="作业标题" min-width="180">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDetail(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="所属课程" width="150">
          <template #default="{ row }">
            {{ getCourseNameById(row.courseId) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column label="题目数" width="80" align="center">
          <template #default="{ row }">
            {{ row.problemCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            <div class="time-info">
              <div>开始: {{ formatTime(row.startTime) }}</div>
              <div :class="{ 'text-danger': isOverdue(row.endTime) }">
                截止: {{ formatTime(row.endTime) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="完成情况" width="120" align="center">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.completionRate || 0" 
              :stroke-width="8"
              :show-text="false"
            />
            <span class="completion-text">{{ row.completedCount || 0 }}/{{ row.studentCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewStatistics(row)">统计</el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="homeworkList.length === 0" description="暂无作业" />
    </el-card>

    <!-- 创建/编辑作业对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑作业' : '布置作业'" 
      width="900px"
      destroy-on-close
    >
      <el-form :model="homeworkForm" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="homeworkForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择课程" prop="courseId">
              <el-select v-model="homeworkForm.courseId" placeholder="请选择课程" @change="onCourseChange" style="width: 100%">
                <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="选择班级" prop="classId">
              <el-select v-model="homeworkForm.classId" placeholder="请选择班级" style="width: 100%">
                <el-option v-for="cls in filteredClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker 
                v-model="homeworkForm.startTime" 
                type="datetime" 
                placeholder="选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间" prop="endTime">
              <el-date-picker 
                v-model="homeworkForm.endTime" 
                type="datetime" 
                placeholder="选择截止时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="作业描述">
          <el-input v-model="homeworkForm.description" type="textarea" :rows="3" placeholder="作业要求说明" />
        </el-form-item>
        
        <el-form-item label="选择题目" prop="problems">
          <div class="problem-selector">
            <el-button type="primary" @click="showProblemSelector = true">
              <el-icon><Plus /></el-icon>
              添加题目
            </el-button>
            <span class="problem-count">已选 {{ homeworkForm.problems.length }} 题</span>
          </div>
          
          <el-table :data="homeworkForm.problems" border size="small" class="problem-table">
            <el-table-column label="序号" width="60" align="center">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column prop="title" label="题目" min-width="200" />
            <el-table-column prop="difficulty" label="难度" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="getDifficultyType(row.difficulty)" size="small">
                  {{ getDifficultyText(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分值" width="120" align="center">
              <template #default="{ row }">
                <el-input-number v-model="row.score" :min="1" :max="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button type="danger" size="small" @click="removeProblem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="total-score" v-if="homeworkForm.problems.length > 0">
            总分: <strong>{{ totalScore }}</strong> 分
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 题目选择对话框 -->
    <el-dialog v-model="showProblemSelector" title="选择题目" width="800px">
      <el-input v-model="problemSearch" placeholder="搜索题目" class="search-input">
        <template #append>
          <el-button @click="searchProblems">搜索</el-button>
        </template>
      </el-input>
      
      <el-table 
        :data="availableProblems" 
        v-loading="loadingProblems"
        @selection-change="onProblemSelectionChange"
        max-height="400"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="题目" min-width="200" />
        <el-table-column prop="difficulty" label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDifficultyType(row.difficulty)" size="small">
              {{ getDifficultyText(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <el-button @click="showProblemSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmProblemSelection">
          确认选择 ({{ selectedProblems.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 作业统计对话框 -->
    <el-dialog v-model="statsDialogVisible" title="作业统计" width="900px">
      <div class="stats-overview" v-if="currentHomework">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value">{{ homeworkStats.studentCount || 0 }}</div>
              <div class="stat-label">学生总数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value success">{{ homeworkStats.completedCount || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value warning">{{ homeworkStats.avgScore?.toFixed(1) || 0 }}</div>
              <div class="stat-label">平均分</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card">
              <div class="stat-value primary">{{ homeworkStats.passRate?.toFixed(1) || 0 }}%</div>
              <div class="stat-label">通过率</div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <el-tabs v-model="statsTab">
        <el-tab-pane label="学生成绩" name="scores">
          <el-table :data="studentScores" max-height="400">
            <el-table-column type="index" label="排名" width="60" align="center" />
            <el-table-column prop="studentName" label="学生" width="120">
              <template #default="{ row }">
                {{ row.studentName || row.username || '学生' + row.studentId }}
              </template>
            </el-table-column>
            <el-table-column label="得分" width="120" align="center">
              <template #default="{ row }">
                <span :class="getScoreClass(row.earnedScore, row.totalScore)">
                  {{ row.earnedScore || 0 }} / {{ row.totalScore || currentHomework?.totalScore }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="完成进度" width="120" align="center">
              <template #default="{ row }">
                {{ row.submittedCount || 0 }} / {{ row.problemCount || 0 }} 题
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isCompleted ? 'success' : 'warning'" size="small">
                  {{ row.isCompleted ? '已完成' : '未完成' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="studentScores.length === 0" description="暂无学生提交数据" />
        </el-tab-pane>
        
        <el-tab-pane label="题目分析" name="problems">
          <el-table :data="problemStats" max-height="400">
            <el-table-column prop="orderNum" label="序号" width="60" align="center" />
            <el-table-column prop="problemTitle" label="题目" min-width="200" />
            <el-table-column prop="score" label="分值" width="80" align="center" />
            <el-table-column label="提交情况" width="150" align="center">
              <template #default="{ row }">
                {{ row.submitCount || 0 }} / {{ row.studentCount || 0 }} 人
              </template>
            </el-table-column>
            <el-table-column label="通过率" width="150">
              <template #default="{ row }">
                <el-progress 
                  :percentage="row.studentCount > 0 ? Math.round((row.passCount || 0) / row.studentCount * 100) : 0" 
                  :stroke-width="10"
                />
              </template>
            </el-table-column>
            <el-table-column label="通过人数" width="100" align="center">
              <template #default="{ row }">
                <span class="pass-count">{{ row.passCount || 0 }}</span> / {{ row.studentCount || 0 }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="problemStats.length === 0" description="暂无题目数据" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getTeacherCourses, getCourseClasses, type Course, type CourseClass } from '@/api/course'

interface HomeworkProblem {
  problemId: number
  title: string
  difficulty: string
  score: number
}

interface Homework {
  id?: number
  title: string
  courseId: number | null
  classId: number | null
  teacherId: number
  description: string
  startTime: string
  endTime: string
  totalScore?: number
  problemCount?: number
  completedCount?: number
  studentCount?: number
  completionRate?: number
  problems: HomeworkProblem[]
}

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const showProblemSelector = ref(false)
const loadingProblems = ref(false)
const statsDialogVisible = ref(false)
const statsTab = ref('scores')
const formRef = ref<FormInstance>()

const homeworkList = ref<Homework[]>([])
const courses = ref<Course[]>([])
const classes = ref<CourseClass[]>([])
const availableProblems = ref<any[]>([])
const selectedProblems = ref<any[]>([])
const problemSearch = ref('')
const currentHomework = ref<Homework | null>(null)
const homeworkStats = ref<any>({})
const studentScores = ref<any[]>([])
const problemStats = ref<any[]>([])

const filterCourseId = ref<number | null>(null)
const filterStatus = ref('')

const homeworkForm = reactive<Homework>({
  title: '',
  courseId: null,
  classId: null,
  teacherId: 0,
  description: '',
  startTime: '',
  endTime: '',
  problems: []
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入作业标题', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
}

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 计算总分
const totalScore = computed(() => {
  return homeworkForm.problems.reduce((sum, p) => sum + (p.score || 0), 0)
})

// 筛选班级
const filteredClasses = computed(() => {
  if (!homeworkForm.courseId) return []
  return classes.value.filter(c => c.courseId === homeworkForm.courseId)
})

// 加载课程列表
const loadCourses = async () => {
  const userId = getUserId()
  if (!userId) return
  
  try {
    courses.value = await getTeacherCourses(userId)
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

// 加载班级列表
const loadClasses = async () => {
  try {
    const allClasses: CourseClass[] = []
    for (const course of courses.value) {
      const cls = await getCourseClasses(course.id)
      allClasses.push(...cls)
    }
    classes.value = allClasses
  } catch (error) {
    console.error('加载班级失败:', error)
  }
}

// 加载作业列表
const loadHomeworks = async () => {
  const userId = getUserId()
  if (!userId) return
  
  loading.value = true
  try {
    // 根据课程加载作业
    let allHomeworks: Homework[] = []
    
    if (filterCourseId.value) {
      const res = await request.get(`/course/homework/course/${filterCourseId.value}`)
      allHomeworks = res || []
    } else {
      // 加载所有课程的作业
      for (const course of courses.value) {
        const res = await request.get(`/course/homework/course/${course.id}`)
        allHomeworks.push(...(res || []))
      }
    }
    
    // 筛选状态
    if (filterStatus.value === 'active') {
      allHomeworks = allHomeworks.filter(h => !isOverdue(h.endTime))
    } else if (filterStatus.value === 'ended') {
      allHomeworks = allHomeworks.filter(h => isOverdue(h.endTime))
    }
    
    homeworkList.value = allHomeworks
  } catch (error) {
    console.error('加载作业失败:', error)
  } finally {
    loading.value = false
  }
}

// 课程变化时加载班级
const onCourseChange = async () => {
  homeworkForm.classId = null
}

// 创建作业
const handleCreate = () => {
  isEdit.value = false
  const userId = getUserId()
  Object.assign(homeworkForm, {
    id: undefined,
    title: '',
    courseId: null,
    classId: null,
    teacherId: userId,
    description: '',
    startTime: new Date().toISOString(),
    endTime: '',
    problems: []
  })
  dialogVisible.value = true
}

// 编辑作业
const handleEdit = async (row: Homework) => {
  isEdit.value = true
  
  try {
    const detail = await request.get(`/course/homework/detail/${row.id}`)
    Object.assign(homeworkForm, {
      ...detail.homework,
      problems: (detail.problems || []).map((p: any) => ({
        problemId: p.problemId,
        title: p.problemTitle || `题目#${p.problemId}`,
        difficulty: p.difficulty || 'MEDIUM',
        score: p.score
      }))
    })
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载作业详情失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (homeworkForm.problems.length === 0) {
      ElMessage.warning('请至少添加一道题目')
      return
    }
    
    submitting.value = true
    try {
      const data = {
        ...homeworkForm,
        problems: homeworkForm.problems.map(p => ({
          problemId: p.problemId,
          score: p.score
        }))
      }
      
      if (isEdit.value) {
        await request.put('/course/homework/update', data)
        ElMessage.success('更新成功')
      } else {
        await request.post('/course/homework/create', data)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadHomeworks()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

// 删除作业
const handleDelete = async (row: Homework) => {
  try {
    await ElMessageBox.confirm(`确定要删除作业"${row.title}"吗？`, '确认删除', { type: 'warning' })
    await request.delete(`/course/homework/delete/${row.id}`)
    ElMessage.success('删除成功')
    loadHomeworks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 查看详情
const viewDetail = (row: Homework) => {
  viewStatistics(row)
}

// 查看统计
const viewStatistics = async (row: Homework) => {
  currentHomework.value = row
  
  try {
    const stats = await request.get(`/course/homework/statistics/${row.id}`)
    homeworkStats.value = stats
    studentScores.value = stats.studentScores || []
    problemStats.value = stats.problemStats || []
  } catch (error) {
    console.error('加载统计失败:', error)
  }
  
  statsDialogVisible.value = true
}

// 搜索题目
const searchProblems = async () => {
  loadingProblems.value = true
  try {
    let res
    if (problemSearch.value) {
      res = await request.get('/problem/search', { params: { title: problemSearch.value, pageNum: 1, pageSize: 50 } })
      availableProblems.value = res || []
    } else {
      res = await request.get('/problem/list', { params: { pageNum: 1, pageSize: 50 } })
      availableProblems.value = res.list || []
    }
  } catch (error) {
    console.error('搜索题目失败:', error)
  } finally {
    loadingProblems.value = false
  }
}

// 题目选择变化
const onProblemSelectionChange = (selection: any[]) => {
  selectedProblems.value = selection
}

// 确认选择题目
const confirmProblemSelection = () => {
  const existingIds = homeworkForm.problems.map(p => p.problemId)
  const newProblems = selectedProblems.value
    .filter(p => !existingIds.includes(p.id))
    .map(p => ({
      problemId: p.id,
      title: p.title,
      difficulty: p.difficulty,
      score: 10
    }))
  
  homeworkForm.problems.push(...newProblems)
  showProblemSelector.value = false
  selectedProblems.value = []
}

// 移除题目
const removeProblem = (index: number) => {
  homeworkForm.problems.splice(index, 1)
}

// 获取课程名称
const getCourseNameById = (courseId: number) => {
  const course = courses.value.find(c => c.id === courseId)
  return course?.name || '-'
}

// 判断是否已截止
const isOverdue = (endTime: string) => {
  if (!endTime) return false
  return new Date(endTime) < new Date()
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取状态类型
const getStatusType = (row: Homework) => {
  if (isOverdue(row.endTime)) return 'info'
  return 'success'
}

// 获取状态文本
const getStatusText = (row: Homework) => {
  if (isOverdue(row.endTime)) return '已结束'
  return '进行中'
}

// 获取难度类型
const getDifficultyType = (difficulty: string) => {
  const map: Record<string, string> = { 'EASY': 'success', 'MEDIUM': 'warning', 'HARD': 'danger' }
  return map[difficulty] || 'info'
}

// 获取难度文本
const getDifficultyText = (difficulty: string) => {
  const map: Record<string, string> = { 'EASY': '简单', 'MEDIUM': '中等', 'HARD': '困难' }
  return map[difficulty] || difficulty
}

// 获取分数样式
const getScoreClass = (score: number, total: number | undefined) => {
  if (!total) return ''
  const rate = score / total
  if (rate >= 0.9) return 'score-excellent'
  if (rate >= 0.6) return 'score-pass'
  return 'score-fail'
}

onMounted(async () => {
  await loadCourses()
  await loadClasses()
  loadHomeworks()
  searchProblems()
})
</script>

<style scoped lang="scss">
.homework-manage {
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
  .time-info {
    font-size: 12px;
    line-height: 1.6;
  }
  
  .completion-text {
    font-size: 12px;
    color: #909399;
  }
}

.text-danger {
  color: #f56c6c;
}

.problem-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  
  .problem-count {
    color: #909399;
    font-size: 14px;
  }
}

.problem-table {
  margin-top: 12px;
}

.total-score {
  margin-top: 12px;
  text-align: right;
  font-size: 14px;
  color: #606266;
  
  strong {
    color: #409eff;
    font-size: 18px;
  }
}

.search-input {
  margin-bottom: 16px;
}

.stats-overview {
  margin-bottom: 20px;
  
  .stat-card {
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      
      &.success { color: #67c23a; }
      &.warning { color: #e6a23c; }
      &.primary { color: #409eff; }
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 8px;
    }
  }
}

.error-tag {
  margin: 2px;
}

.score-excellent { color: #67c23a; font-weight: 600; }
.score-pass { color: #e6a23c; font-weight: 600; }
.score-fail { color: #f56c6c; font-weight: 600; }

.pass-count {
  color: #67c23a;
  font-weight: 600;
}
</style>
