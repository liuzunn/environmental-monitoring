<template>
  <div class="login-page">
    <div class="login-card">
      <div class="app-icon">
        <el-icon :size="34" color="#fff"><Monitor /></el-icon>
      </div>
      <h1 class="title">环境监测保护系统</h1>
      <p class="subtitle">多合一环境监测平台 · 空气 / 水质 / 噪声</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
        <el-form-item prop="adminCode">
          <el-input v-model="form.adminCode" placeholder="管理员账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>

      <p class="tip">默认账号：admin / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ adminCode: 'admin', password: '123456' })
const rules = {
  adminCode: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value.validate().catch(() => Promise.reject())
  loading.value = true
  try {
    const data = await login({ adminCode: form.adminCode, password: form.password })
    userStore.setLogin(data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #f5f7fa 0%, #eef1f6 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.1);
  padding: 40px 36px 28px;
  text-align: center;
}

.app-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: linear-gradient(135deg, #007AFF, #0A84FF);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.3);
}

.title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 4px;
  color: rgba(0, 0, 0, 0.9);
}

.subtitle {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.5);
  margin: 0 0 28px;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  margin-top: 4px;
}

.tip {
  margin-top: 20px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}
</style>
