<template>
    <div class="user-manage">
        <el-page-header content="用户管理" />
        
        <el-card class="toolbar" shadow="never">
            <div class="toolbar-content">
                <div class="search-area">
                    <el-input 
                        v-model="searchParams.keyword" 
                        placeholder="搜索用户名或邮箱" 
                        style="width: 300px;" 
                        clearable
                        @clear="handleSearch"
                    >
                        <template #append>
                            <el-button icon="Search" @click="handleSearch" />
                        </template>
                    </el-input>
                    
                    <el-select 
                        v-model="searchParams.role" 
                        placeholder="角色筛选" 
                        clearable 
                        style="width: 150px; margin-left: 10px;"
                        @change="handleSearch"
                    >
                        <el-option label="全部" value="" />
                        <el-option label="学生" :value="UserRole.STUDENT" />
                        <el-option label="教师" :value="UserRole.TEACHER" />
                        <el-option label="管理员" :value="UserRole.ADMIN" />
                    </el-select>

                    <el-select 
                        v-model="searchParams.status" 
                        placeholder="状态筛选" 
                        clearable 
                        style="width: 150px; margin-left: 10px;"
                        @change="handleSearch"
                    >
                        <el-option label="全部" value="" />
                        <el-option label="正常" :value="1" />
                        <el-option label="禁用" :value="0" />
                    </el-select>
                </div>

                <el-button type="primary" @click="showCreateDialog = true">
                    <el-icon><Plus /></el-icon>
                    创建用户
                </el-button>
            </div>
        </el-card>

        <el-card class="user-list" shadow="hover">
            <el-table :data="users" stripe v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="username" label="用户名" width="120" />
                <el-table-column prop="email" label="邮箱" width="200" />
                <el-table-column prop="realName" label="真实姓名" width="120" />
                <el-table-column prop="role" label="角色" width="100">
                    <template #default="{ row }">
                        <el-tag :type="getRoleType(row.role)">
                            {{ getRoleText(row.role) }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                            {{ row.status === 1 ? '正常' : '禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="school" label="学校" width="150" show-overflow-tooltip />
                <el-table-column prop="createdTime" label="注册时间" width="180" />
                <el-table-column label="操作" width="280" fixed="right">
                    <template #default="{ row }">
                        <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                        <el-button 
                            :type="row.status === 1 ? 'warning' : 'success'" 
                            size="small" 
                            @click="toggleStatus(row)"
                        >
                            {{ row.status === 1 ? '禁用' : '启用' }}
                        </el-button>
                        <el-button type="info" size="small" @click="resetPassword(row)">重置密码</el-button>
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

        <!-- 创建/编辑用户对话框 -->
        <el-dialog 
            v-model="showCreateDialog" 
            :title="editingUser ? '编辑用户' : '创建用户'" 
            width="600px"
        >
            <el-form :model="userForm" :rules="userRules" ref="formRef" label-width="100px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="userForm.username" :disabled="!!editingUser" />
                </el-form-item>
                <el-form-item label="邮箱" prop="email">
                    <el-input v-model="userForm.email" />
                </el-form-item>
                <el-form-item label="密码" prop="password" v-if="!editingUser">
                    <el-input v-model="userForm.password" type="password" show-password />
                </el-form-item>
                <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="userForm.realName" />
                </el-form-item>
                <el-form-item label="角色" prop="role">
                    <el-select v-model="userForm.role" style="width: 100%;">
                        <el-option label="学生" :value="UserRole.STUDENT" />
                        <el-option label="教师" :value="UserRole.TEACHER" />
                        <el-option label="管理员" :value="UserRole.ADMIN" />
                    </el-select>
                </el-form-item>
                <el-form-item label="学校" prop="school">
                    <el-input v-model="userForm.school" />
                </el-form-item>
                <el-form-item label="学号/工号" prop="studentId">
                    <el-input v-model="userForm.studentId" />
                </el-form-item>
                <el-form-item label="专业" prop="major">
                    <el-input v-model="userForm.major" />
                </el-form-item>
                <el-form-item label="状态" prop="status">
                    <el-radio-group v-model="userForm.status">
                        <el-radio :label="1">正常</el-radio>
                        <el-radio :label="0">禁用</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="showCreateDialog = false">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="submitting">
                    {{ editingUser ? '更新' : '创建' }}
                </el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { User } from '@/types'
import { UserRole, UserRoleText } from '@/types'
import { 
    getUserList, 
    createUser, 
    updateUser, 
    updateUserStatus, 
    resetUserPassword, 
    deleteUser 
} from '@/api/admin'

const users = ref<User[]>([])
const loading = ref(false)
const showCreateDialog = ref(false)
const editingUser = ref<User | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 搜索参数
const searchParams = reactive({
    keyword: '',
    role: '',
    status: '' as number | string,
})

// 分页参数
const pagination = reactive({
    page: 1,
    size: 10,
    total: 0,
})

// 用户表单
const userForm = reactive({
    username: '',
    email: '',
    password: '',
    realName: '',
    role: UserRole.STUDENT,
    school: '',
    studentId: '',
    major: '',
    status: 1,
})

// 表单验证规则
const userRules: FormRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' },
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码至少6位', trigger: 'blur' },
    ],
    role: [
        { required: true, message: '请选择角色', trigger: 'change' },
    ],
}

const getRoleText = (role: UserRole) => {
    return UserRoleText[role] || role
}

const getRoleType = (role: UserRole) => {
    switch (role) {
        case UserRole.ADMIN:
            return 'danger'
        case UserRole.TEACHER:
            return 'warning'
        default:
            return 'success'
    }
}

// 加载用户列表
const loadUsers = async () => {
    loading.value = true
    try {
        const response = await getUserList({
            keyword: searchParams.keyword,
            role: searchParams.role,
            status: searchParams.status,
            page: pagination.page,
            size: pagination.size,
        })
        users.value = response.list
        pagination.total = response.total
    } catch (error: any) {
        ElMessage.error(error.message || '加载用户列表失败')
        // 如果API调用失败，显示空列表
        users.value = []
        pagination.total = 0
    } finally {
        loading.value = false
    }
}

// 搜索
const handleSearch = () => {
    pagination.page = 1
    loadUsers()
}

// 重置表单
const resetForm = () => {
    userForm.username = ''
    userForm.email = ''
    userForm.password = ''
    userForm.realName = ''
    userForm.role = UserRole.STUDENT
    userForm.school = ''
    userForm.studentId = ''
    userForm.major = ''
    userForm.status = 1
    editingUser.value = null
    formRef.value?.resetFields()
}

// 编辑用户
const handleEdit = (user: User) => {
    editingUser.value = user
    userForm.username = user.username
    userForm.email = user.email
    userForm.realName = user.realName || ''
    userForm.role = user.role
    userForm.school = user.school || ''
    userForm.studentId = user.studentId || ''
    userForm.major = user.major || ''
    userForm.status = user.status || 1
    showCreateDialog.value = true
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return
    
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        
        submitting.value = true
        try {
            if (editingUser.value) {
                // 调用更新API
                await updateUser(editingUser.value.id, {
                    email: userForm.email,
                    realName: userForm.realName,
                    role: userForm.role,
                    school: userForm.school,
                    studentId: userForm.studentId,
                    major: userForm.major,
                    status: userForm.status,
                })
                ElMessage.success('更新用户成功')
            } else {
                // 调用创建API
                await createUser({
                    username: userForm.username,
                    email: userForm.email,
                    password: userForm.password,
                    realName: userForm.realName,
                    role: userForm.role,
                    school: userForm.school,
                    studentId: userForm.studentId,
                    major: userForm.major,
                    status: userForm.status,
                })
                ElMessage.success('创建用户成功')
            }
            showCreateDialog.value = false
            resetForm()
            await loadUsers()
        } catch (error: any) {
            ElMessage.error(error.message || '操作失败')
        } finally {
            submitting.value = false
        }
    })
}

// 切换状态
const toggleStatus = async (user: User) => {
    try {
        await ElMessageBox.confirm(
            `确定要${user.status === 1 ? '禁用' : '启用'}用户 ${user.username} 吗？`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        
        // 调用API
        await updateUserStatus(user.id, user.status === 1 ? 0 : 1)
        ElMessage.success('状态更新成功')
        await loadUsers()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

// 重置密码
const resetPassword = async (user: User) => {
    try {
        await ElMessageBox.confirm(
            `确定要重置用户 ${user.username} 的密码吗？\n重置后密码为：123456`,
            '提示',
            {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        
        // 调用API
        await resetUserPassword(user.id)
        ElMessage.success('密码已重置为：123456')
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

// 删除用户
const handleDelete = async (user: User) => {
    try {
        await ElMessageBox.confirm(
            `确定要删除用户 ${user.username} 吗？此操作不可恢复！`,
            '警告',
            {
                confirmButtonText: '确定删除',
                cancelButtonText: '取消',
                type: 'error',
            }
        )
        
        // 调用API
        await deleteUser(user.id)
        ElMessage.success('删除成功')
        await loadUsers()
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

onMounted(() => {
    loadUsers()
    
    // 监听对话框关闭事件
    showCreateDialog.value = false
})
</script>

<style scoped>
.user-manage {
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

.user-list {
    margin-top: 20px;
}
</style>
