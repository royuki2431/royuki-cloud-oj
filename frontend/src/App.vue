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
          <el-menu-item index="/submissions">
            <el-icon><Document /></el-icon>
            <span>提交历史</span>
          </el-menu-item>
          <el-menu-item index="/ranking">
            <el-icon><TrophyBase /></el-icon>
            <span>排行榜</span>
          </el-menu-item>
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
                    <el-tag size="small">{{ userStore.userInfo?.role }}</el-tag>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
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
  ArrowDown,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/problem/')) return '/problems'
  return path
})

const isLoginPage = computed(() => route.path === '/login')

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
