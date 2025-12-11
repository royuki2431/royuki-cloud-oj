<template>
    <div class="user-debug">
        <el-card>
            <h2>用户信息调试</h2>
            
            <el-descriptions :column="2" border>
                <el-descriptions-item label="是否登录">
                    <el-tag :type="userStore.isLoggedIn ? 'success' : 'danger'">
                        {{ userStore.isLoggedIn ? '已登录' : '未登录' }}
                    </el-tag>
                </el-descriptions-item>
                
                <el-descriptions-item label="Token">
                    {{ userStore.token ? '存在' : '不存在' }}
                </el-descriptions-item>
                
                <el-descriptions-item label="用户ID">
                    {{ userStore.userInfo?.id || '无' }}
                </el-descriptions-item>
                
                <el-descriptions-item label="用户名">
                    {{ userStore.userInfo?.username || '无' }}
                </el-descriptions-item>
                
                <el-descriptions-item label="邮箱">
                    {{ userStore.userInfo?.email || '无' }}
                </el-descriptions-item>
                
                <el-descriptions-item label="角色（原始值）">
                    <el-tag>{{ userStore.userInfo?.role || '无' }}</el-tag>
                </el-descriptions-item>
                
                <el-descriptions-item label="角色（计算属性）">
                    <el-tag>{{ userStore.userRole || '无' }}</el-tag>
                </el-descriptions-item>
                
                <el-descriptions-item label="是否学生">
                    <el-tag :type="userStore.isStudent ? 'success' : 'info'">
                        {{ userStore.isStudent }}
                    </el-tag>
                </el-descriptions-item>
                
                <el-descriptions-item label="是否教师">
                    <el-tag :type="userStore.isTeacher ? 'warning' : 'info'">
                        {{ userStore.isTeacher }}
                    </el-tag>
                </el-descriptions-item>
                
                <el-descriptions-item label="是否管理员">
                    <el-tag :type="userStore.isAdmin ? 'danger' : 'info'">
                        {{ userStore.isAdmin }}
                    </el-tag>
                </el-descriptions-item>
            </el-descriptions>

            <el-divider />

            <h3>完整用户信息（JSON）</h3>
            <el-input
                v-model="userInfoJson"
                type="textarea"
                :rows="10"
                readonly
            />

            <el-divider />

            <h3>LocalStorage</h3>
            <el-descriptions :column="1" border>
                <el-descriptions-item label="token">
                    {{ localStorage.getItem('token') || '无' }}
                </el-descriptions-item>
                <el-descriptions-item label="userId">
                    {{ localStorage.getItem('userId') || '无' }}
                </el-descriptions-item>
                <el-descriptions-item label="userInfo">
                    {{ localStorage.getItem('userInfo') || '无' }}
                </el-descriptions-item>
            </el-descriptions>

            <el-divider />

            <el-button type="primary" @click="refreshUserInfo">刷新用户信息</el-button>
            <el-button type="warning" @click="clearStorage">清除本地存储</el-button>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const userInfoJson = computed(() => {
    return JSON.stringify(userStore.userInfo, null, 2)
})

const refreshUserInfo = async () => {
    const userId = Number(localStorage.getItem('userId'))
    if (userId) {
        await userStore.loadUserInfo(userId)
        ElMessage.success('用户信息已刷新')
    } else {
        ElMessage.warning('没有找到用户ID')
    }
}

const clearStorage = () => {
    localStorage.clear()
    ElMessage.success('本地存储已清除，请重新登录')
    location.reload()
}

onMounted(() => {
    console.log('用户信息:', userStore.userInfo)
    console.log('用户角色:', userStore.userRole)
    console.log('是否学生:', userStore.isStudent)
    console.log('是否教师:', userStore.isTeacher)
    console.log('是否管理员:', userStore.isAdmin)
})
</script>

<style scoped>
.user-debug {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

h2, h3 {
    margin: 16px 0;
}
</style>
