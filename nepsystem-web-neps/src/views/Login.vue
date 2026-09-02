<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand-row">
        <span class="brand-dot"></span>
        <span class="brand-name">环境监督</span>
      </div>
      <h1 class="title">公众环境监督</h1>
      <p class="subtitle">发现环境问题 · 随手监督 · 全程可查</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { loginPublic } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value.validate().catch(() => Promise.reject())
  loading.value = true
  try {
    const data = await loginPublic({ username: form.username, password: form.password })
    userStore.setLogin(data)
    ElMessage.success('登录成功')
    router.push('/home')
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
.login-card {
  width: 340px;
  background: rgba(23, 29, 27, 0.8);
  backdrop-filter: blur(24px) saturate(140%);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  border-radius: 20px;
  border: 1px solid var(--border-subtle);
  box-shadow: var(--shadow-lg);
  padding: 36px 28px 28px;
  text-align: center;
}
.brand-row { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 18px; }
.brand-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--env-green); box-shadow: 0 0 10px rgba(126, 226, 184, 0.8);
}
.brand-name { font-size: 13px; font-weight: 700; letter-spacing: 0.18em; }
.title { font-size: 22px; font-weight: 700; margin: 0 0 6px; }
.subtitle { font-size: 11px; letter-spacing: 0.06em; color: var(--text-muted); margin: 0 0 26px; }
.login-btn { width: 100%; height: 46px; border-radius: 12px; font-size: 15px; font-weight: 600; letter-spacing: 0.3em; }
</style>
