<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="新建文件夹"
    width="400px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-form-item label="文件夹名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入文件夹名称" maxlength="255" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleCreate">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { createFolder } from '@/utils/api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  parentId: { type: Number, default: 0 }
})

const emit = defineEmits(['update:visible', 'success'])

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  name: ''
})

const rules = {
  name: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { max: 255, message: '名称不能超过255个字符', trigger: 'blur' }
  ]
}

function close() {
  form.name = ''
  emit('update:visible', false)
}

async function handleCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await createFolder(form.name, props.parentId)
    if (res.code === 200) {
      ElMessage.success('文件夹创建成功')
      emit('success')
      close()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}
</script>
