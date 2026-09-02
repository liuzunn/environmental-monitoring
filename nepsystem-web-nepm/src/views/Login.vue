<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand-row">
        <span class="brand-dot"></span>
        <span class="brand-name">环境监管</span>
      </div>
      <h1 class="title">监管管理平台</h1>
      <p class="subtitle">NEPM · ENVIRONMENTAL REGULATION</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
        <el-form-item prop="adminCode">
          <el-input v-model="form.adminCode" placeholder="管理员账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">进 入 平 台</el-button>
      </el-form>
      <p class="tip">默认账号 admin / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'
import { useAdminStore } from '@/store/admin'

const router = useRouter()
const adminStore = useAdminStore()
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
    adminStore.setLogin(data)
    ElMessage.success('登录成功')
    router.push('/workbench')
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.login-page { height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-primary); }
.login-card {
  width: 380px; background: rgba(23, 29, 27, 0.8);
  backdrop-filter: blur(24px); border-radius: 20px;
  border: 1px solid var(--border-subtle); box-shadow: var(--shadow-lg);
  padding: 38px 32px 26px; text-align: center;
}
.brand-row { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 18px; }
.brand-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--env-green); box-shadow: 0 0 10px rgba(126, 226, 184, 0.8); }
.brand-name { font-size: 13px; font-weight: 700; letter-spacing: 0.18em; }
.title { font-size: 22px; font-weight: 700; margin: 0 0 6px; }
.subtitle { font-size: 10px; letter-spacing: 0.18em; color: var(--text-muted); margin: 0 0 28px; }
.login-btn { width: 100%; height: 46px; border-radius: 12px; font-size: 15px; font-weight: 600; letter-spacing: 0.3em; }
.tip { margin-top: 20px; font-size: 11px; color: var(--text-faint); }
</style>
