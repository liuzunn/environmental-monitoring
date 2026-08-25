<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-blob blob-1"></div>
    <div class="bg-blob blob-2"></div>
    <div class="bg-grid"></div>

    <div class="login-card">
      <div class="brand-row">
        <span class="brand-mark"><span class="brand-core"></span></span>
        <span class="brand-text">ENVISION</span>
      </div>
      <h1 class="title">环境监测保护系统</h1>
      <p class="subtitle">ENVIRONMENTAL MONITORING · AIR / WATER / NOISE</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
        <el-form-item prop="adminCode">
          <el-input v-model="form.adminCode" placeholder="管理员账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
          进 入 系 统
        </el-button>
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
  background: var(--bg-primary);
  position: relative;
  overflow: hidden;
}

/* 装饰性模糊光斑（克制） */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(110px);
  opacity: 0.16;
}
.blob-1 {
  width: 480px; height: 480px;
  background: #7EE2B8;
  top: -160px; left: -120px;
  animation: float 14s ease-in-out infinite alternate;
}
.blob-2 {
  width: 420px; height: 420px;
  background: #7CA7FF;
  bottom: -140px; right: -100px;
  animation: float 12s ease-in-out infinite alternate-reverse;
}
@keyframes float {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(36px, -26px) scale(1.06); }
}

/* 极细网格 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 20%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse 70% 60% at 50% 40%, #000 20%, transparent 75%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  background: rgba(23, 29, 27, 0.78);
  backdrop-filter: blur(24px) saturate(140%);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-lg);
  padding: 40px 36px 28px;
  text-align: center;
  animation: card-in 0.6s var(--ease-out);
}

@keyframes card-in {
  from { opacity: 0; transform: translateY(16px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.brand-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: var(--sp-24);
}
.brand-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(126, 226, 184, 0.12);
  border: 1px solid rgba(126, 226, 184, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.brand-core {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--env-green);
  box-shadow: 0 0 12px rgba(126, 226, 184, 0.8);
}
.brand-text {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: var(--text-primary);
}

.title {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.subtitle {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.18em;
  color: var(--text-muted);
  margin: 0 0 32px;
}

.login-card :deep(.el-input__wrapper) {
  height: 48px;
  border-radius: var(--radius-md);
}
.login-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.34em;
  margin-top: 4px;
}

.tip {
  margin-top: 22px;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--text-faint);
}
</style>
