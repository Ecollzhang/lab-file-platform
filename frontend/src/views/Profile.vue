<template>
  <div class="profile">
    <div class="page-toolbar">
      <h3 style="margin: 0">个人中心</h3>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      style="max-width: 500px; margin-top: 20px"
    >
      <el-form-item label="用户名">
        <el-input v-model="form.username" disabled />
      </el-form-item>
      <el-form-item label="角色">
        <el-tag :type="ROLE_MAP[form.role]?.type || 'info'">
          {{ ROLE_MAP[form.role]?.label || '未知' }}
        </el-tag>
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleUpdate">
          保存修改
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ROLE_MAP } from '@/utils/api'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  phone: '',
  role: 3
})

const rules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

async function handleUpdate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await userStore.doUpdateUserInfo(userStore.userId, {
      email: form.email,
      phone: form.phone
    })
    if (result.success) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(result.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.userInfo) {
    form.username = userStore.userInfo.username || ''
    form.email = userStore.userInfo.email || ''
    form.phone = userStore.userInfo.phone || ''
    form.role = userStore.userInfo.role || 3
  }
})
</script>
