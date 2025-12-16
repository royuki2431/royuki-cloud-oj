<template>
  <div class="submission-analysis-page">
    <div class="page-header">
      <h2>提交分析</h2>
    </div>

    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="课程">
          <el-select v-model="filterForm.courseId" placeholder="选择课程" clearable @change="onCourseChange" style="width: 220px">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="filterForm.classId" placeholder="选择班级" clearable @change="loadAnalysis" style="width: 200px">
            <el-option v-for="cls in filteredClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="loadAnalysis"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计概览 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon submissions">
            <el-icon :size="32"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalSubmissions || 0 }}</div>
            <div class="stat-label">总提交数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon accepted">
            <el-icon :size="32"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.acceptedCount || 0 }}</div>
            <div class="stat-label">通过数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon rate">
            <el-icon :size="32"><DataAnalysis /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.acceptRate?.toFixed(1) || 0 }}%</div>
            <div class="stat-label">通过率</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon avg">
            <el-icon :size="32"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.avgAttempts?.toFixed(1) || 0 }}</div>
            <div class="stat-label">平均尝试次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学生提交情况 -->
    <el-card class="student-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>学生提交情况</span>
            <el-radio-group v-model="submissionMode" size="small" @change="onModeChange">
              <el-radio-button value="homework">作业</el-radio-button>
              <el-radio-button value="problem">题库</el-radio-button>
            </el-radio-group>
          </div>
          <el-input 
            v-model="studentSearch" 
            placeholder="搜索学生" 
            style="width: 200px"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>
      
      <!-- 作业模式表格 -->
      <el-table v-if="submissionMode === 'homework'" :data="filteredStudents" stripe v-loading="loading" :default-sort="{ prop: 'completionRate', order: 'descending' }">
        <el-table-column prop="studentName" label="姓名" min-width="90" sortable />
        <el-table-column prop="studentNo" label="学号" min-width="100" sortable>
          <template #default="{ row }">
            {{ row.studentNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="100" sortable />
        <el-table-column prop="completedProblems" label="题目完成" min-width="90" align="center" sortable>
          <template #default="{ row }">
            <span class="text-success">{{ row.completedProblems || 0 }}</span>
            <span class="text-muted">/{{ row.totalProblems || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="completionRate" label="完成率" min-width="120" sortable>
          <template #default="{ row }">
            <el-progress 
              :percentage="row.completionRate || 0" 
              :stroke-width="8"
              :status="row.completionRate >= 80 ? 'success' : row.completionRate >= 50 ? '' : 'exception'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="avgScore" label="平均分" min-width="70" align="center" sortable>
          <template #default="{ row }">
            <span :class="row.avgScore >= 60 ? 'text-success' : 'text-danger'">{{ row.avgScore || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="joinTime" label="加入时间" min-width="140" sortable>
          <template #default="{ row }">
            {{ formatTime(row.joinTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewStudentDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 题库模式表格 -->
      <el-table v-else :data="filteredProblemStudents" stripe v-loading="problemLoading" :default-sort="{ prop: 'totalProblemsSolved', order: 'descending' }">
        <el-table-column prop="studentName" label="姓名" min-width="90" sortable />
        <el-table-column prop="studentNo" label="学号" min-width="100" sortable>
          <template #default="{ row }">
            {{ row.studentNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="100" sortable />
        <el-table-column prop="totalProblemsSolved" label="解决题目" min-width="90" align="center" sortable>
          <template #default="{ row }">
            <el-tag type="success">{{ row.totalProblemsSolved || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalSubmissions" label="总提交" min-width="80" align="center" sortable />
        <el-table-column prop="totalAccepted" label="通过次数" min-width="90" align="center" sortable />
        <el-table-column prop="acceptRate" label="通过率" min-width="120" sortable>
          <template #default="{ row }">
            <el-progress 
              :percentage="row.acceptRate || 0" 
              :stroke-width="8"
              :color="getProgressColor(row.acceptRate || 0)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" min-width="70" align="center" sortable>
          <template #default="{ row }">
            <span class="score">{{ row.totalScore || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewProblemStudentDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="20">
      <!-- 常见错误分析 -->
      <el-col :span="12">
        <el-card class="analysis-card">
          <template #header>
            <div class="card-header">
              <span>常见错误类型</span>
              <el-tag type="info" size="small">Top 10</el-tag>
            </div>
          </template>
          <div class="error-list">
            <div v-for="(error, index) in errorAnalysis" :key="error.type" class="error-item">
              <div class="error-rank">{{ index + 1 }}</div>
              <div class="error-info">
                <div class="error-type">
                  <el-tag :type="getErrorTagType(error.type)" size="small">{{ getErrorText(error.type) }}</el-tag>
                </div>
                <el-progress 
                  :percentage="error.percentage" 
                  :stroke-width="12"
                  :show-text="false"
                  :color="getErrorColor(error.type)"
                />
              </div>
              <div class="error-count">{{ error.count }} 次 ({{ error.percentage.toFixed(1) }}%)</div>
            </div>
            <el-empty v-if="errorAnalysis.length === 0" description="暂无数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 题目通过率分析 -->
      <el-col :span="12">
        <el-card class="analysis-card">
          <template #header>
            <span>题目通过率分析</span>
          </template>
          <el-table :data="problemAnalysis" max-height="350" size="small">
            <el-table-column prop="problemTitle" label="题目" min-width="150" show-overflow-tooltip />
            <el-table-column label="难度" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="getDifficultyType(row.difficulty)" size="small">
                  {{ getDifficultyText(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="通过率" width="150">
              <template #default="{ row }">
                <el-progress 
                  :percentage="row.passRate || 0" 
                  :stroke-width="10"
                  :status="row.passRate >= 60 ? 'success' : row.passRate >= 30 ? 'warning' : 'exception'"
                />
              </template>
            </el-table-column>
            <el-table-column prop="submitCount" label="提交数" width="80" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 学生详情对话框 -->
    <el-dialog v-model="studentDetailVisible" :title="`${currentStudent?.studentName} 的${submissionMode === 'homework' ? '作业' : '题库'}提交记录`" width="1100px" top="3vh">
      <el-descriptions :column="5" border size="small" class="student-desc">
        <el-descriptions-item label="学号">{{ currentStudent?.studentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentStudent?.username || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="submissionMode === 'homework' ? '题目完成' : '解决题目'">
          {{ currentStudent?.completedProblems || 0 }}<template v-if="submissionMode === 'homework'"> / {{ currentStudent?.totalProblems || 0 }}</template>
        </el-descriptions-item>
        <el-descriptions-item :label="submissionMode === 'homework' ? '完成率' : '通过率'">{{ currentStudent?.completionRate || 0 }}%</el-descriptions-item>
        <el-descriptions-item :label="submissionMode === 'homework' ? '平均分' : '总分'">{{ currentStudent?.avgScore || 0 }}</el-descriptions-item>
      </el-descriptions>
      
      <!-- 学生错误分析 -->
      <div class="student-error-analysis" v-if="studentErrorStats.length > 0">
        <h4>错误类型分布</h4>
        <div class="error-stats-row">
          <div v-for="err in studentErrorStats" :key="err.type" class="error-stat-item">
            <el-tag :type="getErrorTagType(err.type)" size="small">{{ getErrorText(err.type) }}</el-tag>
            <span class="error-stat-count">{{ err.count }} 次 ({{ err.percentage.toFixed(0) }}%)</span>
          </div>
        </div>
      </div>
      
      <!-- 作业模式表格 -->
      <el-table v-if="submissionMode === 'homework'" :data="studentSubmissions" max-height="350" class="submission-table" v-loading="studentDetailLoading">
        <el-table-column prop="homeworkTitle" label="作业" min-width="120" show-overflow-tooltip />
        <el-table-column prop="problemTitle" label="题目" min-width="150" show-overflow-tooltip />
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getResultType(row.result)" size="small">
              {{ getResultText(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.score >= row.maxScore * 0.6 ? 'text-success' : 'text-danger'">{{ row.score || 0 }}</span>
            <span class="text-muted"> / {{ row.maxScore || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.submitTime) }}
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 题库模式表格 -->
      <el-table v-else :data="studentSubmissions" max-height="350" class="submission-table" v-loading="studentDetailLoading">
        <el-table-column prop="problemTitle" label="题目" min-width="150" show-overflow-tooltip />
        <el-table-column label="结果" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getResultType(row.result)" size="small">
              {{ getResultText(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80" align="center">
          <template #default="{ row }">
            <span :class="row.score >= 60 ? 'text-success' : 'text-danger'">{{ row.score || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="language" label="语言" width="80" align="center" />
        <el-table-column prop="timeUsed" label="耗时" width="80" align="center">
          <template #default="{ row }">
            {{ row.timeUsed || '-' }}ms
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.submitTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewCode(row)">
              查看代码
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    
    <!-- 代码查看对话框 -->
    <el-dialog v-model="codeDialogVisible" :title="`提交 #${currentSubmission?.id} - ${currentSubmission?.problemTitle}`" width="80%">
      <div class="code-info">
        <el-descriptions :column="5" size="small" border>
          <el-descriptions-item label="结果">
            <el-tag :type="getResultType(currentSubmission?.result)" size="small">
              {{ getResultText(currentSubmission?.result) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="得分">{{ currentSubmission?.score || 0 }}</el-descriptions-item>
          <el-descriptions-item label="语言">{{ currentSubmission?.language }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentSubmission?.timeUsed || '-' }}ms</el-descriptions-item>
          <el-descriptions-item label="内存">{{ currentSubmission?.memoryUsed || '-' }}KB</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="code-container">
        <pre><code>{{ currentSubmission?.code || '暂无代码' }}</code></pre>
      </div>
      <div class="error-message" v-if="currentSubmission?.errorMessage">
        <h4>错误信息</h4>
        <pre class="error-pre">{{ currentSubmission.errorMessage }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, CircleCheck, DataAnalysis, Timer, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getTeacherCourses, getCourseClasses, getClassStudents, getStudentHomeworkSubmissions, type Course, type CourseClass } from '@/api/course'
import { getUserSubmissions } from '@/api/judge'
import { getProblemById } from '@/api/problem'

interface ErrorAnalysis {
  type: string
  count: number
  percentage: number
}

interface ProblemAnalysis {
  problemId: number
  problemTitle: string
  difficulty: string
  submitCount: number
  acceptCount: number
  passRate: number
}

interface StudentAnalysis {
  id: number
  studentId: number
  studentNo: string
  studentName: string
  username: string
  completedProblems: number
  totalProblems: number
  completionRate: number
  avgScore: number
  joinTime: string
}

// 题库模式学生数据
interface ProblemStudentAnalysis {
  studentId: number
  studentNo: string
  studentName: string
  username: string
  totalProblemsSolved: number
  totalSubmissions: number
  totalAccepted: number
  acceptRate: number
  totalScore: number
}

const loading = ref(false)
const problemLoading = ref(false)
const courses = ref<Course[]>([])
const classes = ref<CourseClass[]>([])
const studentSearch = ref('')
const studentDetailVisible = ref(false)
const studentDetailLoading = ref(false)
const currentStudent = ref<StudentAnalysis | null>(null)
const studentSubmissions = ref<any[]>([])
const studentErrorStats = ref<ErrorAnalysis[]>([])

// 提交模式：作业/题库
const submissionMode = ref<'homework' | 'problem'>('homework')
const problemStudentAnalysis = ref<ProblemStudentAnalysis[]>([])
const currentProblemStudent = ref<ProblemStudentAnalysis | null>(null)

// 代码查看
const codeDialogVisible = ref(false)
const currentSubmission = ref<any>(null)

const filterForm = reactive({
  courseId: null as number | null,
  classId: null as number | null,
  dateRange: null as [Date, Date] | null
})

const overview = ref({
  totalSubmissions: 0,
  acceptedCount: 0,
  acceptRate: 0,
  avgAttempts: 0
})

const errorAnalysis = ref<ErrorAnalysis[]>([])
const problemAnalysis = ref<ProblemAnalysis[]>([])
const studentAnalysis = ref<StudentAnalysis[]>([])

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 筛选班级
const filteredClasses = computed(() => {
  if (!filterForm.courseId) return classes.value
  return classes.value.filter(c => c.courseId === filterForm.courseId)
})

// 筛选学生（作业模式）
const filteredStudents = computed(() => {
  if (!studentSearch.value) return studentAnalysis.value
  const keyword = studentSearch.value.toLowerCase()
  return studentAnalysis.value.filter(s => 
    s.studentName.toLowerCase().includes(keyword) ||
    s.studentNo.toLowerCase().includes(keyword) ||
    s.username.toLowerCase().includes(keyword)
  )
})

// 筛选学生（题库模式）
const filteredProblemStudents = computed(() => {
  if (!studentSearch.value) return problemStudentAnalysis.value
  const keyword = studentSearch.value.toLowerCase()
  return problemStudentAnalysis.value.filter(s => 
    s.studentName.toLowerCase().includes(keyword) ||
    s.studentNo.toLowerCase().includes(keyword) ||
    s.username.toLowerCase().includes(keyword)
  )
})

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 模式切换
const onModeChange = () => {
  if (submissionMode.value === 'problem') {
    loadProblemStudentData()
  }
}

// 加载课程
const loadCourses = async () => {
  const userId = getUserId()
  if (!userId) return
  
  try {
    courses.value = await getTeacherCourses(userId)
  } catch (error) {
    console.error('加载课程失败:', error)
  }
}

// 加载班级
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

// 课程变化
const onCourseChange = () => {
  filterForm.classId = null
  loadAnalysis()
}

// 加载分析数据
const loadAnalysis = async () => {
  if (!filterForm.classId) {
    // 清空数据
    overview.value = { totalSubmissions: 0, acceptedCount: 0, acceptRate: 0, avgAttempts: 0 }
    errorAnalysis.value = []
    problemAnalysis.value = []
    studentAnalysis.value = []
    return
  }
  
  loading.value = true
  try {
    // 获取班级学生列表
    const students = await getClassStudents(filterForm.classId)
    
    // 构建学生分析数据
    const studentData: StudentAnalysis[] = []
    
    for (const student of students) {
      studentData.push({
        id: student.studentId,
        studentId: student.studentId,
        studentNo: student.studentNo || '',
        studentName: student.studentName || `用户${student.studentId}`,
        username: student.username || '',
        completedProblems: student.completedProblems || 0,
        totalProblems: student.totalProblems || 0,
        completionRate: student.completionRate || 0,
        avgScore: student.avgScore || 0,
        joinTime: student.joinTime || ''
      })
    }
    
    studentAnalysis.value = studentData
    
    // 从所有学生的提交记录中统计错误类型和题目通过率
    const allErrorCounts: Record<string, number> = {}
    const problemStats: Record<number, { title: string, difficulty: string, submitCount: number, acceptCount: number }> = {}
    let totalSubmissions = 0
    let acceptedCount = 0
    
    // 并行获取所有学生的提交记录
    const submissionPromises = students.slice(0, 30).map(student => 
      getUserSubmissions({ userId: student.studentId, page: 1, size: 100 })
        .then(res => ({ studentId: student.studentId, submissions: res.data || res || [] }))
        .catch(() => ({ studentId: student.studentId, submissions: [] }))
    )
    
    const allStudentSubmissions = await Promise.all(submissionPromises)
    
    for (const { submissions } of allStudentSubmissions) {
      for (const sub of submissions) {
        totalSubmissions++
        const status = sub.status || sub.result
        
        if (status === 'ACCEPTED') {
          acceptedCount++
        } else if (status) {
          allErrorCounts[status] = (allErrorCounts[status] || 0) + 1
        }
        
        // 统计题目通过率
        const problemId = sub.problemId
        if (problemId) {
          if (!problemStats[problemId]) {
            problemStats[problemId] = {
              title: sub.problemTitle || `题目${problemId}`,
              difficulty: sub.difficulty || '',
              submitCount: 0,
              acceptCount: 0
            }
          }
          problemStats[problemId].submitCount++
          if (status === 'ACCEPTED') {
            problemStats[problemId].acceptCount++
          }
        }
      }
    }
    
    // 设置概览数据
    overview.value = {
      totalSubmissions: totalSubmissions,
      acceptedCount: acceptedCount,
      acceptRate: totalSubmissions > 0 ? Math.round(acceptedCount / totalSubmissions * 100) : 0,
      avgAttempts: studentData.length > 0 ? Math.round(totalSubmissions / studentData.length * 10) / 10 : 0
    }
    
    // 错误分析 - 包含所有错误类型
    const allErrorTypes = [
      'WRONG_ANSWER',
      'TIME_LIMIT_EXCEEDED', 
      'RUNTIME_ERROR',
      'COMPILE_ERROR',
      'MEMORY_LIMIT_EXCEEDED',
      'OUTPUT_LIMIT_EXCEEDED',
      'PRESENTATION_ERROR'
    ]
    // 确保所有错误类型都有值
    for (const type of allErrorTypes) {
      if (!(type in allErrorCounts)) {
        allErrorCounts[type] = 0
      }
    }
    const totalErrors = Object.values(allErrorCounts).reduce((a, b) => a + b, 0)
    errorAnalysis.value = allErrorTypes
      .map(type => ({
        type,
        count: allErrorCounts[type] || 0,
        percentage: totalErrors > 0 ? ((allErrorCounts[type] || 0) / totalErrors) * 100 : 0
      }))
      .sort((a, b) => b.count - a.count)
    
    // 题目通过率分析 - 获取题目难度
    const problemList = Object.entries(problemStats)
      .map(([id, stats]) => ({
        problemId: Number(id),
        problemTitle: stats.title,
        difficulty: stats.difficulty,
        submitCount: stats.submitCount,
        acceptCount: stats.acceptCount,
        passRate: stats.submitCount > 0 ? Math.round(stats.acceptCount / stats.submitCount * 100) : 0
      }))
      .sort((a, b) => a.passRate - b.passRate)
      .slice(0, 10)
    
    // 获取缺少难度的题目信息
    for (const problem of problemList) {
      if (!problem.difficulty) {
        try {
          const problemInfo = await getProblemById(problem.problemId)
          problem.difficulty = problemInfo?.difficulty || 'MEDIUM'
        } catch {
          problem.difficulty = 'MEDIUM'
        }
      }
    }
    
    problemAnalysis.value = problemList
    
  } catch (error) {
    console.error('加载分析数据失败:', error)
    overview.value = { totalSubmissions: 0, acceptedCount: 0, acceptRate: 0, avgAttempts: 0 }
    errorAnalysis.value = []
    problemAnalysis.value = []
  } finally {
    loading.value = false
  }
}

// 加载题库模式学生数据
const loadProblemStudentData = async () => {
  if (!filterForm.classId) {
    problemStudentAnalysis.value = []
    return
  }
  
  problemLoading.value = true
  try {
    // 获取班级学生列表
    const students = await getClassStudents(filterForm.classId)
    
    // 并行获取每个学生的提交统计
    const studentPromises = students.slice(0, 50).map(async (student: any) => {
      try {
        const res = await getUserSubmissions({
          userId: student.studentId,
          page: 1,
          size: 200
        })
        const submissions = res.data || res || []
        
        // 统计数据
        const solvedProblems = new Set<number>()
        let totalAccepted = 0
        let totalScore = 0
        
        for (const sub of submissions) {
          const status = sub.status || sub.result
          if (status === 'ACCEPTED') {
            solvedProblems.add(sub.problemId)
            totalAccepted++
            totalScore += sub.score || 0
          }
        }
        
        return {
          studentId: student.studentId,
          studentNo: student.studentNo || '',
          studentName: student.studentName || `用户${student.studentId}`,
          username: student.username || '',
          totalProblemsSolved: solvedProblems.size,
          totalSubmissions: submissions.length,
          totalAccepted: totalAccepted,
          acceptRate: submissions.length > 0 ? Math.round(totalAccepted / submissions.length * 100) : 0,
          totalScore: totalScore
        }
      } catch {
        return {
          studentId: student.studentId,
          studentNo: student.studentNo || '',
          studentName: student.studentName || `用户${student.studentId}`,
          username: student.username || '',
          totalProblemsSolved: 0,
          totalSubmissions: 0,
          totalAccepted: 0,
          acceptRate: 0,
          totalScore: 0
        }
      }
    })
    
    problemStudentAnalysis.value = await Promise.all(studentPromises)
  } catch (error) {
    console.error('加载题库学生数据失败:', error)
    problemStudentAnalysis.value = []
  } finally {
    problemLoading.value = false
  }
}

// 查看题库模式学生详情
const viewProblemStudentDetail = async (student: ProblemStudentAnalysis) => {
  currentProblemStudent.value = student
  // 复用现有的详情对话框
  currentStudent.value = {
    id: student.studentId,
    studentId: student.studentId,
    studentNo: student.studentNo,
    studentName: student.studentName,
    username: student.username,
    completedProblems: student.totalProblemsSolved,
    totalProblems: student.totalSubmissions,
    completionRate: student.acceptRate,
    avgScore: student.totalScore,
    joinTime: ''
  }
  studentDetailVisible.value = true
  studentDetailLoading.value = true
  studentErrorStats.value = []
  
  try {
    const res = await getUserSubmissions({
      userId: student.studentId,
      page: 1,
      size: 100
    })
    
    const submissions = res.data || res || []
    
    studentSubmissions.value = submissions.map((sub: any) => ({
      id: sub.id,
      problemId: sub.problemId,
      problemTitle: sub.problemTitle || `题目${sub.problemId}`,
      result: sub.status,
      score: sub.score || 0,
      language: sub.language,
      code: sub.code,
      timeUsed: sub.timeUsed || 0,
      memoryUsed: sub.memoryUsed || 0,
      errorMessage: sub.errorMessage,
      submitTime: sub.createTime
    }))
    
    // 统计错误类型
    const errorCounts: Record<string, number> = {}
    let totalErrors = 0
    studentSubmissions.value.forEach((sub: any) => {
      if (sub.result && sub.result !== 'ACCEPTED') {
        errorCounts[sub.result] = (errorCounts[sub.result] || 0) + 1
        totalErrors++
      }
    })
    
    studentErrorStats.value = Object.entries(errorCounts)
      .map(([type, count]) => ({
        type,
        count,
        percentage: totalErrors > 0 ? (count / totalErrors) * 100 : 0
      }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)
      
  } catch (error) {
    console.error('加载提交记录失败:', error)
    studentSubmissions.value = []
  } finally {
    studentDetailLoading.value = false
  }
}

// 查看学生详情（作业模式）- 显示作业提交记录
const viewStudentDetail = async (student: StudentAnalysis) => {
  if (!filterForm.classId) return
  
  currentStudent.value = student
  studentDetailVisible.value = true
  studentDetailLoading.value = true
  studentErrorStats.value = []
  
  try {
    // 加载学生作业提交记录
    const submissions = await getStudentHomeworkSubmissions(filterForm.classId, student.studentId)
    
    studentSubmissions.value = (submissions || []).map((sub: any) => ({
      id: sub.id,
      homeworkId: sub.homeworkId,
      homeworkTitle: sub.homeworkTitle || `作业${sub.homeworkId}`,
      problemId: sub.problemId,
      problemTitle: sub.problemTitle || `题目${sub.problemId}`,
      result: sub.status,
      score: sub.score || 0,
      maxScore: sub.maxScore || 0,
      submitTime: sub.submitTime,
      judgeSubmissionId: sub.judgeSubmissionId
    }))
    
    // 统计错误类型
    const errorCounts: Record<string, number> = {}
    let totalErrors = 0
    studentSubmissions.value.forEach((sub: any) => {
      if (sub.result && sub.result !== 'ACCEPTED') {
        errorCounts[sub.result] = (errorCounts[sub.result] || 0) + 1
        totalErrors++
      }
    })
    
    studentErrorStats.value = Object.entries(errorCounts)
      .map(([type, count]) => ({
        type,
        count,
        percentage: totalErrors > 0 ? (count / totalErrors) * 100 : 0
      }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)
      
  } catch (error) {
    console.error('加载作业提交记录失败:', error)
    studentSubmissions.value = []
  } finally {
    studentDetailLoading.value = false
  }
}

// 查看代码
const viewCode = (submission: any) => {
  currentSubmission.value = submission
  codeDialogVisible.value = true
}

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 获取错误标签类型
const getErrorTagType = (type: string) => {
  const map: Record<string, string> = {
    'WRONG_ANSWER': 'danger',
    'TIME_LIMIT_EXCEEDED': 'warning',
    'RUNTIME_ERROR': 'danger',
    'COMPILE_ERROR': 'info',
    'MEMORY_LIMIT_EXCEEDED': 'warning',
    'OUTPUT_LIMIT_EXCEEDED': 'warning',
    'PRESENTATION_ERROR': 'info'
  }
  return map[type] || 'info'
}

// 获取错误文本
const getErrorText = (type: string) => {
  const map: Record<string, string> = {
    'WRONG_ANSWER': '答案错误',
    'TIME_LIMIT_EXCEEDED': '超时',
    'RUNTIME_ERROR': '运行错误',
    'COMPILE_ERROR': '编译错误',
    'MEMORY_LIMIT_EXCEEDED': '内存超限',
    'OUTPUT_LIMIT_EXCEEDED': '输出超限',
    'PRESENTATION_ERROR': '格式错误',
    'ACCEPTED': '通过'
  }
  return map[type] || type
}

// 获取错误颜色
const getErrorColor = (type: string) => {
  const map: Record<string, string> = {
    'WRONG_ANSWER': '#f56c6c',
    'TIME_LIMIT_EXCEEDED': '#e6a23c',
    'RUNTIME_ERROR': '#f56c6c',
    'COMPILE_ERROR': '#909399',
    'MEMORY_LIMIT_EXCEEDED': '#e6a23c'
  }
  return map[type] || '#409eff'
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

// 获取结果类型
const getResultType = (result: string) => {
  if (result === 'ACCEPTED') return 'success'
  if (result === 'PENDING' || result === 'JUDGING') return 'warning'
  return 'danger'
}

// 获取结果文本
const getResultText = (result: string) => {
  const map: Record<string, string> = {
    'ACCEPTED': '通过',
    'WRONG_ANSWER': '答案错误',
    'TIME_LIMIT_EXCEEDED': '超时',
    'RUNTIME_ERROR': '运行错误',
    'COMPILE_ERROR': '编译错误',
    'PENDING': '等待中',
    'JUDGING': '判题中'
  }
  return map[result] || result
}

onMounted(async () => {
  await loadCourses()
  await loadClasses()
  
  // 默认选择第一个课程和班级
  if (courses.value.length > 0) {
    filterForm.courseId = courses.value[0].id
    if (filteredClasses.value.length > 0) {
      filterForm.classId = filteredClasses.value[0].id
      await loadAnalysis()
    }
  }
})
</script>

<style scoped lang="scss">
.submission-analysis-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
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

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  
  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;
    
    &.submissions {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }
    
    &.accepted {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      color: white;
    }
    
    &.rate {
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      color: white;
    }
    
    &.avg {
      background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
      color: white;
    }
  }
  
  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
    }
    
    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

.analysis-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.error-list {
  .error-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #ebeef5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .error-rank {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background: #f5f7fa;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      color: #909399;
      margin-right: 12px;
    }
    
    .error-info {
      flex: 1;
      
      .error-type {
        margin-bottom: 8px;
      }
    }
    
    .error-count {
      width: 120px;
      text-align: right;
      font-size: 14px;
      color: #606266;
    }
  }
}

.student-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }
  }
}

.score {
  font-weight: bold;
  color: #409eff;
  font-size: 14px;
}

.error-tag {
  margin: 2px;
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}

.text-muted {
  color: #c0c4cc;
}

.student-desc {
  margin-bottom: 20px;
}

.submission-table {
  margin-top: 16px;
}

.student-error-analysis {
  margin: 16px 0;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
  
  h4 {
    margin: 0 0 12px 0;
    font-size: 14px;
    color: #606266;
  }
  
  .error-stats-row {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    
    .error-stat-item {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .error-stat-count {
        font-size: 13px;
        color: #909399;
      }
    }
  }
}

.code-info {
  margin-bottom: 16px;
}

.code-container {
  pre {
    background: #282c34;
    color: #abb2bf;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    max-height: 500px;
    margin: 0;
    
    code {
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 14px;
      line-height: 1.6;
    }
  }
}

.error-message {
  margin-top: 16px;
  
  h4 {
    margin: 0 0 8px 0;
    font-size: 14px;
    color: #f56c6c;
  }
  
  .error-pre {
    background: #fef0f0;
    color: #f56c6c;
    padding: 12px;
    border-radius: 4px;
    font-size: 13px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.text-danger {
  color: #f56c6c;
}
</style>
