<template>
  <el-container class="layout">
    <!-- 侧边栏：毛玻璃 -->
    <el-aside width="236px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="22" color="#fff"><Monitor /></el-icon>
        </div>
        <div class="logo-text">
          <div class="logo-title">环境监测</div>
          <div class="logo-sub">保护系统</div>
        </div>
      </div>

      <div class="menu-label">功能导航</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <!-- 用户卡片 -->
      <div class="user-card">
        <div class="user-avatar">
          <el-icon :size="16" color="#007AFF"><UserFilled /></el-icon>
        </div>
        <div class="user-meta">
          <div class="user-name">{{ userStore.adminCode || 'admin' }}</div>
          <div class="user-role">系统管理员</div>
        </div>
        <el-dropdown trigger="click" @command="onCommand" class="user-menu">
          <el-icon class="more-icon" :size="16"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="'SwitchButton'">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶栏：毛玻璃 + 用户信息 -->
      <el-header class="header">
        <div class="header-left">
          <div class="header-title">{{ currentTitle }}</div>
          <span class="header-crumb">环境监测保护系统</span>
        </div>
        <div class="header-right">
          <el-tag v-if="unhandledCount > 0" type="danger" round size="small" class="alert-badge">
            <el-icon class="dot-pulse"><Bell /></el-icon>
            未处理告警 {{ unhandledCount }}
          </el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <span class="chip-avatar"><el-icon :size="14" color="#fff"><UserFilled /></el-icon></span>
              {{ userStore.adminCode || 'admin' }}
              <el-icon class="chip-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
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
let timer = null

const menus = [
  { path: '/dashboard', title: '实时监测', icon: 'Odometer' },
  { path: '/history', title: '历史数据', icon: 'TrendCharts' },
  { path: '/devices', title: '设备管理', icon: 'Cpu' },
  { path: '/alerts', title: '告警中心', icon: 'Bell' },
  { path: '/thresholds', title: '阈值设置', icon: 'SetUp' },
  { path: '/users', title: '用户管理', icon: 'User' }
]

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '')

async function loadUnhandled() {
  try {
    const d = await getUnhandled()
    unhandledCount.value = d.count || 0
  } catch (e) { /* 忽略 */ }
}

async function onCommand(cmd) {
  if (cmd === 'logout') {
    try { await apiLogout() } catch (e) { /* 忽略 */ }
    userStore.clear()
    router.push('/login')
  }
}

onMounted(() => {
  loadUnhandled()
  timer = setInterval(loadUnhandled, 30000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  background: rgba(247, 247, 249, 0.92);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-right: 0.5px solid var(--border-hairline);
  display: flex;
  flex-direction: column;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--sp-12);
  padding: var(--sp-24) var(--sp-20) var(--sp-20);
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: linear-gradient(135deg, #007AFF, #0A84FF);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.35);
}

.logo-title {
  font-size: var(--fs-headline);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.logo-sub {
  font-size: var(--fs-caption-1);
  color: var(--text-sub);
}

.menu-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text-placeholder);
  padding: 0 var(--sp-24) var(--sp-8);
}

.menu {
  border: none;
  background: transparent;
  padding: var(--sp-8);
  flex: 1;
  overflow-y: auto;
}

.menu .el-menu-item {
  height: 42px;
  border-radius: var(--radius-md);
  margin-bottom: 2px;
  color: rgba(0, 0, 0, 0.65);
  font-weight: 500;
  position: relative;
  transition: background var(--dur-base) var(--ease-out), color var(--dur-base) var(--ease-out);
}

.menu .el-menu-item .el-icon {
  font-size: 17px;
  margin-right: 10px;
}

.menu .el-menu-item::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  transform: translateY(-50%) scaleY(0);
  width: 3px;
  height: 18px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  transition: transform var(--dur-base) var(--ease-out);
}

.menu .el-menu-item.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}
.menu .el-menu-item.is-active::before { transform: translateY(-50%) scaleY(1); }

.menu .el-menu-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  gap: var(--sp-12);
  margin: var(--sp-12) var(--sp-16) var(--sp-16);
  padding: var(--sp-12) var(--sp-16);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
  border: 0.5px solid var(--border-hairline);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: var(--fs-footnote);
  font-weight: 600;
  line-height: 1.3;
}

.user-role {
  font-size: 11px;
  color: var(--text-sub);
}

.more-icon {
  color: var(--text-sub);
  cursor: pointer;
  border-radius: var(--radius-full);
  padding: 4px;
  transition: background var(--dur-fast) var(--ease-out);
}
.more-icon:hover { background: var(--fill-secondary); }

/* ---------- 顶栏 ---------- */
.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 0.5px solid var(--border-hairline);
  padding: 0 var(--sp-24);
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: var(--sp-12);
}

.header-title {
  font-size: var(--fs-title-2);
  font-weight: var(--fw-semibold);
  letter-spacing: -0.01em;
}

.header-crumb {
  font-size: var(--fs-footnote);
  color: var(--text-placeholder);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--sp-16);
}

.alert-badge {
  background: rgba(255, 59, 48, 0.1);
  color: var(--color-red);
  border: none;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  animation: badge-in 0.4s var(--ease-out);
}

@keyframes badge-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.dot-pulse { animation: pulse 1.6s ease-in-out infinite; }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: var(--fs-subhead);
  font-weight: 500;
  padding: 5px 12px 5px 6px;
  border-radius: var(--radius-full);
  background: var(--fill-secondary);
  transition: background var(--dur-base) var(--ease-out);
}
.user-chip:hover { background: rgba(118, 118, 128, 0.2); }

.chip-avatar {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.chip-caret { color: var(--text-sub); font-size: 12px; }

/* ---------- 主区 ---------- */
.main {
  background: var(--bg-page);
  padding: 0;
  overflow-y: auto;
}

/* 页面切换过渡（shadcn 风格：淡入 + 轻微上移） */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity var(--dur-slow) var(--ease-out), transform var(--dur-slow) var(--ease-out);
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
