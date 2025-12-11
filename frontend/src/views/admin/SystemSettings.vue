<template>
    <div class="system-settings">
        <el-page-header content="系统设置" />
        
        <el-card class="settings-card" shadow="hover">
            <el-tabs v-model="activeTab">
                <!-- 基础设置 -->
                <el-tab-pane label="基础设置" name="basic">
                    <el-form :model="settings" label-width="140px" style="max-width: 800px;">
                        <el-divider content-position="left">网站信息</el-divider>
                        <el-form-item label="系统名称">
                            <el-input v-model="settings.systemName" placeholder="Royuki Cloud OJ" />
                        </el-form-item>
                        <el-form-item label="系统描述">
                            <el-input v-model="settings.systemDesc" type="textarea" :rows="3" 
                                placeholder="在线代码评测系统" />
                        </el-form-item>
                        <el-form-item label="系统关键词">
                            <el-input v-model="settings.keywords" placeholder="OJ,编程,算法" />
                        </el-form-item>
                        <el-form-item label="ICP备案号">
                            <el-input v-model="settings.icp" placeholder="京ICP备XXXXXXXX号" />
                        </el-form-item>

                        <el-divider content-position="left">注册设置</el-divider>
                        <el-form-item label="开放注册">
                            <el-switch v-model="settings.allowRegister" />
                            <span style="margin-left: 10px; color: #909399;">关闭后新用户无法注册</span>
                        </el-form-item>
                        <el-form-item label="邮箱验证">
                            <el-switch v-model="settings.requireEmailVerify" />
                            <span style="margin-left: 10px; color: #909399;">注册时需要验证邮箱</span>
                        </el-form-item>

                        <el-divider content-position="left">安全设置</el-divider>
                        <el-form-item label="密码最小长度">
                            <el-input-number v-model="settings.minPasswordLength" :min="6" :max="20" />
                        </el-form-item>
                        <el-form-item label="登录失败次数">
                            <el-input-number v-model="settings.maxLoginAttempts" :min="3" :max="10" />
                            <span style="margin-left: 10px; color: #909399;">超过后锁定账号</span>
                        </el-form-item>

                        <el-form-item>
                            <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
                            <el-button @click="resetSettings">重置</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>

                <!-- 评测设置 -->
                <el-tab-pane label="评测设置" name="judge">
                    <el-form :model="settings" label-width="140px" style="max-width: 800px;">
                        <el-divider content-position="left">默认限制</el-divider>
                        <el-form-item label="默认时间限制">
                            <el-input-number v-model="settings.defaultTimeLimit" :min="100" :max="10000" :step="100" />
                            <span style="margin-left: 10px;">毫秒</span>
                        </el-form-item>
                        <el-form-item label="默认内存限制">
                            <el-input-number v-model="settings.defaultMemoryLimit" :min="64" :max="512" :step="64" />
                            <span style="margin-left: 10px;">MB</span>
                        </el-form-item>

                        <el-divider content-position="left">评测配置</el-divider>
                        <el-form-item label="最大并发评测">
                            <el-input-number v-model="settings.maxConcurrentJudge" :min="1" :max="100" />
                            <span style="margin-left: 10px;">个</span>
                        </el-form-item>
                        <el-form-item label="评测超时时间">
                            <el-input-number v-model="settings.judgeTimeout" :min="10" :max="300" />
                            <span style="margin-left: 10px;">秒</span>
                        </el-form-item>
                        <el-form-item label="支持的语言">
                            <el-checkbox-group v-model="settings.supportedLanguages">
                                <el-checkbox label="Java">Java</el-checkbox>
                                <el-checkbox label="Python">Python</el-checkbox>
                                <el-checkbox label="C++">C++</el-checkbox>
                                <el-checkbox label="C">C</el-checkbox>
                                <el-checkbox label="JavaScript">JavaScript</el-checkbox>
                                <el-checkbox label="Go">Go</el-checkbox>
                            </el-checkbox-group>
                        </el-form-item>

                        <el-divider content-position="left">提交限制</el-divider>
                        <el-form-item label="提交间隔">
                            <el-input-number v-model="settings.submitInterval" :min="0" :max="60" />
                            <span style="margin-left: 10px;">秒（0表示不限制）</span>
                        </el-form-item>

                        <el-form-item>
                            <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
                            <el-button @click="resetSettings">重置</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>

                <!-- 邮件设置 -->
                <el-tab-pane label="邮件设置" name="email">
                    <el-form :model="settings" label-width="140px" style="max-width: 800px;">
                        <el-divider content-position="left">SMTP配置</el-divider>
                        <el-form-item label="SMTP服务器">
                            <el-input v-model="settings.smtpHost" placeholder="smtp.example.com" />
                        </el-form-item>
                        <el-form-item label="SMTP端口">
                            <el-input-number v-model="settings.smtpPort" :min="1" :max="65535" />
                        </el-form-item>
                        <el-form-item label="发件人邮箱">
                            <el-input v-model="settings.emailFrom" placeholder="noreply@example.com" />
                        </el-form-item>
                        <el-form-item label="发件人名称">
                            <el-input v-model="settings.emailFromName" placeholder="Royuki Cloud OJ" />
                        </el-form-item>
                        <el-form-item label="SMTP用户名">
                            <el-input v-model="settings.smtpUsername" />
                        </el-form-item>
                        <el-form-item label="SMTP密码">
                            <el-input v-model="settings.smtpPassword" type="password" show-password />
                        </el-form-item>
                        <el-form-item label="启用SSL">
                            <el-switch v-model="settings.smtpSsl" />
                        </el-form-item>

                        <el-form-item>
                            <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
                            <el-button @click="testEmail" :loading="testing">发送测试邮件</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>

                <!-- 系统信息 -->
                <el-tab-pane label="系统信息" name="info">
                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-card shadow="hover">
                                <template #header>
                                    <div class="card-header">
                                        <span>系统状态</span>
                                        <el-button type="primary" size="small" @click="refreshSystemInfo">
                                            刷新
                                        </el-button>
                                    </div>
                                </template>
                                <el-descriptions :column="1" border>
                                    <el-descriptions-item label="系统版本">
                                        <el-tag type="primary">v1.0.0</el-tag>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="运行状态">
                                        <el-tag type="success">正常运行</el-tag>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="启动时间">
                                        {{ systemInfo.startTime }}
                                    </el-descriptions-item>
                                    <el-descriptions-item label="运行时长">
                                        {{ systemInfo.uptime }}
                                    </el-descriptions-item>
                                </el-descriptions>
                            </el-card>
                        </el-col>

                        <el-col :span="12">
                            <el-card shadow="hover">
                                <template #header>
                                    <span>服务状态</span>
                                </template>
                                <el-descriptions :column="1" border>
                                    <el-descriptions-item label="数据库">
                                        <el-tag :type="systemInfo.dbStatus ? 'success' : 'danger'">
                                            {{ systemInfo.dbStatus ? '已连接' : '连接失败' }}
                                        </el-tag>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="Redis">
                                        <el-tag :type="systemInfo.redisStatus ? 'success' : 'danger'">
                                            {{ systemInfo.redisStatus ? '已连接' : '连接失败' }}
                                        </el-tag>
                                    </el-descriptions-item>
                                    <el-descriptions-item label="消息队列">
                                        <el-tag :type="systemInfo.mqStatus ? 'success' : 'danger'">
                                            {{ systemInfo.mqStatus ? '正常' : '异常' }}
                                        </el-tag>
                                    </el-descriptions-item>
                                </el-descriptions>
                            </el-card>
                        </el-col>
                    </el-row>

                    <el-row :gutter="20" style="margin-top: 20px;">
                        <el-col :span="8">
                            <el-card shadow="hover">
                                <el-statistic title="用户总数" :value="systemInfo.userCount">
                                    <template #prefix>
                                        <el-icon style="vertical-align: middle;"><User /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover">
                                <el-statistic title="题目总数" :value="systemInfo.problemCount">
                                    <template #prefix>
                                        <el-icon style="vertical-align: middle;"><Document /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-card>
                        </el-col>
                        <el-col :span="8">
                            <el-card shadow="hover">
                                <el-statistic title="提交总数" :value="systemInfo.submissionCount">
                                    <template #prefix>
                                        <el-icon style="vertical-align: middle;"><List /></el-icon>
                                    </template>
                                </el-statistic>
                            </el-card>
                        </el-col>
                    </el-row>

                    <el-card shadow="hover" style="margin-top: 20px;">
                        <template #header>
                            <span>服务器资源</span>
                        </template>
                        <el-row :gutter="20">
                            <el-col :span="8">
                                <div class="resource-item">
                                    <div class="resource-label">CPU使用率</div>
                                    <el-progress 
                                        :percentage="systemInfo.cpuUsage" 
                                        :color="getProgressColor(systemInfo.cpuUsage)"
                                    />
                                </div>
                            </el-col>
                            <el-col :span="8">
                                <div class="resource-item">
                                    <div class="resource-label">内存使用率</div>
                                    <el-progress 
                                        :percentage="systemInfo.memoryUsage" 
                                        :color="getProgressColor(systemInfo.memoryUsage)"
                                    />
                                </div>
                            </el-col>
                            <el-col :span="8">
                                <div class="resource-item">
                                    <div class="resource-label">磁盘使用率</div>
                                    <el-progress 
                                        :percentage="systemInfo.diskUsage" 
                                        :color="getProgressColor(systemInfo.diskUsage)"
                                    />
                                </div>
                            </el-col>
                        </el-row>
                    </el-card>
                </el-tab-pane>

                <!-- 缓存管理 -->
                <el-tab-pane label="缓存管理" name="cache">
                    <el-space direction="vertical" size="large" style="width: 100%;">
                        <el-alert
                            title="缓存管理"
                            type="info"
                            description="清除缓存可能会导致短时间内系统响应变慢，请谨慎操作"
                            :closable="false"
                        />

                        <el-card>
                            <el-descriptions :column="2" border>
                                <el-descriptions-item label="Redis缓存">
                                    <el-tag>{{ cacheInfo.redisKeys }} 个键</el-tag>
                                </el-descriptions-item>
                                <el-descriptions-item label="操作">
                                    <el-button type="danger" size="small" @click="clearCache('redis')">
                                        清除Redis缓存
                                    </el-button>
                                </el-descriptions-item>
                                <el-descriptions-item label="用户Session">
                                    <el-tag>{{ cacheInfo.sessionCount }} 个会话</el-tag>
                                </el-descriptions-item>
                                <el-descriptions-item label="操作">
                                    <el-button type="warning" size="small" @click="clearCache('session')">
                                        清除所有Session
                                    </el-button>
                                </el-descriptions-item>
                            </el-descriptions>
                        </el-card>
                    </el-space>
                </el-tab-pane>
            </el-tabs>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Document, List } from '@element-plus/icons-vue'

const activeTab = ref('basic')
const saving = ref(false)
const testing = ref(false)

// 系统设置
const settings = reactive({
    // 基础设置
    systemName: 'Royuki Cloud OJ',
    systemDesc: '在线代码评测系统',
    keywords: 'OJ,编程,算法,数据结构',
    icp: '',
    allowRegister: true,
    requireEmailVerify: false,
    minPasswordLength: 6,
    maxLoginAttempts: 5,
    
    // 评测设置
    defaultTimeLimit: 1000,
    defaultMemoryLimit: 256,
    maxConcurrentJudge: 10,
    judgeTimeout: 60,
    supportedLanguages: ['Java', 'Python', 'C++', 'C'],
    submitInterval: 5,
    
    // 邮件设置
    smtpHost: '',
    smtpPort: 587,
    emailFrom: '',
    emailFromName: 'Royuki Cloud OJ',
    smtpUsername: '',
    smtpPassword: '',
    smtpSsl: true,
})

// 系统信息
const systemInfo = reactive({
    startTime: '2024-01-01 00:00:00',
    uptime: '30天 12小时 30分钟',
    dbStatus: true,
    redisStatus: true,
    mqStatus: true,
    userCount: 1234,
    problemCount: 567,
    submissionCount: 89012,
    cpuUsage: 35,
    memoryUsage: 62,
    diskUsage: 45,
})

// 缓存信息
const cacheInfo = reactive({
    redisKeys: 1520,
    sessionCount: 245,
})

// 保存设置
const saveSettings = async () => {
    saving.value = true
    try {
        // TODO: 调用API保存设置
        // await updateSystemSettings(settings)
        await new Promise(resolve => setTimeout(resolve, 1000))
        ElMessage.success('设置保存成功')
    } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
    } finally {
        saving.value = false
    }
}

// 重置设置
const resetSettings = () => {
    ElMessageBox.confirm('确定要重置为默认设置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
    }).then(() => {
        // 重置为默认值
        settings.systemName = 'Royuki Cloud OJ'
        settings.systemDesc = '在线代码评测系统'
        settings.defaultTimeLimit = 1000
        settings.defaultMemoryLimit = 256
        settings.maxConcurrentJudge = 10
        settings.judgeTimeout = 60
        ElMessage.success('已重置为默认设置')
    }).catch(() => {
        // 取消
    })
}

// 测试邮件
const testEmail = async () => {
    if (!settings.smtpHost || !settings.emailFrom) {
        ElMessage.warning('请先配置SMTP服务器和发件人邮箱')
        return
    }
    
    testing.value = true
    try {
        // TODO: 调用API发送测试邮件
        // await sendTestEmail()
        await new Promise(resolve => setTimeout(resolve, 2000))
        ElMessage.success('测试邮件已发送，请查收')
    } catch (error: any) {
        ElMessage.error(error.message || '发送失败')
    } finally {
        testing.value = false
    }
}

// 刷新系统信息
const refreshSystemInfo = async () => {
    try {
        // TODO: 调用API获取系统信息
        // const info = await getSystemInfo()
        // Object.assign(systemInfo, info)
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // 模拟更新
        systemInfo.cpuUsage = Math.floor(Math.random() * 40) + 20
        systemInfo.memoryUsage = Math.floor(Math.random() * 30) + 50
        systemInfo.diskUsage = Math.floor(Math.random() * 20) + 40
        
        ElMessage.success('系统信息已刷新')
    } catch (error: any) {
        ElMessage.error(error.message || '刷新失败')
    }
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
    if (percentage < 50) return '#67c23a'
    if (percentage < 80) return '#e6a23c'
    return '#f56c6c'
}

// 清除缓存
const clearCache = async (type: string) => {
    const typeText = type === 'redis' ? 'Redis缓存' : '所有Session'
    
    try {
        await ElMessageBox.confirm(
            `确定要清除${typeText}吗？此操作不可恢复！`,
            '警告',
            {
                confirmButtonText: '确定清除',
                cancelButtonText: '取消',
                type: 'warning',
            }
        )
        
        // TODO: 调用API清除缓存
        // await clearCacheApi(type)
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        ElMessage.success(`${typeText}已清除`)
        
        // 更新缓存信息
        if (type === 'redis') {
            cacheInfo.redisKeys = 0
        } else {
            cacheInfo.sessionCount = 0
        }
    } catch (error: any) {
        if (error !== 'cancel') {
            ElMessage.error(error.message || '操作失败')
        }
    }
}

onMounted(() => {
    // 加载系统设置
    // loadSystemSettings()
})
</script>

<style scoped>
.system-settings {
    padding: 20px;
}

.settings-card {
    margin-top: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.resource-item {
    margin-bottom: 16px;
}

.resource-label {
    margin-bottom: 8px;
    color: #606266;
    font-size: 14px;
}
</style>
