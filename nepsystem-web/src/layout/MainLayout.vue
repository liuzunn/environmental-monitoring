<template>
  <div class="hud-layout">
    <!-- ═══ Floating HUD：漂浮导航（不占内容空间） ═══ -->
    <nav class="floating-hud" :class="{ 'is-scrolled': scrolled }">
      <div class="hud-brand" @click="$router.push('/dashboard')">
        <span class="brand-mark">
          <span class="brand-core"></span>
        </span>
        <span class="brand-text">ENVISION</span>
      </div>

      <div class="hud-nav">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="hud-item"
          :class="{ active: activeMenu === m.path }"
        >
          <el-icon :size="14"><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </router-link>
      </div>

      <div class="hud-right">
        <span class="hud-live">
          <span class="live-dot"></span>
          <span class="live-text">LIVE</span>
        </span>
        <span class="hud-time tabular-nums">{{ now }}</span>
        <el-badge v-if="unhandledCount > 0" :value="unhandledCount" :max="99" class="hud-alert-badge">
          <el-icon :size="15" class="hud-alert-icon"><Bell /></el-icon>
        </el-badge>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="hud-user">
            <span class="user-avatar">{{ (userStore.adminCode || 'A')[0].toUpperCase() }}</span>
            <span class="user-name">{{ userStore.adminCode || 'admin' }}</span>
            <el-icon :size="11" class="user-caret"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="'SwitchButton'">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>

    <!-- 主内容区 -->
    <main class="hud-main">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUnhandled, logout as apiLogout } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unhandledCount = ref(0)
const now = ref('')
const scrolled = ref(false)
let timer = null
let clockTimer = null

const menus = [
  { path: '/dashboard', title: '实时监测', icon: 'Odometer' },
  { path: '/history', title: '历史数据', icon: 'TrendCharts' },
  { path: '/devices', title: '设备管理', icon: 'Cpu' },
  { path: '/alerts', title: '告警中心', icon: 'Bell' },
  { path: '/thresholds', title: '阈值设置', icon: 'SetUp' },
  { path: '/users', title: '用户管理', icon: 'User' }
]

const activeMenu = computed(() => route.path)

function tick() {
  const d = new Date()
  now.value = [d.getHours(), d.getMinutes(), d.getSeconds()].map(n => String(n).padStart(2, '0')).join(':')
}

async function loadUnhandled() {
  try {
    const d = await getUnhandled()
    unhandledCount.value = d.count || 0
  } catch (e) { /* 忽略 */ }
}

function onScroll() {
  scrolled.value = window.scrollY > 24
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try { await apiLogout() } catch (e) { /* 忽略 */ }
    userStore.clear()
    router.push('/login')
  }
}

onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
  loadUnhandled()
  timer = setInterval(loadUnhandled, 30000)
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => {
  clearInterval(timer)
  clearInterval(clockTimer)
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.hud-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}

/* ═══════════ Floating HUD ═══════════ */
.floating-hud {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: var(--sp-24);
  padding: 8px 12px 8px 16px;
  border-radius: var(--radius-full);
  background: rgba(17, 22, 21, 0.72);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid var(--border-subtle);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  transition: background var(--dur-base) var(--ease-out);
  max-width: calc(100vw - 32px);
}

.floating-hud.is-scrolled {
  background: rgba(14, 18, 17, 0.85);
}

/* 品牌 */
.hud-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  padding-right: var(--sp-16);
  border-right: 1px solid var(--border-subtle);
}

.brand-mark {
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: rgba(126, 226, 184, 0.12);
  border: 1px solid rgba(126, 226, 184, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.brand-core {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--env-green);
  box-shadow: 0 0 10px rgba(126, 226, 184, 0.8);
}

.brand-text {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-primary);
}

/* 导航项 */
.hud-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}

.hud-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color var(--dur-fast) var(--ease-out),
              background var(--dur-fast) var(--ease-out);
  white-space: nowrap;
}
.hud-item:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.05);
}
.hud-item.active {
  color: var(--env-green);
  background: var(--env-green-dim);
}

/* 右侧状态区 */
.hud-right {
  display: flex;
  align-items: center;
  gap: var(--sp-12);
  padding-left: var(--sp-16);
  border-left: 1px solid var(--border-subtle);
}

.hud-live {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}
.live-text {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: var(--env-green);
}

.hud-time {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  letter-spacing: 0.08em;
}

.hud-alert-badge {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: var(--text-secondary);
  transition: color var(--dur-fast) var(--ease-out);
}
.hud-alert-badge:hover { color: var(--danger); }
.hud-alert-badge :deep(.el-badge__content) {
  background: var(--danger);
  border: none;
  font-size: 10px;
  height: 15px;
  line-height: 15px;
  padding: 0 4px;
  min-width: 15px;
}

.hud-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px 4px 4px;
  border-radius: var(--radius-full);
  transition: background var(--dur-fast) var(--ease-out);
}
.hud-user:hover { background: rgba(255, 255, 255, 0.05); }

.user-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--env-green-dim);
  border: 1px solid rgba(126, 226, 184, 0.35);
  color: var(--env-green);
  font-size: 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}
.user-caret { color: var(--text-muted); }

/* ═══════════ 主内容区 ═══════════ */
.hud-main {
  padding-top: 88px;
  min-height: 100vh;
}
</style>
