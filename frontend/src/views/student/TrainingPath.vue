<template>
  <div class="training-path-page" v-loading="loading">
    <div class="page-header">
      <h2>课程化训练路径</h2>
      <p class="page-desc">按照结构化的学习路径，循序渐进地提升编程能力</p>
    </div>

    <!-- 学习路径选择 -->
    <div class="path-selector">
      <el-radio-group v-model="currentPath" size="large" @change="onPathChange">
        <el-radio-button v-for="path in trainingPaths" :key="path.id" :value="path.id">
          <el-icon class="path-icon"><component :is="path.icon" /></el-icon>
          {{ path.name }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 当前路径信息 -->
    <el-card class="path-info-card" v-if="selectedPath">
      <div class="path-header">
        <div class="path-title">
          <el-icon :size="28"><component :is="selectedPath.icon" /></el-icon>
          <div>
            <h3>{{ selectedPath.name }}</h3>
            <p>{{ selectedPath.description }}</p>
          </div>
        </div>
        <div class="path-progress">
          <el-progress 
            type="circle" 
            :percentage="pathProgress" 
            :width="80"
            :stroke-width="8"
          >
            <template #default>
              <span class="progress-text">{{ pathProgress }}%</span>
            </template>
          </el-progress>
          <span class="progress-label">完成进度</span>
        </div>
      </div>
    </el-card>

    <!-- 阶段列表 -->
    <div class="stages-container">
      <div 
        v-for="(stage, stageIndex) in stages" 
        :key="stage.id" 
        class="stage-card"
        :class="{ 'locked': stage.locked, 'completed': stage.completed, 'current': stage.current }"
      >
        <div class="stage-header" @click="toggleStage(stageIndex)">
          <div class="stage-info">
            <div class="stage-number">
              <el-icon v-if="stage.completed"><Check /></el-icon>
              <el-icon v-else-if="stage.locked"><Lock /></el-icon>
              <span v-else>{{ stageIndex + 1 }}</span>
            </div>
            <div class="stage-title">
              <h4>{{ stage.name }}</h4>
              <p>{{ stage.description }}</p>
            </div>
          </div>
          <div class="stage-meta">
            <el-tag :type="getDifficultyType(stage.difficulty)" size="small">
              {{ getDifficultyLabel(stage.difficulty) }}
            </el-tag>
            <span class="stage-progress">{{ stage.completedCount }}/{{ stage.totalCount }}</span>
            <el-icon class="expand-icon" :class="{ 'expanded': stage.expanded }">
              <ArrowDown />
            </el-icon>
          </div>
        </div>
        
        <el-collapse-transition>
          <div class="stage-content" v-show="stage.expanded && !stage.locked">
            <div class="problems-list">
              <div 
                v-for="problem in stage.problems" 
                :key="problem.id" 
                class="problem-item"
                :class="{ 'solved': problem.solved }"
                @click="goToProblem(problem)"
              >
                <div class="problem-status">
                  <el-icon v-if="problem.solved" class="solved-icon"><CircleCheck /></el-icon>
                  <el-icon v-else class="unsolved-icon"><Document /></el-icon>
                </div>
                <div class="problem-info">
                  <span class="problem-title">{{ problem.title }}</span>
                  <div class="problem-tags">
                    <el-tag v-for="tag in problem.tags" :key="tag" size="small" type="info">
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
                <div class="problem-difficulty">
                  <el-tag :type="getDifficultyType(problem.difficulty)" size="small">
                    {{ getDifficultyLabel(problem.difficulty) }}
                  </el-tag>
                </div>
                <div class="problem-action">
                  <el-button 
                    :type="problem.solved ? 'success' : 'primary'" 
                    size="small"
                    @click.stop="goToProblem(problem)"
                  >
                    {{ problem.solved ? '复习' : '开始' }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-collapse-transition>
      </div>
    </div>

    <!-- 学习建议 -->
    <el-card class="tips-card">
      <template #header>
        <div class="tips-header">
          <el-icon><InfoFilled /></el-icon>
          <span>学习建议</span>
        </div>
      </template>
      <div class="tips-content">
        <div class="tip-item">
          <el-icon class="tip-icon"><Timer /></el-icon>
          <div>
            <h5>循序渐进</h5>
            <p>建议按照阶段顺序学习，每个阶段完成80%以上再进入下一阶段</p>
          </div>
        </div>
        <div class="tip-item">
          <el-icon class="tip-icon"><Edit /></el-icon>
          <div>
            <h5>多加练习</h5>
            <p>每道题目建议独立思考后再查看题解，遇到困难可以先跳过</p>
          </div>
        </div>
        <div class="tip-item">
          <el-icon class="tip-icon"><Notebook /></el-icon>
          <div>
            <h5>及时总结</h5>
            <p>完成每个阶段后，建议整理笔记，总结解题思路和技巧</p>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Check, Lock, ArrowDown, CircleCheck, Document, 
  InfoFilled, Timer, Edit, Notebook,
  TrendCharts, DataLine, Cpu, Connection, Grid
} from '@element-plus/icons-vue'
import { getLearningSummary, getUserProgressList } from '@/api/learning'
import { getProblemList } from '@/api/problem'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const currentPath = ref('algorithm')

// 训练路径定义
const trainingPaths = ref([
  { id: 'algorithm', name: '算法基础', icon: TrendCharts, description: '从基础算法开始，掌握常用数据结构和算法' },
  { id: 'advanced', name: '进阶提升', icon: DataLine, description: '深入学习高级算法和复杂数据结构' },
  { id: 'competition', name: '竞赛训练', icon: Cpu, description: '针对算法竞赛的专项训练' },
  { id: 'interview', name: '面试刷题', icon: Connection, description: '常见面试算法题目训练' },
])

const selectedPath = computed(() => {
  return trainingPaths.value.find(p => p.id === currentPath.value)
})

// 阶段数据
const stages = ref<any[]>([])

// 用户已解决的题目ID集合
const solvedProblemIds = ref<Set<number>>(new Set())

// 计算路径完成进度
const pathProgress = computed(() => {
  if (stages.value.length === 0) return 0
  const total = stages.value.reduce((sum, s) => sum + s.totalCount, 0)
  const completed = stages.value.reduce((sum, s) => sum + s.completedCount, 0)
  return total > 0 ? Math.round((completed / total) * 100) : 0
})

// 切换路径
const onPathChange = () => {
  loadStages()
}

// 展开/收起阶段
const toggleStage = (index: number) => {
  if (stages.value[index].locked) {
    ElMessage.warning('请先完成前面的阶段')
    return
  }
  stages.value[index].expanded = !stages.value[index].expanded
}

// 获取难度类型
const getDifficultyType = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return 'success'
    case 'MEDIUM': return 'warning'
    case 'HARD': return 'danger'
    default: return 'info'
  }
}

// 获取难度标签
const getDifficultyLabel = (difficulty: string) => {
  switch (difficulty) {
    case 'EASY': return '简单'
    case 'MEDIUM': return '中等'
    case 'HARD': return '困难'
    default: return difficulty
  }
}

// 跳转到题目
const goToProblem = (problem: any) => {
  router.push(`/problem/${problem.id}`)
}

// 加载用户已解决的题目
const loadSolvedProblems = async () => {
  try {
    const userId = userStore.userInfo?.id
    if (!userId) return
    
    // 获取用户的学习进度列表，筛选出已完成的题目
    const progressList = await getUserProgressList(userId)
    if (progressList && Array.isArray(progressList)) {
      const solvedIds = progressList
        .filter((p: any) => p.status === 'COMPLETED')
        .map((p: any) => p.problemId)
      solvedProblemIds.value = new Set(solvedIds)
    }
  } catch (error) {
    console.error('加载已解决题目失败', error)
  }
}

// 加载阶段数据
const loadStages = async () => {
  loading.value = true
  try {
    // 根据当前路径获取题目
    const problems = await getProblemList({ page: 1, size: 100 })
    const problemList = problems?.list || []
    
    // 根据路径类型生成阶段
    const stageConfigs = getStageConfigs(currentPath.value)
    
    stages.value = stageConfigs.map((config, index) => {
      // 筛选该阶段的题目
      const stageProblems = problemList
        .filter((p: any) => {
          // 按难度筛选
          if (config.difficulty && p.difficulty !== config.difficulty) return false
          // 按分类筛选
          if (config.category && p.category !== config.category) return false
          // 按标签筛选
          if (config.tags && config.tags.length > 0) {
            try {
              const problemTags = p.tags ? (typeof p.tags === 'string' ? JSON.parse(p.tags) : p.tags) : []
              return config.tags.some((t: string) => problemTags.includes(t))
            } catch {
              return false
            }
          }
          return true
        })
        .slice(0, config.maxProblems || 10)
        .map((p: any) => {
          let parsedTags: string[] = []
          try {
            parsedTags = p.tags ? (typeof p.tags === 'string' ? JSON.parse(p.tags) : p.tags) : []
          } catch {
            parsedTags = []
          }
          return {
            ...p,
            solved: solvedProblemIds.value.has(p.id),
            tags: parsedTags
          }
        })
      
      const completedCount = stageProblems.filter((p: any) => p.solved).length
      const prevStageCompleted = index === 0 || 
        (stages.value[index - 1]?.completedCount / stages.value[index - 1]?.totalCount >= 0.8)
      
      return {
        id: config.id,
        name: config.name,
        description: config.description,
        difficulty: config.difficulty,
        problems: stageProblems,
        totalCount: stageProblems.length,
        completedCount,
        locked: index > 0 && !prevStageCompleted,
        completed: completedCount === stageProblems.length && stageProblems.length > 0,
        current: !prevStageCompleted && completedCount < stageProblems.length,
        expanded: index === 0
      }
    })
    
    // 重新计算锁定状态
    for (let i = 1; i < stages.value.length; i++) {
      const prevStage = stages.value[i - 1]
      const prevProgress = prevStage.totalCount > 0 
        ? prevStage.completedCount / prevStage.totalCount 
        : 0
      stages.value[i].locked = prevProgress < 0.8
    }
    
  } catch (error) {
    console.error('加载阶段数据失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 获取阶段配置
const getStageConfigs = (pathId: string) => {
  switch (pathId) {
    case 'algorithm':
      return [
        { id: 'basic', name: '入门基础', description: '基本语法和简单逻辑', difficulty: 'EASY', maxProblems: 10 },
        { id: 'array', name: '数组与字符串', description: '数组操作和字符串处理', difficulty: 'EASY', tags: ['数组', '字符串'], maxProblems: 10 },
        { id: 'sort', name: '排序算法', description: '常见排序算法实现', difficulty: 'MEDIUM', tags: ['排序'], maxProblems: 8 },
        { id: 'search', name: '查找算法', description: '二分查找等搜索技巧', difficulty: 'MEDIUM', tags: ['二分', '查找'], maxProblems: 8 },
        { id: 'recursion', name: '递归与回溯', description: '递归思想和回溯算法', difficulty: 'MEDIUM', tags: ['递归', '回溯'], maxProblems: 8 },
        { id: 'dp-intro', name: '动态规划入门', description: '动态规划基础概念', difficulty: 'MEDIUM', tags: ['动态规划'], maxProblems: 10 },
      ]
    case 'advanced':
      return [
        { id: 'tree', name: '树结构', description: '二叉树和多叉树', difficulty: 'MEDIUM', tags: ['树', '二叉树'], maxProblems: 10 },
        { id: 'graph', name: '图论基础', description: '图的遍历和基本算法', difficulty: 'MEDIUM', tags: ['图', 'BFS', 'DFS'], maxProblems: 10 },
        { id: 'dp-advanced', name: '动态规划进阶', description: '复杂DP问题', difficulty: 'HARD', tags: ['动态规划'], maxProblems: 10 },
        { id: 'greedy', name: '贪心算法', description: '贪心策略应用', difficulty: 'MEDIUM', tags: ['贪心'], maxProblems: 8 },
        { id: 'divide', name: '分治算法', description: '分治思想应用', difficulty: 'HARD', tags: ['分治'], maxProblems: 8 },
      ]
    case 'competition':
      return [
        { id: 'number', name: '数论基础', description: '质数、GCD等', difficulty: 'MEDIUM', tags: ['数论', '数学'], maxProblems: 10 },
        { id: 'segment', name: '线段树', description: '区间查询与修改', difficulty: 'HARD', tags: ['线段树'], maxProblems: 8 },
        { id: 'string-adv', name: '字符串算法', description: 'KMP、Trie等', difficulty: 'HARD', tags: ['字符串', 'KMP'], maxProblems: 8 },
        { id: 'network', name: '网络流', description: '最大流最小割', difficulty: 'HARD', tags: ['网络流'], maxProblems: 6 },
      ]
    case 'interview':
      return [
        { id: 'hot100', name: '高频题目', description: '面试常考题目', difficulty: 'MEDIUM', maxProblems: 15 },
        { id: 'linked', name: '链表专题', description: '链表操作技巧', difficulty: 'MEDIUM', tags: ['链表'], maxProblems: 10 },
        { id: 'stack', name: '栈与队列', description: '栈和队列应用', difficulty: 'MEDIUM', tags: ['栈', '队列'], maxProblems: 10 },
        { id: 'design', name: '设计题', description: '系统设计类题目', difficulty: 'HARD', tags: ['设计'], maxProblems: 8 },
      ]
    default:
      return []
  }
}

onMounted(async () => {
  await loadSolvedProblems()
  await loadStages()
})
</script>

<style scoped>
.training-path-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.page-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.path-selector {
  margin-bottom: 24px;
}

.path-selector :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
}

.path-icon {
  font-size: 16px;
}

.path-info-card {
  margin-bottom: 24px;
}

.path-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.path-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.path-title h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
}

.path-title p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.path-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.progress-text {
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
}

.progress-label {
  font-size: 12px;
  color: #909399;
}

.stages-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.stage-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #EBEEF5;
  overflow: hidden;
  transition: all 0.3s;
}

.stage-card:hover:not(.locked) {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stage-card.locked {
  opacity: 0.6;
}

.stage-card.completed .stage-number {
  background: #67C23A;
  color: #fff;
}

.stage-card.current .stage-number {
  background: #409EFF;
  color: #fff;
}

.stage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.stage-header:hover {
  background: #F5F7FA;
}

.stage-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stage-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #E4E7ED;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #606266;
}

.stage-title h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #303133;
}

.stage-title p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.stage-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stage-progress {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.expand-icon {
  transition: transform 0.3s;
  color: #909399;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.stage-content {
  border-top: 1px solid #EBEEF5;
  padding: 16px 20px;
  background: #FAFAFA;
}

.problems-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.problem-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.problem-item:hover {
  background: #ECF5FF;
}

.problem-item.solved {
  background: #F0F9EB;
}

.problem-status {
  flex-shrink: 0;
}

.solved-icon {
  color: #67C23A;
  font-size: 20px;
}

.unsolved-icon {
  color: #C0C4CC;
  font-size: 20px;
}

.problem-info {
  flex: 1;
  min-width: 0;
}

.problem-title {
  font-size: 14px;
  color: #303133;
  display: block;
  margin-bottom: 4px;
}

.problem-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.problem-difficulty {
  flex-shrink: 0;
}

.problem-action {
  flex-shrink: 0;
}

.tips-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.tips-card :deep(.el-card__header) {
  border-bottom: none;
  padding-bottom: 0;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.tips-content {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.tip-item {
  display: flex;
  gap: 12px;
  color: #fff;
}

.tip-icon {
  font-size: 24px;
  flex-shrink: 0;
  opacity: 0.9;
}

.tip-item h5 {
  margin: 0 0 4px 0;
  font-size: 14px;
}

.tip-item p {
  margin: 0;
  font-size: 12px;
  opacity: 0.85;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .tips-content {
    grid-template-columns: 1fr;
  }
  
  .path-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}
</style>
