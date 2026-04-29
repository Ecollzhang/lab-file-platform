import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getUserById, updateUserInfo, logout } from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id || null)
  const username = computed(() => userInfo.value?.username || '')

  async function doLogin(loginData) {
    const res = await login(loginData)
    if (res.code === 200) {
      token.value = res.data.token
      userInfo.value = {
        id: res.data.id,
        username: res.data.username,
        email: res.data.email,
        phone: res.data.phone,
        role: res.data.role,
        avatar: res.data.avatar,
        status: res.data.status
      }
      return { success: true }
    }
    return { success: false, message: res.message }
  }

  async function doRegister(registerData) {
    const res = await register(registerData)
    if (res.code === 200) {
      return { success: true }
    }
    return { success: false, message: res.message }
  }

  async function doLogout() {
    try {
      await logout()
    } catch (e) {
      // ignore
    }
    token.value = ''
    userInfo.value = null
  }

  async function fetchUserInfo(id) {
    if (!id && userInfo.value) {
      id = userInfo.value.id
    }
    if (!id) return
    const res = await getUserById(id)
    if (res.code === 200) {
      userInfo.value = { ...userInfo.value, ...res.data }
    }
  }

  async function doUpdateUserInfo(id, data) {
    const res = await updateUserInfo(id, data)
    if (res.code === 200) {
      await fetchUserInfo(id)
      return { success: true }
    }
    return { success: false, message: res.message }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userId,
    username,
    doLogin,
    doRegister,
    doLogout,
    fetchUserInfo,
    doUpdateUserInfo
  }
}, {
  persist: {
    key: 'lab-file-user',
    storage: localStorage,
    paths: ['token', 'userInfo']
  }
})
