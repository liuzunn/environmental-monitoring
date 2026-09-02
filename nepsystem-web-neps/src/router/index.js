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
    component: () => import('@/layout/PublicLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'supervise', name: 'Supervise', component: () => import('@/views/Supervise.vue'), meta: { title: '我要监督' } },
      { path: 'mine', name: 'MySupervision', component: () => import('@/views/MySupervision.vue'), meta: { title: '我的监督' } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/Messages.vue'), meta: { title: '消息中心' } },
      { path: 'supervision/:id', name: 'SupervisionDetail', component: () => import('@/views/SupervisionDetail.vue'), meta: { title: '监督详情' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页（与 NEPV 管理端同构）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('nep_neps_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/home')
  } else {
    document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '环境监督'
    next()
  }
})

export default router
