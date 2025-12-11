<template>
    <div class="problem-manage">
        <el-page-header content="题目管理" />
        
        <el-card class="toolbar" shadow="never">
            <div class="toolbar-content">
                <div class="search-area">
                    <el-input 
                        v-model="searchParams.keyword" 
                        placeholder="搜索题目标题" 
                        style="width: 300px;" 
                        clearable
                        @clear="handleSearch"
                    >
                        <template #append>
                            <el-button icon="Search" @click="handleSearch" />
                        </template>
                    </el-input>
                    
                    <el-select 
                        v-model="searchParams.difficulty" 
                        placeholder="难度筛选" 
                        clearable 
                        style="width: 150px; margin-left: 10px;"
                        @change="handleSearch"
                    >
                        <el-option label="全部" value="" />
                        <el-option label="简单" value="EASY" />
                        <el-option label="中等" value="MEDIUM" />
                        <el-option label="困难" value="HARD" />
                    </el-select>

                    <el-select 
                        v-model="searchParams.category" 
                        placeholder="分类筛选" 
                        clearable 
                        style="width: 150px; margin-left: 10px;"
                        @change="handleSearch"
                    >
                        <el-option label="全部" value="" />
                        <el-option label="算法" value="算法" />
                        <el-option label="数据结构" value="数据结构" />
                        <el-option label="数学" value="数学" />
                        <el-option label="字符串" value="字符串" />
                        <el-option label="动态规划" value="动态规划" />
                        <el-option label="图论" value="图论" />
                    </el-select>

                    <el-select 
                        v-model="searchParams.status" 
                        placeholder="状态筛选" 
                        clearable 
                        style="width: 150px; margin-left: 10px;"
                        @change="handleSearch"
                    >
                        <el-option label="全部" value="" />
                        <el-option label="公开" :value="1" />
                        <el-option label="隐藏" :value="0" />
                    </el-select>
                </div>

                <el-button type="primary" @click="handleCreate">
                    <el-icon><Plus /></el-icon>
                    创建题目
                </el-button>
            </div>
        </el-card>

        <el-card class="problem-list" shadow="hover">
            <el-table :data="problems" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="title" label="题目标题" min-width="200" show-overflow-tooltip />
                <el-table-column prop="difficulty" label="难度" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getDifficultyType(row.difficulty)">
                            {{ getDifficultyText(row.difficulty) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="category" label="分类" width="120" show-overflow-tooltip />
                <el-table-column prop="tags" label="标签" width="200">
                    <template #default="{ row }">
                        <el-tag v-for="tag in parseTags(row.tags)?.slice(0, 3)" :key="tag" size="small" style="margin-right: 5px;">
                            {{ tag }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="通过率" width="100">
                    <template #default="{ row }">
                        {{ getAcceptRate(row) }}%
                    </template>
                </el-table-column>
                <el-table-column prop="acceptCount" label="通过数" width="100" />
                <el-table-column prop="submitCount" label="提交数" width="100" />
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'info'">
                            {{ row.status === 1 ? '公开' : '隐藏' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="280" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button type="info" size="small" @click="handleTestCases(row)">测试用例</el-button>
                        <el-button 
                            :type="row.status === 1 ? 'warning' : 'success'" 
                            size="small" 
                            @click="toggleStatus(row)"
                        >
                            {{ row.status === 1 ? '隐藏' : '公开' }}
                        </el-button>
                        <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSearch"
                @current-change="handleSearch"
                style="margin-top: 20px; justify-content: flex-end;"
            />
        </el-card>

        <!-- 测试用例管理对话框 -->
        <el-dialog 
            v-model="showTestCaseDialog" 
            :title="`测试用例管理 - ${currentProblem?.title || ''}`" 
            width="900px"
            :close-on-click-modal="false"
        >
            <div class="testcase-header">
                <el-alert type="info" :closable="false" show-icon>
                    <template #title>
                        <span>共 {{ testCases.length }} 个测试用例，总分 {{ totalScore }} 分</span>
                        <span style="margin-left: 20px;">样例数量：{{ sampleCount }} 个（用户可见）</span>
                    </template>
                </el-alert>
                <el-button type="primary" @click="addTestCase" style="margin-top: 10px;">
                    <el-icon><Plus /></el-icon>
                    添加测试用例
                </el-button>
            </div>
            
            <el-table :data="testCases" stripe style="margin-top: 15px;" max-height="400">
                <el-table-column label="序号" width="70">
                    <template #default="{ $index }">
                        {{ $index + 1 }}
                    </template>
                </el-table-column>
                <el-table-column label="输入" min-width="200">
                    <template #default="{ row }">
                        <el-input 
                            v-model="row.input" 
                            type="textarea" 
                            :rows="2" 
                            placeholder="输入数据"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="预期输出" min-width="200">
                    <template #default="{ row }">
                        <el-input 
                            v-model="row.output" 
                            type="textarea" 
                            :rows="2" 
                            placeholder="预期输出"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="分值" width="100">
                    <template #default="{ row }">
                        <el-input-number 
                            v-model="row.score" 
                            :min="1" 
                            :max="100" 
                            size="small"
                            style="width: 80px;"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="样例" width="80">
                    <template #default="{ row }">
                        <el-switch 
                            v-model="row.isSample" 
                            :active-value="1" 
                            :inactive-value="0"
                        />
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="100">
                    <template #default="{ $index }">
                        <el-button type="danger" size="small" @click="removeTestCase($index)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            
            <template #footer>
                <el-button @click="showTestCaseDialog = false">取消</el-button>
                <el-button type="primary" @click="handleSaveTestCases" :loading="savingTestCases">
                    保存测试用例
                </el-button>
            </template>
        </el-dialog>

        <!-- 创建/编辑题目对话框 -->
        <el-dialog 
            v-model="showDialog" 
            :title="editingProblem ? '编辑题目' : '创建题目'" 
            width="900px"
            :close-on-click-modal="false"
        >
            <el-form :model="problemForm" :rules="problemRules" ref="formRef" label-width="120px">
                <el-tabs v-model="activeTab">
                    <el-tab-pane label="基本信息" name="basic">
                        <el-form-item label="题目标题" prop="title">
                            <el-input v-model="problemForm.title" placeholder="请输入题目标题" />
                        </el-form-item>
                        
                        <el-form-item label="难度" prop="difficulty">
                            <el-select v-model="problemForm.difficulty" style="width: 100%;">
                                <el-option label="简单" value="EASY" />
                                <el-option label="中等" value="MEDIUM" />
                                <el-option label="困难" value="HARD" />
                            </el-select>
                        </el-form-item>

                        <el-form-item label="分类" prop="category">
                            <el-select v-model="problemForm.category" style="width: 100%;" allow-create filterable>
                                <el-option label="算法" value="算法" />
                                <el-option label="数据结构" value="数据结构" />
                                <el-option label="数学" value="数学" />
                                <el-option label="字符串" value="字符串" />
                                <el-option label="动态规划" value="动态规划" />
                                <el-option label="图论" value="图论" />
                            </el-select>
                        </el-form-item>

                        <el-form-item label="标签" prop="tags">
                            <el-select 
                                v-model="problemForm.tags" 
                                multiple 
                                filterable 
                                allow-create 
                                placeholder="请选择或输入标签"
                                style="width: 100%;"
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

                        <el-form-item label="状态" prop="status">
                            <el-radio-group v-model="problemForm.status">
                                <el-radio :label="1">公开</el-radio>
                                <el-radio :label="0">隐藏</el-radio>
                            </el-radio-group>
                        </el-form-item>
                    </el-tab-pane>

                    <el-tab-pane label="题目描述" name="description">
                        <el-form-item label="题目描述" prop="description">
                            <el-input 
                                v-model="problemForm.description" 
                                type="textarea" 
                                :rows="10" 
                                placeholder="请输入题目描述"
                            />
                        </el-form-item>

                        <el-form-item label="输入格式">
                            <el-input 
                                v-model="problemForm.inputFormat" 
                                type="textarea" 
                                :rows="3" 
                                placeholder="请描述输入格式"
                            />
                        </el-form-item>

                        <el-form-item label="输出格式">
                            <el-input 
                                v-model="problemForm.outputFormat" 
                                type="textarea" 
                                :rows="3" 
                                placeholder="请描述输出格式"
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
                                :rows="3" 
                                placeholder="请输入提示信息（可选）"
                            />
                        </el-form-item>
                    </el-tab-pane>

                    <el-tab-pane label="限制条件" name="limits">
                        <el-form-item label="时间限制" prop="timeLimit">
                            <el-input-number v-model="problemForm.timeLimit" :min="100" :max="10000" :step="100" />
                            <span style="margin-left: 10px;">毫秒</span>
                        </el-form-item>

                        <el-form-item label="内存限制" prop="memoryLimit">
                            <el-input-number v-model="problemForm.memoryLimit" :min="64" :max="512" :step="64" />
                            <span style="margin-left: 10px;">MB</span>
                        </el-form-item>
                    </el-tab-pane>
                </el-tabs>
            </el-form>
            <template #footer>
                <el-button @click="showDialog = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">
                    {{ editingProblem ? '更新' : '创建' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Problem } from '@/types'
import { DifficultyText, DifficultyColor } from '@/types'
import { 
    getProblemListAdmin, 
    createProblem, 
    updateProblem, 
    updateProblemStatus, 
    deleteProblem,
    getTestCases,
    saveTestCases,
    type TestCaseItem
} from '@/api/admin'

const problems = ref<Problem[]>([])
const loading = ref(false)
const showDialog = ref(false)
const editingProblem = ref<Problem | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const activeTab = ref('basic')

// 测试用例管理
const showTestCaseDialog = ref(false)
const currentProblem = ref<Problem | null>(null)
const testCases = ref<TestCaseItem[]>([])
const savingTestCases = ref(false)

// 计算属性
const totalScore = computed(() => {
    return testCases.value.reduce((sum, tc) => sum + (tc.score || 0), 0)
})

const sampleCount = computed(() => {
    return testCases.value.filter(tc => tc.isSample === 1).length
})

// 搜索参数
const searchParams = reactive({
    keyword: '',
    difficulty: '',
    category: '',
    status: '' as number | string,
})

// 分页参数
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0,
})

// 题目表单
const problemForm = reactive({
    title: '',
    difficulty: 'EASY',
    category: '',
    tags: [] as string[],
    description: '',
    inputFormat: '',
    outputFormat: '',
    sampleInput: '',
    sampleOutput: '',
    hint: '',
    timeLimit: 1000,
    memoryLimit: 256,
    status: 1,
})

// 表单验证规则
const problemRules: FormRules = {
    title: [
        { required: true, message: '请输入题目标题', trigger: 'blur' },
        { min: 3, max: 100, message: '题目标题长度在3-100个字符', trigger: 'blur' },
    ],
    difficulty: [
        { required: true, message: '请选择难度', trigger: 'change' },
    ],
    description: [
        { required: true, message: '请输入题目描述', trigger: 'blur' },
    ],
    timeLimit: [
        { required: true, message: '请设置时间限制', trigger: 'blur' },
    ],
    memoryLimit: [
        { required: true, message: '请设置内存限制', trigger: 'blur' },
    ],
}

const getDifficultyText = (difficulty: string) => {
    return DifficultyText[difficulty] || difficulty
}

const getDifficultyType = (difficulty: string) => {
    return DifficultyColor[difficulty] as any || 'info'
}

// 计算通过率
const getAcceptRate = (problem: Problem) => {
    if (!problem.submitCount || problem.submitCount === 0) return 0
    return ((problem.acceptCount || 0) / problem.submitCount * 100).toFixed(1)
}

// 解析tags JSON字符串为数组
const parseTags = (tags: string | string[] | undefined): string[] => {
    if (!tags) return []
    if (Array.isArray(tags)) return tags
    try {
        return JSON.parse(tags)
    } catch (e) {
        return []
    }
}

// 加载题目列表
const loadProblems = async () => {
    loading.value = true
    try {
        const response = await getProblemListAdmin({
            keyword: searchParams.keyword,
            difficulty: searchParams.difficulty,
            category: searchParams.category,
            status: searchParams.status,
            page: pagination.page,
            size: pagination.size,
        })
        // 解析tags字段
        problems.value = response.list.map(problem => ({
            ...problem,
            tags: parseTags(problem.tags)
        }))
        pagination.total = response.total
    } catch (error: any) {
        ElMessage.error(error.message || '加载题目列表失败')
        // 如果API调用失败，显示空列表
        problems.value = []
        pagination.total = 0
    } finally {
        loading.value = false
    }
}

// 搜索
const handleSearch = () => {
    pagination.page = 1
    loadProblems()
}

// 重置表单
const resetForm = () => {
    problemForm.title = ''
    problemForm.difficulty = 'EASY'
    problemForm.category = ''
    problemForm.tags = []
    problemForm.description = ''
    problemForm.inputFormat = ''
    problemForm.outputFormat = ''
    problemForm.sampleInput = ''
    problemForm.sampleOutput = ''
    problemForm.hint = ''
    problemForm.timeLimit = 1000
    problemForm.memoryLimit = 256
    problemForm.status = 1
    editingProblem.value = null
    activeTab.value = 'basic'
    formRef.value?.resetFields()
}

// 创建题目
const handleCreate = () => {
    resetForm()
    showDialog.value = true
}

// 编辑题目
const handleEdit = (problem: Problem) => {
    editingProblem.value = problem
    problemForm.title = problem.title
    problemForm.difficulty = problem.difficulty
    problemForm.category = problem.category || ''
    // 解析tags JSON字符串为数组
    try {
        problemForm.tags = problem.tags ? JSON.parse(problem.tags as any) : []
    } catch (e) {
        problemForm.tags = []
    }
    problemForm.description = problem.description || ''
    problemForm.inputFormat = problem.inputFormat || ''
    problemForm.outputFormat = problem.outputFormat || ''
    problemForm.sampleInput = problem.sampleInput || ''
    problemForm.sampleOutput = problem.sampleOutput || ''
    problemForm.hint = problem.hint || ''
    problemForm.timeLimit = problem.timeLimit || 1000
    problemForm.memoryLimit = problem.memoryLimit || 256
    problemForm.status = problem.status || 1
    showDialog.value = true
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return
    
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        
        submitting.value = true
        try {
            const data = {
                title: problemForm.title,
                difficulty: problemForm.difficulty,
                category: problemForm.category,
                tags: JSON.stringify(problemForm.tags), // 将数组转换为JSON字符串
                description: problemForm.description,
                inputFormat: problemForm.inputFormat,
                outputFormat: problemForm.outputFormat,
                sampleInput: problemForm.sampleInput,
                sampleOutput: problemForm.sampleOutput,
                hint: problemForm.hint,
                timeLimit: problemForm.timeLimit,
                memoryLimit: problemForm.memoryLimit,
                status: problemForm.status,
            }
            
            if (editingProblem.value) {
                // 调用更新API
                await updateProblem(editingProblem.value.id, data)
                ElMessage.success('更新题目成功')
            } else {
                // 调用创建API
                await createProblem(data)
                ElMessage.success('创建题目成功')
            }
            showDialog.value = false
            resetForm()
            await loadProblems()
        } catch (error: any) {
            ElMessage.error(error.message || '操作失败')
        } finally {
            submitting.value = false
        }
    })
}

// 切换状态
const toggleStatus = async (problem: Problem) => {
    try {
        await ElMessageBox.confirm(
            `确定要${problem.status === 1 ? '隐藏' : '公开'}题目 “${problem.title}” 吗？`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        
        // 调用API
        await updateProblemStatus(problem.id, problem.status === 1 ? 0 : 1)
        ElMessage.success('状态更新成功')
        await loadProblems()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

// 删除题目
const handleDelete = async (problem: Problem) => {
    try {
        await ElMessageBox.confirm(
            `确定要删除题目 “${problem.title}” 吗？此操作不可恢复！`,
            '警告',
            {
                confirmButtonText: '确定删除',
                cancelButtonText: '取消',
                type: 'error',
            }
        )
        
        // 调用API
        await deleteProblem(problem.id)
        ElMessage.success('删除成功')
        await loadProblems()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

// ==================== 测试用例管理 ====================

// 打开测试用例管理对话框
const handleTestCases = async (problem: Problem) => {
    currentProblem.value = problem
    testCases.value = []
    showTestCaseDialog.value = true
    
    try {
        const data = await getTestCases(problem.id)
        testCases.value = data.map((tc, index) => ({
            ...tc,
            orderNum: tc.orderNum || index + 1
        }))
    } catch (error: any) {
        console.error('加载测试用例失败', error)
        ElMessage.warning('加载测试用例失败，请手动添加')
    }
}

// 添加测试用例
const addTestCase = () => {
    testCases.value.push({
        input: '',
        output: '',
        isSample: 0,
        score: 20,
        orderNum: testCases.value.length + 1
    })
}

// 删除测试用例
const removeTestCase = (index: number) => {
    testCases.value.splice(index, 1)
    // 重新排序
    testCases.value.forEach((tc, i) => {
        tc.orderNum = i + 1
    })
}

// 保存测试用例
const handleSaveTestCases = async () => {
    if (!currentProblem.value) return
    
    // 验证
    if (testCases.value.length === 0) {
        ElMessage.warning('请至少添加一个测试用例')
        return
    }
    
    for (let i = 0; i < testCases.value.length; i++) {
        const tc = testCases.value[i]
        if (!tc.input.trim() || !tc.output.trim()) {
            ElMessage.warning(`测试用例 ${i + 1} 的输入或输出不能为空`)
            return
        }
    }
    
    if (totalScore.value !== 100) {
        try {
            await ElMessageBox.confirm(
                `当前总分为 ${totalScore.value} 分，建议总分为 100 分。是否继续保存？`,
                '提示',
                {
                    confirmButtonText: '继续保存',
                    cancelButtonText: '返回修改',
                    type: 'warning',
                }
            )
        } catch {
            return
        }
    }
    
    savingTestCases.value = true
    try {
        await saveTestCases(currentProblem.value.id, testCases.value)
        ElMessage.success('测试用例保存成功')
        showTestCaseDialog.value = false
    } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
    } finally {
        savingTestCases.value = false
    }
}

onMounted(() => {
    loadProblems()
})
</script>

<style scoped>
.problem-manage {
    padding: 20px;
}

.toolbar {
    margin: 20px 0;
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

.problem-list {
    margin-top: 20px;
}
</style>
