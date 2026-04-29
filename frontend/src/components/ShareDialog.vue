<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="创建分享"
    width="500px"
    :close-on-click-modal="false"
  >
    <div v-if="file" class="mb-16">
      <span>分享文件：<strong>{{ file.originalName || file.fileName }}</strong></span>
    </div>

    <el-form ref="formRef" :model="form" label-width="120px">
      <el-form-item label="分享类型">
        <el-radio-group v-model="form.shareType">
          <el-radio :value="1">公开分享（任何人可访问）</el-radio>
          <el-radio :value="2">私密分享（仅指定用户）</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="form.shareType === 2" label="目标用户">
        <el-select
          v-model="form.targetUserIds"
          multiple
          placeholder="选择可访问的用户"
          style="width: 100%"
        >
          <el-option
            v-for="u in userList"
            :key="u.id"
            :label="`${u.username} (${u.email || '无邮箱'})`"
            :value="u.id"
            :disabled="u.id === userStore.userId"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="过期时间">
        <el-select v-model="form.expireHours" placeholder="选择过期时间" clearable>
          <el-option label="1小时后" :value="1" />
          <el-option label="6小时后" :value="6" />
          <el-option label="1天后" :value="24" />
          <el-option label="7天后" :value="168" />
          <el-option label="30天后" :value="720" />
          <el-option label="永不过期" :value="0" />
        </el-select>
      </el-form-item>

      <el-form-item label="最大下载次数">
        <el-input-number
          v-model="form.maxDownloadCount"
          :min="0"
          :max="99999"
        />
        <span class="ml-8" style="color: #909399; font-size: 12px;">0 表示无限制</span>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleCreate">
        创建分享
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { createShare, getAllUsers } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  file: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const userList = ref([])

const form = reactive({
  shareType: 1,
  targetUserIds: [],
  expireHours: 0,
  maxDownloadCount: 0
})

watch(() => props.visible, async (val) => {
  if (val) {
    form.shareType = 1
    form.targetUserIds = []
    form.expireHours = 0
    form.maxDownloadCount = 0
    await loadUsers()
  }
})

async function loadUsers() {
  try {
    const res = await getAllUsers()
    if (res.code === 200) {
      userList.value = res.data || []
    }
  } catch (e) {
    // ignore
  }
}

function close() {
  emit('update:visible', false)
}

async function handleCreate() {
  if (!props.file) return

  if (form.shareType === 2 && form.targetUserIds.length === 0) {
    ElMessage.warning('私密分享请至少选择一个目标用户')
    return
  }

  loading.value = true
  try {
    const params = {
      fileId: props.file.id,
      shareType: form.shareType,
      maxDownloadCount: form.maxDownloadCount || 0,
      expireHours: form.expireHours > 0 ? form.expireHours : undefined
    }
    if (form.shareType === 2) {
      params.targetUserIds = form.targetUserIds.join(',')
    }

    const res = await createShare(params)
    if (res.code === 200) {
      ElMessage.success('分享创建成功')
      emit('success')
      close()
    } else {
      ElMessage.error(res.message || '创建分享失败')
    }
  } catch (e) {
    ElMessage.error('创建分享失败')
  } finally {
    loading.value = false
  }
}
</script>
