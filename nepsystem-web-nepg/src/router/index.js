import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/layout/GridInspectorLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '我的任务' } },
      { path: 'history', name: 'History', component: () => import('@/views/History.vue'), meta: { title: '历史任务' } },
      { path: 'task/:id', name: 'TaskDetail', component: () => import('@/views/TaskDetail.vue'), meta: { title: '任务详情' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('nep_nepg_token')
  if (to.path !== '/login' && !token) next('/login')
  else if (to.path === '/login' && token) next('/home')
  else {
    document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '网格员工作端'
    next()
  }
})
export default router
