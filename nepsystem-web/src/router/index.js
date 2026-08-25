import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '实时监测', icon: 'Odometer' } },
      { path: 'history', name: 'History', component: () => import('@/views/History.vue'), meta: { title: '历史数据', icon: 'TrendCharts' } },
      { path: 'devices', name: 'Devices', component: () => import('@/views/Devices.vue'), meta: { title: '设备管理', icon: 'Cpu' } },
      { path: 'alerts', name: 'Alerts', component: () => import('@/views/Alerts.vue'), meta: { title: '告警中心', icon: 'Bell' } },
      { path: 'thresholds', name: 'Thresholds', component: () => import('@/views/Thresholds.vue'), meta: { title: '阈值设置', icon: 'SetUp' } },
      { path: 'users', name: 'Users', component: () => import('@/views/Users.vue'), meta: { title: '用户管理', icon: 'User' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('nep_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '环境监测保护系统'
    next()
  }
})

export default router
