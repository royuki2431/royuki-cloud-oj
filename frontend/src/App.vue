<template>
  <el-container class="app-container">
    <el-header v-if="!isLoginPage" class="app-header">
      <div class="header-content">
        <div class="logo">
          <el-icon class="logo-icon"><Platform /></el-icon>
          <span class="logo-text">Royuki Cloud OJ</span>
        </div>
        
        <el-menu
          :default-active="activeMenu"
          class="header-menu"
          mode="horizontal"
          router
        >
          <el-menu-item index="/problems">
            <el-icon><List /></el-icon>
            <span>题库</span>
          </el-menu-item>
          <el-menu-item index="/submissions" v-if="userStore.isLoggedIn">
            <el-icon><Document /></el-icon>
            <span>提交历史</span>
          </el-menu-item>
          <el-menu-item index="/ranking">
            <el-icon><TrophyBase /></el-icon>
            <span>排行榜</span>
          </el-menu-item>

          <!-- 学生专属菜单 -->
          <el-sub-menu index="student" v-if="userStore.isStudent">
            <template #title>
              <el-icon><Reading /></el-icon>
              <span>学生功能</span>
            </template>
            <el-menu-item index="/my-classes">我的班级</el-menu-item>
            <el-menu-item index="/my-homework">我的作业</el-menu-item>
            <el-menu-item index="/training-path">训练路径</el-menu-item>
            <el-menu-item index="/wrong-problems">错题本</el-menu-item>
            <el-menu-item index="/learning-progress">学习进度</el-menu-item>
            <el-menu-item index="/learning-stats">学习统计</el-menu-item>
            <el-menu-item index="/learning-notes">学习笔记</el-menu-item>
          </el-sub-menu>

          <!-- 教师专属菜单 -->
          <el-sub-menu index="teacher" v-if="userStore.hasMinRole(UserRole.TEACHER)">
            <template #title>
              <el-icon><Management /></el-icon>
              <span>教学管理</span>
            </template>
            <el-menu-item index="/teacher/problems">题库管理</el-menu-item>
            <el-menu-item index="/teacher/courses">课程管理</el-menu-item>
            <el-menu-item index="/teacher/classes">班级管理</el-menu-item>
            <el-menu-item index="/teacher/homework">作业管理</el-menu-item>
            <el-menu-item index="/teacher/students">学生管理</el-menu-item>
            <el-menu-item index="/teacher/analysis">提交分析</el-menu-item>
          </el-sub-menu>

          <!-- 管理员专属菜单 -->
          <el-sub-menu index="admin" v-if="userStore.isAdmin">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/admin/users">用户管理</el-menu-item>
            <el-menu-item index="/admin/problems">题目管理</el-menu-item>
            <el-menu-item index="/admin/notes">笔记管理</el-menu-item>
            <el-menu-item index="/admin/system">系统设置</el-menu-item>
          </el-sub-menu>
        </el-menu>

        <div class="user-section">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown>
              <div class="user-info">
                <el-avatar :size="32">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.username }}</span>
                <el-icon class="arrow"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>
                    <el-tag size="small" :type="getRoleTagType()">{{ getRoleText() }}</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="router.push('/profile')">
                    <el-icon><UserFilled /></el-icon>
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="router.push('/login')">
              登录
            </el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="app-main" :class="{ 'no-header': isLoginPage }">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Platform,
  List,
  Document,
  TrophyBase,
  User,
  UserFilled,
  ArrowDown,
  SwitchButton,
  Reading,
  Management,
  Setting,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { UserRole, UserRoleText } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/problem/')) return '/problems'
  return path
})

const isLoginPage = computed(() => route.path === '/login')

// 获取角色文本
const getRoleText = () => {
  const role = userStore.userInfo?.role
  if (!role) return '未知'
  return UserRoleText[role as UserRole] || role
}

// 获取角色标签类型
const getRoleTagType = () => {
  const role = userStore.userInfo?.role
  switch (role) {
    case UserRole.ADMIN:
      return 'danger'
    case UserRole.TEACHER:
      return 'warning'
    case UserRole.STUDENT:
      return 'success'
    default:
      return 'info'
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  userStore.initUser()
})
</script>

<style scoped lang="scss">
.app-container {
  height: 100vh;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0;
  height: 60px;

  .header-content {
    max-width: 1600px;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    align-items: center;
    height: 100%;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-right: 40px;
    cursor: pointer;

    .logo-icon {
      font-size: 28px;
      color: #409eff;
    }

    .logo-text {
      font-size: 20px;
      font-weight: bold;
      color: #409eff;
    }
  }

  .header-menu {
    flex: 1;
    border: none;
  }

  .user-section {
    margin-left: 20px;

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 12px;
      border-radius: 4px;
      cursor: pointer;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      .username {
        font-size: 14px;
        color: #303133;
      }

      .arrow {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.app-main {
  background: #f5f7fa;
  padding: 0;

  &.no-header {
    height: 100vh;
  }
}
</style>
