import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/files',
    children: [
      {
        path: 'files',
        name: 'MyFiles',
        component: () => import('@/views/MyFiles.vue'),
        meta: { title: '我的文件' }
      },
      {
        path: 'shares',
        name: 'ShareManage',
        component: () => import('@/views/ShareManage.vue'),
        meta: { title: '我的分享' }
      },
      {
        path: 'shared-with-me',
        name: 'SharedWithMe',
        component: () => import('@/views/SharedWithMe.vue'),
        meta: { title: '与我共享' }
      },
      {
        path: 'share/view/:shareCode',
        name: 'ShareView',
        component: () => import('@/views/ShareView.vue'),
        meta: { title: '分享文件' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
    next({ name: 'MyFiles' })
  } else {
    next()
  }
})

export default router
