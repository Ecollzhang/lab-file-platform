<template>
  <div class="my-files">
    <!-- Breadcrumb -->
    <BreadcrumbNav :path="path" @navigate="navigateTo" />

    <!-- Toolbar -->
    <div class="page-toolbar">
      <div class="toolbar-left">
        <el-button type="primary" @click="showUploadDialog">
          <el-icon><Upload /></el-icon>上传文件
        </el-button>
        <el-button @click="showCreateFolderDialog">
          <el-icon><FolderAdd /></el-icon>新建文件夹
        </el-button>
        <el-button @click="loadFiles">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="searchQuery"
          placeholder="搜索文件..."
          :prefix-icon="Search"
          clearable
          style="width: 200px"
          @input="filterFiles"
        />
      </div>
    </div>

    <!-- File Table -->
    <el-card shadow="never" class="file-card">
      <el-table
        :data="filteredFiles"
        stripe
        style="width: 100%"
        @row-dblclick="handleRowDoubleClick"
        v-loading="loading"
        empty-text="暂无文件"
      >
        <el-table-column label="" width="50">
          <template #default="{ row }">
            <el-icon :size="22" :color="getIconColor(row)">
              <component :is="getFileIconComponent(row)" />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="名称" min-width="200">
          <template #default="{ row }">
            <span
              class="file-name"
              :class="{ 'is-directory': row.isDirectory === 1 }"
              @click="handleRowDoubleClick(row)"
            >
              {{ row.originalName || row.fileName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            <span v-if="!row.isDirectory">{{ formatFileSize(row.fileSize) }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            {{ row.isDirectory ? '文件夹' : (row.fileExtension || '').toUpperCase() }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDownload(row)">
              下载
            </el-button>
            <el-button
              v-if="canPreview(row.fileExtension) && !row.isDirectory"
              link
              type="primary"
              size="small"
              @click="handlePreview(row)"
            >
              预览
            </el-button>
            <el-button link type="primary" size="small" @click="handleRename(row)">
              重命名
            </el-button>
            <el-button link type="primary" size="small" @click="handleMove(row)">
              移动
            </el-button>
            <el-button link type="primary" size="small" @click="handleShare(row)">
              分享
            </el-button>
            <el-popconfirm title="确定删除此文件？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Dialogs -->
    <UploadDialog
      v-model:visible="uploadVisible"
      :parent-id="currentParentId"
      @success="loadFiles"
    />

    <CreateFolderDialog
      v-model:visible="folderVisible"
      :parent-id="currentParentId"
      @success="loadFiles"
    />

    <RenameDialog
      v-model:visible="renameVisible"
      :file="renameTarget"
      @success="loadFiles"
    />

    <MoveDialog
      v-model:visible="moveVisible"
      :file="moveTarget"
      :current-parent-id="currentParentId"
      @success="loadFiles"
    />

    <ShareDialog
      v-model:visible="shareVisible"
      :file="shareTarget"
      @success="loadFiles"
    />

    <FilePreview
      v-model:visible="previewVisible"
      :file="previewTarget"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getFileList, deleteFile, fetchDownloadBlob, formatFileSize,
  formatDateTime, getFileIcon, canPreview
} from '@/utils/api'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import BreadcrumbNav from '@/components/BreadcrumbNav.vue'
import UploadDialog from '@/components/UploadDialog.vue'
import CreateFolderDialog from '@/components/CreateFolderDialog.vue'
import RenameDialog from '@/components/RenameDialog.vue'
import MoveDialog from '@/components/MoveDialog.vue'
import ShareDialog from '@/components/ShareDialog.vue'
import FilePreview from '@/components/FilePreview.vue'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const files = ref([])
const filteredList = ref([])
const searchQuery = ref('')
const currentParentId = ref(0)
const path = ref([{ id: 0, name: '全部文件' }])

const uploadVisible = ref(false)
const folderVisible = ref(false)
const renameVisible = ref(false)
const renameTarget = ref(null)
const moveVisible = ref(false)
const moveTarget = ref(null)
const shareVisible = ref(false)
const shareTarget = ref(null)
const previewVisible = ref(false)
const previewTarget = ref(null)

const filteredFiles = computed(() => {
  return searchQuery.value ? filteredList.value : files.value
})

function filterFiles(query) {
  if (!query) {
    filteredList.value = files.value
    return
  }
  const q = query.toLowerCase()
  filteredList.value = files.value.filter(f => {
    const name = (f.originalName || f.fileName || '').toLowerCase()
    return name.includes(q)
  })
}

async function loadFiles() {
  loading.value = true
  try {
    const res = await getFileList(userStore.userId, currentParentId.value)
    if (res.code === 200) {
      files.value = res.data || []
      filterFiles(searchQuery.value)
    } else {
      ElMessage.error(res.message || '获取文件列表失败')
    }
  } catch (e) {
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

function navigateTo(item) {
  const idx = path.value.findIndex(p => p.id === item.id)
  if (idx >= 0) {
    path.value = path.value.slice(0, idx + 1)
  } else {
    path.value.push(item)
  }
  currentParentId.value = item.id
  loadFiles()
}

function handleRowDoubleClick(row) {
  if (row.isDirectory === 1) {
    navigateTo({ id: row.id, name: row.originalName || row.fileName })
  }
}

function showUploadDialog() {
  uploadVisible.value = true
}

function showCreateFolderDialog() {
  folderVisible.value = true
}

function handleDownload(row) {
  const name = row.originalName || row.fileName
  fetchDownloadBlob(`/api/file/${row.id}/download`, name).catch(() => {
    ElMessage.error('下载失败')
  })
}

function handlePreview(row) {
  previewTarget.value = row
  previewVisible.value = true
}

function handleRename(row) {
  renameTarget.value = row
  renameVisible.value = true
}

function handleMove(row) {
  moveTarget.value = row
  moveVisible.value = true
}

function handleShare(row) {
  shareTarget.value = row
  shareVisible.value = true
}

async function handleDelete(row) {
  try {
    const res = await deleteFile(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadFiles()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function getFileIconComponent(row) {
  const icon = getFileIcon(row.fileExtension, row.isDirectory === 1)
  const icons = {
    'folder': 'FolderOpened',
    'document': 'Document',
    'picture': 'PictureRounded',
    'video-camera': 'VideoCamera',
    'headset': 'Headset',
    'folder-opened': 'FolderRemove',
    'tools': 'Tools',
    'disc': 'Disc',
    'code': 'Code',
    'question-filled': 'QuestionFilled'
  }
  return icons[icon] || 'QuestionFilled'
}

function getIconColor(row) {
  if (row.isDirectory === 1) return '#e6a23c'
  const ext = (row.fileExtension || '').toLowerCase()
  if (['jpg','jpeg','png','gif','bmp','svg'].includes(ext)) return '#67c23a'
  if (['mp4','avi','mov','mkv'].includes(ext)) return '#409eff'
  if (['mp3','wav','flac'].includes(ext)) return '#909399'
  if (['pdf'].includes(ext)) return '#f56c6c'
  if (['doc','docx'].includes(ext)) return '#2d6b9e'
  if (['xls','xlsx'].includes(ext)) return '#217346'
  if (['ppt','pptx'].includes(ext)) return '#d24726'
  if (['zip','rar','7z','tar','gz'].includes(ext)) return '#e6a23c'
  if (['js','ts','py','java','html','css','json','xml'].includes(ext)) return '#409eff'
  return '#909399'
}

onMounted(() => {
  loadFiles()
})

watch(currentParentId, () => {
  loadFiles()
})
</script>

<style scoped>
.my-files {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.file-card {
  flex: 1;
  overflow: auto;
}

.file-name {
  cursor: default;
  color: #303133;
}

.file-name.is-directory {
  color: #409eff;
  cursor: pointer;
}

.file-name.is-directory:hover {
  text-decoration: underline;
}

.text-muted {
  color: #c0c4cc;
}

:deep(.el-table .cell) {
  white-space: nowrap;
}
</style>
