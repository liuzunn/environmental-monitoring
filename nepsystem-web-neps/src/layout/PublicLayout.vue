<template>
  <div class="neps-shell">
    <header class="neps-header">
      <div class="brand" @click="$router.push('/home')">
        <span class="brand-dot"></span>
        <span class="brand-name">环境监督</span>
      </div>
      <span class="header-user">{{ displayName }}</span>
    </header>

    <main class="neps-main">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <nav class="neps-tabbar">
      <router-link v-for="t in tabs" :key="t.path" :to="t.path" class="tab-item" :class="{ active: active === t.path }">
        <el-icon :size="20"><component :is="t.icon" /></el-icon>
        <span>{{ t.title }}</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { connectWS, onWSMessage, closeWS } from '@/utils/ws'
import { ElNotification } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const tabs = [
  { path: '/home', title: '首页', icon: 'HomeFilled' },
  { path: '/supervise', title: '我要监督', icon: 'EditPen' },
  { path: '/mine', title: '我的监督', icon: 'List' },
  { path: '/messages', title: '消息', icon: 'Bell' }
]

const active = computed(() => {
  const p = route.path
  if (p.startsWith('/supervision/')) return '/mine'
  return p
})
const displayName = computed(() => userStore.nickname || userStore.username || '访客')

let offWS = null

onMounted(() => {
  // 连接通知通道（身份: PUBLIC + userId）
  if (userStore.userId) {
    connectWS('role=PUBLIC&id=' + userStore.userId)
    offWS = onWSMessage(msg => {
      if (msg.type === 'notify' && msg.biz === 'EVENT_CLOSED') {
        ElNotification({
          title: '事件处理完成',
          message: msg.message || '您的监督事件已处理完成',
          type: 'success',
          duration: 6000,
          onClick: () => { if (msg.eventId) router.push('/supervision/' + msg.eventId) }
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
.neps-shell {
  max-width: 480px;
  margin: 0 auto;
  min-height: 100vh;
  background: var(--bg-primary);
  position: relative;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.5);
}

.neps-header {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(17, 22, 21, 0.85);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border-bottom: 1px solid var(--border-subtle);
  box-sizing: border-box;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.brand-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--env-green);
  box-shadow: 0 0 10px rgba(126, 226, 184, 0.8);
}
.brand-name {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.08em;
}
.header-user {
  font-size: 12px;
  color: var(--text-secondary);
}

.neps-main {
  padding: 64px 0 76px;
  min-height: 100vh;
  box-sizing: border-box;
}

.neps-tabbar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  z-index: 100;
  display: flex;
  background: rgba(17, 22, 21, 0.92);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border-top: 1px solid var(--border-subtle);
  padding: 8px 0 calc(8px + env(safe-area-inset-bottom));
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--text-muted);
  text-decoration: none;
  padding: 4px 0;
}
.tab-item.active {
  color: var(--env-green);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s var(--ease-out), transform 0.25s var(--ease-out);
}
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-6px); }
</style>