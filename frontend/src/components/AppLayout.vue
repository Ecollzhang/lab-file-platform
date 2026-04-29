<template>
  <div class="app-container">
    <header class="app-header">
      <div class="logo">
        <el-icon :size="28"><Files /></el-icon>
        <span>智能文件管理与共享平台</span>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click">
          <span class="user-dropdown">
            <el-avatar :size="32" icon="UserFilled" />
            <span class="ml-8">{{ userStore.username }}</span>
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="goToProfile">
                <el-icon><User /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>
    <div class="app-body">
      <aside class="app-sidebar">
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="false"
        >
          <el-menu-item index="/files">
            <el-icon><Folder /></el-icon>
            <template #title>我的文件</template>
          </el-menu-item>
          <el-menu-item index="/shares">
            <el-icon><Share /></el-icon>
            <template #title>我的分享</template>
          </el-menu-item>
          <el-menu-item index="/shared-with-me">
            <el-icon><Link /></el-icon>
            <template #title>与我共享</template>
          </el-menu-item>
        </el-menu>
      </aside>
      <main class="app-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/files')) return '/files'
  if (route.path.startsWith('/shares')) return '/shares'
  if (route.path.startsWith('/shared-with-me')) return '/shared-with-me'
  if (route.path.startsWith('/profile')) return '/profile'
  return '/files'
})

function goToProfile() {
  router.push('/profile')
}

function handleLogout() {
  userStore.doLogout()
  router.push('/login')
}
</script>

<style scoped>
.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}
.user-dropdown:hover {
  background: #f0f2f5;
}
</style>
