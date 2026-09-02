<template>
  <div class="nepm-shell">
    <aside class="nepm-side">
      <div class="side-brand">
        <span class="brand-dot"></span>
        <div>
          <div class="brand-title">环境监管</div>
          <div class="brand-sub">NEPM · ADMIN</div>
        </div>
      </div>
      <nav class="side-nav">
        <router-link v-for="m in menus" :key="m.path" :to="m.path" class="side-item" :class="{ active: isActive(m.path) }">
          <el-icon :size="16"><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="nepm-main">
      <header class="nepm-header">
        <span class="header-title">{{ route.meta.title || '环境监管' }}</span>
        <div class="header-right">
          <el-badge v-if="unhandled > 0" :value="unhandled" :max="99">
            <el-icon :size="16" class="bell"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="admin-name"><el-icon :size="14"><UserFilled /></el-icon> {{ adminStore.adminCode || 'admin' }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="nepm-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/store/admin'
import { getUnhandled, logout as apiLogout } from '@/api'
import { connectWS, onWSMessage, closeWS } from '@/utils/ws'
import { ElNotification } from 'element-plus'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const unhandled = ref(0)

const menus = [
  { path: '/workbench', title: '监管工作台', icon: 'Odometer' },
  { path: '/events', title: '监督事件', icon: 'EditPen' },
  { path: '/grids', title: '网格管理', icon: 'Grid' },
  { path: '/grid-members', title: '网格员管理', icon: 'User' },
  { path: '/tasks', title: '任务管理', icon: 'List' }
]

function isActive(p) {
  if (p === '/events') return route.path.startsWith('/events')
  return route.path === p
}

async function loadUnhandled() {
  try { const d = await getUnhandled(); unhandled.value = d.count || 0 } catch (e) { /* 忽略 */ }
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try { await apiLogout() } catch (e) { /* 忽略 */ }
    adminStore.clear()
    router.push('/login')
  }
}

let offWS = null

const NOTIFY_TITLES = {
  SUPERVISION_CREATED: '新的监督事件',
  TASK_ACCEPTED: '网格员已接单',
  DETECT_SUBMITTED: '检测完成'
}

onMounted(() => {
  loadUnhandled()
  setInterval(loadUnhandled, 30000)
  // 连接通知通道（身份: ADMIN）
  if (adminStore.adminId) {
    connectWS('role=ADMIN&id=' + adminStore.adminId)
    offWS = onWSMessage(msg => {
      if (msg.type !== 'notify') return
      const title = NOTIFY_TITLES[msg.biz]
      if (!title) return
      ElNotification({
        title,
        message: msg.message || '',
        type: msg.biz === 'DETECT_SUBMITTED' ? 'success' : 'info',
        duration: 6000,
        onClick: () => {
          if (msg.eventId) router.push('/events/' + msg.eventId)
          else router.push('/tasks')
        }
      })
      loadUnhandled()
    })
  }
})
onUnmounted(() => {
  if (offWS) offWS()
  closeWS()
})
</script>

<style scoped>
.nepm-shell { display: flex; min-height: 100vh; }
.nepm-side {
  width: 200px; flex-shrink: 0;
  background: rgba(14, 18, 17, 0.9);
  border-right: 1px solid var(--border-subtle);
  display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh;
}
.side-brand { display: flex; align-items: center; gap: 10px; padding: 18px 16px; border-bottom: 1px solid var(--border-subtle); }
.brand-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--env-green); box-shadow: 0 0 10px rgba(126, 226, 184, 0.8); }
.brand-title { font-size: 14px; font-weight: 700; letter-spacing: 0.06em; }
.brand-sub { font-size: 9px; color: var(--text-faint); letter-spacing: 0.2em; margin-top: 2px; }
.side-nav { flex: 1; padding: 10px 8px; display: flex; flex-direction: column; gap: 2px; }
.side-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 10px;
  color: var(--text-secondary); font-size: 13px; text-decoration: none;
  transition: all 0.2s var(--ease-out);
}
.side-item:hover { color: var(--text-primary); background: rgba(255, 255, 255, 0.05); }
.side-item.active { color: var(--env-green); background: var(--env-green-dim); }

.nepm-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.nepm-header {
  height: 56px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  background: rgba(17, 22, 21, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-subtle);
  position: sticky; top: 0; z-index: 50;
}
.header-title { font-size: 14px; font-weight: 600; letter-spacing: 0.04em; }
.header-right { display: flex; align-items: center; gap: 16px; }
.bell { color: var(--text-secondary); }
.admin-name { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; font-size: 13px; color: var(--text-secondary); }
.nepm-content { padding: 20px 24px; flex: 1; }
</style>