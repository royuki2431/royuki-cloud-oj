<template>
    <div class="note-manage">
        <el-page-header content="笔记管理" />
        
        <el-card class="toolbar" shadow="never">
            <div class="toolbar-content">
                <div class="search-area">
                    <el-input 
                        v-model="searchParams.keyword" 
                        placeholder="搜索笔记标题或内容" 
                        style="width: 300px;" 
                        clearable
                        @clear="handleSearch"
                        @keyup.enter="handleSearch"
                    >
                        <template #append>
                            <el-button icon="Search" @click="handleSearch" />
                        </template>
                    </el-input>
                    
                    <el-select 
                        v-model="searchParams.isPublic" 
                        placeholder="公开状态" 
                        clearable 
                        style="width: 150px; margin-left: 10px;"
                        @change="handleSearch"
                    >
                        <el-option label="全部" :value="null" />
                        <el-option label="公开" :value="1" />
                        <el-option label="私密" :value="0" />
                    </el-select>
                </div>
                
                <el-button @click="handleSearch">
                    <el-icon><Refresh /></el-icon>
                    刷新
                </el-button>
            </div>
        </el-card>

        <el-card class="note-list" shadow="hover">
            <el-table :data="notes" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
                <el-table-column prop="authorName" label="作者" width="120">
                    <template #default="{ row }">
                        {{ row.authorName || '未知用户' }}
                    </template>
                </el-table-column>
                <el-table-column prop="problemTitle" label="关联题目" width="160" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span v-if="row.problemId && row.problemId > 0">
                            {{ row.problemTitle || `题目 #${row.problemId}` }}
                        </span>
                        <span v-else class="text-muted">无关联</span>
                    </template>
                </el-table-column>
                <el-table-column prop="isPublic" label="公开状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.isPublic === 1 ? 'success' : 'info'">
                            {{ row.isPublic === 1 ? '公开' : '私密' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="viewCount" label="浏览量" width="100">
                    <template #default="{ row }">
                        <el-icon><View /></el-icon>
                        {{ row.viewCount || 0 }}
                    </template>
                </el-table-column>
                <el-table-column prop="createdTime" label="创建时间" width="180" />
                <el-table-column prop="updatedTime" label="更新时间" width="180" />
                <el-table-column label="操作" width="220" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleView(row)">
                            查看
                        </el-button>
                        <el-button 
                            :type="row.isPublic === 1 ? 'warning' : 'success'" 
                            size="small" 
                            @click="handleTogglePublic(row)"
                        >
                            {{ row.isPublic === 1 ? '设为私密' : '设为公开' }}
                        </el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)">
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination">
                <el-pagination
                    v-model:current-page="pagination.page"
                    v-model:page-size="pagination.size"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="pagination.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="loadNotes"
                    @current-change="loadNotes"
                />
            </div>
        </el-card>

        <!-- 笔记详情对话框 -->
        <el-dialog v-model="viewDialogVisible" title="笔记详情" width="800px">
            <div v-if="currentNote" class="note-detail">
                <div class="note-header">
                    <h2>{{ currentNote.title }}</h2>
                    <div class="note-meta">
                        <span><el-icon><User /></el-icon> {{ currentNote.authorName || '未知用户' }}</span>
                        <span><el-icon><Clock /></el-icon> {{ currentNote.updatedTime }}</span>
                        <span><el-icon><View /></el-icon> {{ currentNote.viewCount || 0 }} 次浏览</span>
                        <el-tag :type="currentNote.isPublic === 1 ? 'success' : 'info'" size="small">
                            {{ currentNote.isPublic === 1 ? '公开' : '私密' }}
                        </el-tag>
                    </div>
                    <div v-if="currentNote.problemId && currentNote.problemId > 0" class="note-problem">
                        <el-tag type="primary">
                            关联题目：{{ currentNote.problemTitle || `题目 #${currentNote.problemId}` }}
                        </el-tag>
                    </div>
                </div>
                <el-divider />
                <div class="note-content" v-html="renderMarkdown(currentNote.content)"></div>
            </div>
            <template #footer>
                <el-button @click="viewDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View, User, Clock } from '@element-plus/icons-vue'
import { 
    adminGetAllNotes, 
    adminDeleteNote, 
    adminUpdateNotePublicStatus,
    type LearningNote 
} from '@/api/learning'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt()

const loading = ref(false)
const notes = ref<LearningNote[]>([])
const viewDialogVisible = ref(false)
const currentNote = ref<LearningNote | null>(null)

const searchParams = reactive({
    keyword: '',
    isPublic: null as number | null
})

const pagination = reactive({
    page: 1,
    size: 10,
    total: 0
})

// 加载笔记列表
const loadNotes = async () => {
    loading.value = true
    try {
        const result = await adminGetAllNotes({
            page: pagination.page,
            size: pagination.size,
            keyword: searchParams.keyword || undefined,
            isPublic: searchParams.isPublic
        })
        notes.value = result.list || []
        pagination.total = result.total || 0
    } catch (error: any) {
        ElMessage.error(error.message || '加载笔记列表失败')
    } finally {
        loading.value = false
    }
}

// 搜索
const handleSearch = () => {
    pagination.page = 1
    loadNotes()
}

// 查看笔记
const handleView = (note: LearningNote) => {
    currentNote.value = note
    viewDialogVisible.value = true
}

// 切换公开状态
const handleTogglePublic = async (note: LearningNote) => {
    const newStatus = note.isPublic === 1 ? 0 : 1
    const statusText = newStatus === 1 ? '公开' : '私密'
    
    try {
        await ElMessageBox.confirm(
            `确定要将此笔记设为${statusText}吗？`,
            '确认操作',
            { type: 'warning' }
        )
        
        await adminUpdateNotePublicStatus(note.id, newStatus)
        ElMessage.success(`已设为${statusText}`)
        loadNotes()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

// 删除笔记
const handleDelete = async (note: LearningNote) => {
    try {
        await ElMessageBox.confirm(
            `确定要删除笔记"${note.title}"吗？此操作不可恢复！`,
            '警告',
            { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
        )
        
        await adminDeleteNote(note.id)
        ElMessage.success('删除成功')
        loadNotes()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '删除失败')
        }
    }
}

// 渲染 Markdown
const renderMarkdown = (content: string) => {
    return md.render(content || '')
}

onMounted(() => {
    loadNotes()
})
</script>

<style scoped>
.note-manage {
    padding: 20px;
}

.toolbar {
    margin-top: 20px;
}

.toolbar-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.search-area {
    display: flex;
    align-items: center;
}

.note-list {
    margin-top: 20px;
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.text-muted {
    color: #909399;
}

.note-detail {
    max-height: 60vh;
    overflow-y: auto;
}

.note-header h2 {
    margin: 0 0 12px 0;
    font-size: 20px;
}

.note-meta {
    display: flex;
    gap: 20px;
    color: #909399;
    font-size: 14px;
    align-items: center;
}

.note-meta span {
    display: flex;
    align-items: center;
    gap: 4px;
}

.note-problem {
    margin-top: 12px;
}

.note-content {
    line-height: 1.8;
}

.note-content :deep(pre) {
    background: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    overflow-x: auto;
}

.note-content :deep(code) {
    background: #f5f7fa;
    padding: 2px 6px;
    border-radius: 3px;
    font-family: 'Consolas', 'Monaco', monospace;
}

.note-content :deep(blockquote) {
    border-left: 4px solid #409eff;
    padding-left: 16px;
    margin-left: 0;
    color: #606266;
}
</style>
