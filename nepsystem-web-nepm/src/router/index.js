import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/workbench',
    children: [
      { path: 'workbench', name: 'Workbench', component: () => import('@/views/Workbench.vue'), meta: { title: '监管工作台', icon: 'Odometer' } },
      { path: 'events', name: 'Events', component: () => import('@/views/SupervisionList.vue'), meta: { title: '监督事件', icon: 'EditPen' } },
      { path: 'events/:id', name: 'EventDetail', component: () => import('@/views/SupervisionDetail.vue'), meta: { title: '事件详情' } },
      { path: 'grids', name: 'Grids', component: () => import('@/views/Grids.vue'), meta: { title: '网格管理', icon: 'Grid' } },
      { path: 'grid-members', name: 'GridMembers', component: () => import('@/views/GridMembers.vue'), meta: { title: '网格员管理', icon: 'User' } },
      { path: 'tasks', name: 'Tasks', component: () => import('@/views/Tasks.vue'), meta: { title: '任务管理', icon: 'List' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/workbench' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('nep_nepm_token')
  if (to.path !== '/login' && !token) next('/login')
  else if (to.path === '/login' && token) next('/workbench')
  else {
    document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '环境监管'
    next()
  }
})
export default router
