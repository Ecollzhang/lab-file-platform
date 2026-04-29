<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="file?.originalName || file?.fileName || '文件预览'"
    width="80%"
    fullscreen
    :close-on-click-modal="false"
    class="preview-dialog"
    @opened="onOpened"
    @closed="onClosed"
  >
    <div v-if="file" class="preview-container">
      <!-- Loading -->
      <el-skeleton v-if="blobLoading" :rows="5" animated style="width: 100%; padding: 40px;" />

      <!-- Image preview -->
      <div v-else-if="isImage && blobUrl" class="preview-image">
        <img :src="blobUrl" :alt="file.originalName" style="max-width: 100%; max-height: 80vh;" />
      </div>

      <!-- PDF preview -->
      <div v-else-if="isPdf && blobUrl" class="preview-pdf">
        <iframe
          :src="blobUrl"
          style="width: 100%; height: 80vh; border: none;"
        />
      </div>

      <!-- Video preview -->
      <div v-else-if="isVideo && blobUrl" class="preview-video">
        <video
          :src="blobUrl"
          controls
          style="max-width: 100%; max-height: 80vh;"
        >
          您的浏览器不支持视频播放
        </video>
      </div>

      <!-- Audio preview -->
      <div v-else-if="isAudio && blobUrl" class="preview-audio">
        <div class="audio-placeholder">
          <el-icon :size="64"><Headset /></el-icon>
          <p>{{ file.originalName }}</p>
        </div>
        <audio :src="blobUrl" controls style="width: 100%; margin-top: 16px;">
          您的浏览器不支持音频播放
        </audio>
      </div>

      <!-- Text preview -->
      <div v-else-if="isText" class="preview-text">
        <el-skeleton :loading="textLoading" animated>
          <pre class="text-content">{{ textContent }}</pre>
        </el-skeleton>
      </div>

      <!-- Unsupported format -->
      <div v-else class="preview-unsupported">
        <el-empty description="此格式不支持预览">
          <el-button type="primary" @click="downloadFile">下载文件</el-button>
        </el-empty>
      </div>
    </div>

    <div v-else class="preview-empty">
      <el-empty description="无文件信息" />
    </div>

    <template #footer>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="color: #909399; font-size: 13px;">
          {{ file?.originalName }} ·
          {{ file?.fileSize ? formatFileSize(file.fileSize) : '' }}
        </span>
        <div>
          <el-button type="primary" @click="downloadFile">
            <el-icon><Download /></el-icon>下载
          </el-button>
          <el-button @click="close">关闭</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { fetchPreviewBlob, fetchDownloadBlob, formatFileSize } from '@/utils/api'
import { Headset, Download } from '@element-plus/icons-vue'

const props = defineProps({
  visible: Boolean,
  file: { type: Object, default: null },
  shareCode: { type: String, default: '' }
})

const emit = defineEmits(['update:visible'])

const blobLoading = ref(false)
const blobUrl = ref('')
const textLoading = ref(false)
const textContent = ref('')

const ext = computed(() => {
  return (props.file?.fileExtension || '').toLowerCase()
})

const isImage = computed(() => {
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(ext.value)
})

const isPdf = computed(() => ext.value === 'pdf')

const isVideo = computed(() => {
  return ['mp4', 'avi', 'mov', 'wmv', 'mkv', 'flv'].includes(ext.value)
})

const isAudio = computed(() => {
  return ['mp3', 'wav', 'flac', 'aac', 'wma'].includes(ext.value)
})

const isText = computed(() => {
  return ['txt', 'md', 'log', 'json', 'xml', 'yaml', 'yml', 'ini', 'cfg', 'conf',
    'js', 'ts', 'py', 'java', 'cpp', 'c', 'h', 'html', 'css', 'sql', 'sh', 'bat', 'properties',
    'toml'].includes(ext.value)
})

async function onOpened() {
  if (!props.file) return

  if (isText.value) {
    await loadTextContent()
  } else if (isImage.value || isPdf.value || isVideo.value || isAudio.value) {
    await loadBlobContent()
  }
}

function onClosed() {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = ''
  }
  textContent.value = ''
}

async function loadBlobContent() {
  blobLoading.value = true
  try {
    const blob = await fetchPreviewBlob(props.file.id, props.shareCode)
    blobUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    blobUrl.value = ''
  } finally {
    blobLoading.value = false
  }
}

async function loadTextContent() {
  textLoading.value = true
  try {
    const blob = await fetchPreviewBlob(props.file.id, props.shareCode)
    textContent.value = await blob.text()
  } catch (e) {
    textContent.value = '无法加载文件内容'
  } finally {
    textLoading.value = false
  }
}

function downloadFile() {
  if (!props.file) return
  const name = props.file.originalName || props.file.fileName
  const downloadUrl = props.shareCode
    ? `/api/share/${props.shareCode}/download?fileId=${props.file.id}`
    : `/api/file/${props.file.id}/download`
  fetchDownloadBlob(downloadUrl, name).catch(() => {})
}

function close() {
  emit('update:visible', false)
}
</script>

<style scoped>
.preview-container {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image,
.preview-video {
  text-align: center;
}

.preview-pdf {
  width: 100%;
}

.preview-audio {
  text-align: center;
  width: 100%;
  max-width: 500px;
}

.audio-placeholder {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.preview-text {
  width: 100%;
  max-height: 80vh;
  overflow: auto;
}

.text-content {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
}

.preview-empty {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-unsupported {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.preview-dialog .el-dialog__body) {
  padding: 16px 20px;
}
</style>
