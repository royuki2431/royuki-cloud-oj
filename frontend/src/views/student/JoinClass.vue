<template>
    <div class="join-class">
        <el-page-header @back="$router.back()" content="加入班级" />
        
        <el-card class="join-card" shadow="hover">
            <div class="join-content">
                <el-icon class="join-icon"><Postcard /></el-icon>
                <h2>使用邀请码加入班级</h2>
                <p>请输入教师提供的班级邀请码</p>
                
                <el-form :model="joinForm" ref="formRef" :rules="rules" label-width="100px">
                    <el-form-item label="邀请码" prop="inviteCode">
                        <el-input
                            v-model="joinForm.inviteCode"
                            placeholder="请输入6位邀请码"
                            maxlength="6"
                            show-word-limit
                            clearable
                        />
                    </el-form-item>
                    
                    <el-form-item>
                        <el-button type="primary" @click="handleJoin" :loading="loading" style="width: 100%;">
                            加入班级
                        </el-button>
                    </el-form-item>
                </el-form>

                <div class="tips">
                    <el-alert
                        title="提示"
                        type="info"
                        :closable="false"
                        show-icon
                    >
                        <template #default>
                            <ul>
                                <li>邀请码由教师创建班级时生成</li>
                                <li>邀请码为6位字符，不区分大小写</li>
                                <li>加入班级后可以查看课程和作业</li>
                            </ul>
                        </template>
                    </el-alert>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Postcard } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const joinForm = reactive({
    inviteCode: '',
})

const rules: FormRules = {
    inviteCode: [
        { required: true, message: '请输入邀请码', trigger: 'blur' },
        { len: 6, message: '邀请码为6位字符', trigger: 'blur' },
    ],
}

const handleJoin = async () => {
    if (!formRef.value) return
    
    await formRef.value.validate(async (valid) => {
        if (!valid) return
        
        loading.value = true
        try {
            // TODO: 调用API加入班级
            await new Promise(resolve => setTimeout(resolve, 1000))
            ElMessage.success('加入班级成功！')
            router.push('/my-courses')
        } catch (error: any) {
            ElMessage.error(error.message || '加入班级失败')
        } finally {
            loading.value = false
        }
    })
}
</script>

<style scoped>
.join-class {
    padding: 20px;
    display: flex;
    justify-content: center;
}

.join-card {
    margin-top: 20px;
    max-width: 600px;
    width: 100%;
}

.join-content {
    text-align: center;
    padding: 20px;
}

.join-icon {
    font-size: 80px;
    color: #409eff;
    margin-bottom: 20px;
}

.join-content h2 {
    margin: 20px 0 10px;
    color: #303133;
}

.join-content p {
    color: #909399;
    margin-bottom: 30px;
}

.tips {
    margin-top: 30px;
    text-align: left;
}

.tips ul {
    margin: 0;
    padding-left: 20px;
}

.tips li {
    margin: 5px 0;
}
</style>
