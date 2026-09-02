<template>
  <div class="nepg-shell">
    <header class="nepg-header">
      <div class="brand" @click="$router.push('/home')">
        <span class="brand-dot"></span>
        <span class="brand-name">网格员工作端</span>
      </div>
      <span class="header-user">{{ displayName }}</span>
    </header>

    <main class="nepg-main">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <nav class="nepg-tabbar">
      <router-link to="/home" class="tab-item" :class="{ active: route.path === '/home' }">
        <el-icon :size="20"><List /></el-icon><span>我的任务</span>
      </router-link>
      <router-link to="/history" class="tab-item" :class="{ active: route.path === '/history' }">
        <el-icon :size="20"><Clock /></el-icon><span>历史任务</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkerStore } from '@/store/worker'
import { connectWS, onWSMessage, closeWS } from '@/utils/ws'
import { ElNotification } from 'element-plus'

const route = useRoute()
const router = useRouter()
const workerStore = useWorkerStore()
const displayName = computed(() => workerStore.nickname || workerStore.username || '网格员')

let offWS = null

onMounted(() => {
  // 连接通知通道（身份: GRID + userId）
  if (workerStore.userId) {
    connectWS('role=GRID&id=' + workerStore.userId)
    offWS = onWSMessage(msg => {
      if (msg.type === 'notify' && msg.biz === 'TASK_ASSIGNED') {
        ElNotification({
          title: '新任务',
          message: msg.message || '您有新任务',
          type: 'info',
          duration: 8000,
          onClick: () => { if (msg.taskId) router.push('/task/' + msg.taskId) }
        })
      }
    })
  }
})
onUnmounted(() => {
  if (offWS) offWS()
  closeWS()
})
</script>

<style scoped>
.nepg-shell { max-width: 480px; margin: 0 auto; min-height: 100vh; background: var(--bg-primary); box-shadow: 0 0 40px rgba(0,0,0,0.5); }
.nepg-header {
  position: fixed; top: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 480px; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; box-sizing: border-box;
  background: rgba(17, 22, 21, 0.85); backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border-bottom: 1px solid var(--border-subtle);
}
.brand { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.brand-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--env-green); box-shadow: 0 0 10px rgba(126,226,184,0.8); }
.brand-name { font-size: 15px; font-weight: 700; letter-spacing: 0.06em; }
.header-user { font-size: 12px; color: var(--text-secondary); }
.nepg-main { padding: 64px 0 76px; min-height: 100vh; box-sizing: border-box; }
.nepg-tabbar {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 480px; z-index: 100;
  display: flex; background: rgba(17, 22, 21, 0.92);
  backdrop-filter: blur(20px) saturate(160%); -webkit-backdrop-filter: blur(20px) saturate(160%);
  border-top: 1px solid var(--border-subtle);
  padding: 8px 0 calc(8px + env(safe-area-inset-bottom));
}
.tab-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; font-size: 11px; color: var(--text-muted); text-decoration: none; padding: 4px 0; }
.tab-item.active { color: var(--env-green); }
.fade-slide-enter-active, .fade-slide-leave-active { transition: opacity 0.25s var(--ease-out), transform 0.25s var(--ease-out); }
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-6px); }
</style>