<template>
  <el-container class="layout">
    <!-- 侧边栏：毛玻璃 -->
    <el-aside width="230px" class="sidebar">
      <div class="logo">
        <el-icon :size="26" color="#007AFF"><Monitor /></el-icon>
        <div class="logo-text">
          <div class="logo-title">环境监测</div>
          <div class="logo-sub">保护系统</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏：毛玻璃 + 用户信息 -->
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <el-tag v-if="unhandledCount > 0" type="danger" round size="small" class="alert-badge">
            未处理告警 {{ unhandledCount }}
          </el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.adminCode || 'admin' }}
              <el-icon><ArrowDown /></el-icon>
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
        <router-view />
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
  padding: var(--sp-24) var(--sp-20) var(--sp-16);
}

.logo-title {
  font-size: var(--fs-headline);
  font-weight: 700;
  line-height: 1.2;
}

.logo-sub {
  font-size: var(--fs-caption-1);
  color: var(--text-sub);
}

.menu {
  border: none;
  background: transparent;
  padding: var(--sp-8);
  flex: 1;
}

.menu .el-menu-item {
  height: 40px;
  border-radius: var(--radius-md);
  margin-bottom: 2px;
  color: rgba(0, 0, 0, 0.65);
}

.menu .el-menu-item.is-active {
  background: rgba(0, 122, 255, 0.1);
  color: var(--color-primary);
  font-weight: 600;
}

.menu .el-menu-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

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
}

.header-title {
  font-size: var(--fs-title-2);
  font-weight: var(--fw-semibold);
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
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: var(--fs-subhead);
  padding: 6px 10px;
  border-radius: var(--radius-full);
  background: var(--fill-secondary);
}

.main {
  background: var(--bg-page);
  padding: 0;
  overflow-y: auto;
}
</style>
