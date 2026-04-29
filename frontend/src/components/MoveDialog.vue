<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="移动文件"
    width="500px"
    :close-on-click-modal="false"
  >
    <div v-if="file" class="move-info mb-16">
      <span>移动：<strong>{{ file.originalName || file.fileName }}</strong></span>
    </div>

    <div v-loading="treeLoading" class="folder-tree">
      <el-tree
        ref="treeRef"
        :data="folderTree"
        :props="treeProps"
        node-key="id"
        default-expand-all
        :highlight-current="true"
        :expand-on-click-node="true"
        @node-click="handleNodeClick"
      />
    </div>

    <div v-if="selectedFolder" class="selected-folder mt-8">
      目标目录：<el-tag>{{ selectedFolder.name }}</el-tag>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!selectedFolder || selectedFolder.id === file?.parentId"
        @click="handleMove"
      >
        移动
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getFileList, moveFile } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  file: { type: Object, default: null },
  currentParentId: { type: Number, default: 0 }
})

const emit = defineEmits(['update:visible', 'success'])

const userStore = useUserStore()

const treeRef = ref(null)
const treeLoading = ref(false)
const loading = ref(false)
const folderTree = ref([])
const selectedFolder = ref(null)

const treeProps = {
  children: 'children',
  label: 'name'
}

watch(() => props.visible, async (val) => {
  if (val) {
    selectedFolder.value = null
    await loadFolderTree()
    // Highlight current parent
    if (treeRef.value) {
      treeRef.value.setCurrentKey(props.currentParentId)
    }
  }
})

// Build folder tree recursively
async function loadFolderTree(parentId = 0, parentNode = null) {
  try {
    const res = await getFileList(userStore.userId, parentId)
    if (res.code === 200) {
      const folders = (res.data || []).filter(f => f.isDirectory === 1)
      const children = folders.map(f => ({
        id: f.id,
        name: f.originalName || f.fileName,
        children: []
      }))

      if (parentNode) {
        parentNode.children = children
        // Load sub-folders for each child
        for (const child of children) {
          await loadFolderTree(child.id, child)
        }
      } else {
        // Root level
        folderTree.value = [
          { id: 0, name: '全部文件（根目录）', children }
        ]
        // Load sub-folders for root children
        for (const child of children) {
          await loadFolderTree(child.id, child)
        }
      }
    }
  } catch (e) {
    // ignore
  }
}

function handleNodeClick(data) {
  if (data.id !== props.file?.id) {
    selectedFolder.value = data
  }
}

function close() {
  emit('update:visible', false)
}

async function handleMove() {
  if (!selectedFolder.value || !props.file) return

  loading.value = true
  try {
    const res = await moveFile(props.file.id, selectedFolder.value.id)
    if (res.code === 200) {
      ElMessage.success('移动成功')
      emit('success')
      close()
    } else {
      ElMessage.error(res.message || '移动失败')
    }
  } catch (e) {
    ElMessage.error('移动失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.folder-tree {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 8px;
}
.move-info {
  padding: 8px 0;
}
.selected-folder {
  font-size: 13px;
  color: #606266;
}
</style>
