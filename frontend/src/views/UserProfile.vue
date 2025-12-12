<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <el-button v-if="!isEditing" type="primary" @click="startEdit">
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
        </div>
      </template>

      <div class="profile-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <el-avatar :size="100" :src="userInfo?.avatar">
            <el-icon :size="50"><User /></el-icon>
          </el-avatar>
          <div class="avatar-info">
            <h2>{{ userInfo?.username }}</h2>
            <el-tag :type="getRoleTagType(userInfo?.role)">
              {{ getRoleText(userInfo?.role) }}
            </el-tag>
          </div>
        </div>

        <el-divider />

        <!-- 信息表单 -->
        <el-form 
          ref="formRef"
          :model="formData" 
          :rules="rules"
          label-width="100px"
          :disabled="!isEditing"
        >
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="用户名">
                <el-input v-model="formData.username" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="formData.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="学校" prop="school">
                <el-input v-model="formData.school" placeholder="请输入学校" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="学号/工号" prop="studentId">
                <el-input v-model="formData.studentId" placeholder="请输入学号或工号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="年级" prop="grade">
                <el-input v-model="formData.grade" placeholder="请输入年级，如：2024级" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="专业" prop="major">
                <el-input v-model="formData.major" placeholder="请输入专业" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item v-if="isEditing">
            <el-button type="primary" @click="saveProfile" :loading="saving">
              保存修改
            </el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <!-- 账户信息 -->
        <div class="account-info">
          <h3>账户信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户ID">
              {{ userInfo?.id }}
            </el-descriptions-item>
            <el-descriptions-item label="账户状态">
              <el-tag :type="userInfo?.status === 1 ? 'success' : 'danger'">
                {{ userInfo?.status === 1 ? '正常' : '已禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ formatDate(userInfo?.createTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              {{ formatDate(userInfo?.lastLoginTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录IP">
              {{ userInfo?.lastLoginIp || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-divider />

        <!-- 修改密码 -->
        <div class="password-section">
          <h3>修改密码</h3>
          <el-form 
            ref="passwordFormRef"
            :model="passwordForm" 
            :rules="passwordRules"
            label-width="100px"
            style="max-width: 400px;"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input 
                v-model="passwordForm.oldPassword" 
                type="password" 
                show-password
                placeholder="请输入原密码" 
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input 
                v-model="passwordForm.newPassword" 
                type="password" 
                show-password
                placeholder="请输入新密码（6-20位）" 
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input 
                v-model="passwordForm.confirmPassword" 
                type="password" 
                show-password
                placeholder="请再次输入新密码" 
              />
            </el-form-item>
            <el-form-item>
              <el-button type="warning" @click="changePassword" :loading="changingPassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, changeUserPassword } from '@/api/user'
import { UserRole, UserRoleText } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const isEditing = ref(false)
const saving = ref(false)
const changingPassword = ref(false)

const formRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()

// 表单数据
const formData = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  school: '',
  studentId: '',
  grade: '',
  major: '',
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 表单验证规则
const rules: FormRules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
}

// 密码验证规则
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
}

// 获取角色文本
const getRoleText = (role?: string) => {
  if (!role) return '未知'
  return UserRoleText[role as UserRole] || role
}

// 获取角色标签类型
const getRoleTagType = (role?: string) => {
  switch (role) {
    case UserRole.ADMIN: return 'danger'
    case UserRole.TEACHER: return 'warning'
    case UserRole.STUDENT: return 'success'
    default: return 'info'
  }
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 初始化表单数据
const initFormData = () => {
  if (userInfo.value) {
    formData.username = userInfo.value.username || ''
    formData.realName = userInfo.value.realName || ''
    formData.email = userInfo.value.email || ''
    formData.phone = userInfo.value.phone || ''
    formData.school = userInfo.value.school || ''
    formData.studentId = userInfo.value.studentId || ''
    formData.grade = userInfo.value.grade || ''
    formData.major = userInfo.value.major || ''
  }
}

// 开始编辑
const startEdit = () => {
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
  initFormData()
}

// 保存个人信息
const saveProfile = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    saving.value = true
    try {
      await updateUserInfo({
        id: userInfo.value!.id,
        realName: formData.realName,
        email: formData.email,
        phone: formData.phone,
        school: formData.school,
        studentId: formData.studentId,
        grade: formData.grade,
        major: formData.major,
      })
      
      // 刷新用户信息
      await userStore.loadUserInfo()
      
      ElMessage.success('个人信息更新成功')
      isEditing.value = false
    } catch (error: any) {
      ElMessage.error(error.message || '更新失败')
    } finally {
      saving.value = false
    }
  })
}

// 修改密码
const changePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    changingPassword.value = true
    try {
      await changeUserPassword({
        userId: userInfo.value!.id,
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      })
      
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } catch (error: any) {
      ElMessage.error(error.message || '密码修改失败')
    } finally {
      changingPassword.value = false
    }
  })
}

onMounted(() => {
  initFormData()
})
</script>

<style scoped lang="scss">
.profile-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}

.profile-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 18px;
    font-weight: bold;
  }
}

.profile-content {
  .avatar-section {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 20px 0;
    
    .avatar-info {
      h2 {
        margin: 0 0 8px 0;
        font-size: 24px;
      }
    }
  }
  
  .account-info, .password-section {
    h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #303133;
    }
  }
}
</style>
