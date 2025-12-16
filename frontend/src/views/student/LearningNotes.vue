<template>
  <div class="learning-notes-page">
    <div class="page-header">
      <h2>我的学习笔记</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索笔记..."
          style="width: 250px; margin-right: 12px"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="openNoteSquare">
          <el-icon><Collection /></el-icon>
          笔记广场
        </el-button>
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          新建笔记
        </el-button>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="notes-grid" v-loading="loading">
      <el-empty v-if="notes.length === 0" description="暂无笔记" />
      <el-card 
        v-for="note in notes" 
        :key="note.id" 
        class="note-card"
        @click="viewNote(note)"
      >
        <template #header>
          <div class="note-header">
            <span class="note-title">{{ note.title }}</span>
            <el-tag v-if="note.isPublic" type="success" size="small">公开</el-tag>
          </div>
        </template>
        <div class="note-content">{{ truncateContent(note.content) }}</div>
        <div class="note-footer">
          <span class="note-time">{{ formatTime(note.updatedTime) }}</span>
          <span class="note-problem" v-if="note.problemId">
            {{ getProblemTitle(note.problemId) }}
          </span>
        </div>
        <div class="note-actions" @click.stop>
          <el-button type="primary" size="small" text @click="editNote(note)">
            编辑
          </el-button>
          <el-button type="danger" size="small" text @click="handleDelete(note)">
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 创建/编辑笔记对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑笔记' : '新建笔记'"
      width="700px"
    >
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="noteForm.title" placeholder="请输入笔记标题" />
        </el-form-item>
        <el-form-item label="关联题目">
          <el-select
            v-model="noteForm.problemId"
            placeholder="选择题目（可选）"
            filterable
            clearable
            style="width: 300px"
          >
            <el-option
              v-for="problem in problemList"
              :key="problem.id"
              :label="problem.title"
              :value="problem.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="noteForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入笔记内容，支持 Markdown 格式"
          />
        </el-form-item>
        <el-form-item label="是否公开">
          <el-switch 
            v-model="noteForm.isPublic" 
            active-text="公开"
            inactive-text="私密"
          />
          <span class="switch-hint">公开后其他用户可以查看此笔记</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看笔记对话框 -->
    <el-dialog v-model="viewDialogVisible" :title="currentNote?.title" width="700px">
      <div class="note-view">
        <div class="note-meta">
          <span v-if="currentNote?.authorName" class="author-info">
            <el-tag size="small" type="info">{{ currentNote.authorName }}</el-tag>
          </span>
          <span v-if="currentNote?.problemId">关联题目：{{ currentNote.problemTitle || getProblemTitle(currentNote.problemId) }}</span>
          <span>更新时间：{{ formatTime(currentNote?.updatedTime) }}</span>
          <span v-if="currentNote?.viewCount !== undefined">
            <el-icon><View /></el-icon> {{ currentNote.viewCount }} 次浏览
          </span>
        </div>
        <div class="note-body" v-html="renderMarkdown(currentNote?.content || '')"></div>
      </div>
    </el-dialog>
    
    <!-- 笔记广场对话框 -->
    <el-dialog 
      v-model="showNoteSquare" 
      title="📚 笔记广场" 
      width="900px"
      top="5vh"
    >
      <div class="note-square" v-loading="loadingPublic">
        <el-empty v-if="publicNotes.length === 0" description="暂无公开笔记" />
        <div v-else class="public-notes-grid">
          <el-card 
            v-for="note in publicNotes" 
            :key="note.id" 
            class="public-note-card"
            shadow="hover"
            @click="viewPublicNote(note)"
          >
            <div class="public-note-header">
              <span class="public-note-title">{{ note.title }}</span>
              <el-tag size="small" type="info">{{ note.authorName || '匿名' }}</el-tag>
            </div>
            <div class="public-note-preview">
              {{ note.content.length > 80 ? note.content.substring(0, 80) + '...' : note.content }}
            </div>
            <div class="public-note-footer">
              <span><el-icon><View /></el-icon> {{ note.viewCount || 0 }}</span>
              <span>{{ formatTime(note.updatedTime) }}</span>
            </div>
          </el-card>
        </div>
        <div class="load-more" v-if="publicNotes.length >= 20">
          <el-button @click="loadMorePublicNotes" :loading="loadingPublic">
            加载更多
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Collection, View } from '@element-plus/icons-vue'
import { 
  getUserNotes, 
  createNote, 
  updateNote, 
  deleteNote,
  searchNotes,
  getAllPublicNotes,
  viewNote as viewNoteApi,
  type LearningNote,
  type CreateNoteRequest,
  type UpdateNoteRequest
} from '@/api/learning'
import { getProblemById, getProblemList } from '@/api/problem'
import type { Problem } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const notes = ref<LearningNote[]>([])
const searchKeyword = ref('')
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const isEdit = ref(false)
const currentNote = ref<LearningNote | null>(null)

// 笔记广场
const showNoteSquare = ref(false)
const publicNotes = ref<LearningNote[]>([])
const loadingPublic = ref(false)
const publicPage = ref(1)

const noteForm = ref({
  id: 0,
  title: '',
  content: '',
  problemId: undefined as number | undefined,
  isPublic: false
})

const problemMap = ref<Map<number, Problem>>(new Map())
const problemList = ref<Problem[]>([])

// 获取当前用户ID
const getUserId = () => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).id
  }
  return null
}

// 加载笔记列表
const loadNotes = async () => {
  const userId = getUserId()
  if (!userId) {
    ElMessage.warning('请先登录')
    return
  }
  
  loading.value = true
  try {
    notes.value = await getUserNotes(userId)
    // 加载题目信息
    await loadProblemInfo()
  } catch (error) {
    console.error('加载笔记失败:', error)
    ElMessage.error('加载笔记失败')
  } finally {
    loading.value = false
  }
}

// 加载题目信息
const loadProblemInfo = async () => {
  const problemIds = notes.value
    .filter(n => n.problemId)
    .map(n => n.problemId)
  const uniqueIds = [...new Set(problemIds)]
  
  const promises = uniqueIds.map(async (id) => {
    if (!id) return
    try {
      const problem = await getProblemById(id)
      if (problem) {
        problemMap.value.set(id, problem)
      }
    } catch (error) {
      console.error(`获取题目${id}信息失败:`, error)
    }
  })
  
  await Promise.all(promises)
}

// 获取题目名称
const getProblemTitle = (problemId: number | undefined) => {
  if (!problemId) return ''
  const problem = problemMap.value.get(problemId)
  return problem?.title || `题目 #${problemId}`
}

// 加载题目列表（用于下拉选择）
const loadProblemList = async () => {
  try {
    const res = await getProblemList({ pageNum: 1, pageSize: 200 })
    // 兼容不同的返回格式
    problemList.value = res.records || res.list || []
    console.log('加载题目列表:', problemList.value.length)
  } catch (error) {
    console.error('加载题目列表失败:', error)
  }
}

// 搜索笔记
const handleSearch = async () => {
  const userId = getUserId()
  if (!userId) return
  
  if (!searchKeyword.value.trim()) {
    loadNotes()
    return
  }
  
  loading.value = true
  try {
    notes.value = await searchNotes(userId, searchKeyword.value)
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  noteForm.value = {
    id: 0,
    title: '',
    content: '',
    problemId: undefined,
    isPublic: false
  }
  dialogVisible.value = true
}

// 编辑笔记
const editNote = (note: LearningNote) => {
  isEdit.value = true
  noteForm.value = {
    id: note.id,
    title: note.title,
    content: note.content,
    problemId: note.problemId || undefined,
    isPublic: !!note.isPublic
  }
  dialogVisible.value = true
}

// 查看笔记
const viewNote = (note: LearningNote) => {
  currentNote.value = note
  viewDialogVisible.value = true
}

// 提交笔记
const handleSubmit = async () => {
  if (!noteForm.value.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!noteForm.value.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  
  const userId = getUserId()
  if (!userId) return
  
  submitting.value = true
  try {
    if (isEdit.value) {
      const data: UpdateNoteRequest = {
        id: noteForm.value.id,
        title: noteForm.value.title,
        content: noteForm.value.content,
        isPublic: noteForm.value.isPublic ? 1 : 0
      }
      await updateNote(data)
      ElMessage.success('保存成功')
    } else {
      const data: CreateNoteRequest = {
        userId,
        problemId: noteForm.value.problemId || 0,
        title: noteForm.value.title,
        content: noteForm.value.content,
        isPublic: noteForm.value.isPublic ? 1 : 0
      }
      await createNote(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadNotes()
  } catch (error) {
    ElMessage.error(isEdit.value ? '保存失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

// 删除笔记
const handleDelete = async (note: LearningNote) => {
  try {
    await ElMessageBox.confirm('确定删除此笔记吗？', '警告', {
      type: 'warning'
    })
    await deleteNote(note.id)
    ElMessage.success('删除成功')
    loadNotes()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 截断内容
const truncateContent = (content: string) => {
  if (content.length > 100) {
    return content.substring(0, 100) + '...'
  }
  return content
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 简单的 Markdown 渲染（实际项目中可使用 marked 等库）
const renderMarkdown = (content: string) => {
  return content
    .replace(/\n/g, '<br>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
}

// 加载公开笔记
const loadPublicNotes = async () => {
  loadingPublic.value = true
  try {
    publicNotes.value = await getAllPublicNotes(publicPage.value, 20)
  } catch (error) {
    console.error('加载公开笔记失败:', error)
    ElMessage.error('加载公开笔记失败')
  } finally {
    loadingPublic.value = false
  }
}

// 查看公开笔记详情
const viewPublicNote = async (note: LearningNote) => {
  try {
    const detail = await viewNoteApi(note.id)
    currentNote.value = detail
    viewDialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载笔记详情失败')
  }
}

// 打开笔记广场时加载数据
const openNoteSquare = () => {
  publicPage.value = 1
  publicNotes.value = []
  showNoteSquare.value = true
  loadPublicNotes()
}

// 加载更多公开笔记
const loadMorePublicNotes = async () => {
  publicPage.value++
  loadingPublic.value = true
  try {
    const moreNotes = await getAllPublicNotes(publicPage.value, 20)
    publicNotes.value = [...publicNotes.value, ...moreNotes]
  } catch (error) {
    console.error('加载更多笔记失败:', error)
  } finally {
    loadingPublic.value = false
  }
}

onMounted(() => {
  loadNotes()
  loadProblemList()
})
</script>

<style scoped lang="scss">
.learning-notes-page {
  padding: 20px;
  max-width: 1200px;
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
  
  .header-actions {
    display: flex;
    align-items: center;
  }
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.note-card {
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  
  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  
  .note-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .note-title {
    font-weight: 600;
    font-size: 16px;
    color: #303133;
  }
  
  .note-content {
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
    min-height: 60px;
    flex: 1;
  }
  
  .note-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 12px;
    font-size: 12px;
    color: #909399;
  }
  
  .note-actions {
    margin-top: auto;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}

.switch-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}

.note-view {
  .note-meta {
    display: flex;
    gap: 20px;
    margin-bottom: 16px;
    font-size: 14px;
    color: #909399;
    align-items: center;
    flex-wrap: wrap;
    
    .author-info {
      margin-right: 8px;
    }
    
    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
  
  .note-body {
    line-height: 1.8;
    font-size: 15px;
    
    :deep(code) {
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 4px;
      font-family: monospace;
    }
  }
}

// 笔记广场样式
.note-square {
  min-height: 400px;
  
  .public-notes-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }
  
  .public-note-card {
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .public-note-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      
      .public-note-title {
        font-weight: 600;
        font-size: 14px;
        color: #303133;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
        margin-right: 8px;
      }
    }
    
    .public-note-preview {
      color: #606266;
      font-size: 13px;
      line-height: 1.5;
      min-height: 40px;
      margin-bottom: 8px;
    }
    
    .public-note-footer {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #909399;
      
      span {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
  
  .load-more {
    text-align: center;
    margin-top: 20px;
  }
}
</style>
