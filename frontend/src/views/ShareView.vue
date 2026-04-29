<template>
  <div class="share-view">
    <div class="page-toolbar">
      <div class="toolbar-left">
        <h3 style="margin: 0">分享文件</h3>
        <el-tag v-if="shareInfo" type="info">分享码: {{ shareInfo.shareCode }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>返回
        </el-button>
      </div>
    </div>

    <!-- Share info -->
    <el-card v-if="shareInfo" shadow="never" class="mb-16">
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="分享类型">
          <el-tag v-if="shareInfo.shareType === 1" type="success" size="small">公开分享</el-tag>
          <el-tag v-else type="warning" size="small">私密分享</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下载次数">
          {{ shareInfo.downloadCount }}{{ shareInfo.maxDownloadCount > 0 ? '/' + shareInfo.maxDownloadCount : '' }}
        </el-descriptions-item>
        <el-descriptions-item label="过期时间">
          {{ shareInfo.expireTime ? formatDateTime(shareInfo.expireTime) : '永不过期' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="shareInfo.status === 1" type="success" size="small">有效</el-tag>
          <el-tag v-else type="info" size="small">已失效</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- File Preview Dialog -->
    <FilePreview
      v-model:visible="previewVisible"
      :file="previewTarget"
      :share-code="shareCode"
    />

    <!-- Breadcrumb -->
    <BreadcrumbNav
      :path="sharePath"
      @navigate="navigateTo"
    />

    <!-- File List -->
    <el-card shadow="never">
      <el-table
        :data="files"
        stripe
        v-loading="loading"
        style="width: 100%"
        @row-dblclick="handleRowDoubleClick"
        empty-text="暂无文件"
      >
        <el-table-column label="" width="50">
          <template #default="{ row }">
            <el-icon :size="22" :color="row.isDirectory ? '#e6a23c' : '#409eff'">
              <FolderOpened v-if="row.isDirectory" />
              <Document v-else />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="名称" min-width="250">
          <template #default="{ row }">
            <span
              class="file-link"
              :class="{ 'is-dir': row.isDirectory }"
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.isDirectory"
              link
              type="primary"
              size="small"
              @click="handleDownload(row)"
            >
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
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getShareInfo, getShareFileList, formatFileSize,
  formatDateTime, canPreview, fetchDownloadBlob
} from '@/utils/api'
import { Back } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import BreadcrumbNav from '@/components/BreadcrumbNav.vue'
import FilePreview from '@/components/FilePreview.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const shareInfo = ref(null)
const files = ref([])
const currentParentId = ref(0)
const sharePath = ref([{ id: 0, name: '分享根目录' }])
const previewVisible = ref(false)
const previewTarget = ref(null)

const shareCode = ref('')

async function loadShareInfo() {
  try {
    const res = await getShareInfo(shareCode.value)
    if (res.code === 200) {
      shareInfo.value = res.data
    } else {
      ElMessage.error(res.message || '分享已失效')
      return false
    }
    return true
  } catch (e) {
    ElMessage.error('获取分享信息失败')
    return false
  }
}

async function loadFiles() {
  loading.value = true
  try {
    const res = await getShareFileList(shareCode.value, currentParentId.value)
    if (res.code === 200) {
      files.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

function navigateTo(item) {
  const idx = sharePath.value.findIndex(p => p.id === item.id)
  if (idx >= 0) {
    sharePath.value = sharePath.value.slice(0, idx + 1)
  } else {
    sharePath.value.push(item)
  }
  currentParentId.value = item.id
  loadFiles()
}

function handleRowDoubleClick(row) {
  if (row.isDirectory === 1) {
    navigateTo({ id: row.id, name: row.originalName || row.fileName })
  }
}

function handleDownload(row) {
  const url = row.id
    ? `/api/share/${shareCode.value}/download?fileId=${row.id}`
    : `/api/share/${shareCode.value}/download`
  const name = row.originalName || row.fileName
  fetchDownloadBlob(url, name).catch(() => {
    ElMessage.error('下载失败')
  })
}

function handlePreview(row) {
  previewTarget.value = row
  previewVisible.value = true
}

function goBack() {
  router.back()
}

onMounted(async () => {
  shareCode.value = route.params.shareCode
  if (shareCode.value) {
    const ok = await loadShareInfo()
    if (ok) {
      loadFiles()
    }
  }
})
</script>

<style scoped>
.file-link {
  cursor: default;
}
.file-link.is-dir {
  color: #409eff;
  cursor: pointer;
}
.file-link.is-dir:hover {
  text-decoration: underline;
}
.text-muted {
  color: #c0c4cc;
}
</style>
