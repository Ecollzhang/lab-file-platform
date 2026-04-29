<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="重命名"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-form-item label="新名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入新名称" maxlength="255" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleRename">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { renameFile } from '@/utils/api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  file: { type: Object, default: null }
})

const emit = defineEmits(['update:visible', 'success'])

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  name: ''
})

const rules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 255, message: '名称不能超过255个字符', trigger: 'blur' }
  ]
}

watch(() => props.visible, (val) => {
  if (val && props.file) {
    form.name = props.file.originalName || props.file.fileName || ''
  }
})

function close() {
  emit('update:visible', false)
}

async function handleRename() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await renameFile(props.file.id, form.name)
    if (res.code === 200) {
      ElMessage.success('重命名成功')
      emit('success')
      close()
    } else {
      ElMessage.error(res.message || '重命名失败')
    }
  } catch (e) {
    ElMessage.error('重命名失败')
  } finally {
    loading.value = false
  }
}
</script>
