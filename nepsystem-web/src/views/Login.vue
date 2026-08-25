<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-blob blob-1"></div>
    <div class="bg-blob blob-2"></div>
    <div class="bg-blob blob-3"></div>
    <div class="bg-grid"></div>

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
  background: linear-gradient(160deg, #eef3fb 0%, #f2f7f4 55%, #f7f4ee 100%);
  position: relative;
  overflow: hidden;
}

/* 装饰性模糊光斑 */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.55;
  animation: float 12s ease-in-out infinite alternate;
}
.blob-1 {
  width: 420px; height: 420px;
  background: rgba(0, 122, 255, 0.28);
  top: -120px; left: -80px;
}
.blob-2 {
  width: 360px; height: 360px;
  background: rgba(52, 199, 89, 0.2);
  bottom: -100px; right: -60px;
  animation-delay: -4s;
}
.blob-3 {
  width: 300px; height: 300px;
  background: rgba(90, 200, 250, 0.25);
  bottom: 20%; left: 12%;
  animation-delay: -8s;
}
@keyframes float {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(40px, -30px) scale(1.08); }
}

/* 极细网格纹理（shadcn 风格背景点缀） */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 30%, transparent 75%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border-radius: 24px;
  border: 0.5px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 64px rgba(31, 55, 99, 0.18);
  padding: 40px 36px 28px;
  text-align: center;
  animation: card-in 0.5s var(--ease-out);
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
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
  box-shadow: 0 8px 20px rgba(0, 122, 255, 0.35);
}

.title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 4px;
  color: rgba(0, 0, 0, 0.9);
  letter-spacing: -0.01em;
}

.subtitle {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.5);
  margin: 0 0 28px;
}

.login-card :deep(.el-input__wrapper) {
  height: 48px;
  border-radius: 12px;
  background: rgba(118, 118, 128, 0.08);
}
.login-card :deep(.el-input__wrapper.is-focus) {
  background: #fff;
}
.login-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.3em;
  margin-top: 4px;
}

.tip {
  margin-top: 20px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.35);
}
</style>
